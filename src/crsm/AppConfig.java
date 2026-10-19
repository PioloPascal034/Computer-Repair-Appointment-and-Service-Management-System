package crsm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Reads local .env settings, with process environment variables taking precedence. */
final class AppConfig {
    private static final Map<String, String> DOT_ENV = loadDotEnv();

    private AppConfig() {}

    static String get(String name) {
        String environmentValue = System.getenv(name);
        if (environmentValue != null && !environmentValue.isBlank()) return environmentValue;
        return DOT_ENV.get(name);
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> values = new HashMap<>();
        Path file = Path.of(System.getProperty("user.dir", "."), ".env");
        if (!Files.isRegularFile(file)) return values;

        try {
            for (String line : Files.readAllLines(file)) {
                String entry = line.trim();
                if (entry.isEmpty() || entry.startsWith("#")) continue;
                int separator = entry.indexOf('=');
                if (separator <= 0) continue;
                String name = entry.substring(0, separator).trim();
                String value = entry.substring(separator + 1).trim();
                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!name.isEmpty()) values.put(name, value);
            }
        } catch (IOException ignored) {
            // Missing or unreadable local configuration is reported by the
            // specific service when it validates its required settings.
        }
        return Map.copyOf(values);
    }
}
