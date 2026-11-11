-- Older installations can have both the original and the expanded status check.
-- The original rejects Awaiting Payment/Ready for Testing even when RPCs exist.
begin;
alter table public.repair_requests drop constraint if exists chk_repair_status;
alter table public.repair_requests drop constraint if exists chk_repair_status_allowed;
alter table public.repair_requests add constraint chk_repair_status_allowed check (repair_status in (
 'Pending','Confirmed','Under Diagnosis','Under Repair','Waiting for Parts','Ready for Testing',
 'Awaiting Payment','Ready for Pickup','Completed','Cancelled'
));
notify pgrst, 'reload schema';
commit;
