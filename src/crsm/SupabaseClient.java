package crsm;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

final class SupabaseClient {
    private final String baseUrl;
    private final String anonKey;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    SupabaseClient() {
        String url = AppConfig.get("SUPABASE_URL"), key = AppConfig.get("SUPABASE_ANON_KEY");
        if (url == null || url.isBlank() || key == null || key.isBlank()) throw new IllegalStateException("Set SUPABASE_URL and SUPABASE_ANON_KEY to use account sign-in and uploads.");
        baseUrl = url.replaceAll("/+$", ""); anonKey = key;
    }

    HttpResponse<String> send(String method, String path, String token, Object body, Map<String,String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(30))
                .header("apikey", anonKey).header("Accept", "application/json");
        if (token != null && !token.isBlank()) builder.header("Authorization", "Bearer " + token);
        if (body != null) builder.header("Content-Type", "application/json");
        if (headers != null) headers.forEach(builder::header);
        String payload = body == null ? "" : JsonCodec.stringify(body);
        builder.method(method, body == null && (method.equals("GET") || method.equals("DELETE")) ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(payload));
        try { return http.send(builder.build(), HttpResponse.BodyHandlers.ofString()); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw e; }
    }

    static String queryValue(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    static String errorMessage(HttpResponse<String> response) {
        try {
            Object parsed = JsonCodec.parse(response.body());
            if (parsed instanceof Map<?,?> map) {
                Object message = map.get("msg"); if (message == null) message = map.get("message"); if (message == null) message = map.get("error_description"); if (message == null) message = map.get("error");
                if (message != null) return message.toString();
            }
        } catch (RuntimeException ignored) { }
        return "Supabase request failed with HTTP " + response.statusCode() + ".";
    }
    static void requireSuccess(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException(errorMessage(response));
    }
}
