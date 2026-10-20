package crsm;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads the designer-provided device catalog without hardcoding brands or models. */
final class DeviceCatalogLoader {
    private DeviceCatalogLoader() {}

    static List<Map<String, Object>> load() throws IOException {
        Path csv = locate();
        List<Map<String, Object>> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) throw new IOException("Device catalog is empty.");
            // Excel and some editors save UTF-8 CSV files with a BOM before the first header.
            if (!header.isEmpty() && header.charAt(0) == '\uFEFF') header = header.substring(1);
            List<String> columns = parseLine(header);
            if (columns.size() < 4 || !"Type".equalsIgnoreCase(columns.get(0).trim())
                    || !"Brand".equalsIgnoreCase(columns.get(1).trim())
                    || !"Model".equalsIgnoreCase(columns.get(2).trim())
                    || !"Specification".equalsIgnoreCase(columns.get(3).trim())) {
                throw new IOException("Device catalog must contain Type, Brand, Model, Specification columns.");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> values = parseLine(line);
                if (values.size() < 4) continue;
                String type = values.get(0).trim(), brand = values.get(1).trim(), model = values.get(2).trim();
                String specification = values.get(3).trim();
                if (type.isEmpty() || brand.isEmpty() || model.isEmpty()) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("device_type", type); row.put("brand", brand); row.put("model", model);
                row.put("specification", specification);
                rows.add(row);
            }
        }
        return rows;
    }

    private static Path locate() throws IOException {
        List<Path> candidates = new ArrayList<>();
        Path working = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (Path folder = working; folder != null; folder = folder.getParent()) {
            candidates.add(folder.resolve("docs/device_catalog_for_repair_management.csv"));
            candidates.add(folder.resolve("src/crsm/assets/device_catalog_for_repair_management.csv"));
            candidates.add(folder.resolve("device_catalog_for_repair_management.csv"));
        }
        try {
            Path classes = Path.of(DeviceCatalogLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(classes)) classes = classes.getParent();
            for (Path folder = classes.toAbsolutePath().normalize(); folder != null; folder = folder.getParent()) {
                candidates.add(folder.resolve("docs/device_catalog_for_repair_management.csv"));
                candidates.add(folder.resolve("../docs/device_catalog_for_repair_management.csv").normalize());
            }
        } catch (Exception ignored) { }
        for (Path candidate : candidates) if (Files.isRegularFile(candidate)) return candidate;
        throw new IOException("device_catalog_for_repair_management.csv was not found. Start the app from the project folder or keep the docs folder beside the compiled classes.");
    }

    private static List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { current.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) { values.add(current.toString()); current.setLength(0); }
            else current.append(c);
        }
        values.add(current.toString()); return values;
    }
}
