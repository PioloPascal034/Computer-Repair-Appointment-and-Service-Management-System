UPDATE Repair_Requests
SET repair_status = 'Under Repair'
WHERE repair_request_id = 1;

UPDATE Repair_Requests
SET repair_status = 'Fixing'
WHERE repair_request_id = 1;

UPDATE Payments
SET amount_paid = -500
WHERE payment_id = 1;

UPDATE Repair_Services
SET service_price = -100
WHERE service_id = 1;

//test

INSERT INTO Customers
(customer_name, contact_number, email, address)
VALUES
('Test Customer', '09123456789', 'test@gmail.com', 'Batac, Ilocos Norte');

SELECT * FROM Customers;

SELECT *
FROM Customers;

SELECT *
FROM Customers
WHERE customer_id = 1;

UPDATE Customers
SET contact_number = '09999999999'
WHERE customer_id = 1;

SELECT *
FROM Customers
WHERE customer_id = 1;

DELETE FROM Customers
WHERE customer_name = 'Test Customer';


SELECT
    c.customer_name,
    comp.brand,
    comp.model,
    r.repair_status
FROM Customers c
JOIN Computers comp
    ON c.customer_id = comp.customer_id
JOIN Repair_Requests r
    ON comp.computer_id = r.computer_id;


//gentest

SELECT
    r.repair_request_id,
    c.customer_name,
    comp.brand,
    comp.model,
    t.technician_name,
    r.repair_status,
    r.estimated_cost
FROM Repair_Requests r
JOIN Computers comp
    ON r.computer_id = comp.computer_id
JOIN Customers c
    ON comp.customer_id = c.customer_id
LEFT JOIN Technicians t
    ON r.technician_id = t.technician_id
WHERE r.repair_status <> 'Completed';
