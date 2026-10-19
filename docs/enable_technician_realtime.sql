-- Enable technician availability change events in this existing Supabase project.
-- Safe to re-run. Preserves other tables already in supabase_realtime.
-- Run docs/harden_supabase_security.sql first so the private role helper exists.

alter table public.technicians enable row level security;
revoke all on table public.technicians from anon, authenticated;
grant select on table public.technicians to authenticated;

drop policy if exists crsm_technicians_read_staff_admin on public.technicians;
create policy crsm_technicians_read_staff_admin on public.technicians
    for select to authenticated
    using (private.current_crsm_role() in ('STAFF', 'ADMIN'));

do $$
begin
    if not exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
        execute 'create publication supabase_realtime';
    end if;
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'technicians'
    ) then
        execute 'alter publication supabase_realtime add table public.technicians';
    end if;
end;
$$;
