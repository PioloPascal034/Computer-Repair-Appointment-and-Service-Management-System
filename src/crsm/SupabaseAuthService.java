package crsm;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SupabaseAuthService {
    private final SupabaseClient client = new SupabaseClient();

    public UserSession signIn(String email, String password) throws IOException, InterruptedException {
        Map<String,Object> body = Map.of("email", email, "password", password);
        HttpResponse<String> response = client.send("POST", "/auth/v1/token?grant_type=password", null, body, null);
        SupabaseClient.requireSuccess(response);
        Map<?,?> payload = (Map<?,?>)JsonCodec.parse(response.body());
        String token = String.valueOf(payload.get("access_token"));
        Map<?,?> user = (Map<?,?>)payload.get("user");
        String id = String.valueOf(user.get("id"));
        return loadProfile(id, token, String.valueOf(user.get("email")));
    }

    public boolean signUp(String fullName, String username, String email, String password) throws IOException, InterruptedException {
        Map<String,Object> metadata = new LinkedHashMap<>(); metadata.put("full_name", fullName); metadata.put("username", username);
        Map<String,Object> body = Map.of("email", email, "password", password, "data", metadata);
        HttpResponse<String> response = client.send("POST", "/auth/v1/signup", null, body, null);
        SupabaseClient.requireSuccess(response);
        Map<?,?> payload = (Map<?,?>)JsonCodec.parse(response.body());
        return payload.get("access_token") != null && !String.valueOf(payload.get("access_token")).isBlank();
    }

    public UserSession loadProfile(String userId, String token, String email) throws IOException, InterruptedException {
        String path = "/rest/v1/crsm_profiles?id=eq." + SupabaseClient.queryValue(userId) + "&select=id,email,full_name,username,role,avatar_path";
        HttpResponse<String> response = client.send("GET", path, token, null, null); SupabaseClient.requireSuccess(response);
        Object parsed = JsonCodec.parse(response.body());
        if (!(parsed instanceof List<?> profiles) || profiles.isEmpty()) throw new IllegalStateException("Your account profile is not initialized. Contact the system administrator.");
        Map<?,?> profile = (Map<?,?>)profiles.get(0);
        return new UserSession(String.valueOf(profile.get("id")), email,
                str(profile.get("full_name")), str(profile.get("username")), str(profile.get("role")), token, str(profile.get("avatar_path")));
    }

    public List<Map<String,Object>> listProfiles(UserSession session) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send("GET", "/rest/v1/crsm_profiles?select=id,email,full_name,username,role,created_at,avatar_path&order=created_at.desc", session.accessToken(), null, null);
        SupabaseClient.requireSuccess(response);
        Object parsed = JsonCodec.parse(response.body());
        if (!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected profile response.");
        return list.stream().map(v -> (Map<String,Object>)v).toList();
    }

    public void promote(UserSession session, String targetUserId, String role) throws IOException, InterruptedException {
        if (!role.equals("STAFF") && !role.equals("ADMIN")) throw new IllegalArgumentException("Only Staff or Admin promotion is allowed.");
        HttpResponse<String> response = client.send("POST", "/rest/v1/rpc/promote_crsm_user", session.accessToken(), Map.of("target_user", targetUserId, "target_role", role), null);
        SupabaseClient.requireSuccess(response);
    }

    public void updateAvatar(UserSession session, String path) throws IOException, InterruptedException {
        Map<String,Object> body = Map.of("avatar_path", path);
        HttpResponse<String> response = client.send("PATCH", "/rest/v1/crsm_profiles?id=eq." + SupabaseClient.queryValue(session.userId()), session.accessToken(), body, Map.of("Prefer", "return=minimal"));
        SupabaseClient.requireSuccess(response);
    }

    public void updateBusinessLogo(UserSession session, String path) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send("POST", "/rest/v1/rpc/set_crsm_business_logo", session.accessToken(), Map.of("logo_path", path), null);
        SupabaseClient.requireSuccess(response);
    }

    public void sendPasswordReset(String email) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send("POST", "/auth/v1/recover", null, Map.of("email", email), null);
        SupabaseClient.requireSuccess(response);
    }

    public void signOut(UserSession session) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send("POST", "/auth/v1/logout", session.accessToken(), null, null);
        SupabaseClient.requireSuccess(response);
    }

    private static String str(Object value) { return value == null ? "" : value.toString(); }
}
