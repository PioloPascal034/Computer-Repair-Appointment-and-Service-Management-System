-- Repeatable demonstration content. Does not create/reset Auth users or change account roles.
-- All added devices, repairs, receipts and part names are clearly marked DEMO.
-- Sample amounts are not verified shop/supplier prices; do not use for real billing.
begin;
do $$
declare customer_user uuid; staff_user uuid; cid integer; tid integer; svc integer;
 comp integer; rid integer; i integer; amount numeric; serial text; receipt text;
begin
 -- Prefer existing linked accounts; never take over or relink existing business records.
 select p.id,c.customer_id into customer_user,cid from public.crsm_profiles p join public.customers c on c.auth_user_id=p.id
 where p.role='CUSTOMER' and p.is_active order by p.created_at limit 1;
 select p.id,t.technician_id into staff_user,tid from public.crsm_profiles p join public.technicians t on t.auth_user_id=p.id
 where p.role='STAFF' and p.is_active order by p.created_at limit 1;
 select service_id into svc from public.repair_services order by case when lower(service_name) like '%diagnos%' then 0 else 1 end,service_id limit 1;
 if cid is null or svc is null then raise exception 'A linked active Customer and at least one service are required to seed demonstrations'; end if;
 for i in 0..6 loop
  serial:='CRSM-DEMO-V2-'||i;receipt:='DEMO-V2-RECEIPT-'||i;amount:=500+i*100;
  select computer_id into comp from public.computers where serial_number=serial;
  if comp is null then
   insert into public.computers(customer_id,device_type,brand,model,specification,serial_number,reported_problem)
   values(cid,'Laptop','CRSM Demo','Training laptop '||i,'Demonstration record only',serial,'[DEMO] Sample repair for workflow testing') returning computer_id into comp;
  end if;
  select repair_request_id into rid from public.repair_requests where computer_id=comp and reported_problem like '[DEMO]%' order by repair_request_id limit 1;
  if rid is null then
   insert into public.repair_requests(computer_id,technician_id,service_id,reported_problem,diagnosis,repair_status,estimated_cost,final_cost,requested_work,pickup_confirmed_at)
   values(comp,tid,svc,'[DEMO] Sample repair '||i,'Demonstration only',case when i=6 then 'Awaiting Payment' else 'Completed' end,amount,amount,'Demonstration only',case when i<6 then now() else null end) returning repair_request_id into rid;
   insert into public.repair_history_events(repair_request_id,event_type,details)
   values(rid,'demo_seeded',jsonb_build_object('source','demo_workspace_seed.sql','demonstration_only',true));
  end if;
  if not exists(select 1 from public.payments where receipt_number=receipt) then
   insert into public.payments(repair_request_id,amount_paid,payment_date,payment_status,payment_method,receipt_number)
   values(rid,case when i=6 then 200 else amount end,(date_trunc('month',current_date)-make_interval(months=>case when i=6 then 0 else i end))::date+5,case when i=6 then 'Partial' else 'Paid' end,'Cash',receipt);
  end if;
 end loop;
 -- Unassigned intake record provides an assignment demonstration without exceeding technical capacity.
 serial:='CRSM-DEMO-V2-INTAKE';select computer_id into comp from public.computers where serial_number=serial;
 if comp is null then
  insert into public.computers(customer_id,device_type,brand,model,serial_number,reported_problem)
  values(cid,'Laptop','CRSM Demo','Intake training laptop',serial,'[DEMO] Device awaiting diagnosis') returning computer_id into comp;
 end if;
 if not exists(select 1 from public.repair_requests where computer_id=comp and reported_problem='[DEMO] Device awaiting diagnosis') then
  insert into public.repair_requests(computer_id,service_id,reported_problem,repair_status,requested_work)
  values(comp,svc,'[DEMO] Device awaiting diagnosis','Pending','Demonstration only');
 end if;
 if not exists(select 1 from public.appointments where computer_id=comp and purpose='[DEMO] Drop-off appointment') then
  insert into public.appointments(customer_id,computer_id,appointment_date,appointment_time,purpose,appointment_status)
  values(cid,comp,current_date+case extract(isodow from current_date)::integer when 5 then 3 when 6 then 2 else 1 end,'10:00','[DEMO] Drop-off appointment','Pending');
 end if;
end $$;
-- Give every active linked Customer account one independent walkthrough record.
-- This keeps the demo useful when more than one customer account exists.
do $$
declare customer_user uuid; cid integer; tid integer; svc integer; comp integer; rid integer; serial text; receipt text;
begin
 select technician_id into tid from public.technicians t join public.crsm_profiles p on p.id=t.auth_user_id
 where p.role='STAFF' and p.is_active order by t.technician_id limit 1;
 select service_id into svc from public.repair_services order by case when lower(service_name) like '%diagnos%' then 0 else 1 end,service_id limit 1;
 for customer_user,cid in
   select p.id,c.customer_id from public.crsm_profiles p join public.customers c on c.auth_user_id=p.id
   where p.role='CUSTOMER' and p.is_active
 loop
   serial:='CRSM-DEMO-V2-ACCOUNT-'||cid; receipt:='DEMO-V2-ACCOUNT-'||cid;
   select computer_id into comp from public.computers where serial_number=serial;
   if comp is null then
     insert into public.computers(customer_id,device_type,brand,model,specification,serial_number,reported_problem)
     values(cid,'Laptop','CRSM Demo','Account walkthrough laptop','Presentation walkthrough record',serial,'[DEMO] Account walkthrough: intermittent startup issue') returning computer_id into comp;
   end if;
   select repair_request_id into rid from public.repair_requests where computer_id=comp and reported_problem like '[DEMO] Account walkthrough:%' limit 1;
   if rid is null then
     insert into public.repair_requests(computer_id,technician_id,service_id,reported_problem,diagnosis,repair_status,estimated_cost,final_cost,requested_work,pickup_confirmed_at)
     values(comp,tid,svc,'[DEMO] Account walkthrough: intermittent startup issue','[DEMO] Diagnostic result: startup storage health checked','Completed',650,650,'[DEMO] Inspect startup performance and verify storage health',now()) returning repair_request_id into rid;
     insert into public.repair_history_events(repair_request_id,event_type,details)
     values(rid,'demo_seeded',jsonb_build_object('source','demo_workspace_seed.sql','demonstration_only',true,'account_walkthrough',true));
   end if;
   if not exists(select 1 from public.payments where receipt_number=receipt) then
     insert into public.payments(repair_request_id,amount_paid,payment_date,payment_status,payment_method,receipt_number)
     values(rid,650,current_date,'Paid','Cash',receipt);
   end if;
   if not exists(select 1 from public.appointments where computer_id=comp and purpose='[DEMO] Account walkthrough appointment') then
     insert into public.appointments(customer_id,computer_id,appointment_date,appointment_time,purpose,appointment_status)
     values(cid,comp,current_date+case extract(isodow from current_date)::integer when 5 then 3 when 6 then 2 else 1 end,'11:00','[DEMO] Account walkthrough appointment','Completed');
   end if;
 end loop;
end $$;
-- Compatibility intentionally uses a fictional brand/model, so these sample prices
-- cannot be mistaken for a quote for a real supported device.
insert into public.repair_service_part_prices(service_id,device_type,brand,model,specification,part_name,part_price)
select s.service_id,'Laptop','CRSM Demo','Training laptop','Demonstration only',p.part_name,p.amount
from (values ('battery','[DEMO] Laptop battery',1200.00),('screen','[DEMO] Display panel',2400.00),('keyboard','[DEMO] Laptop keyboard',800.00),('hardware','[DEMO] 500GB SSD',1700.00)) p(keyword,part_name,amount)
cross join lateral(select service_id from public.repair_services where lower(service_name) like '%'||p.keyword||'%' order by service_id limit 1) s
where not exists(select 1 from public.repair_service_part_prices existing where existing.part_name=p.part_name and existing.brand='CRSM Demo');
notify pgrst, 'reload schema';
commit;
