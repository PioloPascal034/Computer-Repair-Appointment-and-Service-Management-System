-- Security hardening for the CRSM schema created by the earlier setup script.
-- Run in Supabase SQL Editor. Review on a copy/branch first; does not drop data.
-- Requires PostgreSQL 15+ for security_invoker views (Supabase projects are PG15+).

begin;

-- Prevent the seven report views from running with their owner's privileges.
alter view public.active_repair_status set (security_invoker = true);
alter view public.appointment_schedule set (security_invoker = true);
alter view public.repair_history set (security_invoker = true);
alter view public.technician_workload set (security_invoker = true);
alter view public.repair_revenue set (security_invoker = true);
alter view public.pending_repairs set (security_invoker = true);
alter view public.most_requested_services set (security_invoker = true);

-- Keep SECURITY DEFINER helpers outside the exposed public API schema.
create schema if not exists private;
revoke all on schema private from public;
revoke all on schema private from anon;
grant usage on schema private to authenticated;

create or replace function private.current_crsm_role()
returns text language sql stable security definer set search_path = ''
as $$ select p.role from public.crsm_profiles p where p.id = auth.uid() $$;

create or replace function private.is_crsm_admin()
returns boolean language sql stable security definer set search_path = ''
as $$ select coalesce(private.current_crsm_role() = 'ADMIN', false) $$;

revoke all on function private.current_crsm_role() from public, anon;
revoke all on function private.is_crsm_admin() from public, anon;
grant execute on function private.current_crsm_role() to authenticated;
grant execute on function private.is_crsm_admin() to authenticated;

-- Move the signup trigger to the private function. It always sets CUSTOMER.
create or replace function private.create_crsm_profile_for_new_user()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
    insert into public.crsm_profiles (id, email, full_name, username, role)
    values (
        new.id,
        new.email,
        coalesce(nullif(new.raw_user_meta_data ->> 'full_name', ''), split_part(new.email, '@', 1)),
        coalesce(nullif(new.raw_user_meta_data ->> 'username', ''), split_part(new.email, '@', 1) || '_' || left(new.id::text, 8)),
        'CUSTOMER'
    );
    return new;
end;
$$;

drop trigger if exists crsm_create_profile_after_signup on auth.users;
create trigger crsm_create_profile_after_signup after insert on auth.users
for each row execute procedure private.create_crsm_profile_for_new_user();
revoke all on function private.create_crsm_profile_for_new_user() from public, anon, authenticated;

-- Replace the prior SECURITY DEFINER RPCs with invoker functions. Their RLS
-- policies enforce the admin check, so they do not need owner privileges.
create or replace function public.promote_crsm_user(target_user uuid, target_role text)
returns void language plpgsql security invoker set search_path = ''
as $$
begin
    if not private.is_crsm_admin() then raise exception 'Only an administrator may promote accounts'; end if;
    if target_role not in ('STAFF', 'ADMIN') then raise exception 'Promotion role must be STAFF or ADMIN'; end if;
    update public.crsm_profiles set role = target_role where id = target_user;
    if not found then raise exception 'Target account was not found'; end if;
end;
$$;

create or replace function public.set_crsm_business_logo(logo_path text)
returns void language plpgsql security invoker set search_path = ''
as $$
begin
    if not private.is_crsm_admin() then raise exception 'Only an administrator may change the business logo'; end if;
    insert into public.crsm_settings (setting_key, setting_value, updated_at)
    values ('business_logo_path', logo_path, now())
    on conflict (setting_key) do update set setting_value = excluded.setting_value, updated_at = now();
end;
$$;

-- Rebuild CRSM policies to use the private helpers and keep profile/settings
-- writes restricted to self or Admin as appropriate.
drop policy if exists crsm_profiles_read_self_or_admin on public.crsm_profiles;
create policy crsm_profiles_read_self_or_admin on public.crsm_profiles for select to authenticated
using (id = auth.uid() or private.is_crsm_admin());

drop policy if exists crsm_profiles_update_self on public.crsm_profiles;
create policy crsm_profiles_update_self on public.crsm_profiles for update to authenticated
using (id = auth.uid()) with check (id = auth.uid() and role = private.current_crsm_role());

drop policy if exists crsm_profiles_admin_update on public.crsm_profiles;
create policy crsm_profiles_admin_update on public.crsm_profiles for update to authenticated
using (private.is_crsm_admin()) with check (private.is_crsm_admin());

drop policy if exists crsm_settings_read_authenticated on public.crsm_settings;
create policy crsm_settings_read_authenticated on public.crsm_settings for select to authenticated using (true);
drop policy if exists crsm_settings_admin_insert on public.crsm_settings;
create policy crsm_settings_admin_insert on public.crsm_settings for insert to authenticated
with check (private.is_crsm_admin());
drop policy if exists crsm_settings_admin_update on public.crsm_settings;
create policy crsm_settings_admin_update on public.crsm_settings for update to authenticated
using (private.is_crsm_admin()) with check (private.is_crsm_admin());

drop policy if exists crsm_technicians_read_staff_admin on public.technicians;
create policy crsm_technicians_read_staff_admin on public.technicians for select to authenticated
using (private.current_crsm_role() in ('STAFF', 'ADMIN'));

-- Storage policies use the private role helper as well.
drop policy if exists crsm_avatar_read_self_or_admin on storage.objects;
create policy crsm_avatar_read_self_or_admin on storage.objects for select to authenticated
using (bucket_id = 'crsm-profile-images' and ((storage.foldername(name))[1] = auth.uid()::text or private.is_crsm_admin()));

drop policy if exists crsm_brand_insert_admin on storage.objects;
create policy crsm_brand_insert_admin on storage.objects for insert to authenticated
with check (bucket_id = 'crsm-brand-assets' and private.is_crsm_admin());
drop policy if exists crsm_brand_update_admin on storage.objects;
create policy crsm_brand_update_admin on storage.objects for update to authenticated
using (bucket_id = 'crsm-brand-assets' and private.is_crsm_admin())
with check (bucket_id = 'crsm-brand-assets' and private.is_crsm_admin());
drop policy if exists crsm_brand_delete_admin on storage.objects;
create policy crsm_brand_delete_admin on storage.objects for delete to authenticated
using (bucket_id = 'crsm-brand-assets' and private.is_crsm_admin());

-- Remove API execution grants from the old public SECURITY DEFINER helpers and
-- trigger function. They are no longer called by CRSM policies or triggers.
revoke execute on function public.current_crsm_role() from public, anon, authenticated;
revoke execute on function public.is_crsm_admin() from public, anon, authenticated;
revoke execute on function public.create_crsm_profile_for_new_user() from public, anon, authenticated;

-- The Advisor screenshot also shows public.rls_auto_enable(). The project
-- setup does not create it; if present, prevent API roles from invoking it
-- directly. This leaves event triggers intact; the database owner retains
-- owner privileges.
do $$
begin
    if to_regprocedure('public.rls_auto_enable()') is not null then
        execute 'revoke execute on function public.rls_auto_enable() from public, anon, authenticated';
    end if;
end;
$$;

-- RPCs remain callable only by signed-in users; internal RLS/admin checks apply.
revoke execute on function public.promote_crsm_user(uuid, text) from public, anon, authenticated;
grant execute on function public.promote_crsm_user(uuid, text) to authenticated;
revoke execute on function public.set_crsm_business_logo(text) from public, anon, authenticated;
grant execute on function public.set_crsm_business_logo(text) to authenticated;

grant select, insert, update on public.crsm_settings to authenticated;
revoke all on public.crsm_settings from anon;

commit;

-- The existing RLS-enabled business tables without policies remain deny-by-
-- default to Supabase API roles. Do not add broad policies just to silence the
-- Advisor; first define account-to-customer ownership and role permissions.
