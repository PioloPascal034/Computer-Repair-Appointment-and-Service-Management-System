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
CHECK (
    estimated_cost >= 0
);


ALTER TABLE Repair_Requests
ADD CONSTRAINT chk_final_cost
CHECK (
    final_cost >= 0
);


ALTER TABLE Repair_Services
ADD CONSTRAINT chk_service_price
CHECK (
    service_price >= 0
);


ALTER TABLE Payments
ADD CONSTRAINT chk_amount_paid
CHECK (
    amount_paid >= 0
);



