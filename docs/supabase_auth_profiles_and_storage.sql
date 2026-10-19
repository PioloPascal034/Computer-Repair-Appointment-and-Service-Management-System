-- Additive Supabase setup for customer-first accounts, administrator promotion,
-- profile photos, and the business logo. Review and run in the Supabase SQL editor.
-- This does not alter the existing seven repair-domain tables.

create table if not exists public.crsm_profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    email text not null unique,
    full_name text not null,
    username text not null unique,
    role text not null default 'CUSTOMER'
        check (role in ('CUSTOMER', 'STAFF', 'ADMIN')),
    avatar_path text,
    created_at timestamptz not null default now()
);

create table if not exists public.crsm_settings (
    setting_key text primary key,
    setting_value text,
    updated_at timestamptz not null default now()
);

create or replace function public.current_crsm_role()
returns text
language sql
stable
security definer
set search_path = ''
as $$
    select p.role from public.crsm_profiles p where p.id = auth.uid()
$$;

create or replace function public.is_crsm_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select coalesce(public.current_crsm_role() = 'ADMIN', false)
$$;

create or replace function public.create_crsm_profile_for_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
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
create trigger crsm_create_profile_after_signup
    after insert on auth.users
    for each row execute procedure public.create_crsm_profile_for_new_user();

create or replace function public.promote_crsm_user(target_user uuid, target_role text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not public.is_crsm_admin() then
        raise exception 'Only an administrator may promote accounts';
    end if;
    if target_role not in ('STAFF', 'ADMIN') then
        raise exception 'Promotion role must be STAFF or ADMIN';
    end if;
    update public.crsm_profiles set role = target_role where id = target_user;
    if not found then raise exception 'Target account was not found'; end if;
end;
$$;

create or replace function public.set_crsm_business_logo(logo_path text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not public.is_crsm_admin() then
        raise exception 'Only an administrator may change the business logo';
    end if;
    insert into public.crsm_settings (setting_key, setting_value, updated_at)
    values ('business_logo_path', logo_path, now())
    on conflict (setting_key) do update
        set setting_value = excluded.setting_value, updated_at = now();
end;
$$;

alter table public.crsm_profiles enable row level security;
alter table public.crsm_settings enable row level security;

drop policy if exists crsm_profiles_read_self_or_admin on public.crsm_profiles;
create policy crsm_profiles_read_self_or_admin on public.crsm_profiles
    for select to authenticated
    using (id = auth.uid() or public.is_crsm_admin());

drop policy if exists crsm_profiles_update_self on public.crsm_profiles;
create policy crsm_profiles_update_self on public.crsm_profiles
    for update to authenticated
    using (id = auth.uid())
    with check (id = auth.uid() and role = public.current_crsm_role());

drop policy if exists crsm_profiles_admin_update on public.crsm_profiles;
create policy crsm_profiles_admin_update on public.crsm_profiles
    for update to authenticated
    using (public.is_crsm_admin())
    with check (public.is_crsm_admin());

drop policy if exists crsm_settings_read_authenticated on public.crsm_settings;
create policy crsm_settings_read_authenticated on public.crsm_settings
    for select to authenticated using (true);

revoke all on public.crsm_profiles from anon;
grant select, update on public.crsm_profiles to authenticated;
revoke all on public.crsm_settings from anon;
grant select on public.crsm_settings to authenticated;
grant execute on function public.promote_crsm_user(uuid, text) to authenticated;
grant execute on function public.set_crsm_business_logo(text) to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('crsm-profile-images', 'crsm-profile-images', false, 2097152, array['image/png', 'image/jpeg'])
on conflict (id) do update set public = false, file_size_limit = 2097152, allowed_mime_types = array['image/png', 'image/jpeg'];

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('crsm-brand-assets', 'crsm-brand-assets', false, 2097152, array['image/png', 'image/jpeg'])
on conflict (id) do update set public = false, file_size_limit = 2097152, allowed_mime_types = array['image/png', 'image/jpeg'];

drop policy if exists crsm_avatar_read_self_or_admin on storage.objects;
create policy crsm_avatar_read_self_or_admin on storage.objects
    for select to authenticated
    using (bucket_id = 'crsm-profile-images' and (storage.foldername(name))[1] = auth.uid()::text or bucket_id = 'crsm-profile-images' and public.is_crsm_admin());

drop policy if exists crsm_avatar_insert_self on storage.objects;
create policy crsm_avatar_insert_self on storage.objects
    for insert to authenticated
    with check (bucket_id = 'crsm-profile-images' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists crsm_avatar_update_self on storage.objects;
create policy crsm_avatar_update_self on storage.objects
    for update to authenticated
    using (bucket_id = 'crsm-profile-images' and (storage.foldername(name))[1] = auth.uid()::text)
    with check (bucket_id = 'crsm-profile-images' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists crsm_avatar_delete_self on storage.objects;
create policy crsm_avatar_delete_self on storage.objects
    for delete to authenticated
    using (bucket_id = 'crsm-profile-images' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists crsm_brand_read_authenticated on storage.objects;
create policy crsm_brand_read_authenticated on storage.objects
    for select to authenticated using (bucket_id = 'crsm-brand-assets');

drop policy if exists crsm_brand_insert_admin on storage.objects;
create policy crsm_brand_insert_admin on storage.objects
    for insert to authenticated with check (bucket_id = 'crsm-brand-assets' and public.is_crsm_admin());

drop policy if exists crsm_brand_update_admin on storage.objects;
create policy crsm_brand_update_admin on storage.objects
    for update to authenticated using (bucket_id = 'crsm-brand-assets' and public.is_crsm_admin())
    with check (bucket_id = 'crsm-brand-assets' and public.is_crsm_admin());

drop policy if exists crsm_brand_delete_admin on storage.objects;
create policy crsm_brand_delete_admin on storage.objects
    for delete to authenticated using (bucket_id = 'crsm-brand-assets' and public.is_crsm_admin());

-- Bootstrap the first administrator only after signing up with the chosen admin email.
-- Run manually in the Supabase SQL editor and replace the email; do not enable client-side role selection.
-- update public.crsm_profiles set role = 'ADMIN' where email = 'admin@example.com';
