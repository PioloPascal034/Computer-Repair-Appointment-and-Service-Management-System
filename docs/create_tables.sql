CREATE DATABASE computer_repair_system;

USE computer_repair_system;

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




