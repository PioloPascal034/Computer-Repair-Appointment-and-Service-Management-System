-- ============================================================
-- COMPUTER REPAIR APPOINTMENT AND SERVICE MANAGEMENT SYSTEM
-- Complete MySQL Database Script
-- ============================================================

CREATE DATABASE IF NOT EXISTS computer_repair_system;
USE computer_repair_system;

-- ============================================================
-- 1. TABLE CREATION
-- ============================================================

CREATE TABLE Customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    contact_number VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    address VARCHAR(255)
);

CREATE TABLE Computers (
    computer_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    device_type VARCHAR(50) NOT NULL,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(100),
    serial_number VARCHAR(100),
    reported_problem TEXT,

    FOREIGN KEY (customer_id)
        REFERENCES Customers(customer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE Technicians (
    technician_id INT AUTO_INCREMENT PRIMARY KEY,
    technician_name VARCHAR(100) NOT NULL,
    contact_number VARCHAR(20),
    specialization VARCHAR(100),
    availability VARCHAR(50)
);

CREATE TABLE Repair_Services (
    service_id INT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    description TEXT,
    service_price DECIMAL(10,2) NOT NULL
);

CREATE TABLE Appointments (
    appointment_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    computer_id INT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    purpose VARCHAR(255),
    appointment_status VARCHAR(50) NOT NULL,

    FOREIGN KEY (customer_id)
        REFERENCES Customers(customer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    FOREIGN KEY (computer_id)
        REFERENCES Computers(computer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE Repair_Requests (
    repair_request_id INT AUTO_INCREMENT PRIMARY KEY,
    computer_id INT NOT NULL,
    technician_id INT,
    service_id INT,
    reported_problem TEXT NOT NULL,
    diagnosis TEXT,
    repair_status VARCHAR(50) NOT NULL,
    estimated_cost DECIMAL(10,2),
    final_cost DECIMAL(10,2),

    FOREIGN KEY (computer_id)
        REFERENCES Computers(computer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    FOREIGN KEY (technician_id)
        REFERENCES Technicians(technician_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    FOREIGN KEY (service_id)
        REFERENCES Repair_Services(service_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

CREATE TABLE Payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    repair_request_id INT NOT NULL,
    amount_paid DECIMAL(10,2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_status VARCHAR(50) NOT NULL,

    FOREIGN KEY (repair_request_id)
        REFERENCES Repair_Requests(repair_request_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- ============================================================
-- 2. VALIDATION CONSTRAINTS
-- ============================================================

ALTER TABLE Repair_Requests
ADD CONSTRAINT chk_repair_status
CHECK (
    repair_status IN (
        'Pending',
        'Confirmed',
        'Under Diagnosis',
        'Under Repair',
        'Waiting for Parts',
        'Ready for Pickup',
        'Completed'
    )
);

ALTER TABLE Appointments
ADD CONSTRAINT chk_appointment_status
CHECK (
    appointment_status IN (
        'Pending',
        'Confirmed',
        'Completed',
        'Cancelled'
    )
);

ALTER TABLE Payments
ADD CONSTRAINT chk_payment_status
CHECK (
    payment_status IN (
        'Unpaid',
        'Partial',
        'Paid'
    )
);

ALTER TABLE Repair_Requests
ADD CONSTRAINT chk_estimated_cost
CHECK (estimated_cost >= 0);

ALTER TABLE Repair_Requests
ADD CONSTRAINT chk_final_cost
CHECK (final_cost >= 0);

ALTER TABLE Repair_Services
ADD CONSTRAINT chk_service_price
CHECK (service_price >= 0);

ALTER TABLE Payments
ADD CONSTRAINT chk_amount_paid
CHECK (amount_paid >= 0);

-- ============================================================
-- 3. SAMPLE DATA
-- ============================================================

-- CUSTOMERS: 10 records

INSERT INTO Customers
(customer_name, contact_number, email, address)
VALUES
('Juan Dela Cruz', '09171234567', 'juan.delacruz@gmail.com', 'Batac, Ilocos Norte'),
('Maria Santos', '09281234567', 'maria.santos@gmail.com', 'Laoag City, Ilocos Norte'),
('Carlo Reyes', '09391234567', 'carlo.reyes@gmail.com', 'San Nicolas, Ilocos Norte'),
('Angela Garcia', '09451234567', 'angela.garcia@gmail.com', 'Batac, Ilocos Norte'),
('Mark Villanueva', '09561234567', 'mark.villanueva@gmail.com', 'Laoag City, Ilocos Norte'),
('Sofia Mendoza', '09671234567', 'sofia.mendoza@gmail.com', 'Paoay, Ilocos Norte'),
('Daniel Flores', '09781234567', 'daniel.flores@gmail.com', 'San Nicolas, Ilocos Norte'),
('Kristine Ramos', '09891234567', 'kristine.ramos@gmail.com', 'Batac, Ilocos Norte'),
('Patrick Aquino', '09981234567', 'patrick.aquino@gmail.com', 'Laoag City, Ilocos Norte'),
('Nicole Fernandez', '09081234567', 'nicole.fernandez@gmail.com', 'Dingras, Ilocos Norte');

-- COMPUTERS: 10 records

INSERT INTO Computers
(customer_id, device_type, brand, model, serial_number, reported_problem)
VALUES
(1, 'Laptop', 'Acer', 'Aspire 5', 'ACR10001', 'Laptop is overheating and shutting down.'),
(2, 'Desktop', 'Dell', 'OptiPlex 7090', 'DLL10002', 'Computer does not boot properly.'),
(3, 'Laptop', 'Lenovo', 'IdeaPad 3', 'LNV10003', 'Laptop is running very slowly.'),
(4, 'Desktop', 'HP', 'ProDesk 600', 'HP10004', 'No display after turning on the computer.'),
(5, 'Laptop', 'ASUS', 'VivoBook 15', 'ASUS10005', 'Keyboard has several unresponsive keys.'),
(6, 'Laptop', 'MSI', 'Modern 14', 'MSI10006', 'Laptop battery drains very quickly.'),
(7, 'Desktop', 'Acer', 'Veriton X', 'ACR10007', 'Computer randomly restarts.'),
(8, 'Laptop', 'HP', 'Pavilion 15', 'HP10008', 'Operating system has errors.'),
(9, 'Desktop', 'Lenovo', 'ThinkCentre M70', 'LNV10009', 'Computer is infected with malware.'),
(10, 'Laptop', 'Dell', 'Inspiron 15', 'DLL10010', 'Laptop screen is flickering.');

-- TECHNICIANS: 10 records

INSERT INTO Technicians
(technician_name, contact_number, specialization, availability)
VALUES
('John Martinez', '09170000001', 'Hardware Repair', 'Available'),
('Kevin Torres', '09170000002', 'Software Troubleshooting', 'Available'),
('Ryan Bautista', '09170000003', 'Laptop Repair', 'Busy'),
('Michael Cruz', '09170000004', 'Hardware Repair', 'Available'),
('Joshua Navarro', '09170000005', 'Operating System', 'Available'),
('Edward Ramos', '09170000006', 'Networking', 'Busy'),
('Christian Aquino', '09170000007', 'Malware Removal', 'Available'),
('Brian Mendoza', '09170000008', 'Hardware Diagnostics', 'Available'),
('Anthony Flores', '09170000009', 'Laptop Repair', 'Busy'),
('Daniel Garcia', '09170000010', 'Software Installation', 'Available');

-- REPAIR_SERVICES: 10 records

INSERT INTO Repair_Services
(service_name, description, service_price)
VALUES
('Hardware Diagnostics', 'Complete inspection and diagnosis of computer hardware.', 300.00),
('Operating System Installation', 'Installation and basic configuration of an operating system.', 800.00),
('Virus and Malware Removal', 'Removal of viruses, malware, and other unwanted software.', 700.00),
('Laptop Cleaning', 'Internal cleaning and dust removal for laptops.', 500.00),
('Desktop Cleaning', 'Internal cleaning and dust removal for desktop computers.', 500.00),
('Keyboard Replacement', 'Replacement of a damaged or defective keyboard.', 1200.00),
('Screen Replacement', 'Replacement of a damaged laptop or monitor screen.', 3500.00),
('Battery Replacement', 'Replacement of a defective laptop battery.', 1800.00),
('Software Troubleshooting', 'Diagnosis and repair of software-related problems.', 600.00),
('System Optimization', 'Performance optimization and removal of unnecessary files.', 500.00);

-- APPOINTMENTS: 10 records

INSERT INTO Appointments
(customer_id, computer_id, appointment_date, appointment_time, purpose, appointment_status)
VALUES
(1, 1, '2026-09-15', '09:00:00', 'Laptop overheating diagnosis', 'Confirmed'),
(2, 2, '2026-09-15', '10:00:00', 'Desktop boot problem', 'Confirmed'),
(3, 3, '2026-09-16', '09:30:00', 'Slow laptop troubleshooting', 'Pending'),
(4, 4, '2026-09-16', '11:00:00', 'No display diagnosis', 'Confirmed'),
(5, 5, '2026-09-17', '13:00:00', 'Keyboard problem', 'Pending'),
(6, 6, '2026-09-17', '14:00:00', 'Battery inspection', 'Confirmed'),
(7, 7, '2026-09-18', '09:00:00', 'Random restart problem', 'Pending'),
(8, 8, '2026-09-18', '10:30:00', 'Operating system repair', 'Confirmed'),
(9, 9, '2026-09-19', '13:30:00', 'Malware removal', 'Confirmed'),
(10, 10, '2026-09-19', '15:00:00', 'Screen flickering diagnosis', 'Pending');

-- REPAIR_REQUESTS: 10 records

INSERT INTO Repair_Requests
(computer_id, technician_id, service_id, reported_problem, diagnosis, repair_status, estimated_cost, final_cost)
VALUES
(1, 1, 4, 'Laptop is overheating and shutting down.',
 'Dust buildup and poor thermal performance detected.',
 'Under Repair', 800.00, NULL),

(2, 4, 1, 'Computer does not boot properly.',
 'Faulty power supply detected.',
 'Waiting for Parts', 2500.00, NULL),

(3, 2, 10, 'Laptop is running very slowly.',
 'Too many startup programs and insufficient storage space.',
 'Completed', 500.00, 500.00),

(4, 8, 1, 'No display after turning on the computer.',
 'Loose display cable and faulty RAM detected.',
 'Under Diagnosis', 1000.00, NULL),

(5, 3, 6, 'Keyboard has several unresponsive keys.',
 'Several keyboard switches are damaged.',
 'Confirmed', 1200.00, NULL),

(6, 9, 8, 'Laptop battery drains very quickly.',
 'Battery health is below recommended capacity.',
 'Ready for Pickup', 1800.00, 1800.00),

(7, 1, 1, 'Computer randomly restarts.',
 'Overheating caused by dust buildup.',
 'Under Repair', 800.00, NULL),

(8, 5, 2, 'Operating system has errors.',
 'Corrupted operating system files detected.',
 'Completed', 800.00, 800.00),

(9, 7, 3, 'Computer is infected with malware.',
 'Multiple malware programs detected.',
 'Under Repair', 700.00, NULL),

(10, 8, 7, 'Laptop screen is flickering.',
 'Damaged LCD panel detected.',
 'Pending', 3500.00, NULL);

-- PAYMENTS: 10 records

INSERT INTO Payments
(repair_request_id, amount_paid, payment_date, payment_status)
VALUES
(1, 0.00, '2026-09-15', 'Unpaid'),
(2, 1000.00, '2026-09-15', 'Partial'),
(3, 500.00, '2026-09-16', 'Paid'),
(4, 0.00, '2026-09-16', 'Unpaid'),
(5, 500.00, '2026-09-17', 'Partial'),
(6, 1800.00, '2026-09-17', 'Paid'),
(7, 0.00, '2026-09-18', 'Unpaid'),
(8, 800.00, '2026-09-18', 'Paid'),
(9, 300.00, '2026-09-19', 'Partial'),
(10, 0.00, '2026-09-19', 'Unpaid');

-- ============================================================
-- 4. REPORT VIEWS
-- ============================================================

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

-- ============================================================
-- 5. QUICK VERIFICATION QUERIES
-- ============================================================

SELECT * FROM Customers;
SELECT * FROM Computers;
SELECT * FROM Technicians;
SELECT * FROM Repair_Services;
SELECT * FROM Appointments;
SELECT * FROM Repair_Requests;
SELECT * FROM Payments;

SELECT * FROM Active_Repair_Status;
SELECT * FROM Appointment_Schedule;
SELECT * FROM Repair_History;
SELECT * FROM Technician_Workload;
SELECT * FROM Repair_Revenue;
SELECT * FROM Pending_Repairs;
SELECT * FROM Most_Requested_Services;

-- ============================================================
-- END OF SCRIPT
-- ============================================================
