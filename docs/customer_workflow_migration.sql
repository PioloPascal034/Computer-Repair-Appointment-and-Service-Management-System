-- CRSM ownership and workflow foundation. Apply after supabase_fresh_setup.sql.
-- Customer accounts map to one customer row; technicians are Staff accounts
-- linked explicitly by an administrator. Active statuses consume capacity.
begin;

alter table public.customers add column if not exists auth_user_id uuid unique references auth.users(id) on delete set null;
alter table public.computers add column if not exists condition_on_intake text;
alter table public.computers add column if not exists accessories_received text;
alter table public.repair_requests add column if not exists requested_work text;
alter table public.repair_requests add column if not exists customer_decision text;
alter table public.repair_requests add column if not exists customer_decision_at timestamptz;
alter table public.repair_requests add column if not exists pickup_confirmed_at timestamptz;
alter table public.technicians add column if not exists auth_user_id uuid unique references auth.users(id) on delete set null;
alter table public.crsm_profiles add column if not exists is_active boolean not null default true;
alter table public.crsm_profiles add column if not exists last_login_at timestamptz;
alter table public.payments add column if not exists payment_method text;
alter table public.payments add column if not exists receipt_number text unique;
alter table public.payments add column if not exists refunded_amount numeric(10,2) not null default 0 check(refunded_amount >= 0);
alter table public.payments add column if not exists refunded_at timestamptz;
alter table public.repair_requests drop constraint if exists chk_repair_status_allowed;
alter table public.repair_requests add constraint chk_repair_status_allowed check (repair_status in (
 'Pending','Confirmed','Under Diagnosis','Under Repair','Waiting for Parts','Ready for Testing',
 'Awaiting Payment','Ready for Pickup','Completed','Cancelled'
));

create table if not exists public.repair_estimate_options (
  option_id bigint generated always as identity primary key,
  repair_request_id integer not null references public.repair_requests on delete cascade,
  option_name text not null,
  description text,
  price numeric(10,2) not null check(price >= 0),
  selected boolean not null default false,
  created_at timestamptz not null default now()
);
create table if not exists public.repair_history_events (
  event_id bigint generated always as identity primary key,
  repair_request_id integer not null references public.repair_requests on delete cascade,
  actor uuid references auth.users(id),
  event_type text not null,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
create table if not exists public.repair_photos (
  photo_id bigint generated always as identity primary key,
  repair_request_id integer not null references public.repair_requests on delete cascade,
  object_path text not null unique,
  uploaded_by uuid not null references auth.users(id),
  created_at timestamptz not null default now()
);
create table if not exists public.repair_work_entries (
  work_entry_id bigint generated always as identity primary key,
  repair_request_id integer not null references public.repair_requests on delete cascade,
  actor uuid not null references auth.users(id),
  work_description text not null,
  parts_used text,
  cost numeric(10,2) not null default 0 check(cost >= 0),
  created_at timestamptz not null default now()
);
create table if not exists public.crsm_login_activity (
  activity_id bigint generated always as identity primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  occurred_at timestamptz not null default now(),
  client_label text not null default 'CRSM Desktop'
);
create table if not exists public.payment_refunds (
  refund_id bigint generated always as identity primary key,
  payment_id integer not null references public.payments on delete restrict,
  amount numeric(10,2) not null check(amount>0),
  reason text not null,
  refunded_by uuid not null references auth.users(id),
  refunded_at timestamptz not null default now()
);
create table if not exists public.repair_estimate_reviews (
  review_id bigint generated always as identity primary key,
  repair_request_id integer not null references public.repair_requests on delete cascade,
  requested_by uuid not null references auth.users(id),
  customer_note text not null,
  review_status text not null default 'Pending' check(review_status in ('Pending','Requote','No Change','Closed')),
  admin_note text,
  reviewed_by uuid references auth.users(id),
  created_at timestamptz not null default now(),
  reviewed_at timestamptz
);
create unique index if not exists crsm_one_pending_estimate_review_per_repair
on public.repair_estimate_reviews(repair_request_id) where review_status='Pending';

create or replace function private.current_crsm_role()
returns text language sql stable security definer set search_path = ''
as $$ select p.role from public.crsm_profiles p where p.id=auth.uid() and p.is_active $$;
create or replace function private.is_crsm_staff()
returns boolean language sql stable security definer set search_path = ''
as $$ select coalesce(private.current_crsm_role() in ('STAFF','ADMIN'),false) $$;
create or replace function private.owns_computer(cid integer)
returns boolean language sql stable security definer set search_path = ''
as $$ select private.current_crsm_role() is not null and exists(select 1 from public.computers c join public.customers u using(customer_id) where c.computer_id=cid and u.auth_user_id=auth.uid()) $$;
revoke all on function private.is_crsm_staff(), private.owns_computer(integer) from public,anon;
grant execute on function private.is_crsm_staff(), private.owns_computer(integer) to authenticated;

alter table public.customers enable row level security;
alter table public.computers enable row level security;
alter table public.appointments enable row level security;
alter table public.repair_requests enable row level security;
alter table public.repair_services enable row level security;
alter table public.payments enable row level security;
alter table public.repair_estimate_options enable row level security;
alter table public.repair_history_events enable row level security;
alter table public.repair_photos enable row level security;
alter table public.repair_work_entries enable row level security;
alter table public.crsm_login_activity enable row level security;
alter table public.payment_refunds enable row level security;
alter table public.repair_estimate_reviews enable row level security;

do $$ declare t text; begin
 foreach t in array array['customers','computers','appointments','repair_requests','repair_services','payments','repair_estimate_options','repair_history_events','repair_photos','repair_work_entries'] loop
  execute format('revoke all on public.%I from anon,authenticated',t);
 execute format('grant select on public.%I to authenticated',t);
 end loop;
end $$;

drop policy if exists crsm_customer_owner_select on public.customers;
create policy crsm_customer_owner_select on public.customers for select to authenticated using((auth_user_id=auth.uid() and private.current_crsm_role() is not null) or private.is_crsm_staff());
drop policy if exists crsm_computer_owner_select on public.computers;
create policy crsm_computer_owner_select on public.computers for select to authenticated using(private.owns_computer(computer_id) or private.is_crsm_staff());
drop policy if exists crsm_appointment_owner_select on public.appointments;
create policy crsm_appointment_owner_select on public.appointments for select to authenticated using(private.owns_computer(computer_id) or private.is_crsm_staff());
drop policy if exists crsm_repair_owner_select on public.repair_requests;
create policy crsm_repair_owner_select on public.repair_requests for select to authenticated using(private.owns_computer(computer_id) or private.is_crsm_staff());
drop policy if exists crsm_service_signedin_select on public.repair_services;
create policy crsm_service_signedin_select on public.repair_services for select to authenticated using(private.current_crsm_role() is not null);
drop policy if exists crsm_payment_owner_select on public.payments;
create policy crsm_payment_owner_select on public.payments for select to authenticated using(exists(select 1 from public.repair_requests r where r.repair_request_id=payments.repair_request_id and (private.owns_computer(r.computer_id) or private.is_crsm_staff())));
drop policy if exists crsm_options_owner_select on public.repair_estimate_options;
create policy crsm_options_owner_select on public.repair_estimate_options for select to authenticated using(exists(select 1 from public.repair_requests r where r.repair_request_id=repair_estimate_options.repair_request_id and (private.owns_computer(r.computer_id) or private.is_crsm_staff())));
drop policy if exists crsm_history_owner_select on public.repair_history_events;
create policy crsm_history_owner_select on public.repair_history_events for select to authenticated using(exists(select 1 from public.repair_requests r where r.repair_request_id=repair_history_events.repair_request_id and (private.owns_computer(r.computer_id) or private.is_crsm_staff())));
drop policy if exists crsm_photos_owner_select on public.repair_photos;
create policy crsm_photos_owner_select on public.repair_photos for select to authenticated using(exists(select 1 from public.repair_requests r where r.repair_request_id=repair_photos.repair_request_id and (private.owns_computer(r.computer_id) or private.is_crsm_staff())));
drop policy if exists crsm_work_owner_select on public.repair_work_entries;
create policy crsm_work_owner_select on public.repair_work_entries for select to authenticated using(exists(select 1 from public.repair_requests r where r.repair_request_id=repair_work_entries.repair_request_id and (private.owns_computer(r.computer_id) or private.is_crsm_staff())));
drop policy if exists crsm_login_activity_admin_select on public.crsm_login_activity;
create policy crsm_login_activity_admin_select on public.crsm_login_activity for select to authenticated using(private.is_crsm_admin());
revoke all on public.crsm_login_activity from anon,authenticated;
grant select on public.crsm_login_activity to authenticated;
revoke all on public.payment_refunds from anon,authenticated;
grant select on public.payment_refunds to authenticated;
drop policy if exists crsm_refunds_owner_select on public.payment_refunds;
create policy crsm_refunds_owner_select on public.payment_refunds for select to authenticated using(exists(select 1 from public.payments p join public.repair_requests r using(repair_request_id) where p.payment_id=payment_refunds.payment_id and (private.owns_computer(r.computer_id) or private.is_crsm_staff())));
revoke all on public.repair_estimate_reviews from anon,authenticated;
grant select on public.repair_estimate_reviews to authenticated;
drop policy if exists crsm_estimate_review_owner_select on public.repair_estimate_reviews;
create policy crsm_estimate_review_owner_select on public.repair_estimate_reviews for select to authenticated using(private.is_crsm_admin() or (requested_by=auth.uid() and private.current_crsm_role()='CUSTOMER'));

create or replace view public.repair_revenue as
select r.repair_request_id,c.customer_name,comp.brand,comp.model,rs.service_name,r.final_cost,
       coalesce(sum(p.amount_paid-p.refunded_amount),0) as total_paid,
       r.final_cost-coalesce(sum(p.amount_paid-p.refunded_amount),0) as remaining_balance
from public.repair_requests r
join public.computers comp on r.computer_id=comp.computer_id
join public.customers c on comp.customer_id=c.customer_id
left join public.repair_services rs on r.service_id=rs.service_id
left join public.payments p on r.repair_request_id=p.repair_request_id
group by r.repair_request_id,c.customer_name,comp.brand,comp.model,rs.service_name,r.final_cost;
alter view public.repair_revenue set (security_invoker=true);

create or replace function public.promote_crsm_user(target_user uuid,target_role text)
returns void language plpgsql security definer set search_path = '' as $$
begin
 if not private.is_crsm_admin() then raise exception 'Administrator access required'; end if;
 if target_role not in ('STAFF','ADMIN') then raise exception 'Role must be STAFF or ADMIN'; end if;
 update public.crsm_profiles set role=target_role where id=target_user;
 if not found then raise exception 'Target account not found'; end if;
end $$;

create or replace function public.set_crsm_account_active(target_user uuid,active boolean)
returns void language plpgsql security definer set search_path = '' as $$
declare target_role text; active_admins integer;
begin
 if not private.is_crsm_admin() then raise exception 'Administrator access required'; end if;
 if target_user=auth.uid() and not active then raise exception 'You cannot deactivate your own account'; end if;
 perform pg_advisory_xact_lock(hashtextextended('crsm_admin_account_activation',0));
 select role into target_role from public.crsm_profiles where id=target_user for update;
 if not found then raise exception 'Target account not found'; end if;
 if target_role='ADMIN' and not active then
  select count(*) into active_admins from public.crsm_profiles where role='ADMIN' and is_active;
  if active_admins<=1 then raise exception 'The last active administrator cannot be deactivated'; end if;
 end if;
 update public.crsm_profiles set is_active=active where id=target_user;
end $$;

create or replace function public.record_crsm_login_activity()
returns void language plpgsql security definer set search_path = '' as $$
begin
 if private.current_crsm_role() is null then raise exception 'Account is inactive'; end if;
 update public.crsm_profiles set last_login_at=now() where id=auth.uid();
 insert into public.crsm_login_activity(user_id) values(auth.uid());
end $$;

create or replace function public.link_crsm_technician_account(target_technician integer,target_user uuid)
returns void language plpgsql security definer set search_path = '' as $$
declare target_role text; linked_user uuid;
begin
 if not private.is_crsm_admin() then raise exception 'Administrator access required'; end if;
 select role into target_role from public.crsm_profiles where id=target_user and is_active;
 if target_role is distinct from 'STAFF' then raise exception 'Technician login must be an active Staff account'; end if;
 select auth_user_id into linked_user from public.technicians where technician_id=target_technician for update;
 if not found then raise exception 'Technician record not found'; end if;
 if linked_user is not null and linked_user<>target_user then raise exception 'Technician is already linked to another account'; end if;
 update public.technicians set auth_user_id=target_user where technician_id=target_technician;
end $$;

create or replace function public.link_crsm_customer_record(target_customer integer,target_user uuid)
returns void language plpgsql security definer set search_path = '' as $$
declare target_role text; linked_user uuid;
begin
 if not private.is_crsm_admin() then raise exception 'Administrator access required'; end if;
 select role into target_role from public.crsm_profiles where id=target_user and is_active;
 if target_role is distinct from 'CUSTOMER' then raise exception 'Target account must be an active Customer'; end if;
 select auth_user_id into linked_user from public.customers where customer_id=target_customer for update;
 if not found then raise exception 'Customer record not found'; end if;
 if linked_user is not null and linked_user<>target_user then raise exception 'Customer record is already linked to another account'; end if;
 update public.customers set auth_user_id=target_user where customer_id=target_customer;
end $$;

-- Complete profile and create the business customer record in one transaction.
create or replace function public.complete_crsm_customer_profile(contact text,address text default null)
returns integer language plpgsql security definer set search_path = '' as $$
declare p public.crsm_profiles; cid integer;
begin
 select * into p from public.crsm_profiles where id=auth.uid() for update;
 if p.id is null or p.role <> 'CUSTOMER' or private.current_crsm_role() is distinct from 'CUSTOMER' then raise exception 'Active Customer account required'; end if;
 if nullif(trim(contact),'') is null then raise exception 'Contact number is required'; end if;
 select customer_id into cid from public.customers where auth_user_id=auth.uid();
 if cid is null then
  insert into public.customers(customer_name,contact_number,email,address,auth_user_id)
   values(p.full_name,trim(contact),p.email,nullif(trim(address),''),auth.uid()) returning customer_id into cid;
 else update public.customers set customer_name=p.full_name,contact_number=trim(contact),address=nullif(trim(address),'') where customer_id=cid;
 end if;
 return cid;
end $$;

create or replace function public.submit_crsm_repair_request(cid integer,device_type text,brand text,model text,serial text,problem text,work text,condition text,accessories text,appointment_date date,appointment_time time,purpose text)
returns integer language plpgsql security definer set search_path = '' as $$
declare comp integer; app integer; req integer; max_advance integer; max_daily integer; open_at time; close_at time; day_list text; requested_date date;
begin
 if private.current_crsm_role() is distinct from 'CUSTOMER' then raise exception 'Active Customer account required'; end if;
 if not exists(select 1 from public.customers where customer_id=cid and auth_user_id=auth.uid()) then raise exception 'Customer ownership required'; end if;
 if appointment_date < current_date then raise exception 'Appointment must be today or later'; end if;
 requested_date:=appointment_date;
 select coalesce((select setting_value from public.crsm_settings where setting_key='appointment_advance_days'),'90')::integer into max_advance;
 select coalesce((select setting_value from public.crsm_settings where setting_key='appointment_daily_limit'),'0')::integer into max_daily;
 select coalesce((select setting_value from public.crsm_settings where setting_key='appointment_open_time'),'09:00')::time into open_at;
 select coalesce((select setting_value from public.crsm_settings where setting_key='appointment_close_time'),'17:00')::time into close_at;
 select coalesce((select setting_value from public.crsm_settings where setting_key='appointment_working_days'),'1,2,3,4,5') into day_list;
 if appointment_date > current_date+max_advance then raise exception 'Appointment is outside the advance booking window'; end if;
 if close_at<=open_at then raise exception 'Business appointment hours are misconfigured'; end if;
 if appointment_time < open_at or appointment_time >= close_at then raise exception 'Appointment time is outside business hours'; end if;
 if strpos(','||day_list||',',','||extract(isodow from appointment_date)::integer::text||',')=0 then raise exception 'Business is closed on the selected day'; end if;
 if max_daily>0 then
  perform pg_advisory_xact_lock(hashtextextended(requested_date::text,0));
  if (select count(*) from public.appointments a where a.appointment_date=requested_date and a.appointment_status<>'Cancelled')>=max_daily then raise exception 'No appointment capacity remains for the selected day'; end if;
 end if;
 insert into public.computers(customer_id,device_type,brand,model,serial_number,reported_problem,condition_on_intake,accessories_received)
 values(cid,trim(device_type),trim(brand),nullif(trim(model),''),nullif(trim(serial),''),trim(problem),nullif(trim(condition),''),nullif(trim(accessories),'')) returning computer_id into comp;
 insert into public.appointments(customer_id,computer_id,appointment_date,appointment_time,purpose,appointment_status)
 values(cid,comp,appointment_date,appointment_time,nullif(trim(purpose),''),'Pending') returning appointment_id into app;
 insert into public.repair_requests(computer_id,reported_problem,requested_work,repair_status)
 values(comp,trim(problem),nullif(trim(work),''),'Pending') returning repair_request_id into req;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details)
 values(req,auth.uid(),'submitted',jsonb_build_object('appointment_id',app));
 return req;
end $$;

-- Staff assignment is serialized on the technician row. 'Busy' or 'Unavailable'
-- disables acceptance; active statuses consume the one-job capacity.
create or replace function public.assign_crsm_technician(rid integer,tid integer)
returns void language plpgsql security definer set search_path = '' as $$
declare av text; active_count integer;
begin
 if not private.is_crsm_staff() then raise exception 'Staff access required'; end if;
 select availability into av from public.technicians where technician_id=tid for update;
 if not found then raise exception 'Technician not found'; end if;
 if lower(coalesce(av,'')) in ('busy','unavailable','off duty') then raise exception 'Technician is unavailable'; end if;
 select count(*) into active_count from public.repair_requests where technician_id=tid and repair_status in ('Confirmed','Under Diagnosis','Under Repair','Waiting for Parts','Ready for Testing','Ready for Pickup');
 if active_count >= 1 then raise exception 'Technician already has an active repair'; end if;
 update public.repair_requests set technician_id=tid,repair_status='Confirmed' where repair_request_id=rid and repair_status in ('Pending','Confirmed');
 if not found then raise exception 'Repair cannot be assigned in its current status'; end if;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details) values(rid,auth.uid(),'assigned',jsonb_build_object('technician_id',tid));
end $$;

create or replace function public.save_crsm_estimate(rid integer,base_amount numeric,options jsonb)
returns void language plpgsql security definer set search_path = '' as $$
declare item jsonb; comp integer;
begin
 if not private.is_crsm_staff() then raise exception 'Staff access required'; end if;
 if base_amount < 0 then raise exception 'Estimate cannot be negative'; end if;
 update public.repair_requests set estimated_cost=base_amount,repair_status='Under Diagnosis',customer_decision=null,customer_decision_at=null
  where repair_request_id=rid and repair_status in ('Confirmed','Under Diagnosis');
 if not found then raise exception 'Repair cannot be estimated in its current status'; end if;
 delete from public.repair_estimate_options where repair_request_id=rid;
 for item in select value from jsonb_array_elements(coalesce(options,'[]'::jsonb)) loop
  insert into public.repair_estimate_options(repair_request_id,option_name,description,price)
  values(rid,item->>'name',item->>'description',(item->>'price')::numeric);
 end loop;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details)
 values(rid,auth.uid(),'estimate_created',jsonb_build_object('base_amount',base_amount,'options',coalesce(options,'[]'::jsonb)));
end $$;

-- Customer may approve the base repair and any subset of optional items.
-- Declining pauses the repair in Pending; staff may clarify or close it.
create or replace function public.decide_crsm_estimate(rid integer,decision text,selected_options bigint[] default '{}')
returns void language plpgsql security definer set search_path = '' as $$
declare comp integer; current_status text; prior_decision text;
begin
 if decision not in ('APPROVE','DECLINE') then raise exception 'Decision must be APPROVE or DECLINE'; end if;
 select r.computer_id,r.repair_status,r.customer_decision into comp,current_status,prior_decision from public.repair_requests r where r.repair_request_id=rid;
 if comp is null or not private.owns_computer(comp) then raise exception 'Customer ownership required'; end if;
 if current_status<>'Under Diagnosis' or prior_decision is not null then raise exception 'This estimate is not awaiting a Customer decision'; end if;
 if decision='DECLINE' then
  update public.repair_requests set customer_decision='DECLINED',customer_decision_at=now(),repair_status='Pending' where repair_request_id=rid and estimated_cost is not null and repair_status='Under Diagnosis' and customer_decision is null;
 else
  if cardinality(coalesce(selected_options,'{}')) <> (select count(*) from public.repair_estimate_options o where o.repair_request_id=rid and o.option_id=any(coalesce(selected_options,'{}'))) then
   raise exception 'One or more selected options do not belong to this estimate';
  end if;
  update public.repair_estimate_options set selected=(option_id=any(coalesce(selected_options,'{}')) ) where repair_request_id=rid;
  update public.repair_requests set customer_decision='APPROVED',customer_decision_at=now(),repair_status='Under Repair'
   where repair_request_id=rid and estimated_cost is not null and repair_status='Under Diagnosis' and customer_decision is null;
 end if;
 if not found then raise exception 'No estimate is awaiting a decision'; end if;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details)
 values(rid,auth.uid(),'estimate_decision',jsonb_build_object('decision',decision,'selected_options',to_jsonb(selected_options)));
end $$;

insert into storage.buckets (id,name,public,file_size_limit,allowed_mime_types)
values('crsm-repair-photos','crsm-repair-photos',false,5242880,array['image/png','image/jpeg'])
on conflict(id) do update set public=false,file_size_limit=5242880,allowed_mime_types=array['image/png','image/jpeg'];
drop policy if exists crsm_repair_photo_upload_self on storage.objects;
create policy crsm_repair_photo_upload_self on storage.objects for insert to authenticated
with check(bucket_id='crsm-repair-photos' and private.current_crsm_role() is not null and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists crsm_repair_photo_read_owner_staff on storage.objects;
create policy crsm_repair_photo_read_owner_staff on storage.objects for select to authenticated
using(bucket_id='crsm-repair-photos' and ((private.current_crsm_role() is not null and (storage.foldername(name))[1]=auth.uid()::text) or private.is_crsm_staff()));
drop policy if exists crsm_repair_photo_update_self on storage.objects;
create policy crsm_repair_photo_update_self on storage.objects for update to authenticated
using(bucket_id='crsm-repair-photos' and private.current_crsm_role() is not null and (storage.foldername(name))[1]=auth.uid()::text)
with check(bucket_id='crsm-repair-photos' and private.current_crsm_role() is not null and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists crsm_repair_photo_delete_self on storage.objects;
create policy crsm_repair_photo_delete_self on storage.objects for delete to authenticated
using(bucket_id='crsm-repair-photos' and private.current_crsm_role() is not null and (storage.foldername(name))[1]=auth.uid()::text);

create or replace function public.confirm_crsm_pickup(rid integer)
returns void language plpgsql security definer set search_path = '' as $$
declare balance numeric;
begin
 if not private.is_crsm_staff() then raise exception 'Staff access required'; end if;
 select r.final_cost-coalesce(sum(p.amount_paid-p.refunded_amount),0) into balance from public.repair_requests r left join public.payments p using(repair_request_id)
 where r.repair_request_id=rid and r.repair_status='Ready for Pickup' group by r.final_cost;
 if balance is null or balance>0 then raise exception 'Repair must be fully paid and ready for pickup'; end if;
 update public.repair_requests set pickup_confirmed_at=now(),repair_status='Completed' where repair_request_id=rid and repair_status='Ready for Pickup';
 if not found then raise exception 'Repair must be paid and ready for pickup'; end if;
 insert into public.repair_history_events(repair_request_id,actor,event_type) values(rid,auth.uid(),'pickup_confirmed');
end $$;

create or replace function public.register_crsm_repair_photo(rid integer,path text)
returns void language plpgsql security definer set search_path = '' as $$
declare comp integer;
begin
 select computer_id into comp from public.repair_requests where repair_request_id=rid;
 if comp is null or not private.owns_computer(comp) then raise exception 'Customer ownership required'; end if;
 if split_part(path,'/',1) <> auth.uid()::text or split_part(path,'/',2) <> rid::text then raise exception 'Invalid photo path'; end if;
 insert into public.repair_photos(repair_request_id,object_path,uploaded_by) values(rid,path,auth.uid());
 insert into public.repair_history_events(repair_request_id,actor,event_type) values(rid,auth.uid(),'photo_added');
end $$;

create or replace function public.record_crsm_work(rid integer,p_diagnosis text,work text,parts text,cost numeric)
returns void language plpgsql security definer set search_path = '' as $$
begin
 if not private.is_crsm_staff() then raise exception 'Staff access required'; end if;
 if cost < 0 or nullif(trim(work),'') is null then raise exception 'Work description and nonnegative cost are required'; end if;
 update public.repair_requests set diagnosis=coalesce(nullif(trim(p_diagnosis),''),repair_requests.diagnosis),repair_status='Under Repair'
 where repair_request_id=rid and repair_status in ('Confirmed','Under Diagnosis','Under Repair','Waiting for Parts','Ready for Testing');
 if not found then raise exception 'Repair cannot accept work in its current status'; end if;
 insert into public.repair_work_entries(repair_request_id,actor,work_description,parts_used,cost)
 values(rid,auth.uid(),trim(work),nullif(trim(parts),''),cost);
 insert into public.repair_history_events(repair_request_id,actor,event_type,details)
 values(rid,auth.uid(),'work_recorded',jsonb_build_object('cost',cost));
end $$;

create or replace function public.finish_crsm_repair(rid integer,total_cost numeric)
returns void language plpgsql security definer set search_path = '' as $$
declare paid numeric;
begin
 if not private.is_crsm_staff() then raise exception 'Staff access required'; end if;
 if total_cost < 0 then raise exception 'Final cost cannot be negative'; end if;
 select coalesce(sum(amount_paid-refunded_amount),0) into paid from public.payments where repair_request_id=rid;
 update public.repair_requests set final_cost=total_cost,repair_status=case when paid>=total_cost then 'Ready for Pickup' else 'Awaiting Payment' end
 where repair_request_id=rid and repair_status in ('Under Repair','Ready for Testing','Awaiting Payment');
 if not found then raise exception 'Repair must be under repair or testing before completion'; end if;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details)
 values(rid,auth.uid(),'repair_finished',jsonb_build_object('final_cost',total_cost,'balance_due',greatest(total_cost-paid,0)));
end $$;

create or replace function public.record_crsm_payment(rid integer,amount numeric,method text)
returns text language plpgsql security definer set search_path = '' as $$
declare total_cost numeric; paid numeric; new_id integer; receipt text;
begin
 if not private.is_crsm_staff() then raise exception 'Staff access required'; end if;
 if amount<=0 or nullif(trim(method),'') is null then raise exception 'Positive amount and payment method are required'; end if;
 select final_cost into total_cost from public.repair_requests where repair_request_id=rid for update;
 if total_cost is null then raise exception 'Record final repair cost before taking payment'; end if;
 select coalesce(sum(amount_paid-refunded_amount),0) into paid from public.payments where repair_request_id=rid;
 if amount>total_cost-paid then raise exception 'Payment exceeds the outstanding balance'; end if;
 insert into public.payments(repair_request_id,amount_paid,payment_date,payment_status,payment_method)
 values(rid,amount,current_date,case when paid+amount>=total_cost then 'Paid' else 'Partial' end,trim(method)) returning payment_id into new_id;
 receipt='CRSM-'||to_char(current_date,'YYYYMMDD')||'-'||lpad(new_id::text,6,'0');
 update public.payments set receipt_number=receipt where payment_id=new_id;
 if paid+amount>=total_cost then update public.repair_requests set repair_status='Ready for Pickup' where repair_request_id=rid and repair_status='Awaiting Payment'; end if;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details)
 values(rid,auth.uid(),'payment_recorded',jsonb_build_object('amount',amount,'method',trim(method),'receipt_number',receipt));
 return receipt;
end $$;

create or replace function public.refund_crsm_payment(pid integer,amount numeric,reason text)
returns void language plpgsql security definer set search_path = '' as $$
declare repair_id integer; gross numeric; already_refunded numeric; total_cost numeric;
begin
 if not private.is_crsm_admin() then raise exception 'Administrator access required to issue refunds'; end if;
 if amount<=0 or nullif(trim(reason),'') is null then raise exception 'Positive refund amount and reason are required'; end if;
 select p.repair_request_id,p.amount_paid,p.refunded_amount into repair_id,gross,already_refunded
 from public.payments p where p.payment_id=pid for update;
 if repair_id is null then raise exception 'Payment not found'; end if;
 if amount>gross-already_refunded then raise exception 'Refund exceeds the remaining refundable amount'; end if;
 update public.payments set refunded_amount=refunded_amount+amount,refunded_at=now(),payment_status=case when refunded_amount+amount>=amount_paid then 'Unpaid' else 'Partial' end where payment_id=pid;
 insert into public.payment_refunds(payment_id,amount,reason,refunded_by) values(pid,amount,trim(reason),auth.uid());
 select final_cost into total_cost from public.repair_requests where repair_request_id=repair_id for update;
 if total_cost is not null and (select coalesce(sum(amount_paid-refunded_amount),0) from public.payments where repair_request_id=repair_id)<total_cost then
  update public.repair_requests set repair_status='Awaiting Payment' where repair_request_id=repair_id and repair_status='Ready for Pickup';
 end if;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details)
 values(repair_id,auth.uid(),'refund_recorded',jsonb_build_object('payment_id',pid,'amount',amount,'reason',trim(reason)));
end $$;

create or replace function public.request_crsm_estimate_review(rid integer,note text)
returns bigint language plpgsql security definer set search_path = '' as $$
declare comp integer; new_id bigint;
begin
 select computer_id into comp from public.repair_requests where repair_request_id=rid and customer_decision='DECLINED';
 if comp is null or not private.owns_computer(comp) then raise exception 'Only the owner of a declined estimate may request review'; end if;
 if nullif(trim(note),'') is null then raise exception 'Describe the estimate concern'; end if;
 if exists(select 1 from public.repair_estimate_reviews where repair_request_id=rid and review_status='Pending') then raise exception 'A review is already pending'; end if;
 insert into public.repair_estimate_reviews(repair_request_id,requested_by,customer_note)
 values(rid,auth.uid(),trim(note)) returning review_id into new_id;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details) values(rid,auth.uid(),'estimate_review_requested',jsonb_build_object('review_id',new_id));
 return new_id;
end $$;

create or replace function public.resolve_crsm_estimate_review(review bigint,outcome text,note text)
returns void language plpgsql security definer set search_path = '' as $$
declare rid integer;
begin
 if not private.is_crsm_admin() then raise exception 'Administrator access required'; end if;
 if outcome not in ('REQUOTE','NO_CHANGE','CLOSE') or nullif(trim(note),'') is null then raise exception 'Choose a valid outcome and provide an admin note'; end if;
 update public.repair_estimate_reviews set review_status=case outcome when 'REQUOTE' then 'Requote' when 'NO_CHANGE' then 'No Change' else 'Closed' end,
  admin_note=trim(note),reviewed_by=auth.uid(),reviewed_at=now()
 where review_id=review and review_status='Pending' returning repair_request_id into rid;
 if not found then raise exception 'Review is not pending'; end if;
 if outcome='REQUOTE' then update public.repair_requests set repair_status='Confirmed' where repair_request_id=rid and repair_status='Pending';
 elsif outcome='CLOSE' then update public.repair_requests set repair_status='Cancelled' where repair_request_id=rid and repair_status='Pending'; end if;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details) values(rid,auth.uid(),'estimate_review_resolved',jsonb_build_object('outcome',outcome,'note',trim(note)));
end $$;

revoke all on function public.complete_crsm_customer_profile(text,text), public.submit_crsm_repair_request(integer,text,text,text,text,text,text,text,text,date,time,text), public.assign_crsm_technician(integer,integer), public.save_crsm_estimate(integer,numeric,jsonb), public.decide_crsm_estimate(integer,text,bigint[]), public.confirm_crsm_pickup(integer), public.register_crsm_repair_photo(integer,text), public.record_crsm_work(integer,text,text,text,numeric), public.finish_crsm_repair(integer,numeric), public.record_crsm_payment(integer,numeric,text), public.refund_crsm_payment(integer,numeric,text), public.request_crsm_estimate_review(integer,text), public.resolve_crsm_estimate_review(bigint,text,text) from public,anon,authenticated;
grant execute on function public.complete_crsm_customer_profile(text,text), public.submit_crsm_repair_request(integer,text,text,text,text,text,text,text,text,date,time,text), public.assign_crsm_technician(integer,integer), public.save_crsm_estimate(integer,numeric,jsonb), public.decide_crsm_estimate(integer,text,bigint[]), public.confirm_crsm_pickup(integer), public.register_crsm_repair_photo(integer,text), public.record_crsm_work(integer,text,text,text,numeric), public.finish_crsm_repair(integer,numeric), public.record_crsm_payment(integer,numeric,text) to authenticated;
grant execute on function public.refund_crsm_payment(integer,numeric,text) to authenticated;
grant execute on function public.request_crsm_estimate_review(integer,text), public.resolve_crsm_estimate_review(bigint,text,text) to authenticated;
revoke all on function public.promote_crsm_user(uuid,text), public.set_crsm_account_active(uuid,boolean), public.record_crsm_login_activity(), public.link_crsm_technician_account(integer,uuid), public.link_crsm_customer_record(integer,uuid) from public,anon,authenticated;
grant execute on function public.promote_crsm_user(uuid,text), public.set_crsm_account_active(uuid,boolean), public.record_crsm_login_activity(), public.link_crsm_technician_account(integer,uuid), public.link_crsm_customer_record(integer,uuid) to authenticated;
revoke update on public.crsm_profiles from authenticated;
grant update(full_name,username,avatar_path) on public.crsm_profiles to authenticated;
commit;
