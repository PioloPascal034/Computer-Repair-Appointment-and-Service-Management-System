package crsm;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Customer operations use the signed-in Supabase JWT and ownership policies. */
final class CustomerWorkflowService {
    private final SupabaseClient client = new SupabaseClient();

    int completeProfile(UserSession session, String phone, String address) throws IOException, InterruptedException {
        Object result = rpc(session, "complete_crsm_customer_profile", Map.of("contact", phone, "address", address));
        return Integer.parseInt(String.valueOf(result));
    }

    int customerId(UserSession session) throws IOException, InterruptedException {
        String path = "/rest/v1/customers?auth_user_id=eq." + SupabaseClient.queryValue(session.userId()) + "&select=customer_id";
        HttpResponse<String> response = client.send("GET", path, session.accessToken(), null, null);
        SupabaseClient.requireSuccess(response);
        Object parsed = JsonCodec.parse(response.body());
        if (!(parsed instanceof List<?> rows) || rows.isEmpty()) return -1;
        return Integer.parseInt(String.valueOf(((Map<?,?>) rows.get(0)).get("customer_id")));
    }

    int submit(UserSession session, int customerId, Map<String,Object> fields) throws IOException, InterruptedException {
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
        return Integer.parseInt(String.valueOf(result));
    }

    List<Map<String,Object>> repairs(UserSession session) throws IOException, InterruptedException {
        HttpResponse<String> response=client.send("GET","/rest/v1/repair_requests?select=repair_request_id,reported_problem,requested_work,repair_status,estimated_cost,final_cost,customer_decision,computers!inner(brand,model,device_type,specification,verified_specification)&order=repair_request_id.desc",session.accessToken(),null,null);
        SupabaseClient.requireSuccess(response);
        Object parsed=JsonCodec.parse(response.body());
        if (!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected repair list response.");
        return rowsAsMaps(list);
    }

    List<Map<String,Object>> services(UserSession session) throws IOException,InterruptedException {
        String fullPath="/rest/v1/repair_services?select=service_id,service_name,description,service_price,category,common_problems&order=service_name";
        try {
            return parseServiceRows(client.send("GET",fullPath,session.accessToken(),null,null));
        } catch (IllegalStateException metadataUnavailable) {
            // Older databases do not have the optional catalog metadata columns yet.
            // Load the actual service records so the catalog remains usable; the UI derives
            // a sensible category from the service name until the migration is applied.
            String legacyPath="/rest/v1/repair_services?select=service_id,service_name,description,service_price&order=service_name";
            return parseServiceRows(client.send("GET",legacyPath,session.accessToken(),null,null));
        }
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

    List<Map<String,Object>> deviceCatalog(UserSession session) throws IOException,InterruptedException {
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

    List<Map<String,Object>> estimateOptions(UserSession session,int repairId) throws IOException,InterruptedException {
        String path="/rest/v1/repair_estimate_options?repair_request_id=eq."+repairId+"&select=option_id,option_name,description,price&order=option_id";
        HttpResponse<String> response=client.send("GET",path,session.accessToken(),null,null); SupabaseClient.requireSuccess(response);
        Object parsed=JsonCodec.parse(response.body());
        if (!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected estimate options response.");
        return rowsAsMaps(list);
    }

    List<Map<String,Object>> payments(UserSession session,int repairId) throws IOException,InterruptedException {
        String path="/rest/v1/payments?repair_request_id=eq."+repairId+"&select=payment_id,amount_paid,refunded_amount,payment_date,payment_status,payment_method,receipt_number&order=payment_id";
        HttpResponse<String> response=client.send("GET",path,session.accessToken(),null,null); SupabaseClient.requireSuccess(response);
        Object parsed=JsonCodec.parse(response.body());
        if (!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected payment response.");
        return rowsAsMaps(list);
    }

    List<Map<String,Object>> repairPhotos(UserSession session,int repairId) throws IOException,InterruptedException {
        String path="/rest/v1/repair_photos?repair_request_id=eq."+repairId+"&select=object_path,created_at&order=photo_id";
        HttpResponse<String> response=client.send("GET",path,session.accessToken(),null,null); SupabaseClient.requireSuccess(response);
        Object parsed=JsonCodec.parse(response.body());
        if(!(parsed instanceof List<?> list)) throw new IllegalStateException("Unexpected photo list response.");
        return rowsAsMaps(list);
    }

    void decideEstimate(UserSession session,int repairId,String decision,List<Long> selected) throws IOException,InterruptedException {
        rpcVoid(session,"decide_crsm_estimate",Map.of("rid",repairId,"decision",decision,"selected_options",selected));
    }

    void registerPhoto(UserSession session,int repairId,String path) throws IOException,InterruptedException {
        rpcVoid(session,"register_crsm_repair_photo",Map.of("rid",repairId,"path",path));
    }

    void recordWork(UserSession session,int repairId,String diagnosis,String work,String parts,String cost) throws IOException,InterruptedException {
        Map<String,Object> args=new LinkedHashMap<>(); args.put("rid",repairId); args.put("diagnosis",diagnosis); args.put("work",work); args.put("parts",parts); args.put("cost",new java.math.BigDecimal(cost));
        rpcVoid(session,"record_crsm_work",args);
    }

    void verifyDeviceSpecification(UserSession session,int repairId,String verified) throws IOException,InterruptedException {
        rpcVoid(session,"verify_crsm_device_specification",Map.of("rid",repairId,"verified",verified == null ? "" : verified));
    }

    void finishRepair(UserSession session,int repairId,String total) throws IOException,InterruptedException {
        rpcVoid(session,"finish_crsm_repair",Map.of("rid",repairId,"total_cost",new java.math.BigDecimal(total)));
    }

    String recordPayment(UserSession session,int repairId,String amount,String method) throws IOException,InterruptedException {
        Object result=rpc(session,"record_crsm_payment",Map.of("rid",repairId,"amount",new java.math.BigDecimal(amount),"method",method));
        return String.valueOf(result);
    }

    void confirmPickup(UserSession session,int repairId) throws IOException,InterruptedException {
        rpcVoid(session,"confirm_crsm_pickup",Map.of("rid",repairId));
    }

    void assignTechnician(UserSession session,int repairId,int technicianId) throws IOException,InterruptedException {
        rpcVoid(session,"assign_crsm_technician",Map.of("rid",repairId,"tid",technicianId));
    }

    void saveEstimate(UserSession session,int repairId,String base,List<Map<String,Object>> options) throws IOException,InterruptedException {
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
