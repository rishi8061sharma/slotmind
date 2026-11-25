-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: ai_rescheduling
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `reschedule_suggestions`
--

DROP TABLE IF EXISTS `reschedule_suggestions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reschedule_suggestions` (
  `id` varchar(255) NOT NULL,
  `appointment_id` varchar(50) NOT NULL,
  `recommended_slot_id` varchar(50) NOT NULL,
  `alternate_provider_id` varchar(50) DEFAULT NULL,
  `confidence` decimal(38,2) DEFAULT NULL,
  `reasoning` varchar(255) DEFAULT NULL,
  `status` enum('PENDING','ACCEPTED','REJECTED') NOT NULL,
  `trigger_reason` enum('INITIAL','PROVIDER_OFFLINE','SLOT_CANCELLED') NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `pending_key` varchar(120) GENERATED ALWAYS AS ((case when (`status` = _utf8mb4'PENDING') then concat(`appointment_id`,_utf8mb4':',`trigger_reason`) else NULL end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_pending_suggestion` (`pending_key`),
  KEY `fk_suggestion_slot` (`recommended_slot_id`),
  KEY `fk_suggestion_provider` (`alternate_provider_id`),
  KEY `idx_suggestions_appointment_status` (`appointment_id`,`status`),
  CONSTRAINT `fk_suggestion_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointments` (`id`),
  CONSTRAINT `fk_suggestion_provider` FOREIGN KEY (`alternate_provider_id`) REFERENCES `providers` (`id`),
  CONSTRAINT `fk_suggestion_slot` FOREIGN KEY (`recommended_slot_id`) REFERENCES `slots` (`id`),
  CONSTRAINT `chk_confidence` CHECK (((`confidence` >= 0) and (`confidence` <= 1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reschedule_suggestions`
--

LOCK TABLES `reschedule_suggestions` WRITE;
/*!40000 ALTER TABLE `reschedule_suggestions` DISABLE KEYS */;
INSERT INTO `reschedule_suggestions` (`id`, `appointment_id`, `recommended_slot_id`, `alternate_provider_id`, `confidence`, `reasoning`, `status`, `trigger_reason`, `created_at`, `updated_at`) VALUES ('DEMO-RSG-001','DEMO-APT-001','DEMO-SLT-002','DEMO-PRV-002',0.92,'The original provider is offline. Dr. Lucas Reed has the nearest available general slot tomorrow.','PENDING','PROVIDER_OFFLINE','2026-11-25 18:45:20','2026-11-25 18:45:20'),('DEMO-RSG-002','DEMO-APT-002','DEMO-SLT-004','DEMO-PRV-003',0.98,'This urgent appointment is being moved to the earliest suitable specialist slot available.','PENDING','SLOT_CANCELLED','2026-11-25 18:45:20','2026-11-25 18:45:20'),('DEMO-RSG-003','DEMO-APT-003','DEMO-SLT-009',NULL,0.88,'The same provider has an open follow-up slot later this week.','PENDING','INITIAL','2026-11-25 18:45:20','2026-11-25 18:45:20'),('DEMO-RSG-004','DEMO-APT-004','DEMO-SLT-008','DEMO-PRV-002',0.76,'The original slot was cancelled. This is the next available suitable appointment.','PENDING','SLOT_CANCELLED','2026-11-25 18:45:20','2026-11-25 18:45:20'),('RSG-001','APT-001','SLT-007','PRV-004',0.92,'Dr. Sarah Chen is unavailable. The nearest available general appointment is with Dr. Rahul Verma tomorrow.','ACCEPTED','PROVIDER_OFFLINE','2026-11-25 17:08:48','2026-11-25 18:05:26'),('RSG-002','APT-002','SLT-006','PRV-002',0.87,'The original slot is no longer available. Dr. James Okonkwo has the nearest suitable open slot.','PENDING','SLOT_CANCELLED','2026-11-25 17:08:48','2026-11-25 17:08:48'),('RSG-003','APT-004','SLT-005','PRV-004',0.81,'The replacement slot was accepted by the front desk after confirming with the patient.','ACCEPTED','INITIAL','2026-11-25 17:08:48','2026-11-25 17:08:48'),('RSG-11cdb120-b7ff-4686-9527-384c9e3da9b5','APT-005','SLT-006',NULL,0.95,'Nearest suitable slot with the same provider.','PENDING','INITIAL','2026-11-25 18:38:56','2026-11-25 18:38:56'),('RSG-3b07e45a-fea3-41b5-8e0c-952f6f835ff1','APT-005','SLT-006',NULL,0.95,'Nearest suitable slot with the same provider.','REJECTED','INITIAL','2026-11-25 18:36:06','2026-11-25 18:37:37'),('RSG-42df46d3-e069-4f37-a03c-49ccba11e4bf','APT-005','SLT-006',NULL,0.95,'Nearest suitable slot with the same provider.','REJECTED','INITIAL','2026-11-25 18:37:37','2026-11-25 18:38:56'),('RSG-79d87ae3-85af-41fa-9772-e87f86d19e5d','APT-003','SLT-004','PRV-005',0.80,'Nearest suitable slot with a matching specialty.','ACCEPTED','INITIAL','2026-11-25 18:05:42','2026-11-25 18:05:48'),('RSG-a022fb05-f19a-49b0-8750-afa92d1dafa0','APT-003','SLT-006','PRV-002',0.80,'Nearest suitable slot with a matching specialty.','PENDING','INITIAL','2026-11-25 18:06:00','2026-11-25 18:06:00'),('RSG-c5d8bcfc-1656-4999-9883-c0063d43e500','APT-002','SLT-003','PRV-003',0.80,'Nearest suitable slot with a matching specialty.','ACCEPTED','INITIAL','2026-11-25 18:34:03','2026-11-25 18:34:36'),('RSG-d9118779-1625-4ce3-ab8e-4a08e1c9aae4','APT-004','SLT-006','PRV-002',0.80,'Nearest suitable slot with a matching specialty.','PENDING','INITIAL','2026-11-25 18:36:29','2026-11-25 18:36:29'),('RSG-f7c5a185-1463-469c-9eac-d90dab4edabc','APT-001','SLT-003','PRV-003',0.80,'Nearest suitable slot with a matching specialty.','PENDING','INITIAL','2026-11-25 18:34:03','2026-11-25 18:34:03');
/*!40000 ALTER TABLE `reschedule_suggestions` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-25 18:55:43
