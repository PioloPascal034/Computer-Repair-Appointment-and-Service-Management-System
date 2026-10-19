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
import java.time.LocalDate;
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

    public int countWhere(RepairDataModel.Entity entity, String column, String value) throws SQLException {
        if (!entity.fields.contains(column) && !column.equals(entity.idColumn)) throw new IllegalArgumentException("Unsupported column.");
        try (Connection c = DatabaseConfig.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM " + entity.table + " WHERE " + column + " = ?")) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public Map<String, Integer> countGroup(RepairDataModel.Entity entity, String column) throws SQLException {
        if (!entity.fields.contains(column)) throw new IllegalArgumentException("Unsupported column.");
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (Connection c = DatabaseConfig.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + column + ", COUNT(*) AS total FROM " + entity.table + " GROUP BY " + column + " ORDER BY " + column);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) counts.put(String.valueOf(rs.getObject(1)), rs.getInt("total"));
        }
        return counts;
    }

    public BigDecimal sumPayments() throws SQLException {
        try (Connection c = DatabaseConfig.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(amount_paid),0) FROM Payments");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
    }

    public BigDecimal sumPaymentsByStatus(String status) throws SQLException {
        try (Connection c = DatabaseConfig.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(amount_paid),0) FROM Payments WHERE payment_status = ?")) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        }
    }

    public int countTodayAppointments() throws SQLException {
        try (Connection c = DatabaseConfig.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM Appointments WHERE appointment_date = CURRENT_DATE");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
    }

    public int countActiveRepairs() throws SQLException {
        try (Connection c = DatabaseConfig.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM Repair_Requests WHERE repair_status <> 'Completed'");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
    }

    public Map<String, BigDecimal> monthlyPaymentTotals() throws SQLException {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        try (Connection c = DatabaseConfig.openConnection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT to_char(date_trunc('month', payment_date), 'YYYY-MM') AS month_label,
                            COALESCE(SUM(amount_paid),0) AS total
                     FROM Payments GROUP BY date_trunc('month', payment_date)
                     ORDER BY date_trunc('month', payment_date)
                     """);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) values.put(rs.getString("month_label"), rs.getBigDecimal("total"));
        }
        return values;
    }

    public int countAppointmentsInMonth(LocalDate monthStart) throws SQLException {
        try (Connection c = DatabaseConfig.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM Appointments WHERE appointment_date >= ? AND appointment_date < ?")) {
            ps.setDate(1, Date.valueOf(monthStart));
            ps.setDate(2, Date.valueOf(monthStart.plusMonths(1)));
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public List<Lookup> lookup(RepairDataModel.Entity entity) throws SQLException {
        String label = switch (entity) {
            case CUSTOMERS -> "customer_name";
            case COMPUTERS -> "coalesce(brand,'') || ' ' || coalesce(model,'')";
            case TECHNICIANS -> "technician_name";
            case REPAIR_SERVICES -> "service_name";
            case REPAIR_REQUESTS -> "cast(repair_request_id as text)";
            default -> throw new IllegalArgumentException("No lookup list for " + entity.label);
        };
        List<Lookup> rows = new ArrayList<>();
        try (Connection c = DatabaseConfig.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + entity.idColumn + ", " + label + " AS label FROM " + entity.table + " ORDER BY " + entity.idColumn);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(new Lookup(rs.getInt(1), String.valueOf(rs.getObject("label"))));
        }
        return rows;
    }

    public RepairDataModel.Row findById(RepairDataModel.Entity entity, int id) throws SQLException {
        try (Connection c = DatabaseConfig.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM " + entity.table + " WHERE " + entity.idColumn + " = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> values = new LinkedHashMap<>();
                values.put(entity.idColumn, rs.getObject(entity.idColumn));
                for (String field : entity.fields) values.put(field, rs.getObject(field));
                return new RepairDataModel.Row(entity, values);
            }
        }
    }

    public DisplayPage findDisplay(RepairDataModel.Entity entity, String search, String status, int page, int pageSize) throws SQLException {
        QuerySpec spec = displayQuery(entity);
        List<String> filters = new ArrayList<>();
        List<String> params = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            filters.add("(" + spec.searchSql + ")");
            String like = "%" + search.trim() + "%";
            for (int i = 0; i < spec.searchParams; i++) params.add(like);
        }
        if (status != null && !status.isBlank() && !status.equals("All") && spec.statusColumn != null) {
            filters.add(spec.statusColumn + " = ?");
            params.add(status);
        }
        String where = filters.isEmpty() ? "" : " WHERE " + String.join(" AND ", filters);
        String countSql = "SELECT COUNT(*) FROM (" + spec.sql + ") listing" + where;
        String pageSql = "SELECT * FROM (" + spec.sql + ") listing" + where + " ORDER BY " + spec.idColumn + " DESC LIMIT ? OFFSET ?";
        int total;
        try (Connection c = DatabaseConfig.openConnection()) {
            try (PreparedStatement ps = c.prepareStatement(countSql)) {
                for (int i = 0; i < params.size(); i++) ps.setString(i + 1, params.get(i));
                try (ResultSet rs = ps.executeQuery()) { rs.next(); total = rs.getInt(1); }
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(pageSql)) {
                for (int i = 0; i < params.size(); i++) ps.setString(i + 1, params.get(i));
                ps.setInt(params.size() + 1, pageSize);
                ps.setInt(params.size() + 2, Math.max(0, (page - 1) * pageSize));
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= meta.getColumnCount(); i++) row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                        rows.add(row);
                    }
                }
            }
            return new DisplayPage(spec.columns, rows, total);
        }
    }

    public List<Map<String, Object>> recentRepairs(int limit) throws SQLException {
        return query("""
                SELECT r.repair_request_id, c.customer_name, comp.brand, comp.model, t.technician_name,
                       r.repair_status, r.estimated_cost
                FROM Repair_Requests r
                JOIN Computers comp ON r.computer_id = comp.computer_id
                JOIN Customers c ON comp.customer_id = c.customer_id
                LEFT JOIN Technicians t ON r.technician_id = t.technician_id
                ORDER BY r.repair_request_id DESC LIMIT ?
                """, ps -> ps.setInt(1, limit));
    }

    public List<Map<String, Object>> todayAppointments() throws SQLException {
        return query("""
                SELECT a.appointment_id, a.appointment_time, c.customer_name, comp.device_type, comp.brand, a.appointment_status, a.purpose
                FROM Appointments a
                JOIN Customers c ON a.customer_id = c.customer_id
                JOIN Computers comp ON a.computer_id = comp.computer_id
                WHERE a.appointment_date = CURRENT_DATE
                ORDER BY a.appointment_time
                """, ps -> {});
    }

    public List<Map<String, Object>> recentPayments(int limit) throws SQLException {
        return query("""
                SELECT p.payment_id, p.payment_date, p.amount_paid, p.payment_status, c.customer_name
                FROM Payments p
                JOIN Repair_Requests r ON p.repair_request_id = r.repair_request_id
                JOIN Computers comp ON r.computer_id = comp.computer_id
                JOIN Customers c ON comp.customer_id = c.customer_id
                ORDER BY p.payment_id DESC LIMIT ?
                """, ps -> ps.setInt(1, limit));
    }

    private List<Map<String, Object>> query(String sql, SqlBinder binder) throws SQLException {
        try (Connection c = DatabaseConfig.openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    private QuerySpec displayQuery(RepairDataModel.Entity entity) {
        return switch (entity) {
            case CUSTOMERS -> new QuerySpec(
                    """
                    SELECT c.customer_id, c.customer_name, c.contact_number, c.email, c.address,
                           (SELECT COUNT(*) FROM Computers x WHERE x.customer_id = c.customer_id) AS total_devices,
                           (SELECT COUNT(*) FROM Computers x JOIN Repair_Requests r ON r.computer_id = x.computer_id WHERE x.customer_id = c.customer_id) AS total_repairs
                    FROM Customers c
                    """,
                    "CAST(customer_id AS TEXT) ILIKE ? OR customer_name ILIKE ? OR contact_number ILIKE ? OR COALESCE(email,'') ILIKE ?",
                    null, "customer_id",
                    List.of("customer_id", "customer_name", "contact_number", "email", "address", "total_devices", "total_repairs"),
                    4);
            case COMPUTERS -> new QuerySpec(
                    """
                    SELECT comp.computer_id, cust.customer_name, comp.device_type, comp.brand, comp.model, comp.serial_number, comp.reported_problem,
                           (SELECT r.repair_status FROM Repair_Requests r WHERE r.computer_id = comp.computer_id ORDER BY r.repair_request_id DESC LIMIT 1) AS latest_repair_status
                    FROM Computers comp JOIN Customers cust ON comp.customer_id = cust.customer_id
                    """,
                    "CAST(computer_id AS TEXT) ILIKE ? OR customer_name ILIKE ? OR device_type ILIKE ? OR brand ILIKE ? OR COALESCE(model,'') ILIKE ?",
                    "latest_repair_status", "computer_id",
                    List.of("computer_id", "customer_name", "device_type", "brand", "model", "serial_number", "reported_problem", "latest_repair_status"),
                    5);
            case APPOINTMENTS -> new QuerySpec(
                    """
                    SELECT a.appointment_id, a.appointment_date, a.appointment_time, c.customer_name,
                           comp.device_type || ' ' || COALESCE(comp.brand,'') AS device, a.purpose, a.appointment_status
                    FROM Appointments a
                    JOIN Customers c ON a.customer_id = c.customer_id
                    JOIN Computers comp ON a.computer_id = comp.computer_id
                    """,
                    "CAST(appointment_id AS TEXT) ILIKE ? OR customer_name ILIKE ? OR purpose ILIKE ? OR appointment_status ILIKE ?",
                    "appointment_status", "appointment_id",
                    List.of("appointment_id", "appointment_date", "appointment_time", "customer_name", "device", "purpose", "appointment_status"),
                    4);
            case REPAIR_REQUESTS -> new QuerySpec(
                    """
                    SELECT r.repair_request_id, c.customer_name, comp.device_type || ' ' || COALESCE(comp.brand,'') AS device,
                           s.service_name, t.technician_name, r.repair_status, r.estimated_cost, r.final_cost, r.reported_problem
                    FROM Repair_Requests r
                    JOIN Computers comp ON r.computer_id = comp.computer_id
                    JOIN Customers c ON comp.customer_id = c.customer_id
                    LEFT JOIN Technicians t ON r.technician_id = t.technician_id
                    LEFT JOIN Repair_Services s ON r.service_id = s.service_id
                    """,
                    "CAST(repair_request_id AS TEXT) ILIKE ? OR customer_name ILIKE ? OR COALESCE(service_name,'') ILIKE ? OR COALESCE(technician_name,'') ILIKE ? OR repair_status ILIKE ?",
                    "repair_status", "repair_request_id",
                    List.of("repair_request_id", "customer_name", "device", "service_name", "technician_name", "repair_status", "estimated_cost", "final_cost", "reported_problem"),
                    5);
            case TECHNICIANS -> new QuerySpec(
                    """
                    SELECT t.technician_id, t.technician_name, t.contact_number, t.specialization, t.availability,
                           (SELECT COUNT(*) FROM Repair_Requests r WHERE r.technician_id = t.technician_id) AS assignment_count
                    FROM Technicians t
                    """,
                    "CAST(technician_id AS TEXT) ILIKE ? OR technician_name ILIKE ? OR COALESCE(specialization,'') ILIKE ?",
                    null, "technician_id",
                    List.of("technician_id", "technician_name", "contact_number", "specialization", "availability", "assignment_count"),
                    3);
            case REPAIR_SERVICES -> new QuerySpec(
                    """
                    SELECT service_id, service_name, description, service_price FROM Repair_Services
                    """,
                    "CAST(service_id AS TEXT) ILIKE ? OR service_name ILIKE ? OR COALESCE(description,'') ILIKE ?",
                    null, "service_id",
                    List.of("service_id", "service_name", "description", "service_price"),
                    3);
            case PAYMENTS -> new QuerySpec(
                    """
                    SELECT p.payment_id, p.payment_date, c.customer_name, p.repair_request_id, s.service_name, p.amount_paid, p.payment_status
                    FROM Payments p
                    JOIN Repair_Requests r ON p.repair_request_id = r.repair_request_id
                    JOIN Computers comp ON r.computer_id = comp.computer_id
                    JOIN Customers c ON comp.customer_id = c.customer_id
                    LEFT JOIN Repair_Services s ON r.service_id = s.service_id
                    """,
                    "CAST(payment_id AS TEXT) ILIKE ? OR customer_name ILIKE ? OR CAST(repair_request_id AS TEXT) ILIKE ? OR payment_status ILIKE ?",
                    "payment_status", "payment_id",
                    List.of("payment_id", "payment_date", "customer_name", "repair_request_id", "service_name", "amount_paid", "payment_status"),
                    4);
        };
    }

    public record Lookup(int id, String label) {
        @Override public String toString() { return id + " — " + label; }
    }

    public record DisplayPage(List<String> columns, List<Map<String, Object>> rows, int total) {}

    private record QuerySpec(String sql, String searchSql, String statusColumn, String idColumn, List<String> columns, int searchParams) {}

    @FunctionalInterface private interface SqlBinder { void bind(PreparedStatement ps) throws SQLException; }

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
