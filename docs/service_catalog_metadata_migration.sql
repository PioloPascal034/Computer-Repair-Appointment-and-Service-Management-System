-- Adds searchable catalog metadata without changing service IDs, prices, or relationships.
begin;

alter table public.repair_services add column if not exists category text;
alter table public.repair_services add column if not exists common_problems text;

update public.repair_services
set category = case
    when lower(service_name) like '%diagnos%' then 'Diagnostics'
    when lower(service_name) like '%clean%' then 'Cleaning'
    when lower(service_name) like '%screen%' or lower(service_name) like '%display%' then 'Display'
    when lower(service_name) like '%battery%' or lower(service_name) like '%power%' then 'Battery and Power'
    when lower(service_name) like '%keyboard%' or lower(service_name) like '%input%' then 'Keyboard and Input'
    when lower(service_name) like '%storage%' or lower(service_name) like '%data%' then 'Storage and Data Recovery'
    when lower(service_name) like '%network%' or lower(service_name) like '%wifi%' or lower(service_name) like '%internet%' then 'Network and Connectivity'
    when lower(service_name) like '%hardware%' or lower(service_name) like '%replacement%' then 'Hardware Repair'
    else 'Software'
end
where category is null or btrim(category) = '';

update public.repair_services
set common_problems = case
    when lower(service_name) like '%diagnos%' then 'Unknown fault; slow performance; unexpected shutdowns; no power'
    when lower(service_name) like '%clean%' then 'Dust buildup; overheating; fan noise; maintenance'
    when lower(service_name) like '%screen%' or lower(service_name) like '%display%' then 'Cracked screen; no display; flickering; dead pixels'
    when lower(service_name) like '%battery%' or lower(service_name) like '%power%' then 'Battery not charging; short battery life; power failure'
    when lower(service_name) like '%keyboard%' or lower(service_name) like '%input%' then 'Unresponsive keys; damaged keyboard; input problems'
    when lower(service_name) like '%storage%' or lower(service_name) like '%data%' then 'Full drive; slow storage; missing or inaccessible files'
    when lower(service_name) like '%network%' or lower(service_name) like '%wifi%' or lower(service_name) like '%internet%' then 'Cannot connect; unstable Wi-Fi; network configuration'
    else service_name
end
where common_problems is null or btrim(common_problems) = '';

commit;
