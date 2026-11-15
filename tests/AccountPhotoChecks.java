package crsm;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Local HTTP contract checks; no credentials or production writes. */
public class AccountPhotoChecks {
    static String method,path,body,auth,userAgent,referrer,clientInfo;
    static int requestCount;
    static int code=200;
    static String retryAfter="";
    static String response="{}";
    static void check(boolean condition){if(!condition)throw new AssertionError("Local HTTP contract check failed.");}
    static void rejected(RunnableWithException action) throws Exception {try{action.run();throw new AssertionError("Expected rejection");}catch(IllegalArgumentException|SecurityException expected){}}
    interface RunnableWithException{void run()throws Exception;}
    public static void main(String[] args)throws Exception{
        if(!"http://127.0.0.1:18764".equals(System.getenv("SUPABASE_URL"))||!"local-contract-test".equals(System.getenv("SUPABASE_ANON_KEY")))
            throw new IllegalStateException("Tests require the exact localhost environment from test-workflows.ps1. Live configuration is refused before any HTTP request.");
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",18764),0);
        server.createContext("/",e->{requestCount++;userAgent=e.getRequestHeaders().getFirst("User-Agent");referrer=e.getRequestHeaders().getFirst("Referer");clientInfo=e.getRequestHeaders().getFirst("X-Client-Info");method=e.getRequestMethod();path=e.getRequestURI().toString();auth=e.getRequestHeaders().getFirst("Authorization");body=new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);byte[] bytes=(path.startsWith("/storage/")?"stored-image-bytes":response).getBytes(StandardCharsets.UTF_8);if(!retryAfter.isEmpty())e.getResponseHeaders().set("Retry-After",retryAfter);e.sendResponseHeaders(code,bytes.length);e.getResponseBody().write(bytes);e.close();});server.start();
        try{
            UserSession customer=new UserSession("self","me@example.test","Old","old","CUSTOMER","test-jwt","");
            java.util.concurrent.atomic.AtomicLong clock=new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
            SupabaseAuthService service=new SupabaseAuthService(new AuthEmailRequestGuard(clock::get));
            response="{\"full_name\":\"Saved Name\",\"username\":\"saved\",\"contact_number\":\"09123456789\"}";
            check(service.accountDetails(customer).get("full_name").equals("Saved Name"));check(path.endsWith("get_crsm_my_account")&&auth.equals("Bearer test-jwt"));
            response="null";service.saveAccount(customer," New Name "," new ","09123456789","Address");
            check(method.equals("POST")&&path.endsWith("update_crsm_my_account"));
            Map<?,?> payload=(Map<?,?>)JsonCodec.parse(body);check(payload.size()==4&&payload.get("p_name").equals("New Name")&&!payload.containsKey("role")&&!payload.containsKey("auth_user_id"));
            rejected(()->service.saveAccount(customer,"","u","09123456789",""));
            rejected(()->service.saveAccount(customer,"Name","u","bad",""));
            rejected(()->service.saveAccount(customer,"Name","u","",""));
            rejected(()->service.changePassword(customer,"short","short"));
            rejected(()->service.changePassword(customer,"longpassword","different"));
            service.changePassword(customer,"longpassword","longpassword");check(method.equals("PUT")&&path.equals("/auth/v1/user")&&body.contains("longpassword"));
            code=403;try{service.saveAccount(customer,"Name","u","09123456789","");throw new AssertionError("HTTP denial ignored");}catch(IllegalStateException expected){}code=200;
            CustomerWorkflowService workflow=new CustomerWorkflowService();response="[{\"object_path\":\"owner/7/photo.png\"}]";
            check(workflow.repairPhotos(customer,7).size()==1);check(path.contains("repair_request_id=eq.7")&&auth.equals("Bearer test-jwt"));
            response="[{\"object_path\":\"owner/7/photo.png\"}]";
            check(new String(workflow.repairPhoto(customer,7,"owner/7/photo.png"),StandardCharsets.UTF_8).equals("stored-image-bytes"));check(path.equals("/storage/v1/object/crsm-repair-photos/owner/7/photo.png")&&auth.equals("Bearer test-jwt"));
            for(String role:List.of("STAFF","ADMIN")){service.saveAccount(new UserSession("self","me@example.test","Name","u",role,"test-jwt",""),"Name","u","09123456789","");check(path.endsWith("update_crsm_my_account"));}
            response="[]";rejected(()->workflow.repairPhoto(customer,7,"other/8/private.png"));check(path.startsWith("/rest/v1/repair_photos"));
            check(SupabaseAuthService.normalizeEmail(" customer@gmail.com ").equals("customer@gmail.com"));
            rejected(()->SupabaseAuthService.normalizeEmail("\"customer@gmail.com\""));
            rejected(()->SupabaseAuthService.normalizeEmail("customer @gmail.com"));
            rejected(()->service.signUp("Name","ab","person@example.com","longpassword"));
            rejected(()->service.signUp("Name","person","person@example.com","short"));
            response="{\"id\":\"new-user\",\"email\":\"person@example.com\",\"identities\":[{\"provider\":\"email\"}]}";
            check(!service.signUp(" Name "," person "," person@example.com ","longpassword"));
            check(path.equals("/auth/v1/signup")&&method.equals("POST"));
            Map<?,?> signup=(Map<?,?>)JsonCodec.parse(body);check(signup.get("email").equals("person@example.com")&&!signup.containsKey("role")&&((Map<?,?>)signup.get("data")).get("full_name").equals("Name"));
            clock.addAndGet(3600001);response="{\"access_token\":\"token\",\"user\":{\"id\":\"new-user\",\"identities\":[{}]}}";check(service.signUp("Name","person","person@example.com","longpassword"));
            clock.addAndGet(3600001);code=422;response="{\"code\":\"email_address_invalid\",\"msg\":\"Email address is invalid\"}";
            try{service.signUp("Name","person","customer@gmail.com","longpassword");throw new AssertionError("Expected provider rejection");}catch(IllegalStateException expected){check(expected.getMessage().contains("provider rejected"));}
            clock.addAndGet(3600001);int beforeRate=requestCount;code=429;response="{\"code\":\"over_email_send_rate_limit\"}";try{service.signUp("Name","person","person@example.com","longpassword");throw new AssertionError("Expected rate limit");}catch(IllegalStateException expected){check(expected instanceof AuthRateLimitException);check(expected.getMessage().toLowerCase(java.util.Locale.ROOT).contains("check your inbox"));check(((AuthRateLimitException)expected).retryAfterSeconds().isEmpty());check(((AuthRateLimitException)expected).appPauseSeconds()==60);}
            check(requestCount==beforeRate+1);check(userAgent!=null&&userAgent.startsWith("Java-http-client/"));check(referrer==null&&"crsm-java-desktop/1.0".equals(clientInfo));
            int beforeBlocked=requestCount;try{service.signUp("Name","person","person@example.com","longpassword");throw new AssertionError("Expected local pause");}catch(IllegalStateException expected){check(!(expected instanceof AuthRateLimitException)&&expected.getMessage().contains("paused"));}
            try{service.sendPasswordReset("person@example.com");throw new AssertionError("Expected shared reset pause");}catch(IllegalStateException expected){check(expected.getMessage().contains("paused"));}check(requestCount==beforeBlocked);
            clock.addAndGet(3600001);retryAfter="120";try{service.signUp("Name","person","person@example.com","longpassword");throw new AssertionError("Expected rate limit");}catch(AuthRateLimitException expected){check(expected.retryAfterSeconds().orElseThrow()==120&&expected.appPauseSeconds()==120);}
            clock.addAndGet(3600001);retryAfter="invalid-header";try{service.sendPasswordReset("person@example.com");throw new AssertionError("Expected reset rate limit");}catch(AuthRateLimitException expected){check(expected.retryAfterSeconds().isEmpty()&&expected.appPauseSeconds()==60);}
            clock.addAndGet(3600001);retryAfter=java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).plusSeconds(180).format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME);
            try{service.sendPasswordReset("person@example.com");throw new AssertionError("Expected reset rate limit");}catch(AuthRateLimitException expected){check(expected.retryAfterSeconds().orElseThrow()>=175&&expected.retryAfterSeconds().orElseThrow()<=180);}
            retryAfter="";code=200;
            response="invalid";try{workflow.repairPhotos(customer,7);throw new AssertionError("Malformed metadata accepted");}catch(RuntimeException expected){}
            System.out.println("Account/photo checks passed: load, save payload, validation, password, authenticated metadata, denied photo error propagation signup validation/provider responses and email retry metadata.");
        }finally{server.stop(0);}
    }
}
