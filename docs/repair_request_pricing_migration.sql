-- Device-aware repair estimates and explicit replacement-part pricing.
-- Run after customer_workflow_migration.sql, customer_catalog_and_demo_seed.sql,
-- and device_specification_migration.sql.
-- No part prices are seeded here: only verified business prices should be entered.

begin;

alter table public.repair_services
  add column if not exists requires_replacement_part boolean not null default false;

update public.repair_services
set requires_replacement_part = true
where lower(service_name) like '%screen%replacement%'
   or lower(service_name) like '%display%replacement%'
   or lower(service_name) like '%battery%replacement%'
   or lower(service_name) like '%keyboard%replacement%';

create table if not exists public.repair_service_part_prices (
  part_price_id bigint generated always as identity primary key,
  service_id integer not null references public.repair_services(service_id) on delete cascade,
  device_type text not null,
  brand text,
  model text,
  specification text,
  part_name text not null,
  part_price numeric(10,2) not null check (part_price >= 0),
  active boolean not null default true
);

create unique index if not exists repair_service_part_prices_compatibility_unique
  on public.repair_service_part_prices (
    service_id,
    lower(btrim(device_type)),
    lower(btrim(coalesce(brand, ''))),
    lower(btrim(coalesce(model, ''))),
    lower(btrim(coalesce(specification, ''))),
    lower(btrim(part_name))
  );
create index if not exists repair_service_part_prices_lookup
  on public.repair_service_part_prices (service_id, lower(btrim(device_type)))
  where active;

alter table public.repair_service_part_prices enable row level security;
revoke all on public.repair_service_part_prices from anon, authenticated;
grant select on public.repair_service_part_prices to authenticated;
drop policy if exists repair_service_part_prices_read_active on public.repair_service_part_prices;
create policy repair_service_part_prices_read_active
  on public.repair_service_part_prices for select to authenticated
  using (active and private.current_crsm_role() is not null);

-- Returns a total only when a compatible estimate can be built from configured
-- service fees and (for replacement services) one unambiguous part-price record.
create or replace function public.quote_crsm_service(
  p_service_id integer,
  p_device_type text,
  p_brand text,
  p_model text,
  p_specification text
)
returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  base_fee numeric(10,2);
  requires_part boolean;
  best_specificity integer;
  best_count integer;
  part_name text;
  part_price numeric(10,2);
  total numeric(10,2);
begin
  if private.current_crsm_role() is null then
    raise exception 'Active CRSM account required';
  end if;
  if p_service_id is null then
    return jsonb_build_object('status','inspection_required','amount',null,'base_fee',null,'part_name',null,'part_price',null,'message','Choose a service to calculate an estimate.');
  end if;

  select coalesce(dp.service_price, rs.service_price), rs.requires_replacement_part
    into base_fee, requires_part
  from public.repair_services rs
  left join public.repair_service_device_prices dp
    on dp.service_id = rs.service_id and lower(btrim(dp.device_type)) = lower(btrim(coalesce(p_device_type,'')))
  where rs.service_id = p_service_id;

  if not found then
    raise exception 'Selected service does not exist';
  end if;
  if nullif(btrim(coalesce(p_device_type,'')), '') is null
     or nullif(btrim(coalesce(p_brand,'')), '') is null
     or nullif(btrim(coalesce(p_model,'')), '') is null then
    return jsonb_build_object('status','inspection_required','amount',null,'base_fee',base_fee,'part_name',null,'part_price',null,'message','Select the device type, brand, and model to calculate an estimate.');
  end if;

  if requires_part then
    select max((case when p.brand is not null then 1 else 0 end)
             + (case when p.model is not null then 1 else 0 end)
             + (case when p.specification is not null then 1 else 0 end))
      into best_specificity
    from public.repair_service_part_prices p
    where p.active and p.service_id = p_service_id
      and lower(btrim(p.device_type)) = lower(btrim(p_device_type))
      and (p.brand is null or lower(btrim(p.brand)) = lower(btrim(p_brand)))
      and (p.model is null or lower(btrim(p.model)) = lower(btrim(p_model)))
      and (p.specification is null or lower(btrim(p.specification)) = lower(btrim(coalesce(p_specification,''))));

    if best_specificity is null then
      return jsonb_build_object('status','inspection_required','amount',null,'base_fee',base_fee,'part_name',null,'part_price',null,'message','No compatible replacement-part price is on file. A technician must inspect the device before a total estimate is available.');
    end if;

    select count(*), min(p.part_name), min(p.part_price)
      into best_count, part_name, part_price
    from public.repair_service_part_prices p
    where p.active and p.service_id = p_service_id
      and lower(btrim(p.device_type)) = lower(btrim(p_device_type))
      and (p.brand is null or lower(btrim(p.brand)) = lower(btrim(p_brand)))
      and (p.model is null or lower(btrim(p.model)) = lower(btrim(p_model)))
      and (p.specification is null or lower(btrim(p.specification)) = lower(btrim(coalesce(p_specification,''))))
      and ((case when p.brand is not null then 1 else 0 end)
         + (case when p.model is not null then 1 else 0 end)
         + (case when p.specification is not null then 1 else 0 end)) = best_specificity;

    if best_count <> 1 then
      return jsonb_build_object('status','inspection_required','amount',null,'base_fee',base_fee,'part_name',null,'part_price',null,'message','More than one compatible replacement-part price matches this device. A technician must confirm the part and final estimate.');
    end if;
    total := base_fee + part_price;
  else
    total := base_fee;
  end if;

  return jsonb_build_object('status','estimated','amount',total,'base_fee',base_fee,'part_name',part_name,'part_price',part_price,'message','Estimate only; final cost is confirmed after hands-on inspection.');
end;
$$;

revoke all on function public.quote_crsm_service(integer,text,text,text,text) from public, anon, authenticated;
grant execute on function public.quote_crsm_service(integer,text,text,text,text) to authenticated;

-- Replace the earlier request function so the same database-side quote shown in
-- the form is stored in estimated_cost. final_cost remains null until staff set it.
drop function if exists public.submit_crsm_repair_request_v2(integer,text,text,text,text,text,text,text,text,text,date,time,text,integer);
create or replace function public.submit_crsm_repair_request_v2(
  cid integer, device_type text, brand text, model text, specification text,
  serial text, problem text, work text, condition text, accessories text,
  appointment_date date, appointment_time time, purpose text, selected_service integer
)
returns integer
language plpgsql security definer set search_path = '' as $$
declare
  comp integer;
  app integer;
  req integer;
  request_estimate numeric(10,2);
  service_quote jsonb;
begin
  if private.current_crsm_role() is distinct from 'CUSTOMER' then
    raise exception 'Active Customer account required';
  end if;
  if not exists (select 1 from public.customers where customer_id = cid and auth_user_id = auth.uid()) then
    raise exception 'Customer ownership required';
  end if;
  if selected_service is not null and not exists (select 1 from public.repair_services where service_id = selected_service) then
    raise exception 'Selected service does not exist';
  end if;
  if appointment_date < current_date then
    raise exception 'Appointment must be today or later';
  end if;

  if selected_service is not null then
    service_quote := public.quote_crsm_service(selected_service, device_type, brand, model, specification);
    request_estimate := nullif(service_quote->>'amount','')::numeric;
  end if;

  insert into public.computers(customer_id,device_type,brand,model,specification,serial_number,reported_problem,condition_on_intake,accessories_received)
  values(cid,trim(device_type),trim(brand),nullif(trim(model),''),nullif(trim(specification),''),nullif(trim(serial),''),trim(problem),nullif(trim(condition),''),nullif(trim(accessories),''))
  returning computer_id into comp;
  insert into public.appointments(customer_id,computer_id,appointment_date,appointment_time,purpose,appointment_status)
  values(cid,comp,appointment_date,appointment_time,nullif(trim(purpose),''),'Pending')
  returning appointment_id into app;
  insert into public.repair_requests(computer_id,service_id,reported_problem,requested_work,repair_status,estimated_cost)
  values(comp,selected_service,trim(problem),nullif(trim(work),''),'Pending',request_estimate)
  returning repair_request_id into req;
  insert into public.repair_history_events(repair_request_id,actor,event_type,details)
  values(req,auth.uid(),'submitted',jsonb_build_object('appointment_id',app,'service_id',selected_service,'estimated_cost',request_estimate));
  return req;
end;
$$;

revoke all on function public.submit_crsm_repair_request_v2(integer,text,text,text,text,text,text,text,text,text,date,time,text,integer) from public, anon, authenticated;
grant execute on function public.submit_crsm_repair_request_v2(integer,text,text,text,text,text,text,text,text,text,date,time,text,integer) to authenticated;

notify pgrst, 'reload schema';
commit;
