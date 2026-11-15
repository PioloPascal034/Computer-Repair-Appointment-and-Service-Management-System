package crsm;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Customer operations use the signed-in Supabase JWT and ownership policies. */
public final class CustomerWorkflowService {
    private final SupabaseClient client = new SupabaseClient();

    public int completeProfile(UserSession session, String phone, String address) throws IOException, InterruptedException {
        Object result = rpc(session, "complete_crsm_customer_profile", Map.of("contact", phone, "address", address));
        return integerId(result, "complete_crsm_customer_profile customer_id");
    }

    public int customerId(UserSession session) throws IOException, InterruptedException {
        String path = "/rest/v1/customers?auth_user_id=eq." + SupabaseClient.queryValue(session.userId()) + "&select=customer_id";
        HttpResponse<String> response = client.send("GET", path, session.accessToken(), null, null);
        SupabaseClient.requireSuccess(response);
        Object parsed = JsonCodec.parse(response.body());
        if (!(parsed instanceof List<?> rows) || rows.isEmpty()) return -1;
        return integerId(((Map<?,?>) rows.get(0)).get("customer_id"), "customers.customer_id");
    }

    public int submit(UserSession session, int customerId, Map<String,Object> fields) throws IOException, InterruptedException {
        if (session == null || session.accessToken() == null || session.accessToken().isBlank())
            throw new IllegalArgumentException("Sign in again before submitting a repair request.");
        if (customerId <= 0) throw new IllegalArgumentException("Complete your customer profile before submitting a repair request.");
        requireText(fields, "device_type", "Choose a device type.");
        requireText(fields, "brand", "Choose a device brand.");
        requireText(fields, "model", "Enter the device model.");
        requireText(fields, "problem", "Describe the device problem.");
        parseDate(fields.get("appointment_date"), "Enter a valid appointment date (YYYY-MM-DD).");
        parseTime(fields.get("appointment_time"), "Enter a valid appointment time (HH:MM).");
        Object serviceId = fields.get("service_id");
        if (serviceId != null && (!String.valueOf(serviceId).matches("\\d+") || Integer.parseInt(String.valueOf(serviceId)) <= 0))
            throw new IllegalArgumentException("Choose a valid repair service.");
        Map<String,Object> args = new LinkedHashMap<>();
        args.put("cid", customerId);
        args.put("device_type", fields.get("device_type")); args.put("brand", fields.get("brand"));
        args.put("model", fields.get("model")); args.put("specification", fields.get("specification")); args.put("serial", fields.get("serial"));
        args.put("problem", fields.get("problem")); args.put("work", fields.get("work"));
        args.put("condition", fields.get("condition")); args.put("accessories", fields.get("accessories"));
        args.put("appointment_date", fields.get("appointment_date")); args.put("appointment_time", fields.get("appointment_time"));
        args.put("purpose", fields.get("purpose"));
        args.put("selected_service",fields.get("service_id"));
        Object result = rpc(session, "submit_crsm_repair_request_v2", args);
        return integerId(result, "submit_crsm_repair_request_v2 repair_request_id");
    }

    private static void requireText(Map<String,Object> fields, String key, String message) {
        if (fields == null || fields.get(key) == null || fields.get(key).toString().trim().isEmpty()) throw new IllegalArgumentException(message);
    }

    private static void parseDate(Object value, String message) {
        try { LocalDate.parse(value == null ? "" : value.toString().trim()); }
        catch (RuntimeException ex) { throw new IllegalArgumentException(message, ex); }
    }

    private static void parseTime(Object value, String message) {
        try { LocalTime.parse(value == null ? "" : value.toString().trim()); }
        catch (RuntimeException ex) { throw new IllegalArgumentException(message, ex); }
    }

    Map<String,Object> quoteService(UserSession session, int serviceId, String deviceType, String brand, String model, String specification) throws IOException, InterruptedException {
        Map<String,Object> args = new LinkedHashMap<>();
        args.put("p_service_id", serviceId);
        args.put("p_device_type", deviceType);
        args.put("p_brand", brand);
        args.put("p_model", model);
        args.put("p_specification", specification);
        Object result = rpc(session, "quote_crsm_service", args);
        if (!(result instanceof Map<?,?> map)) throw new IllegalStateException("The pricing service returned an unexpected estimate response.");
        Map<String,Object> response = new LinkedHashMap<>();
        map.forEach((key,value) -> response.put(String.valueOf(key), value));
        return response;
    }

    static int integerId(Object value, String field) {
        if (value == null) throw new IllegalStateException("Supabase returned no value for " + field + ".");
        try {
            return new BigDecimal(String.valueOf(value).trim()).intValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new IllegalStateException("Supabase returned an invalid " + field + " (" + value + "); expected a whole-number ID.", ex);
        }
    }

    static BigDecimal moneyValue(Object value) {
        if (value == null) return null;
        try { return new BigDecimal(String.valueOf(value).trim()); }
        catch (NumberFormatException ex) { throw new IllegalStateException("Supabase returned an invalid monetary amount (" + value + ").", ex); }
    }

    public List<Map<String,Object>> repairs(UserSession session) throws IOException, InterruptedException {
        String path="/rest/v1/repair_requests?select=repair_request_id,technician_id,service_id,reported_problem,requested_work,repair_status,estimated_cost,final_cost,customer_decision,repair_services(service_name),computers!inner(brand,model,device_type,specification,verified_specification)&order=repair_request_id.desc";
        if("STAFF".equals(session.role())) {
            HttpResponse<String> linked=client.send("GET","/rest/v1/technicians?auth_user_id=eq."+SupabaseClient.queryValue(session.userId())+"&select=technician_id",session.accessToken(),null,null);
            SupabaseClient.requireSuccess(linked);Object parsed=JsonCodec.parse(linked.body());
            if(!(parsed instanceof List<?> rows)) throw new IllegalStateException("Unexpected technician account link response.");
            if(rows.isEmpty()) return List.of();
            path+="&technician_id=eq."+integerId(((Map<?,?>)rows.get(0)).get("technician_id"),"technician_id");
        }
        HttpResponse<String> response=client.send("GET",path,session.accessToken(),null,null);
        SupabaseClient.requireSuccess(response);
        Object parsed=JsonCodec.parse(response.body());
        if (!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected repair list response.");
        return rowsAsMaps(list);
    }

    public List<Map<String,Object>> computers(UserSession session) throws IOException, InterruptedException {
        HttpResponse<String> response=client.send("GET","/rest/v1/computers?select=computer_id,customer_id,device_type,brand,model,specification,serial_number,reported_problem&order=computer_id.desc",session.accessToken(),null,null);
        SupabaseClient.requireSuccess(response); Object parsed=JsonCodec.parse(response.body());
        if(!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected computers response.");
        return rowsAsMaps(list);
    }

    public List<Map<String,Object>> appointments(UserSession session) throws IOException, InterruptedException {
        HttpResponse<String> response=client.send("GET","/rest/v1/appointments?select=appointment_id,customer_id,computer_id,appointment_date,appointment_time,purpose,appointment_status&order=appointment_date.desc,appointment_time.desc",session.accessToken(),null,null);
        SupabaseClient.requireSuccess(response); Object parsed=JsonCodec.parse(response.body());
        if(!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected appointments response.");
        return rowsAsMaps(list);
    }

    public List<Map<String,Object>> services(UserSession session) throws IOException,InterruptedException {
        String fullPath="/rest/v1/repair_services?select=service_id,service_name,description,service_price,category,common_problems,requires_replacement_part&order=service_name";
        try {
            return parseServiceRows(client.send("GET",fullPath,session.accessToken(),null,null));
        } catch (IllegalStateException metadataUnavailable) {
            try {
                String metadataPath="/rest/v1/repair_services?select=service_id,service_name,description,service_price,category,common_problems&order=service_name";
                return parseServiceRows(client.send("GET",metadataPath,session.accessToken(),null,null));
            } catch (IllegalStateException olderMetadataUnavailable) {
                String legacyPath="/rest/v1/repair_services?select=service_id,service_name,description,service_price&order=service_name";
                return parseServiceRows(client.send("GET",legacyPath,session.accessToken(),null,null));
            }
        }
    }

    /** Technician choices are loaded from the authenticated API so assignment dialogs can
     * show specialization and availability instead of asking staff to remember an ID. */
    public List<Map<String,Object>> technicians(UserSession session) throws IOException,InterruptedException {
        Object parsed=rpc(session,"list_crsm_technician_availability",Map.of());
        if(!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected technicians response.");
        return rowsAsMaps(list);
    }

    public Map<String,Object> billingSummary(UserSession session,int repairId) throws IOException,InterruptedException {
        Object parsed=rpc(session,"get_crsm_repair_bill",Map.of("rid",repairId));
        if(!(parsed instanceof Map<?,?> map)) throw new IllegalStateException("Unexpected billing response.");
        Map<String,Object> result=new LinkedHashMap<>();map.forEach((key,value)->result.put(key.toString(),value));return result;
    }

    public Map<String,Object> dashboardStatistics(UserSession session) throws IOException,InterruptedException {
        Object parsed=rpc(session,"get_crsm_dashboard_statistics",Map.of());
        if(!(parsed instanceof Map<?,?> map)) throw new IllegalStateException("Unexpected dashboard response.");
        Map<String,Object> result=new LinkedHashMap<>();map.forEach((key,value)->result.put(key.toString(),value));return result;
    }

    public List<Map<String,Object>> partPrices(UserSession session) throws IOException,InterruptedException {
        HttpResponse<String> response=client.send("GET","/rest/v1/repair_service_part_prices?active=eq.true&select=part_price_id,service_id,device_type,brand,model,specification,part_name,part_price,repair_services(service_name)&order=part_name",session.accessToken(),null,null);
        SupabaseClient.requireSuccess(response);Object parsed=JsonCodec.parse(response.body());
        if(!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected parts catalog response.");return rowsAsMaps(list);
    }

    static BigDecimal paymentAmount(String value,boolean allowZero) {
        try {
            BigDecimal amount=new BigDecimal(value.trim());
            if(amount.signum()<0 || (!allowZero&&amount.signum()==0) || amount.scale()>2 || amount.compareTo(new BigDecimal("99999999.99"))>0) throw new NumberFormatException();
            return amount;
        }catch(RuntimeException e){throw new IllegalArgumentException("Enter a "+(allowZero?"nonnegative":"positive")+" amount with at most two decimal places (maximum 99,999,999.99).");}
    }

    private List<Map<String,Object>> parseServiceRows(HttpResponse<String> response) throws IOException {
        SupabaseClient.requireSuccess(response); Object parsed=JsonCodec.parse(response.body());
        if(!(parsed instanceof List<?> list))throw new IllegalStateException("Unexpected services response.");
        return rowsAsMaps(list);
    }

    List<Map<String,Object>> serviceDevicePrices(UserSession session) throws IOException,InterruptedException {
        HttpResponse<String> response=client.send("GET","/rest/v1/repair_service_device_prices?select=service_id,device_type,service_price&order=device_type,service_id",session.accessToken(),null,null);
        SupabaseClient.requireSuccess(response); Object parsed=JsonCodec.parse(response.body());
        if(!(parsed instanceof List<?> list))throw new IllegalStateException("Unexpected service compatibility response.");
        return rowsAsMaps(list);
    }

    public List<Map<String,Object>> deviceCatalog(UserSession session) throws IOException,InterruptedException {
        HttpResponse<String> response=client.send("GET","/rest/v1/crsm_device_catalog?select=device_type,brand,model,specification&order=device_type,brand,model",session.accessToken(),null,null);
        SupabaseClient.requireSuccess(response); Object parsed=JsonCodec.parse(response.body());
        if(!(parsed instanceof List<?> list))throw new IllegalStateException("Unexpected device catalog response.");
        return rowsAsMaps(list);
    }

    String devicePrice(UserSession session,int serviceId,String deviceType) throws IOException,InterruptedException {
        String path="/rest/v1/repair_service_device_prices?service_id=eq."+serviceId+"&device_type=eq."+SupabaseClient.queryValue(deviceType)+"&select=service_price";
        HttpResponse<String> response=client.send("GET",path,session.accessToken(),null,null);SupabaseClient.requireSuccess(response);
        Object parsed=JsonCodec.parse(response.body());
        if(parsed instanceof List<?> list&&!list.isEmpty()&&list.get(0) instanceof Map<?,?> row)return String.valueOf(row.get("service_price"));
        return "";
    }

    public List<Map<String,Object>> estimateOptions(UserSession session,int repairId) throws IOException,InterruptedException {
        String path="/rest/v1/repair_estimate_options?repair_request_id=eq."+repairId+"&select=option_id,option_name,description,price&order=option_id";
        HttpResponse<String> response=client.send("GET",path,session.accessToken(),null,null); SupabaseClient.requireSuccess(response);
        Object parsed=JsonCodec.parse(response.body());
        if (!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected estimate options response.");
        return rowsAsMaps(list);
    }

    public List<Map<String,Object>> payments(UserSession session,int repairId) throws IOException,InterruptedException {
        String path="/rest/v1/payments?repair_request_id=eq."+repairId+"&select=payment_id,repair_request_id,amount_paid,refunded_amount,payment_date,payment_status,payment_method,receipt_number&order=payment_id";
        HttpResponse<String> response=client.send("GET",path,session.accessToken(),null,null); SupabaseClient.requireSuccess(response);
        Object parsed=JsonCodec.parse(response.body());
        if (!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected payment response.");
        return rowsAsMaps(list);
    }

    public List<Map<String,Object>> repairPhotos(UserSession session,int repairId) throws IOException,InterruptedException {
        String path="/rest/v1/repair_photos?repair_request_id=eq."+repairId+"&select=object_path,created_at&order=photo_id";
        HttpResponse<String> response=client.send("GET",path,session.accessToken(),null,null); SupabaseClient.requireSuccess(response);
        Object parsed=JsonCodec.parse(response.body());
        if(!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected photo list response.");
        return rowsAsMaps(list);
    }

    public byte[] repairPhoto(UserSession session, int repairId, String objectPath) throws IOException, InterruptedException {
        if (repairPhotos(session,repairId).stream().noneMatch(p->objectPath.equals(p.get("object_path"))))
            throw new SecurityException("Photo unavailable or repair access denied.");
        return new SupabaseStorageService().download(session,"crsm-repair-photos",objectPath);
    }

    public void decideEstimate(UserSession session,int repairId,String decision,List<Long> selected) throws IOException,InterruptedException {
        rpcVoid(session,"decide_crsm_estimate",Map.of("rid",repairId,"decision",decision,"selected_options",selected));
    }

    public void registerPhoto(UserSession session,int repairId,String path) throws IOException,InterruptedException {
        rpcVoid(session,"register_crsm_repair_photo",Map.of("rid",repairId,"path",path));
    }

    public void recordWork(UserSession session,int repairId,String diagnosis,String work,String parts,String cost) throws IOException,InterruptedException {
        Map<String,Object> args=new LinkedHashMap<>(); args.put("rid",repairId); args.put("diagnosis",diagnosis); args.put("work",work); args.put("parts",parts); args.put("cost",new java.math.BigDecimal(cost));
        rpcVoid(session,"record_crsm_work",args);
    }

    void verifyDeviceSpecification(UserSession session,int repairId,String verified) throws IOException,InterruptedException {
        rpcVoid(session,"verify_crsm_device_specification",Map.of("rid",repairId,"verified",verified == null ? "" : verified));
    }

    public void finishRepair(UserSession session,int repairId,String total) throws IOException,InterruptedException {
        rpcVoid(session,"finish_crsm_repair",Map.of("rid",repairId,"total_cost",paymentAmount(total,true)));
    }

    public String recordPayment(UserSession session,int repairId,String amount,String method) throws IOException,InterruptedException {
        if(!List.of("ADMIN","STAFF").contains(session.role())) throw new SecurityException("Only the assigned Technician or Admin may record shop payments.");
        if(!List.of("Cash","GCash","Bank transfer","Card").contains(method)) throw new IllegalArgumentException("Choose a supported payment method.");
        Object result=rpc(session,"record_crsm_payment",Map.of("rid",repairId,"amount",paymentAmount(amount,false),"method",method));
        return String.valueOf(result);
    }

    public void confirmPickup(UserSession session,int repairId) throws IOException,InterruptedException {
        rpcVoid(session,"confirm_crsm_pickup",Map.of("rid",repairId));
    }

    public void assignTechnician(UserSession session,int repairId,int technicianId) throws IOException,InterruptedException {
        rpcVoid(session,"assign_crsm_technician",Map.of("rid",repairId,"tid",technicianId));
    }

    public void saveEstimate(UserSession session,int repairId,String base,List<Map<String,Object>> options) throws IOException,InterruptedException {
        Map<String,Object> args=new LinkedHashMap<>(); args.put("rid",repairId); args.put("base_amount",new java.math.BigDecimal(base)); args.put("options",options);
        rpcVoid(session,"save_crsm_estimate",args);
    }

    void refundPayment(UserSession session,int paymentId,String amount,String reason) throws IOException,InterruptedException {
        Map<String,Object> args=new LinkedHashMap<>();args.put("pid",paymentId);args.put("amount",new java.math.BigDecimal(amount));args.put("reason",reason);
        rpcVoid(session,"refund_crsm_payment",args);
    }

    long requestEstimateReview(UserSession session,int repairId,String note) throws IOException,InterruptedException {
        Object result=rpc(session,"request_crsm_estimate_review",Map.of("rid",repairId,"note",note));
        return Long.parseLong(String.valueOf(result));
    }

    List<Map<String,Object>> pendingEstimateReviews(UserSession session) throws IOException,InterruptedException {
        HttpResponse<String> response=client.send("GET","/rest/v1/repair_estimate_reviews?review_status=eq.Pending&select=review_id,repair_request_id,customer_note,requested_by,created_at&order=created_at",session.accessToken(),null,null);
        SupabaseClient.requireSuccess(response); Object parsed=JsonCodec.parse(response.body());
        if(!(parsed instanceof List<?> list))throw new IllegalStateException("Unexpected review response.");
        return rowsAsMaps(list);
    }

    void resolveEstimateReview(UserSession session,long reviewId,String outcome,String note) throws IOException,InterruptedException {
        rpcVoid(session,"resolve_crsm_estimate_review",Map.of("review",reviewId,"outcome",outcome,"note",note));
    }

    private Object rpc(UserSession session, String name, Map<String,Object> args) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send("POST", "/rest/v1/rpc/" + name, session.accessToken(), args, null);
        SupabaseClient.requireSuccess(response);
        return JsonCodec.parse(response.body());
    }

    private void rpcVoid(UserSession session,String name,Map<String,Object> args) throws IOException,InterruptedException {
        HttpResponse<String> response=client.send("POST","/rest/v1/rpc/"+name,session.accessToken(),args,null);
        SupabaseClient.requireSuccess(response);
    }

    private static List<Map<String,Object>> rowsAsMaps(List<?> rows) {
        List<Map<String,Object>> result=new java.util.ArrayList<>();
        for(Object item:rows) {
            if (!(item instanceof Map<?,?> source)) continue;
            Map<String,Object> row=new LinkedHashMap<>();
            source.forEach((key,value)->row.put(String.valueOf(key),value));
            result.add(row);
        }
        return result;
    }
}
