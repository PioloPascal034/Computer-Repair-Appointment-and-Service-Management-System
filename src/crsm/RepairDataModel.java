package crsm;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Shared metadata and row model for the seven entities in docs/create_tables.sql. */
public final class RepairDataModel {
    private RepairDataModel() {}

    public enum Entity {
        CUSTOMERS("Customers", "customer_id", "Customers", new String[]{"customer_name", "contact_number", "email", "address"}, new String[]{"customer_name", "contact_number"}),
        COMPUTERS("Computers", "computer_id", "Computers", new String[]{"customer_id", "device_type", "brand", "model", "specification", "verified_specification", "serial_number", "reported_problem"}, new String[]{"customer_id", "device_type", "brand"}),
        APPOINTMENTS("Appointments", "appointment_id", "Appointments", new String[]{"customer_id", "computer_id", "appointment_date", "appointment_time", "purpose", "appointment_status"}, new String[]{"customer_id", "computer_id", "appointment_date", "appointment_time", "appointment_status"}),
        REPAIR_REQUESTS("Repair Requests", "repair_request_id", "Repair_Requests", new String[]{"computer_id", "technician_id", "service_id", "reported_problem", "diagnosis", "repair_status", "estimated_cost", "final_cost"}, new String[]{"computer_id", "reported_problem", "repair_status"}),
        TECHNICIANS("Technicians", "technician_id", "Technicians", new String[]{"technician_name", "contact_number", "specialization", "availability"}, new String[]{"technician_name"}),
        REPAIR_SERVICES("Services", "service_id", "Repair_Services", new String[]{"service_name", "description", "service_price"}, new String[]{"service_name", "service_price"}),
        PAYMENTS("Payments", "payment_id", "Payments", new String[]{"repair_request_id", "amount_paid", "payment_date", "payment_status"}, new String[]{"repair_request_id", "amount_paid", "payment_date", "payment_status"});

        public final String label, table, idColumn;
        public final List<String> fields, requiredFields;
        Entity(String label, String idColumn, String table, String[] fields, String[] required) {
            this.label = label; this.idColumn = idColumn; this.table = table;
            this.fields = List.of(fields); this.requiredFields = List.of(required);
        }
        public static Entity fromLabel(String label) {
            for (Entity entity : values()) if (entity.label.equals(label)) return entity;
            throw new IllegalArgumentException("Unsupported data area: " + label);
        }
    }

    public record Row(Entity entity, Map<String, Object> values) {
        public Row { values = Collections.unmodifiableMap(new LinkedHashMap<>(values)); }
        public Object id() { return values.get(entity.idColumn); }
        public String value(String column) { Object value = values.get(column); return value == null ? "" : value.toString(); }
    }
}
