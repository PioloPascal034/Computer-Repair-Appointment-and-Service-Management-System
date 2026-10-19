CREATE VIEW Active_Repair_Status AS
SELECT
    r.repair_request_id,
    c.customer_name,
    comp.device_type,
    comp.brand,
    comp.model,
    t.technician_name,
    r.repair_status,
    r.estimated_cost,
    r.final_cost
FROM Repair_Requests r
JOIN Computers comp
    ON r.computer_id = comp.computer_id
JOIN Customers c
    ON comp.customer_id = c.customer_id
LEFT JOIN Technicians t
    ON r.technician_id = t.technician_id
WHERE r.repair_status <> 'Completed';


CREATE VIEW Appointment_Schedule AS
SELECT
    a.appointment_id,
    a.appointment_date,
    a.appointment_time,
    c.customer_name,
    comp.device_type,
    comp.brand,
    comp.model,
    a.purpose,
    a.appointment_status
FROM Appointments a
JOIN Customers c
    ON a.customer_id = c.customer_id
JOIN Computers comp
    ON a.computer_id = comp.computer_id;

CREATE VIEW Repair_History AS
SELECT
    c.customer_id,
    c.customer_name,
    comp.computer_id,
    comp.brand,
    comp.model,
    r.repair_request_id,
    r.reported_problem,
    r.diagnosis,
    r.repair_status,
    r.estimated_cost,
    r.final_cost
FROM Customers c
JOIN Computers comp
    ON c.customer_id = comp.customer_id
JOIN Repair_Requests r
    ON comp.computer_id = r.computer_id;


CREATE VIEW Technician_Workload AS
SELECT
    t.technician_id,
    t.technician_name,
    t.specialization,
    t.availability,
    COUNT(r.repair_request_id) AS total_repairs
FROM Technicians t
LEFT JOIN Repair_Requests r
    ON t.technician_id = r.technician_id
GROUP BY
    t.technician_id,
    t.technician_name,
    t.specialization,
    t.availability;


CREATE VIEW Repair_Revenue AS
SELECT
    r.repair_request_id,
    c.customer_name,
    comp.brand,
    comp.model,
    rs.service_name,
    r.final_cost,
    COALESCE(SUM(p.amount_paid), 0) AS total_paid,
    r.final_cost - COALESCE(SUM(p.amount_paid), 0) AS remaining_balance
FROM Repair_Requests r
JOIN Computers comp
    ON r.computer_id = comp.computer_id
JOIN Customers c
    ON comp.customer_id = c.customer_id
LEFT JOIN Repair_Services rs
    ON r.service_id = rs.service_id
LEFT JOIN Payments p
    ON r.repair_request_id = p.repair_request_id
GROUP BY
    r.repair_request_id,
    c.customer_name,
    comp.brand,
    comp.model,
    rs.service_name,
    r.final_cost;



CREATE VIEW Pending_Repairs AS
SELECT
    r.repair_request_id,
    c.customer_name,
    comp.brand,
    comp.model,
    t.technician_name,
    r.repair_status,
    r.estimated_cost,
    r.final_cost
FROM Repair_Requests r
JOIN Computers comp
    ON r.computer_id = comp.computer_id
JOIN Customers c
    ON comp.customer_id = c.customer_id
LEFT JOIN Technicians t
    ON r.technician_id = t.technician_id
WHERE r.repair_status IN (
    'Pending',
    'Confirmed',
    'Under Diagnosis',
    'Under Repair',
    'Waiting for Parts',
    'Ready for Pickup'
);


CREATE VIEW Most_Requested_Services AS
SELECT
    rs.service_id,
    rs.service_name,
    rs.service_price,
    COUNT(r.repair_request_id) AS times_requested
FROM Repair_Services rs
LEFT JOIN Repair_Requests r
    ON rs.service_id = r.service_id
GROUP BY
    rs.service_id,
    rs.service_name,
    rs.service_price;
//test
SHOW FULL TABLES
WHERE TABLE_TYPE = 'VIEW';



