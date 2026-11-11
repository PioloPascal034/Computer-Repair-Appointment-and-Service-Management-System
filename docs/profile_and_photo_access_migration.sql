-- Apply after customer_workflow_migration.sql. Account edits are atomic and self-only.
begin;
alter table public.crsm_profiles add column if not exists contact_number varchar(20);
alter table public.crsm_profiles add column if not exists address varchar(255);
create or replace function public.get_crsm_my_account()
returns jsonb language plpgsql security definer set search_path='' as $$
declare result jsonb;
begin
 if private.current_crsm_role() is null then raise exception 'Active account required'; end if;
 select jsonb_build_object('full_name',p.full_name,'username',p.username,'email',p.email,
 'contact_number',coalesce(c.contact_number,t.contact_number,p.contact_number,''),
 'address',coalesce(c.address,p.address,'')) into result
 from public.crsm_profiles p left join public.customers c on c.auth_user_id=p.id
 left join public.technicians t on t.auth_user_id=p.id where p.id=auth.uid();
 return result;
end $$;
create or replace function public.update_crsm_my_account(p_name text,p_username text,p_phone text,p_address text)
returns void language plpgsql security definer set search_path='' as $$
begin
 if private.current_crsm_role() is null then raise exception 'Active account required'; end if;
 if nullif(trim(p_name),'') is null or length(trim(p_name))>100 then raise exception 'Enter a full name of up to 100 characters'; end if;
 if nullif(trim(p_username),'') is null or length(trim(p_username))>50 then raise exception 'Enter a username of up to 50 characters'; end if;
 if p_phone is null or (trim(p_phone)<>'' and (p_phone !~ '^[+0-9() .-]{8,20}$' or length(regexp_replace(p_phone,'[^0-9]','','g'))<8)) then raise exception 'Invalid contact number'; end if;
 if private.current_crsm_role()='CUSTOMER' and trim(p_phone)='' then raise exception 'Contact number required'; end if;
 if p_address is null or length(p_address)>255 then raise exception 'Address must be at most 255 characters'; end if;
 update public.crsm_profiles set full_name=trim(p_name),username=trim(p_username),contact_number=trim(p_phone),address=trim(p_address) where id=auth.uid();
 update public.customers set customer_name=trim(p_name),contact_number=trim(p_phone),address=trim(p_address) where auth_user_id=auth.uid();
 update public.technicians set technician_name=trim(p_name),contact_number=trim(p_phone) where auth_user_id=auth.uid();
end $$;
-- Security-definer helper avoids recursive RLS and checks current assignment on every read.
create or replace function private.can_view_crsm_repair(rid integer)
returns boolean language sql stable security definer set search_path='' as $$
 select coalesce(private.current_crsm_role()='ADMIN' or exists(
 select 1 from public.repair_requests r left join public.technicians t on t.technician_id=r.technician_id
 join public.computers c on c.computer_id=r.computer_id join public.customers u on u.customer_id=c.customer_id
 where r.repair_request_id=rid and ((private.current_crsm_role()='CUSTOMER' and u.auth_user_id=auth.uid())
 or (private.current_crsm_role()='STAFF' and t.auth_user_id=auth.uid()))),false)
$$;
drop policy if exists crsm_photos_owner_select on public.repair_photos;
create policy crsm_photos_owner_select on public.repair_photos for select to authenticated
 using(private.can_view_crsm_repair(repair_request_id));
drop policy if exists crsm_repair_photo_read_owner_staff on storage.objects;
create policy crsm_repair_photo_read_owner_staff on storage.objects for select to authenticated
 using(bucket_id='crsm-repair-photos' and exists(select 1 from public.repair_photos p
 where p.object_path=name and private.can_view_crsm_repair(p.repair_request_id)));
revoke all on function public.get_crsm_my_account(),public.update_crsm_my_account(text,text,text,text),private.can_view_crsm_repair(integer) from public,anon;
grant execute on function public.get_crsm_my_account(),public.update_crsm_my_account(text,text,text,text),private.can_view_crsm_repair(integer) to authenticated;
commit;
