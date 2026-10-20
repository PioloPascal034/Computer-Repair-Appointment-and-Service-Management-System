-- Device intake specification and technician verification.
-- Run after customer_workflow_migration.sql and customer_catalog_and_demo_seed.sql.
begin;
alter table public.computers add column if not exists specification text;
alter table public.computers add column if not exists verified_specification text;
alter table public.crsm_device_catalog add column if not exists specification text;
alter table public.crsm_device_catalog drop constraint if exists crsm_device_catalog_unique;
alter table public.crsm_device_catalog add constraint crsm_device_catalog_unique unique(device_type,brand,model,specification);

drop function if exists public.submit_crsm_repair_request_v2(integer,text,text,text,text,text,text,text,text,date,time,text,integer);
create or replace function public.submit_crsm_repair_request_v2(
 cid integer,device_type text,brand text,model text,specification text,serial text,problem text,work text,condition text,accessories text,
 appointment_date date,appointment_time time,purpose text,selected_service integer)
returns integer language plpgsql security definer set search_path = '' as $$
declare comp integer; app integer; req integer;
begin
 if private.current_crsm_role() is distinct from 'CUSTOMER' then raise exception 'Active Customer account required'; end if;
 if not exists(select 1 from public.customers where customer_id=cid and auth_user_id=auth.uid()) then raise exception 'Customer ownership required'; end if;
 if selected_service is not null and not exists(select 1 from public.repair_services where service_id=selected_service) then raise exception 'Selected service does not exist'; end if;
 if appointment_date < current_date then raise exception 'Appointment must be today or later'; end if;
 insert into public.computers(customer_id,device_type,brand,model,specification,serial_number,reported_problem,condition_on_intake,accessories_received)
 values(cid,trim(device_type),trim(brand),nullif(trim(model),''),nullif(trim(specification),''),nullif(trim(serial),''),trim(problem),nullif(trim(condition),''),nullif(trim(accessories),'')) returning computer_id into comp;
 insert into public.appointments(customer_id,computer_id,appointment_date,appointment_time,purpose,appointment_status)
 values(cid,comp,appointment_date,appointment_time,nullif(trim(purpose),''),'Pending') returning appointment_id into app;
 insert into public.repair_requests(computer_id,service_id,reported_problem,requested_work,repair_status)
 values(comp,selected_service,trim(problem),nullif(trim(work),''),'Pending') returning repair_request_id into req;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details) values(req,auth.uid(),'submitted',jsonb_build_object('appointment_id',app,'service_id',selected_service));
 return req;
end $$;
revoke all on function public.submit_crsm_repair_request_v2(integer,text,text,text,text,text,text,text,text,text,date,time,text,integer) from public,anon,authenticated;
grant execute on function public.submit_crsm_repair_request_v2(integer,text,text,text,text,text,text,text,text,text,date,time,text,integer) to authenticated;

create or replace function public.verify_crsm_device_specification(rid integer,verified text)
returns void language plpgsql security definer set search_path = '' as $$
declare comp integer;
begin
 if not private.is_crsm_staff() then raise exception 'Staff access required'; end if;
 select computer_id into comp from public.repair_requests where repair_request_id=rid;
 if comp is null then raise exception 'Repair request not found'; end if;
 update public.computers set verified_specification=nullif(trim(verified),'') where computer_id=comp;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details) values(rid,auth.uid(),'device_specification_verified',jsonb_build_object('verified_specification',nullif(trim(verified),'')));
end $$;
revoke all on function public.verify_crsm_device_specification(integer,text) from public,anon,authenticated;
grant execute on function public.verify_crsm_device_specification(integer,text) to authenticated;
commit;
