-- SlotMind UI demo data
-- Adds multiple independent scenarios without deleting existing records.
-- Run this script in MySQL Workbench while using the ai_rescheduling database.

USE ai_rescheduling;

INSERT INTO providers (id, name, specialty, status) VALUES
('DEMO-PRV-001', 'Dr. Maya Patel', 'GENERAL', 'OFFLINE'),
('DEMO-PRV-002', 'Dr. Lucas Reed', 'GENERAL', 'AVAILABLE'),
('DEMO-PRV-003', 'Dr. Noor Khan', 'SPECIALIST', 'AVAILABLE'),
('DEMO-PRV-004', 'Dr. Elena Rossi', 'PEDIATRIC', 'BUSY');

INSERT INTO slots (id, provider_id, start_time, duration_minutes, status) VALUES
('DEMO-SLT-001', 'DEMO-PRV-001', DATE_ADD(NOW(), INTERVAL 1 DAY), 30, 'BOOKED'),
('DEMO-SLT-002', 'DEMO-PRV-002', DATE_ADD(NOW(), INTERVAL 1 DAY), 30, 'OPEN'),
('DEMO-SLT-003', 'DEMO-PRV-002', DATE_ADD(NOW(), INTERVAL 2 DAY), 30, 'OPEN'),
('DEMO-SLT-004', 'DEMO-PRV-003', DATE_ADD(NOW(), INTERVAL 3 DAY), 45, 'OPEN'),
('DEMO-SLT-005', 'DEMO-PRV-003', DATE_ADD(NOW(), INTERVAL 4 DAY), 30, 'BOOKED'),
('DEMO-SLT-006', 'DEMO-PRV-004', DATE_ADD(NOW(), INTERVAL 1 DAY), 15, 'CANCELLED'),
('DEMO-SLT-007', 'DEMO-PRV-004', DATE_ADD(NOW(), INTERVAL 2 DAY), 30, 'BOOKED'),
('DEMO-SLT-008', 'DEMO-PRV-002', DATE_ADD(NOW(), INTERVAL 5 DAY), 30, 'BOOKED'),
('DEMO-SLT-009', 'DEMO-PRV-003', DATE_ADD(NOW(), INTERVAL 6 DAY), 30, 'OPEN');

-- All appointments below appear in the Needs rescheduling view.
INSERT INTO appointments
(id, patient_name, appointment_type, provider_id, slot_id, status) VALUES
('DEMO-APT-001', 'Olivia Thompson', 'ROUTINE', 'DEMO-PRV-001', 'DEMO-SLT-001', 'RESCHEDULE_PENDING'),
('DEMO-APT-002', 'Ethan Williams', 'URGENT', 'DEMO-PRV-004', 'DEMO-SLT-007', 'RESCHEDULE_PENDING'),
('DEMO-APT-003', 'Sophia Martinez', 'FOLLOW_UP', 'DEMO-PRV-003', 'DEMO-SLT-005', 'RESCHEDULE_PENDING'),
('DEMO-APT-004', 'Noah Johnson', 'ROUTINE', 'DEMO-PRV-001', 'DEMO-SLT-006', 'RESCHEDULE_PENDING'),
('DEMO-APT-005', 'Ava Brown', 'ROUTINE', 'DEMO-PRV-002', 'DEMO-SLT-008', 'RESCHEDULE_PENDING');

INSERT INTO reschedule_suggestions
(id, appointment_id, recommended_slot_id, alternate_provider_id, confidence, reasoning, status, trigger_reason) VALUES
('DEMO-RSG-001', 'DEMO-APT-001', 'DEMO-SLT-002', 'DEMO-PRV-002', 0.9200,
 'The original provider is offline. Dr. Lucas Reed has the nearest available general slot tomorrow.',
 'PENDING', 'PROVIDER_OFFLINE'),
('DEMO-RSG-002', 'DEMO-APT-002', 'DEMO-SLT-004', 'DEMO-PRV-003', 0.9800,
 'This urgent appointment is being moved to the earliest suitable specialist slot available.',
 'PENDING', 'SLOT_CANCELLED'),
('DEMO-RSG-003', 'DEMO-APT-003', 'DEMO-SLT-009', NULL, 0.8800,
 'The same provider has an open follow-up slot later this week.',
 'PENDING', 'INITIAL'),
('DEMO-RSG-004', 'DEMO-APT-004', 'DEMO-SLT-008', 'DEMO-PRV-002', 0.7600,
 'The original slot was cancelled. This is the next available suitable appointment.',
 'PENDING', 'SLOT_CANCELLED');

-- DEMO-APT-005 intentionally has no suggestion to demonstrate:
-- “No suitable slot available.”

SELECT id, patient_name, appointment_type, status
FROM appointments
WHERE id LIKE 'DEMO-%'
ORDER BY id;