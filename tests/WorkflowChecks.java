package crsm;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;

/** Local request/validation checks. Database invariants are covered by workflow_regression.sql. */
public class WorkflowChecks {
    static String path,body,auth,response="{}";static int status=200,calls;
    static void check(boolean test){if(!test)throw new AssertionError("Local HTTP contract check failed.");}
    interface Action{void run()throws Exception;}
    static void rejects(Action work,Class<? extends Exception> type)throws Exception{try{work.run();throw new AssertionError("Expected "+type.getSimpleName());}catch(Exception e){if(!type.isInstance(e))throw e;}}
    static UserSession session(String role){return new UserSession("self","test@example.test","Test","test",role,"local-jwt","");}
    public static void main(String[] args)throws Exception{
        if(!"http://127.0.0.1:18765".equals(System.getenv("SUPABASE_URL"))||!"local-contract-test".equals(System.getenv("SUPABASE_ANON_KEY")))
            throw new IllegalStateException("Tests require the exact localhost environment from test-workflows.ps1. Live configuration is refused before any HTTP request.");
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",18765),0);
        server.createContext("/",exchange->{calls++;path=exchange.getRequestURI().toString();body=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);auth=exchange.getRequestHeaders().getFirst("Authorization");byte[] bytes=response.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});server.start();
        try{
            CustomerWorkflowService service=new CustomerWorkflowService();
            response="1";
            Map<String,Object> intake=new LinkedHashMap<>(Map.of("device_type","Laptop","brand","Dell","model","Inspiron","problem","Intermittent startup","appointment_date","2026-11-12","appointment_time","10:00","service_id",1));
            int intakeCalls=calls; service.submit(session("CUSTOMER"),1,intake); check(calls==intakeCalls+1);
            intake.put("appointment_date","not-a-date"); intakeCalls=calls; rejects(()->service.submit(session("CUSTOMER"),1,intake),IllegalArgumentException.class); check(calls==intakeCalls);
            intake.put("appointment_date","2026-11-12"); intake.put("model",""); intakeCalls=calls; rejects(()->service.submit(session("CUSTOMER"),1,intake),IllegalArgumentException.class); check(calls==intakeCalls);
            for(String bad:List.of("", "-1", "0", "NaN", "Infinity", "1.234", "100000000", "abc")) rejects(()->CustomerWorkflowService.paymentAmount(bad,false),IllegalArgumentException.class);
            check(CustomerWorkflowService.paymentAmount(" 100.25 ",false).equals(new BigDecimal("100.25")));check(CustomerWorkflowService.paymentAmount("0",true).signum()==0);
            int before=calls;rejects(()->service.recordPayment(session("CUSTOMER"),7,"100","Cash"),SecurityException.class);check(calls==before);
            rejects(()->service.recordPayment(session("STAFF"),7,"100","Unknown"),IllegalArgumentException.class);
            response="\"CRSM-TEST-000001\"";
            for(String role:List.of("STAFF","ADMIN")){check(service.recordPayment(session(role),7,"100.25","Cash").equals("CRSM-TEST-000001"));check(path.endsWith("record_crsm_payment")&&auth.equals("Bearer local-jwt"));Map<?,?> argsMap=(Map<?,?>)JsonCodec.parse(body);check(argsMap.get("rid").toString().equals("7")&&argsMap.get("amount").toString().equals("100.25"));}
            response="{\"repair_request_id\":7,\"final_cost\":500,\"net_paid\":200,\"balance_due\":300,\"payments\":[]}";
            check(service.billingSummary(session("CUSTOMER"),7).get("balance_due").toString().equals("300"));check(path.endsWith("get_crsm_repair_bill")&&body.equals("{\"rid\":7}"));
            response="[{\"technician_id\":1,\"availability\":\"Busy\",\"duty_status\":\"Available\",\"active_repairs\":1,\"can_assign\":false}]";
            check(Boolean.FALSE.equals(service.technicians(session("ADMIN")).get(0).get("can_assign")));check(path.endsWith("list_crsm_technician_availability"));
            status=403;response="{\"message\":\"Repair access denied\"}";rejects(()->service.billingSummary(session("STAFF"),8),IllegalStateException.class);rejects(()->service.recordPayment(session("STAFF"),8,"10","Cash"),IllegalStateException.class);status=200;
            response="[]";check(service.repairs(session("STAFF")).isEmpty());check(path.startsWith("/rest/v1/technicians?auth_user_id=eq.self"));
            response="[{\"technician_id\":42}]";service.repairs(session("STAFF"));check(path.contains("technician_id=eq.42"));
            service.payments(session("CUSTOMER"),7);check(path.contains("select=payment_id,repair_request_id,"));
            response="{\"repair_count\":2,\"net_paid\":100,\"status_counts\":[],\"monthly_payments\":[],\"recent_repairs\":[]}";
            check(service.dashboardStatistics(session("CUSTOMER")).get("repair_count").toString().equals("2"));check(path.endsWith("get_crsm_dashboard_statistics")&&auth.equals("Bearer local-jwt"));
            response="[{\"part_name\":\"Battery\",\"part_price\":1200,\"device_type\":\"Laptop\"}]";check(service.partPrices(session("CUSTOMER")).size()==1);check(path.contains("repair_service_part_prices?active=eq.true")&&path.contains("repair_services(service_name)"));
            String supplierLink=SupplierLinks.shopeeSearch(Map.of("brand","A&B","part_name","Battery + charger"));check(supplierLink.startsWith("https://shopee.ph/search?keyword=")&&supplierLink.contains("A%26B")&&supplierLink.contains("%2B"));rejects(()->SupplierLinks.shopeeSearch(Map.of()),IllegalArgumentException.class);
            response="{\"message\":\"Active account required\"}";status=403;rejects(()->service.dashboardStatistics(session("CUSTOMER")),IllegalStateException.class);status=200;
            Map<String,Object> receiptBill=new LinkedHashMap<>(Map.of("repair_request_id",7,"final_cost",500,"net_paid",150,"balance_due",350,"repair_status","Awaiting Payment"));
            String receiptText=PaymentReceipt.text("Customer",receiptBill,Map.of("receipt_number","CRSM-TEST","payment_date","2026-11-11","payment_method","Cash","amount_paid",200,"refunded_amount",50));
            check(receiptText.contains("Amount received: PHP 200.00")&&receiptText.contains("Net payment: PHP 150.00")&&receiptText.contains("Balance due: PHP 350.00")&&receiptText.contains("Receipt: CRSM-TEST"));
            receiptBill.remove("final_cost");receiptBill.remove("balance_due");check(PaymentReceipt.text("Customer",receiptBill,Map.of("amount_paid",200)).contains("Final repair cost: Not finalized"));
            System.out.println("Workflow checks passed: amounts, authorized payment roles, payloads, bills, availability RPC, denied payments, assigned-repair filtering payment IDs digital receipt totals, dashboard scope requests, parts catalog and supplier links.");
        }finally{server.stop(0);}
    }
}
