-- Run as database owner in a TEST Supabase database after all feature migrations.
-- Also apply dashboard_statistics_migration.sql before running.
-- Fixtures and data changes are rolled back. Identity sequence values may advance.
begin;
do $$
declare admin_id uuid:=gen_random_uuid(); staff_id uuid:=gen_random_uuid(); other_id uuid:=gen_random_uuid(); customer_user uuid:=gen_random_uuid();
 cid integer; comp integer; tech integer; rid integer; rid2 integer; receipt text; bill jsonb; availability jsonb;
begin
 insert into auth.users(id,email,raw_user_meta_data) values
 (admin_id,admin_id||'@test.invalid',jsonb_build_object('full_name','Test Admin','username',admin_id::text)),
 (staff_id,staff_id||'@test.invalid',jsonb_build_object('full_name','Test Tech','username',staff_id::text)),
 (other_id,other_id||'@test.invalid',jsonb_build_object('full_name','Other Tech','username',other_id::text)),
 (customer_user,customer_user||'@test.invalid',jsonb_build_object('full_name','Test Customer','username',customer_user::text));
 update public.crsm_profiles set role='ADMIN',is_active=true where id=admin_id;
 update public.crsm_profiles set role='STAFF',is_active=true where id in (staff_id,other_id);
 insert into public.customers(customer_name,contact_number,auth_user_id) values('Workflow test','09123456789',customer_user) returning customer_id into cid;
 insert into public.computers(customer_id,device_type,brand,model) values(cid,'Laptop','Test','Test') returning computer_id into comp;
 insert into public.technicians(technician_name,contact_number,availability,auth_user_id) values('Workflow test','09123456789','Available',staff_id) returning technician_id into tech;
 insert into public.repair_requests(computer_id,reported_problem,repair_status) values(comp,'Workflow test','Pending') returning repair_request_id into rid;
 insert into public.repair_requests(computer_id,reported_problem,repair_status) values(comp,'Workflow test 2','Pending') returning repair_request_id into rid2;
 perform set_config('request.jwt.claim.sub',admin_id::text,true);
 perform public.assign_crsm_technician(rid,tech);
 perform public.assign_crsm_technician(rid,tech); -- idempotent assignment, no status reset
 availability:=public.list_crsm_technician_availability();
 if not exists(select 1 from jsonb_array_elements(availability) x where (x->>'technician_id')::integer=tech and x->>'availability'='Busy' and (x->>'can_assign')::boolean=false) then raise exception 'Busy availability not derived from workload'; end if;
 begin perform public.assign_crsm_technician(rid2,tech); raise exception 'TEST: second active job accepted';
 exception when others then if SQLERRM like 'TEST:%' then raise; end if; end;
 update public.repair_requests set repair_status='Under Repair' where repair_request_id=rid;
 perform set_config('request.jwt.claim.sub',staff_id::text,true);
 perform public.finish_crsm_repair(rid,500);
 availability:=public.list_crsm_technician_availability();
 if not exists(select 1 from jsonb_array_elements(availability) x where (x->>'technician_id')::integer=tech and (x->>'can_assign')::boolean) then raise exception 'Finishing technical work did not release capacity'; end if;
 begin perform public.confirm_crsm_pickup(rid); raise exception 'TEST: unpaid pickup accepted';
 exception when others then if SQLERRM like 'TEST:%' then raise; end if; end;
 receipt:=public.record_crsm_payment(rid,200,'Cash');
 bill:=public.get_crsm_repair_bill(rid);
 if (bill->>'balance_due')::numeric<>300 or bill->>'payment_status'<>'Partial' or receipt is null then raise exception 'Partial payment/balance incorrect'; end if;
 begin perform public.record_crsm_payment(rid,301,'Cash'); raise exception 'TEST: overpayment accepted';
 exception when others then if SQLERRM like 'TEST:%' then raise; end if; end;
 begin perform public.record_crsm_payment(rid,null,'Cash'); raise exception 'TEST: null payment accepted';
 exception when others then if SQLERRM like 'TEST:%' then raise; end if; end;
 begin perform public.record_crsm_payment(rid,1.234,'Cash'); raise exception 'TEST: excessive precision accepted';
 exception when others then if SQLERRM like 'TEST:%' then raise; end if; end;
 perform set_config('request.jwt.claim.sub',other_id::text,true);
 if (public.get_crsm_dashboard_statistics()->>'repair_count')::integer<>0 then raise exception 'Unassigned technician statistics leaked'; end if;
 begin perform public.record_crsm_payment(rid,1,'Cash'); raise exception 'TEST: unassigned technician accepted';
 exception when others then if SQLERRM like 'TEST:%' then raise; end if; end;
 perform set_config('request.jwt.claim.sub',customer_user::text,true);
 bill:=public.get_crsm_repair_bill(rid); -- owner can read, cannot record
 if (public.get_crsm_dashboard_statistics()->>'repair_count')::integer<>2 or (public.get_crsm_dashboard_statistics()->>'net_paid')::numeric<>200 then raise exception 'Customer statistics scope/totals incorrect'; end if;
 begin perform public.record_crsm_payment(rid,1,'Cash'); raise exception 'TEST: customer self-payment accepted';
 exception when others then if SQLERRM like 'TEST:%' then raise; end if; end;
 perform set_config('request.jwt.claim.sub',staff_id::text,true);
 receipt:=public.record_crsm_payment(rid,300,'Cash');bill:=public.get_crsm_repair_bill(rid);
 if (bill->>'balance_due')::numeric<>0 or bill->>'repair_status'<>'Ready for Pickup' then raise exception 'Full settlement did not enable pickup'; end if;
 begin perform public.record_crsm_payment(rid,1,'Cash'); raise exception 'TEST: settled bill accepted payment';
 exception when others then if SQLERRM like 'TEST:%' then raise; end if; end;
 perform public.confirm_crsm_pickup(rid);
 perform set_config('request.jwt.claim.sub',admin_id::text,true);
 update public.technicians set availability='Off Duty' where technician_id=tech;
 begin perform public.assign_crsm_technician(rid2,tech); raise exception 'TEST: off-duty assignment accepted';
 exception when others then if SQLERRM like 'TEST:%' then raise; end if; end;
 update public.technicians set availability='Available' where technician_id=tech;
 perform public.assign_crsm_technician(rid2,tech);
 update public.repair_requests set repair_status='Under Repair' where repair_request_id=rid2;
 perform set_config('request.jwt.claim.sub',staff_id::text,true);
 perform public.finish_crsm_repair(rid2,0);perform public.confirm_crsm_pickup(rid2);
 raise notice 'Workflow database regressions passed';
end $$;
rollback;
