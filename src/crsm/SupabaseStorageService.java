package crsm;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class SupabaseStorageService {
    public String uploadAvatar(UserSession session, Path file) throws IOException, InterruptedException {
        validateImage(file);
        return upload(session, "crsm-profile-images", session.userId() + "/" + UUID.randomUUID() + extension(file), file);
    }

    public String uploadLogo(UserSession session, Path file) throws IOException, InterruptedException {
        validateImage(file);
        return upload(session, "crsm-brand-assets", "branding/" + UUID.randomUUID() + extension(file), file);
    }

    public String uploadRepairPhoto(UserSession session, int repairId, Path file) throws IOException, InterruptedException {
        validateImage(file);
        return upload(session,"crsm-repair-photos",session.userId()+"/"+repairId+"/"+UUID.randomUUID()+extension(file),file);
    }

    private String upload(UserSession session, String bucket, String path, Path file) throws IOException, InterruptedException {
        String encodedPath = encodePath(path);
        byte[] bytes = Files.readAllBytes(file);
        String mime = file.getFileName().toString().toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
        HttpRequest request = HttpRequest.newBuilder(java.net.URI.create(AppConfig.get("SUPABASE_URL").replaceAll("/+$", "") + "/storage/v1/object/" + bucket + "/" + encodedPath))
                .timeout(Duration.ofSeconds(60))
                .header("apikey", AppConfig.get("SUPABASE_ANON_KEY"))
                .header("Authorization", "Bearer " + session.accessToken())
                .header("Content-Type", mime).header("x-upsert", "true")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
        HttpResponse<String> response = java.net.http.HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String detail = response.body();
            try {
                Object parsed = JsonCodec.parse(detail);
                if (parsed instanceof java.util.Map<?,?> map) {
                    Object value = map.get("message"); if (value == null) value = map.get("error"); if (value == null) value = map.get("msg");
                    if (value != null) detail = value.toString();
                }
            } catch (RuntimeException ignored) { }
            throw new IllegalStateException("Supabase Storage upload failed (HTTP " + response.statusCode() + "): " + detail);
        }
        return path;
    }

    public byte[] download(UserSession session, String bucket, String path) throws IOException, InterruptedException {
        String url = AppConfig.get("SUPABASE_URL").replaceAll("/+$", "") + "/storage/v1/object/" + bucket + "/" + encodePath(path);
        HttpRequest request = HttpRequest.newBuilder(java.net.URI.create(url)).timeout(Duration.ofSeconds(60)).header("apikey", AppConfig.get("SUPABASE_ANON_KEY"))
                .header("Authorization", "Bearer " + session.accessToken()).GET().build();
        HttpResponse<byte[]> response = java.net.http.HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("Image download failed with HTTP " + response.statusCode() + ".");
        return response.body();
    }

    static void validateImage(Path file) throws IOException {
        if (!Files.isRegularFile(file)) throw new IllegalArgumentException("Choose an existing image file.");
        String name = file.getFileName().toString().toLowerCase();
        if (!(name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))) throw new IllegalArgumentException("Choose a PNG or JPEG image.");
        if (Files.size(file) > 2L * 1024 * 1024) throw new IllegalArgumentException("Images must be 2 MB or smaller.");
        String extension = name.substring(name.lastIndexOf('.') + 1);
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
            if (input == null) throw unreadableImage();
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw unreadableImage();
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName();
                boolean png = extension.equals("png") && format.equalsIgnoreCase("png");
                boolean jpeg = (extension.equals("jpg") || extension.equals("jpeg")) && format.equalsIgnoreCase("jpeg");
                if (!png && !jpeg) throw unreadableImage();
                reader.getWidth(0);
                reader.getHeight(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("The file could not be read as a PNG or JPEG image. Open it and save or export it as PNG or JPG, then try again.", exception);
        }
    }
    private static IllegalArgumentException unreadableImage() {
        return new IllegalArgumentException("The file is not a readable PNG or JPEG image. It may be corrupted or another format renamed with a .png or .jpg extension. Save or export it as PNG or JPG and try again.");
    }
    private static String extension(Path file) { String name = file.getFileName().toString(); return name.substring(name.lastIndexOf('.')).toLowerCase(); }
    private static String encodePath(String path) {
        return java.util.Arrays.stream(path.split("/", -1)).map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20")).collect(java.util.stream.Collectors.joining("/"));
    }
}
