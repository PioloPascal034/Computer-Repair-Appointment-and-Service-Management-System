package crsm;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** PostgreSQL DAO for records represented in the current seven-table SQL schema. */
public class RepairRepository {
    public List<RepairDataModel.Row> findAll(RepairDataModel.Entity entity, String search) throws SQLException {
        String sql = "SELECT * FROM " + entity.table;
        List<String> searchFields = searchFields(entity);
        boolean hasSearch = search != null && !search.isBlank() && !searchFields.isEmpty();
        if (hasSearch) sql += " WHERE " + searchFields.stream().map(f -> "CAST(" + f + " AS TEXT) ILIKE ?").collect(Collectors.joining(" OR "));
        sql += " ORDER BY " + entity.idColumn + " DESC";
        try (Connection c = DatabaseConfig.openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (hasSearch) for (int i = 0; i < searchFields.size(); i++) ps.setString(i + 1, "%" + search.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<RepairDataModel.Row> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put(entity.idColumn, rs.getObject(entity.idColumn));
                    for (String field : entity.fields) values.put(field, rs.getObject(field));
                    rows.add(new RepairDataModel.Row(entity, values));
                }
                return rows;
            }
        }
    }

    public int count(RepairDataModel.Entity entity) throws SQLException {
        try (Connection c = DatabaseConfig.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM " + entity.table);
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
    }

    public List<Map<String, Object>> findReport(String viewName) throws SQLException {
        if (!List.of("Active_Repair_Status", "Appointment_Schedule", "Repair_History", "Technician_Workload", "Repair_Revenue", "Pending_Repairs", "Most_Requested_Services").contains(viewName)) {
            throw new IllegalArgumentException("Unsupported report view.");
        }
        try (Connection c = DatabaseConfig.openConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM " + viewName); ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData(); int columnCount = meta.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) row.put(meta.getColumnLabel(i), rs.getObject(i));
                rows.add(row);
            }
            return rows;
        }
    }

    public void save(RepairDataModel.Entity entity, Integer id, Map<String, String> input) throws SQLException {
        List<String> fields = entity.fields;
        String sql;
        if (id == null) {
            sql = "INSERT INTO " + entity.table + " (" + String.join(",", fields) + ") VALUES (" + fields.stream().map(f -> "?").collect(Collectors.joining(",")) + ")";
        } else {
            sql = "UPDATE " + entity.table + " SET " + fields.stream().map(f -> f + "=?").collect(Collectors.joining(",")) + " WHERE " + entity.idColumn + "=?";
        }
        try (Connection c = DatabaseConfig.openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < fields.size(); i++) bind(ps, i + 1, fields.get(i), input.getOrDefault(fields.get(i), "").trim());
            if (id != null) ps.setInt(fields.size() + 1, id);
            ps.executeUpdate();
        }
    }

    public void delete(RepairDataModel.Entity entity, int id) throws SQLException {
        try (Connection c = DatabaseConfig.openConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM " + entity.table + " WHERE " + entity.idColumn + "=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, int index, String field, String value) throws SQLException {
        if (value.isEmpty()) { ps.setNull(index, Types.NULL); return; }
        if (field.endsWith("_id") || field.equals("customer_id") || field.equals("computer_id") || field.equals("technician_id") || field.equals("service_id") || field.equals("repair_request_id")) ps.setInt(index, Integer.parseInt(value));
        else if (field.endsWith("_cost") || field.equals("service_price") || field.equals("amount_paid")) ps.setBigDecimal(index, new BigDecimal(value));
        else if (field.endsWith("_date")) ps.setDate(index, Date.valueOf(value));
        else if (field.endsWith("_time")) ps.setTime(index, Time.valueOf(value.length() == 5 ? value + ":00" : value));
        else ps.setString(index, value);
    }

    private static List<String> searchFields(RepairDataModel.Entity entity) {
        return switch (entity) {
            case CUSTOMERS -> List.of("customer_name", "contact_number", "email");
            case COMPUTERS -> List.of("brand", "model", "serial_number", "device_type");
            case APPOINTMENTS -> List.of("purpose", "appointment_status");
            case REPAIR_REQUESTS -> List.of("reported_problem", "repair_status", "diagnosis");
            case TECHNICIANS -> List.of("technician_name", "specialization");
            case REPAIR_SERVICES -> List.of("service_name", "description");
            case PAYMENTS -> List.of("payment_status");
        };
    }
}
