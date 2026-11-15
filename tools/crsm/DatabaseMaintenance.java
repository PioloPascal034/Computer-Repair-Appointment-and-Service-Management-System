package crsm;

import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Local administration using configured credentials. Never prints secrets or passwords. */
public final class DatabaseMaintenance {
    public static void main(String[] args) throws Exception {
        try(Connection db=DatabaseConfig.openConnection()) {
            if(args.length==0||args[0].equals("inspect")){inspect(db);return;}
            if(args[0].equals("verify")){verify(db);return;}
            if(args[0].equals("apply")){
                for(int i=1;i<args.length;i++){
                    Path file=Path.of(args[i]).toAbsolutePath().normalize();
                    if(!file.startsWith(Path.of("docs").toAbsolutePath().normalize())||!file.toString().endsWith(".sql"))throw new IllegalArgumentException("Only project docs SQL files are accepted.");
                    try(Statement statement=db.createStatement()){statement.execute(Files.readString(file));}
                    System.out.println("Applied "+file.getFileName());
                }
                try(Statement statement=db.createStatement()){statement.execute("notify pgrst, 'reload schema'");}
                inspect(db);return;
            }
            throw new IllegalArgumentException("Use inspect or apply <docs/*.sql>.");
        }catch(SQLException failure){System.err.println("Database check failed (SQLSTATE "+failure.getSQLState()+"): "+failure.getMessage());System.exit(1);}
    }
    private static void verify(Connection db)throws Exception {
        List<String[]> accounts=new ArrayList<>();
        try(Statement query=db.createStatement();ResultSet rows=query.executeQuery("select id::text,role from public.crsm_profiles where is_active order by role")){while(rows.next())accounts.add(new String[]{rows.getString(1),rows.getString(2)});}
        for(String[] account:accounts){
            db.setAutoCommit(false);
            try {
                try(PreparedStatement identity=db.prepareStatement("select set_config('request.jwt.claim.sub',?,true)")){identity.setString(1,account[0]);identity.execute();}
                try(Statement role=db.createStatement()){role.execute("set local role authenticated");}
                try(Statement query=db.createStatement();ResultSet rows=query.executeQuery("select public.get_crsm_dashboard_statistics()")){rows.next();Map<?,?> data=(Map<?,?>)JsonCodec.parse(rows.getString(1));System.out.println(account[1]+" dashboard: repairs="+data.get("repair_count")+", monthlyPoints="+((List<?>)data.get("monthly_payments")).size());if(((List<?>)data.get("monthly_payments")).size()!=6)throw new AssertionError("Expected six monthly chart points");}
                try(Statement query=db.createStatement();ResultSet rows=query.executeQuery("select public.get_crsm_my_account()")){rows.next();if(!(JsonCodec.parse(rows.getString(1)) instanceof Map<?,?>))throw new AssertionError("Account read failed");System.out.println(account[1]+" profile: OK");}
                try(Statement query=db.createStatement();ResultSet rows=query.executeQuery("select count(*) from public.repair_service_part_prices where active")){rows.next();System.out.println(account[1]+" visible parts: "+rows.getInt(1));}
                if(!account[1].equals("CUSTOMER"))try(Statement query=db.createStatement();ResultSet rows=query.executeQuery("select public.list_crsm_technician_availability()")){rows.next();if(!(JsonCodec.parse(rows.getString(1)) instanceof List<?>))throw new AssertionError("Availability failed");System.out.println(account[1]+" technician availability: OK");}
                if(account[1].equals("STAFF")){
                    Integer demoRepair=null;
                    try(Statement query=db.createStatement();ResultSet rows=query.executeQuery("select r.repair_request_id from public.repair_requests r where r.reported_problem like '[DEMO]%' and r.repair_status='Awaiting Payment' and private.can_view_crsm_repair(r.repair_request_id) limit 1")){if(rows.next())demoRepair=rows.getInt(1);}
                    if(demoRepair!=null){
                        java.math.BigDecimal before;
                        try(PreparedStatement query=db.prepareStatement("select public.get_crsm_repair_bill(?)")){query.setInt(1,demoRepair);try(ResultSet rows=query.executeQuery()){rows.next();Map<?,?> bill=(Map<?,?>)JsonCodec.parse(rows.getString(1));before=new java.math.BigDecimal(bill.get("balance_due").toString());}}
                        java.math.BigDecimal testAmount=before.min(new java.math.BigDecimal("1.00"));
                        try(PreparedStatement query=db.prepareStatement("select public.record_crsm_payment(?,?,?)")){query.setInt(1,demoRepair);query.setBigDecimal(2,testAmount);query.setString(3,"Cash");try(ResultSet rows=query.executeQuery()){rows.next();if(rows.getString(1)==null)throw new AssertionError("Receipt missing");}}
                        try(PreparedStatement query=db.prepareStatement("select public.get_crsm_repair_bill(?)")){query.setInt(1,demoRepair);try(ResultSet rows=query.executeQuery()){rows.next();Map<?,?> bill=(Map<?,?>)JsonCodec.parse(rows.getString(1));if(new java.math.BigDecimal(bill.get("balance_due").toString()).compareTo(before.subtract(testAmount))!=0)throw new AssertionError("Balance did not decrease");}}
                        System.out.println("STAFF demo payment/receipt/balance: OK (transaction rolled back)");
                    }
                }
                try(Statement query=db.createStatement();ResultSet rows=query.executeQuery("select public.quote_crsm_service(service_id,'Laptop','CRSM Demo','Training laptop','Demonstration only') from public.repair_services order by service_id limit 1")){if(rows.next()&&!(JsonCodec.parse(rows.getString(1)) instanceof Map<?,?>))throw new AssertionError("Quote failed");System.out.println(account[1]+" pricing quote: OK");}
                try(Statement query=db.createStatement();ResultSet rows=query.executeQuery("select public.get_crsm_repair_bill(r.repair_request_id) from public.repair_requests r where private.can_view_crsm_repair(r.repair_request_id) limit 1")){if(rows.next()){if(!(JsonCodec.parse(rows.getString(1)) instanceof Map<?,?>))throw new AssertionError("Billing failed");System.out.println(account[1]+" billing: OK");}}
            }finally{db.rollback();db.setAutoCommit(true);}
        }
        SupabaseClient api=new SupabaseClient();var response=api.send("POST","/rest/v1/rpc/get_crsm_dashboard_statistics",null,Map.of(),null);
        if(response.statusCode()>=200&&response.statusCode()<300)throw new AssertionError("Anonymous statistics access must be denied");
        if(response.statusCode()==404)throw new AssertionError("PostgREST still cannot find the statistics function");
        System.out.println("PostgREST statistics route exists; anonymous access denied (HTTP "+response.statusCode()+").");
        System.out.println("Live database role checks passed; no sign-in tokens or passwords were used.");
    }

    private static void inspect(Connection db)throws Exception {
        String[] functions={"complete_crsm_customer_profile","quote_crsm_service","submit_crsm_repair_request_v2","get_crsm_my_account","update_crsm_my_account","get_crsm_dashboard_statistics","list_crsm_technician_availability","get_crsm_repair_bill"};
        for(String name:functions)try(PreparedStatement query=db.prepareStatement("select count(*) from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname='public' and p.proname=?")){query.setString(1,name);try(ResultSet rows=query.executeQuery()){rows.next();System.out.println(name+": "+(rows.getInt(1)>0?"present":"MISSING"));}}
        String[] tables={"crsm_profiles","customers","computers","appointments","technicians","repair_requests","payments","crsm_device_catalog","repair_services","repair_service_part_prices"};
        for(String name:tables)try(Statement query=db.createStatement();ResultSet rows=query.executeQuery("select count(*) from public."+name)){rows.next();System.out.println(name+" rows: "+rows.getLong(1));}catch(SQLException missing){System.out.println(name+": unavailable ("+missing.getSQLState()+")");}
        try(Statement query=db.createStatement();ResultSet rows=query.executeQuery("select role,count(*) as accounts from public.crsm_profiles group by role order by role")){while(rows.next())System.out.println("Accounts "+rows.getString(1)+": "+rows.getInt(2));}
        try(Statement query=db.createStatement();ResultSet rows=query.executeQuery("select p.username,p.role,p.is_active,exists(select 1 from public.customers c where c.auth_user_id=p.id) as customer_link,exists(select 1 from public.technicians t where t.auth_user_id=p.id) as technician_link from public.crsm_profiles p order by role,username")){while(rows.next())System.out.println("Account "+rows.getString(1)+": "+rows.getString(2)+", active="+rows.getBoolean(3)+", customerLink="+rows.getBoolean(4)+", technicianLink="+rows.getBoolean(5));}
        SupabaseClient api=new SupabaseClient();var settings=api.send("GET","/auth/v1/settings",null,null,null);System.out.println("Auth settings HTTP: "+settings.statusCode());
        if(settings.statusCode()==200&&JsonCodec.parse(settings.body()) instanceof Map<?,?> map)for(String key:List.of("disable_signup","mailer_autoconfirm","external","captcha_enabled"))if(map.containsKey(key))System.out.println("Auth "+key+": "+JsonCodec.stringify(map.get(key)));
        try(Statement query=db.createStatement();ResultSet rows=query.executeQuery("select count(*) from pg_trigger where tgrelid='auth.users'::regclass and not tgisinternal and tgname='crsm_create_profile_after_signup'")){rows.next();System.out.println("Signup profile trigger: "+(rows.getInt(1)>0?"present":"MISSING"));}
    }
}
