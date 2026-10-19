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

INSERT INTO Repair_Requests
(computer_id, technician_id, service_id, reported_problem, diagnosis, repair_status, estimated_cost, final_cost)
VALUES
(1, 1, 4,
 'Laptop is overheating and shutting down.',
 'Dust buildup and poor thermal performance detected.',
 'Under Repair',
 800.00,
 NULL),

(2, 4, 1,
 'Computer does not boot properly.',
 'Faulty power supply detected.',
 'Waiting for Parts',
 2500.00,
 NULL),

(3, 2, 10,
 'Laptop is running very slowly.',
 'Too many startup programs and insufficient storage space.',
 'Completed',
 500.00,
 500.00),

(4, 8, 1,
 'No display after turning on the computer.',
 'Loose display cable and faulty RAM detected.',
 'Under Diagnosis',
 1000.00,
 NULL),

(5, 3, 6,
 'Keyboard has several unresponsive keys.',
 'Several keyboard switches are damaged.',
 'Confirmed',
 1200.00,
 NULL),

(6, 9, 8,
 'Laptop battery drains very quickly.',
 'Battery health is below recommended capacity.',
 'Ready for Pickup',
 1800.00,
 1800.00),

(7, 1, 1,
 'Computer randomly restarts.',
 'Overheating caused by dust buildup.',
 'Under Repair',
 800.00,
 NULL),

(8, 5, 2,
 'Operating system has errors.',
 'Corrupted operating system files detected.',
 'Completed',
 800.00,
 800.00),

(9, 7, 3,
 'Computer is infected with malware.',
 'Multiple malware programs detected.',
 'Under Repair',
 700.00,
 NULL),

(10, 8, 7,
 'Laptop screen is flickering.',
 'Damaged LCD panel detected.',
 'Pending',
 3500.00,
 NULL);

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


