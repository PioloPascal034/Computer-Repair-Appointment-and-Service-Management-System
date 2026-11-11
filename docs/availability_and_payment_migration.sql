-- Apply after profile_and_photo_access_migration.sql. No external payment gateway.
begin;
-- A finished repair no longer consumes a technical work slot, even if unpaid.
create or replace function private.crsm_technical_status(status text)
returns boolean language sql immutable set search_path='' as $$
 select status in ('Pending','Confirmed','Under Diagnosis','Under Repair','Waiting for Parts','Ready for Testing')
$$;
create or replace function public.list_crsm_technician_availability()
returns jsonb language plpgsql stable security definer set search_path='' as $$
declare result jsonb;
begin
 if not private.is_crsm_staff() then raise exception 'Staff access required'; end if;
 select coalesce(jsonb_agg(to_jsonb(x) order by x.technician_name),'[]'::jsonb) into result from (
 select t.technician_id,t.technician_name,t.specialization,t.contact_number,t.availability as duty_status,
 (select count(*) from public.repair_requests r where r.technician_id=t.technician_id and private.crsm_technical_status(r.repair_status)) as active_repairs,
 case when lower(trim(coalesce(t.availability,'')))<>'available' then coalesce(t.availability,'Unavailable')
 when t.auth_user_id is not null and not exists(select 1 from public.crsm_profiles p where p.id=t.auth_user_id and p.is_active and p.role='STAFF') then 'Unavailable'
 when exists(select 1 from public.repair_requests r where r.technician_id=t.technician_id and private.crsm_technical_status(r.repair_status)) then 'Busy' else 'Available' end as availability,
 lower(trim(coalesce(t.availability,'')))='available'
 and (t.auth_user_id is null or exists(select 1 from public.crsm_profiles p where p.id=t.auth_user_id and p.is_active and p.role='STAFF'))
 and not exists(select 1 from public.repair_requests r where r.technician_id=t.technician_id and private.crsm_technical_status(r.repair_status)) as can_assign
 from public.technicians t) x;
 return result;
end $$;
-- Enforce capacity even for legacy/direct SQL edits, using the technician as mutex.
create or replace function private.guard_crsm_technician_capacity()
returns trigger language plpgsql security definer set search_path='' as $$
declare tech public.technicians;
begin
 if new.technician_id is null or not private.crsm_technical_status(new.repair_status) then return new; end if;
 if TG_OP='UPDATE' then
  if old.technician_id is not distinct from new.technician_id and private.crsm_technical_status(old.repair_status) then return new; end if;
 end if;
 select * into tech from public.technicians where technician_id=new.technician_id for update;
 if not found or lower(trim(coalesce(tech.availability,'')))<>'available' then raise exception 'Technician is not accepting new repairs'; end if;
 if tech.auth_user_id is not null and not exists(select 1 from public.crsm_profiles p where p.id=tech.auth_user_id and p.is_active and p.role='STAFF') then raise exception 'Technician account is inactive or no longer Staff'; end if;
 if exists(select 1 from public.repair_requests r where r.technician_id=new.technician_id and r.repair_request_id<>new.repair_request_id and private.crsm_technical_status(r.repair_status)) then raise exception 'Technician already has active technical work'; end if;
 return new;
end $$;
drop trigger if exists crsm_technician_capacity on public.repair_requests;
create trigger crsm_technician_capacity before insert or update of technician_id,repair_status on public.repair_requests
 for each row execute function private.guard_crsm_technician_capacity();
create or replace function public.assign_crsm_technician(rid integer,tid integer)
returns void language plpgsql security definer set search_path='' as $$
declare repair public.repair_requests;
begin
 if not private.is_crsm_admin() then raise exception 'Admin assigns technicians'; end if;
 select * into repair from public.repair_requests where repair_request_id=rid for update;
 if not found or repair.repair_status not in ('Pending','Confirmed') then raise exception 'Repair cannot be assigned in its current status'; end if;
 if repair.technician_id=tid then return; end if;
 update public.repair_requests set technician_id=tid,repair_status='Confirmed' where repair_request_id=rid;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details) values(rid,auth.uid(),'assigned',jsonb_build_object('technician_id',tid));
end $$;
create or replace function public.get_crsm_repair_bill(rid integer)
returns jsonb language plpgsql stable security definer set search_path='' as $$
declare repair public.repair_requests; paid numeric; history jsonb;
begin
 if not private.can_view_crsm_repair(rid) then raise exception 'Repair access denied'; end if;
 select * into repair from public.repair_requests where repair_request_id=rid;
 if not found then raise exception 'Repair not found'; end if;
 select coalesce(sum(p.amount_paid-p.refunded_amount),0),coalesce(jsonb_agg(to_jsonb(p) order by p.payment_id),'[]'::jsonb)
 into paid,history from public.payments p where p.repair_request_id=rid;
 return jsonb_build_object('repair_request_id',rid,'repair_status',repair.repair_status,'final_cost',repair.final_cost,'net_paid',paid,
 'balance_due',case when repair.final_cost is null then null else greatest(repair.final_cost-paid,0) end,
 'payment_status',case when repair.final_cost is null then 'Not finalized' when paid>=repair.final_cost then 'Paid' when paid>0 then 'Partial' else 'Unpaid' end,
 'can_pay',repair.final_cost is not null and repair.repair_status='Awaiting Payment' and paid<repair.final_cost,'payments',history);
end $$;
-- Lock the repair before reading payments; finish/payment operations share this lock.
create or replace function public.finish_crsm_repair(rid integer,total_cost numeric)
returns void language plpgsql security definer set search_path='' as $$
declare repair public.repair_requests; paid numeric;
begin
 if not private.is_crsm_staff() or not private.can_view_crsm_repair(rid) then raise exception 'Assigned Technician or Admin required'; end if;
 if total_cost is null or total_cost::text in ('NaN','Infinity','-Infinity') or total_cost<0 or total_cost>99999999.99 or total_cost<>round(total_cost,2) then raise exception 'Enter a valid nonnegative final cost with at most two decimals'; end if;
 select * into repair from public.repair_requests where repair_request_id=rid for update;
 if not found or repair.repair_status not in ('Under Repair','Ready for Testing','Awaiting Payment') then raise exception 'Repair must be under repair or testing before completion'; end if;
 select coalesce(sum(amount_paid-refunded_amount),0) into paid from public.payments where repair_request_id=rid;
 if total_cost<paid then raise exception 'Final cost cannot be less than net payments received'; end if;
 update public.repair_requests set final_cost=total_cost,repair_status=case when paid>=total_cost then 'Ready for Pickup' else 'Awaiting Payment' end where repair_request_id=rid;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details) values(rid,auth.uid(),'repair_finished',jsonb_build_object('final_cost',total_cost,'balance_due',total_cost-paid));
end $$;
create or replace function public.record_crsm_payment(rid integer,amount numeric,method text)
returns text language plpgsql security definer set search_path='' as $$
declare repair public.repair_requests; paid numeric; new_id integer; receipt text;
begin
 if not private.is_crsm_staff() or not private.can_view_crsm_repair(rid) then raise exception 'Assigned Technician or Admin required to record received payments'; end if;
 if amount is null or amount::text in ('NaN','Infinity','-Infinity') or amount<=0 or amount>99999999.99 or amount<>round(amount,2) then raise exception 'Enter a positive amount with at most two decimals'; end if;
 if method is null or method not in ('Cash','GCash','Bank transfer','Card') then raise exception 'Choose a supported payment method'; end if;
 select * into repair from public.repair_requests where repair_request_id=rid for update;
 if not found or repair.final_cost is null or repair.repair_status<>'Awaiting Payment' then raise exception 'An unpaid finalized repair bill is required'; end if;
 select coalesce(sum(amount_paid-refunded_amount),0) into paid from public.payments where repair_request_id=rid;
 if amount>repair.final_cost-paid then raise exception 'Payment exceeds the outstanding balance. Refresh the bill before retrying'; end if;
 insert into public.payments(repair_request_id,amount_paid,payment_date,payment_status,payment_method)
 values(rid,amount,current_date,case when paid+amount>=repair.final_cost then 'Paid' else 'Partial' end,method) returning payment_id into new_id;
 receipt='CRSM-'||to_char(current_date,'YYYYMMDD')||'-'||lpad(new_id::text,6,'0');
 update public.payments set receipt_number=receipt where payment_id=new_id;
 if paid+amount=repair.final_cost then update public.repair_requests set repair_status='Ready for Pickup' where repair_request_id=rid; end if;
 insert into public.repair_history_events(repair_request_id,actor,event_type,details) values(rid,auth.uid(),'payment_recorded',jsonb_build_object('amount',amount,'method',method,'receipt_number',receipt));
 return receipt;
end $$;
create or replace function public.confirm_crsm_pickup(rid integer)
returns void language plpgsql security definer set search_path='' as $$
declare repair public.repair_requests; paid numeric;
begin
 if not private.is_crsm_staff() or not private.can_view_crsm_repair(rid) then raise exception 'Assigned Technician or Admin required'; end if;
 select * into repair from public.repair_requests where repair_request_id=rid for update;
 if not found or repair.repair_status<>'Ready for Pickup' or repair.final_cost is null then raise exception 'Repair is not ready for pickup'; end if;
 select coalesce(sum(amount_paid-refunded_amount),0) into paid from public.payments where repair_request_id=rid;
 if paid<repair.final_cost then raise exception 'Settle the outstanding balance before pickup'; end if;
 update public.repair_requests set repair_status='Completed',pickup_confirmed_at=now() where repair_request_id=rid;
 insert into public.repair_history_events(repair_request_id,actor,event_type) values(rid,auth.uid(),'pickup_confirmed');
end $$;
revoke all on function private.crsm_technical_status(text),private.guard_crsm_technician_capacity(),public.list_crsm_technician_availability(),public.get_crsm_repair_bill(integer) from public,anon;
grant execute on function public.list_crsm_technician_availability(),public.get_crsm_repair_bill(integer) to authenticated;
revoke all on function public.assign_crsm_technician(integer,integer),public.finish_crsm_repair(integer,numeric),public.record_crsm_payment(integer,numeric,text),public.confirm_crsm_pickup(integer) from public,anon;
grant execute on function public.assign_crsm_technician(integer,integer),public.finish_crsm_repair(integer,numeric),public.record_crsm_payment(integer,numeric,text),public.confirm_crsm_pickup(integer) to authenticated;
commit;
