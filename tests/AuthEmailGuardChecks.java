package crsm;

import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Deterministic clock checks: no network, credentials, messages or sleeps. */
public final class AuthEmailGuardChecks {
    private static void check(boolean value){if(!value)throw new AssertionError("Email-request guard check failed.");}
    private static void blocked(AuthEmailRequestGuard guard){try{guard.begin();throw new AssertionError("Expected email request to be blocked.");}catch(IllegalStateException expected){}}
    private static AuthRateLimitException limit(String retry){
        return new AuthRateLimitException(new HttpResponse<Void>(){
            public int statusCode(){return 429;}public HttpRequest request(){return null;}public Optional<HttpResponse<Void>> previousResponse(){return Optional.empty();}
            public HttpHeaders headers(){return HttpHeaders.of(retry.isEmpty()?Map.of():Map.of("Retry-After",List.of(retry)),(a,b)->true);}public Void body(){return null;}
            public Optional<javax.net.ssl.SSLSession> sslSession(){return Optional.empty();}public URI uri(){return URI.create("http://127.0.0.1");}public HttpClient.Version version(){return HttpClient.Version.HTTP_1_1;}
        });
    }
    public static void main(String[] args)throws Exception{
        AtomicLong clock=new AtomicLong(1000000);AuthEmailRequestGuard guard=new AuthEmailRequestGuard(clock::get);
        guard.begin();blocked(guard);guard.complete(limit(""),false);check(guard.remainingSeconds()==60);blocked(guard);
        clock.addAndGet(60001);guard.begin();guard.complete(limit(""),false);check(guard.remainingSeconds()==120);
        clock.addAndGet(120001);guard.begin();guard.complete(limit("900"),false);check(guard.remainingSeconds()==900);
        clock.addAndGet(900001);guard.begin();guard.complete(null,true);check(guard.remainingSeconds()==60);
        clock.addAndGet(60001);guard.begin();guard.complete(limit(""),false);check(guard.remainingSeconds()==60); // accepted request reset the fallback sequence
        Path directory=Files.createTempDirectory("crsm-email-guard-");Path state=directory.resolve("pause.properties");
        try{
            AuthEmailRequestGuard first=new AuthEmailRequestGuard(clock::get,state);first.begin();first.complete(limit("300"),false);
            AuthEmailRequestGuard reopened=new AuthEmailRequestGuard(clock::get,state);check(reopened.remainingSeconds()==300);blocked(reopened);
            Properties properties=new Properties();try(var input=Files.newInputStream(state)){properties.load(input);}check(properties.stringPropertyNames().equals(Set.of("retry_not_before_ms","limited_responses")));
            clock.addAndGet(300001);reopened.begin();reopened.complete(null,false);check(reopened.remainingSeconds()==60);
        }finally{Files.deleteIfExists(state);Files.deleteIfExists(directory);}
        System.out.println("Email guard checks passed: single in-flight request, shared cooldown, Retry-After, progressive fallback, accepted-request reset and restart persistence. No HTTP requests sent.");
    }
}
