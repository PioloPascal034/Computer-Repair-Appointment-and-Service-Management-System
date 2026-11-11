package crsm;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SupabaseAuthService {
    private final SupabaseClient client = new SupabaseClient();
    private final AuthEmailRequestGuard emailGuard;
    public SupabaseAuthService(){this(AuthEmailRequestGuard.forProject(AppConfig.get("SUPABASE_URL")));}
    SupabaseAuthService(AuthEmailRequestGuard guard){this.emailGuard=guard;}
    public static long emailRetrySeconds(){return AuthEmailRequestGuard.forProject(AppConfig.get("SUPABASE_URL")).remainingSeconds();}
    private HttpResponse<String> sendEmailRequest(String path,Map<String,Object> body) throws IOException,InterruptedException {
        emailGuard.begin();AuthRateLimitException limited=null;boolean accepted=false;
        try{
            HttpResponse<String> response=client.send("POST",path,null,body,null);
            if(isEmailRateLimit(response)){limited=new AuthRateLimitException(response);throw limited;}
            accepted=response.statusCode()>=200&&response.statusCode()<300;return response;
        }finally{emailGuard.complete(limited,accepted);}
    }

    public UserSession signIn(String email, String password) throws IOException, InterruptedException {
        Map<String,Object> body = Map.of("email", normalizeEmail(email), "password", password);
        HttpResponse<String> response = client.send("POST", "/auth/v1/token?grant_type=password", null, body, null);
        SupabaseClient.requireSuccess(response);
        Map<?,?> payload = (Map<?,?>)JsonCodec.parse(response.body());
        String token = String.valueOf(payload.get("access_token"));
        Map<?,?> user = (Map<?,?>)payload.get("user");
        String id = String.valueOf(user.get("id"));
        return loadProfile(id, token, String.valueOf(user.get("email")));
    }

    public static String normalizeEmail(String email) {
        String value=email==null?"":email.strip();
        if(value.length()>254 || !value.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+"))
            throw new IllegalArgumentException("Enter a valid email address without quotes or spaces, for example yourname@yourdomain.com.");
        return value;
    }

    public boolean signUp(String fullName, String username, String email, String password) throws IOException, InterruptedException {
        String address=normalizeEmail(email);
        if(fullName==null||fullName.isBlank()||fullName.strip().length()>100)throw new IllegalArgumentException("Enter your full name (up to 100 characters).");
        if(username==null||!username.strip().matches("[A-Za-z0-9_]{3,50}"))throw new IllegalArgumentException("Use 3-50 letters, numbers or underscores for your username.");
        if(password==null||password.length()<8)throw new IllegalArgumentException("Use at least 8 characters for your password.");
        Map<String,Object> metadata = new LinkedHashMap<>();metadata.put("full_name",fullName.strip());metadata.put("username",username.strip());
        HttpResponse<String> response = sendEmailRequest("/auth/v1/signup",Map.of("email",address,"password",password,"data",metadata));
        if(response.statusCode()<200||response.statusCode()>=300){if(isEmailRateLimit(response))throw new AuthRateLimitException(response);throw new IllegalStateException(signupError(response));}
        Object parsed=JsonCodec.parse(response.body());
        if(!(parsed instanceof Map<?,?> payload)||(!payload.containsKey("user")&&!payload.containsKey("id")))throw new IllegalStateException("The authentication provider returned an incomplete signup response. Check your inbox before retrying.");
        if(payload.get("user") instanceof Map<?,?> user && user.get("identities") instanceof List<?> identities && identities.isEmpty())
            throw new IllegalStateException("This address may already have an account. Try signing in or resetting your password.");
        return payload.get("access_token") != null && !String.valueOf(payload.get("access_token")).isBlank();
    }

    private static boolean isEmailRateLimit(HttpResponse<String> response) {
        if(response.statusCode()==429)return true;
        try{if(JsonCodec.parse(response.body()) instanceof Map<?,?> map){Object code=map.get("error_code");if(code==null)code=map.get("code");return "over_email_send_rate_limit".equals(code)||"over_request_rate_limit".equals(code);}}catch(RuntimeException ignored){}
        return false;
    }

    private static String signupError(HttpResponse<String> response) {
        String code="";
        try{if(JsonCodec.parse(response.body()) instanceof Map<?,?> map){Object value=map.get("error_code");if(value==null)value=map.get("code");if(value!=null)code=value.toString();}}catch(RuntimeException ignored){}
        return switch(code){
            case "email_address_invalid" -> "The authentication provider rejected this email address. Use an email address you own, without quotes or spaces. Demo database records do not create sign-in accounts.";
            case "email_address_not_authorized" -> "Email delivery is restricted by the project's authentication settings. An administrator must configure email delivery for this address.";
            case "email_exists","user_already_exists" -> "An account already uses this email address. Sign in or reset your password.";
            case "over_email_send_rate_limit","over_request_rate_limit" -> "Too many signup or email requests. Wait before trying again; check your inbox first.";
            case "weak_password" -> "The authentication provider requires a stronger password. Use at least 8 characters and follow the project's password requirements.";
            case "signup_disabled" -> "Account creation is disabled in the authentication settings. Contact an administrator.";
            default -> "Account creation failed at the authentication provider (HTTP "+response.statusCode()+"). Check the project Auth logs with account details and credentials redacted.";
        };
    }

    public UserSession loadProfile(String userId, String token, String email) throws IOException, InterruptedException {
        String path = "/rest/v1/crsm_profiles?id=eq." + SupabaseClient.queryValue(userId) + "&select=id,email,full_name,username,role,avatar_path,is_active";
        HttpResponse<String> response = client.send("GET", path, token, null, null); SupabaseClient.requireSuccess(response);
        Object parsed = JsonCodec.parse(response.body());
        if (!(parsed instanceof List<?> profiles) || profiles.isEmpty()) throw new IllegalStateException("Your account profile is not initialized. Contact the system administrator.");
        Map<?,?> profile = (Map<?,?>)profiles.get(0);
        if (!Boolean.TRUE.equals(profile.get("is_active"))) throw new IllegalStateException("This account has been deactivated. Contact an administrator.");
        HttpResponse<String> activity = client.send("POST", "/rest/v1/rpc/record_crsm_login_activity", token, Map.of(), null);
        SupabaseClient.requireSuccess(activity);
        return new UserSession(String.valueOf(profile.get("id")), email,
                str(profile.get("full_name")), str(profile.get("username")), str(profile.get("role")), token, str(profile.get("avatar_path")));
    }

    public List<Map<String,Object>> listProfiles(UserSession session) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send("GET", "/rest/v1/crsm_profiles?select=id,email,full_name,username,role,created_at,avatar_path,is_active,last_login_at&order=created_at.desc", session.accessToken(), null, null);
        SupabaseClient.requireSuccess(response);
        Object parsed = JsonCodec.parse(response.body());
        if (!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected profile response.");
        return rowsAsMaps(list);
    }

    public void promote(UserSession session, String targetUserId, String role) throws IOException, InterruptedException {
        if (!role.equals("STAFF") && !role.equals("ADMIN")) throw new IllegalArgumentException("Only Staff or Admin promotion is allowed.");
        HttpResponse<String> response = client.send("POST", "/rest/v1/rpc/promote_crsm_user", session.accessToken(), Map.of("target_user", targetUserId, "target_role", role), null);
        SupabaseClient.requireSuccess(response);
    }

    public void setAccountActive(UserSession session,String targetUserId,boolean active) throws IOException,InterruptedException {
        HttpResponse<String> response=client.send("POST","/rest/v1/rpc/set_crsm_account_active",session.accessToken(),Map.of("target_user",targetUserId,"active",active),null);
        SupabaseClient.requireSuccess(response);
    }

    public List<Map<String,Object>> loginActivity(UserSession session,String targetUserId) throws IOException,InterruptedException {
        String path="/rest/v1/crsm_login_activity?user_id=eq."+SupabaseClient.queryValue(targetUserId)+"&select=occurred_at,client_label&order=occurred_at.desc&limit=100";
        HttpResponse<String> response=client.send("GET",path,session.accessToken(),null,null); SupabaseClient.requireSuccess(response);
        Object parsed=JsonCodec.parse(response.body());
        if(!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected activity response.");
        return rowsAsMaps(list);
    }

    public void linkTechnician(UserSession session,int technicianId,String targetUserId) throws IOException,InterruptedException {
        HttpResponse<String> response=client.send("POST","/rest/v1/rpc/link_crsm_technician_account",session.accessToken(),Map.of("target_technician",technicianId,"target_user",targetUserId),null);
        SupabaseClient.requireSuccess(response);
    }

    public void linkCustomer(UserSession session,int customerId,String targetUserId) throws IOException,InterruptedException {
        HttpResponse<String> response=client.send("POST","/rest/v1/rpc/link_crsm_customer_record",session.accessToken(),Map.of("target_customer",customerId,"target_user",targetUserId),null);
        SupabaseClient.requireSuccess(response);
    }

    public void updateAvatar(UserSession session, String path) throws IOException, InterruptedException {
        Map<String,Object> body = Map.of("avatar_path", path);
        HttpResponse<String> response = client.send("PATCH", "/rest/v1/crsm_profiles?id=eq." + SupabaseClient.queryValue(session.userId()), session.accessToken(), body, Map.of("Prefer", "return=minimal"));
        SupabaseClient.requireSuccess(response);
    }

    public void updateProfile(UserSession session, String fullName, String username) throws IOException, InterruptedException {
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Enter your full name.");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Enter a username.");
        Map<String,Object> body = Map.of("full_name", fullName.trim(), "username", username.trim());
        HttpResponse<String> response = client.send("PATCH",
                "/rest/v1/crsm_profiles?id=eq." + SupabaseClient.queryValue(session.userId()),
                session.accessToken(), body, Map.of("Prefer", "return=minimal"));
        SupabaseClient.requireSuccess(response);
    }

    public Map<String,Object> accountDetails(UserSession session) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send("POST", "/rest/v1/rpc/get_crsm_my_account", session.accessToken(), Map.of(), null);
        SupabaseClient.requireSuccess(response);
        Object value = JsonCodec.parse(response.body());
        if (!(value instanceof Map<?,?> map)) throw new IllegalStateException("Unexpected account response.");
        Map<String,Object> result = new LinkedHashMap<>();
        map.forEach((k,v)->result.put(k.toString(),v));
        return result;
    }

    public void saveAccount(UserSession session, String name, String username, String phone, String address) throws IOException, InterruptedException {
        if (name == null || name.isBlank() || name.trim().length()>100) throw new IllegalArgumentException("Enter a full name of up to 100 characters.");
        if (username == null || username.isBlank() || username.trim().length()>50) throw new IllegalArgumentException("Enter a username of up to 50 characters.");
        if (phone == null || (!phone.isBlank() && (!phone.matches("[+0-9() .-]{8,20}") || phone.replaceAll("[^0-9]", "").length()<8))) throw new IllegalArgumentException("Enter a valid contact number (8?20 characters).");
        if ("CUSTOMER".equals(session.role()) && phone.isBlank()) throw new IllegalArgumentException("Contact number is required.");
        if (address == null || address.length()>255) throw new IllegalArgumentException("Address must be at most 255 characters.");
        HttpResponse<String> response = client.send("POST", "/rest/v1/rpc/update_crsm_my_account", session.accessToken(),
                Map.of("p_name",name.trim(),"p_username",username.trim(),"p_phone",phone.trim(),"p_address",address.trim()), null);
        SupabaseClient.requireSuccess(response);
    }

    public void changePassword(UserSession session, String password, String confirmation) throws IOException, InterruptedException {
        if (password == null || password.length()<8) throw new IllegalArgumentException("Use at least 8 characters for your new password.");
        if (!password.equals(confirmation)) throw new IllegalArgumentException("Passwords do not match.");
        HttpResponse<String> response = client.send("PUT", "/auth/v1/user", session.accessToken(), Map.of("password",password), null);
        SupabaseClient.requireSuccess(response);
    }

    public Map<String,String> settings(UserSession session) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send("GET",
                "/rest/v1/crsm_settings?select=setting_key,setting_value&order=setting_key",
                session.accessToken(), null, null);
        SupabaseClient.requireSuccess(response);
        Object parsed = JsonCodec.parse(response.body());
        if (!(parsed instanceof List<?> rows)) throw new IllegalStateException("Unexpected settings response.");
        Map<String,String> result = new LinkedHashMap<>();
        for (Object item : rows) {
            if (!(item instanceof Map<?,?> row)) continue;
            Object key = row.get("setting_key");
            Object value = row.get("setting_value");
            if (key != null && value != null) result.put(key.toString(), value.toString());
        }
        return result;
    }

    public void saveSettings(UserSession session, Map<String,String> settings) throws IOException, InterruptedException {
        if (settings.isEmpty()) return;
        List<Map<String,Object>> values = new java.util.ArrayList<>();
        settings.forEach((key, value) -> values.add(Map.of("setting_key", key, "setting_value", value)));
        HttpResponse<String> response = client.send("POST",
                "/rest/v1/crsm_settings?on_conflict=setting_key", session.accessToken(), values,
                Map.of("Prefer", "resolution=merge-duplicates,return=minimal"));
        SupabaseClient.requireSuccess(response);
    }

    public void updateBusinessLogo(UserSession session, String path) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send("POST", "/rest/v1/rpc/set_crsm_business_logo", session.accessToken(), Map.of("logo_path", path), null);
        SupabaseClient.requireSuccess(response);
    }

    public void sendPasswordReset(String email) throws IOException, InterruptedException {
        HttpResponse<String> response = sendEmailRequest("/auth/v1/recover",Map.of("email",normalizeEmail(email)));
        if(isEmailRateLimit(response))throw new AuthRateLimitException(response);
        SupabaseClient.requireSuccess(response);
    }

    public void signOut(UserSession session) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send("POST", "/auth/v1/logout", session.accessToken(), null, null);
        SupabaseClient.requireSuccess(response);
    }

    private static String str(Object value) { return value == null ? "" : value.toString(); }

    private static List<Map<String,Object>> rowsAsMaps(List<?> rows) {
        List<Map<String,Object>> result=new java.util.ArrayList<>();
        for(Object item:rows) {
            if(!(item instanceof Map<?,?> source)) continue;
            Map<String,Object> row=new LinkedHashMap<>();
            source.forEach((key,value)->row.put(String.valueOf(key),value));
            result.add(row);
        }
        return result;
    }
}
