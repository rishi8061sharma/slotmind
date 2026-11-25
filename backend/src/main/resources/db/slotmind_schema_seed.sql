-- SlotMind / CareBridge schema and seed data
-- MySQL 8.0+
-- Re-running this script resets the SlotMind tables.

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS reschedule_suggestions;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS slots;
DROP TABLE IF EXISTS providers;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE providers (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    specialty ENUM('GENERAL', 'SPECIALIST', 'PEDIATRIC') NOT NULL,
    status ENUM('AVAILABLE', 'BUSY', 'OFFLINE') NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB;

CREATE TABLE slots (
    id VARCHAR(50) PRIMARY KEY,
    provider_id VARCHAR(50) NOT NULL,
    start_time DATETIME NOT NULL,
    duration_minutes INT NOT NULL,
    status ENUM('OPEN', 'BOOKED', 'CANCELLED') NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_slot_provider FOREIGN KEY (provider_id) REFERENCES providers(id),
    CONSTRAINT chk_slot_duration CHECK (duration_minutes > 0),
    INDEX idx_slots_provider_status_time (provider_id, status, start_time)
) ENGINE = InnoDB;

CREATE TABLE appointments (
    id VARCHAR(50) PRIMARY KEY,
    patient_name VARCHAR(150) NOT NULL,
    appointment_type ENUM('ROUTINE', 'URGENT', 'FOLLOW_UP') NOT NULL,
    provider_id VARCHAR(50) NOT NULL,
    slot_id VARCHAR(50) NOT NULL,
    status ENUM('CONFIRMED', 'RESCHEDULE_PENDING', 'RESCHEDULED', 'COMPLETED') NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointment_provider FOREIGN KEY (provider_id) REFERENCES providers(id),
    CONSTRAINT fk_appointment_slot FOREIGN KEY (slot_id) REFERENCES slots(id),
    CONSTRAINT uq_appointment_slot UNIQUE (slot_id),
    INDEX idx_appointments_status (status),
    INDEX idx_appointments_provider_status (provider_id, status)
) ENGINE = InnoDB;

CREATE TABLE reschedule_suggestions (
    id VARCHAR(50) PRIMARY KEY,
    appointment_id VARCHAR(50) NOT NULL,
    recommended_slot_id VARCHAR(50) NOT NULL,
    alternate_provider_id VARCHAR(50) NULL,
    confidence DECIMAL(5,4) NOT NULL,
    reasoning TEXT NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL,
    trigger_reason ENUM('INITIAL', 'PROVIDER_OFFLINE', 'SLOT_CANCELLED') NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_suggestion_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_suggestion_slot FOREIGN KEY (recommended_slot_id) REFERENCES slots(id),
    CONSTRAINT fk_suggestion_provider FOREIGN KEY (alternate_provider_id) REFERENCES providers(id),
    CONSTRAINT chk_confidence CHECK (confidence >= 0 AND confidence <= 1),
    INDEX idx_suggestions_appointment_status (appointment_id, status)
) ENGINE = InnoDB;

-- Only one pending suggestion is allowed per appointment and trigger reason.
ALTER TABLE reschedule_suggestions
ADD pending_key VARCHAR(120)
    GENERATED ALWAYS AS (
        CASE
            WHEN status = 'PENDING' THEN CONCAT(appointment_id, ':', trigger_reason)
            ELSE NULL
        END
    ) STORED;

CREATE UNIQUE INDEX uq_pending_suggestion
    ON reschedule_suggestions(pending_key);

-- Providers
INSERT INTO providers (id, name, specialty, status) VALUES
('PRV-001', 'Dr. Sarah Chen',    'GENERAL',    'OFFLINE'),
('PRV-002', 'Dr. James Okonkwo', 'SPECIALIST', 'AVAILABLE'),
('PRV-003', 'Dr. Priya Sharma',  'PEDIATRIC',  'BUSY'),
('PRV-004', 'Dr. Rahul Verma',   'GENERAL',    'AVAILABLE'),
('PRV-005', 'Dr. Ananya Iyer',  'SPECIALIST', 'BUSY');

-- Slots: booked slots correspond to appointments below.
INSERT INTO slots (id, provider_id, start_time, duration_minutes, status) VALUES
('SLT-001', 'PRV-001', DATE_ADD(NOW(), INTERVAL 1 HOUR),  30, 'BOOKED'),
('SLT-002', 'PRV-001', DATE_ADD(NOW(), INTERVAL 2 HOUR),  30, 'BOOKED'),
('SLT-003', 'PRV-003', DATE_ADD(NOW(), INTERVAL 3 HOUR),  45, 'BOOKED'),
('SLT-004', 'PRV-005', DATE_ADD(NOW(), INTERVAL 4 HOUR),  60, 'OPEN'),
('SLT-005', 'PRV-004', DATE_ADD(NOW(), INTERVAL 5 HOUR),  30, 'BOOKED'),
('SLT-006', 'PRV-002', DATE_ADD(NOW(), INTERVAL 6 HOUR),  30, 'OPEN'),
('SLT-007', 'PRV-004', DATE_ADD(NOW(), INTERVAL 1 DAY),   30, 'OPEN'),
('SLT-008', 'PRV-002', DATE_ADD(NOW(), INTERVAL 2 DAY),   45, 'OPEN'),
('SLT-009', 'PRV-003', DATE_ADD(NOW(), INTERVAL 3 DAY),   15, 'OPEN'),
('SLT-010', 'PRV-005', DATE_ADD(NOW(), INTERVAL 4 DAY),   30, 'CANCELLED'),
('SLT-011', 'PRV-002', DATE_ADD(NOW(), INTERVAL 5 DAY),   30, 'BOOKED');

-- Appointments: includes confirmed, pending, rescheduled, and completed examples.
INSERT INTO appointments
(id, patient_name, appointment_type, provider_id, slot_id, status) VALUES
('APT-001', 'John Martinez', 'ROUTINE',    'PRV-001', 'SLT-001', 'RESCHEDULE_PENDING'),
('APT-002', 'Emily Watson',  'FOLLOW_UP',  'PRV-001', 'SLT-002', 'RESCHEDULE_PENDING'),
('APT-003', 'Aisha Patel',   'URGENT',     'PRV-003', 'SLT-003', 'CONFIRMED'),
('APT-004', 'Robert Kim',    'ROUTINE',    'PRV-004', 'SLT-005', 'RESCHEDULED'),
('APT-005', 'Maria Garcia',  'FOLLOW_UP',  'PRV-002', 'SLT-011', 'COMPLETED');

-- Suggestions: one provider-offline pending suggestion, one slot-cancelled pending
-- suggestion, and one historical accepted suggestion.
INSERT INTO reschedule_suggestions
(id, appointment_id, recommended_slot_id, alternate_provider_id, confidence, reasoning, status, trigger_reason) VALUES
('RSG-001', 'APT-001', 'SLT-007', 'PRV-004', 0.9200,
 'Dr. Sarah Chen is unavailable. The nearest available general appointment is with Dr. Rahul Verma tomorrow.',
 'PENDING', 'PROVIDER_OFFLINE'),
('RSG-002', 'APT-002', 'SLT-006', 'PRV-002', 0.8700,
 'The original slot is no longer available. Dr. James Okonkwo has the nearest suitable open slot.',
 'PENDING', 'SLOT_CANCELLED'),
('RSG-003', 'APT-004', 'SLT-005', 'PRV-004', 0.8100,
 'The replacement slot was accepted by the front desk after confirming with the patient.',
 'ACCEPTED', 'INITIAL');

-- Verification queries
SELECT 'providers' AS table_name, COUNT(*) AS record_count FROM providers
UNION ALL SELECT 'slots', COUNT(*) FROM slots
UNION ALL SELECT 'appointments', COUNT(*) FROM appointments
UNION ALL SELECT 'reschedule_suggestions', COUNT(*) FROM reschedule_suggestions;