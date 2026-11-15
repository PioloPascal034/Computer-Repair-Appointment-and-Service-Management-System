-- Run after profile_and_photo_access_migration.sql and customer_workflow_migration.sql.
-- Stats and recent repairs use the same ownership/assignment boundary as repair details.
begin;
create or replace function public.get_crsm_dashboard_statistics()
returns jsonb language plpgsql stable security definer set search_path='' set timezone='Asia/Manila' as $$
declare result jsonb;
begin
 if private.current_crsm_role() is null then raise exception 'Active account required'; end if;
 with visible as (
 select r.*,(select coalesce(sum(p.amount_paid-p.refunded_amount),0) from public.payments p where p.repair_request_id=r.repair_request_id) as net_paid
 from public.repair_requests r where private.can_view_crsm_repair(r.repair_request_id)
 ), months as (
 select generate_series(date_trunc('month',current_date)-interval '5 months',date_trunc('month',current_date),interval '1 month')::date as month
 ), recent as (
 select repair_request_id,repair_status,reported_problem,estimated_cost,final_cost from visible order by repair_request_id desc limit 8
 )
 select jsonb_build_object('repair_count',count(*),
 'active_count',count(*) filter(where repair_status not in ('Completed','Cancelled')),
 'completed_count',count(*) filter(where repair_status='Completed'),
 'net_paid',coalesce(sum(net_paid),0),
 'balance_due',coalesce(sum(greatest(final_cost-net_paid,0)) filter(where final_cost is not null and repair_status<>'Cancelled'),0),
 'status_counts',(select coalesce(jsonb_agg(jsonb_build_object('status',s.repair_status,'count',s.n) order by s.repair_status),'[]'::jsonb) from (select repair_status,count(*) as n from visible group by repair_status) s),
 'monthly_payments',(select jsonb_agg(jsonb_build_object('month',to_char(m.month,'Mon YYYY'),'amount',
 (select coalesce(sum(p.amount_paid-p.refunded_amount),0) from public.payments p join visible v on v.repair_request_id=p.repair_request_id where p.payment_date>=m.month and p.payment_date<m.month+interval '1 month')) order by m.month) from months m),
 'recent_repairs',(select coalesce(jsonb_agg(to_jsonb(recent)),'[]'::jsonb) from recent)) into result from visible;
 return result;
end $$;
revoke all on function public.get_crsm_dashboard_statistics() from public,anon;
grant execute on function public.get_crsm_dashboard_statistics() to authenticated;
commit;
