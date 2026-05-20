CREATE DATABASE  IF NOT EXISTS `kamotomo_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `kamotomo_db`;
-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: kamotomo_db
-- ------------------------------------------------------
-- Server version	8.0.40

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
-- Table structure for table `backup_log`
--

DROP TABLE IF EXISTS `backup_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `backup_log` (
  `backupID` int NOT NULL AUTO_INCREMENT,
  `userID` int DEFAULT NULL,
  `actionType` enum('Backup','Restore') NOT NULL,
  `fileName` varchar(100) DEFAULT NULL,
  `filePath` varchar(255) DEFAULT NULL,
  `actionDate` datetime DEFAULT CURRENT_TIMESTAMP,
  `status` enum('Success','Failed') NOT NULL,
  PRIMARY KEY (`backupID`),
  KEY `userID` (`userID`),
  CONSTRAINT `backup_log_ibfk_1` FOREIGN KEY (`userID`) REFERENCES `user` (`userID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `backup_log`
--

LOCK TABLES `backup_log` WRITE;
/*!40000 ALTER TABLE `backup_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `backup_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `po_item`
--

DROP TABLE IF EXISTS `po_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `po_item` (
  `poId` int DEFAULT NULL,
  `productID` int DEFAULT NULL,
  `orderQty` int DEFAULT NULL,
  KEY `poId` (`poId`),
  CONSTRAINT `po_item_ibfk_1` FOREIGN KEY (`poId`) REFERENCES `purchase_order` (`poId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `po_item`
--

LOCK TABLES `po_item` WRITE;
/*!40000 ALTER TABLE `po_item` DISABLE KEYS */;
INSERT INTO `po_item` VALUES (1,4,18),(1,7,15),(1,8,13),(1,9,15),(1,10,13),(1,12,10),(1,15,16),(1,1,10),(2,1,1),(3,1,1),(4,1,1),(5,1,10),(6,1,10),(7,2,10),(8,1,1),(9,1,10),(10,1,10),(11,2,1),(12,1,1),(13,1,1),(14,2,1),(15,1,1),(16,1,1),(17,5,1),(18,7,1),(19,5,1),(20,5,1),(20,1,1),(21,5,1),(21,16,1),(22,5,16),(22,16,12);
/*!40000 ALTER TABLE `po_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product`
--

DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `productID` int NOT NULL AUTO_INCREMENT,
  `productName` varchar(30) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `description` text,
  `stockQuantity` int NOT NULL,
  `status` enum('Active','Archived') DEFAULT 'Active',
  `category` varchar(50) DEFAULT 'Accessories',
  PRIMARY KEY (`productID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product`
--

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
/*!40000 ALTER TABLE `product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_order`
--

DROP TABLE IF EXISTS `purchase_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order` (
  `poId` int NOT NULL AUTO_INCREMENT,
  `supplierName` varchar(100) DEFAULT NULL,
  `dateCreated` datetime DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `createdBy` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`poId`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_order`
--

LOCK TABLES `purchase_order` WRITE;
/*!40000 ALTER TABLE `purchase_order` DISABLE KEYS */;
INSERT INTO `purchase_order` VALUES (1,'General Supplier A','2026-05-19 14:27:52','Received','System Admin'),(2,'General Supplier A','2026-05-19 14:32:22','Received','System Admin'),(3,'General Supplier A','2026-05-19 14:32:58','Received','System Admin'),(4,'General Supplier A','2026-05-19 14:34:58','Received','System Admin'),(5,'General Supplier A','2026-05-20 09:14:38','Cancelled','System Admin'),(6,'General Supplier A','2026-05-20 09:20:30','Cancelled','System Admin'),(7,'General Supplier A','2026-05-20 09:20:51','Cancelled','System Admin'),(8,'General Supplier A','2026-05-20 09:21:21','Received','System Admin'),(9,'General Supplier A','2026-05-20 09:25:40','Cancelled','System Admin'),(10,'General Supplier A','2026-05-20 09:25:57','Cancelled','System Admin'),(11,'General Supplier A','2026-05-20 09:26:12','Received','System Admin'),(12,'General Supplier A','2026-05-20 09:26:32','Received','System Admin'),(13,'General Supplier A','2026-05-20 09:26:47','Received','System Admin'),(14,'General Supplier A','2026-05-20 09:27:05','Cancelled','System Admin'),(15,'General Supplier A','2026-05-20 09:30:39','Cancelled','System Admin'),(16,'General Supplier A','2026-05-20 09:30:49','Received','System Admin'),(17,'General Supplier A','2026-05-20 09:30:59','Cancelled','System Admin'),(18,'General Supplier A','2026-05-20 09:31:15','Received','System Admin'),(19,'General Supplier A','2026-05-20 16:33:30','Cancelled','System Administrator'),(20,'General Supplier A','2026-05-20 22:17:26','Cancelled','System Administrator'),(21,'General Supplier A','2026-05-20 23:10:37','Received','System Administrator'),(22,'General Supplier A','2026-05-20 23:10:49','Cancelled','System Administrator');
/*!40000 ALTER TABLE `purchase_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `report`
--

DROP TABLE IF EXISTS `report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report` (
  `reportID` int NOT NULL AUTO_INCREMENT,
  `reportType` enum('Sales','Inventory','Bundles') NOT NULL,
  `generatedDate` datetime DEFAULT CURRENT_TIMESTAMP,
  `userID` int DEFAULT NULL,
  PRIMARY KEY (`reportID`),
  KEY `userID` (`userID`),
  CONSTRAINT `report_ibfk_1` FOREIGN KEY (`userID`) REFERENCES `user` (`userID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report`
--

LOCK TABLES `report` WRITE;
/*!40000 ALTER TABLE `report` DISABLE KEYS */;
/*!40000 ALTER TABLE `report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_alert`
--

DROP TABLE IF EXISTS `stock_alert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_alert` (
  `alertID` int NOT NULL AUTO_INCREMENT,
  `productID` int DEFAULT NULL,
  `alertDate` datetime DEFAULT CURRENT_TIMESTAMP,
  `thresholdQuantity` int NOT NULL,
  `status` enum('Pending','Resolved') DEFAULT 'Pending',
  PRIMARY KEY (`alertID`),
  KEY `productID` (`productID`),
  CONSTRAINT `stock_alert_ibfk_1` FOREIGN KEY (`productID`) REFERENCES `product` (`productID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_alert`
--

LOCK TABLES `stock_alert` WRITE;
/*!40000 ALTER TABLE `stock_alert` DISABLE KEYS */;
/*!40000 ALTER TABLE `stock_alert` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_log`
--

DROP TABLE IF EXISTS `system_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_log` (
  `logID` int NOT NULL AUTO_INCREMENT,
  `timestamp` datetime DEFAULT CURRENT_TIMESTAMP,
  `username` varchar(30) NOT NULL,
  `action` varchar(50) NOT NULL,
  `details` text,
  PRIMARY KEY (`logID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_log`
--

LOCK TABLES `system_log` WRITE;
/*!40000 ALTER TABLE `system_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `system_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transaction`
--

DROP TABLE IF EXISTS `transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transaction` (
  `transactionID` int NOT NULL AUTO_INCREMENT,
  `userID` int DEFAULT NULL,
  `transactionDate` datetime DEFAULT CURRENT_TIMESTAMP,
  `totalAmount` decimal(10,2) NOT NULL,
  `paymentMethod` enum('Cash','GCash') NOT NULL,
  `discountAmount` decimal(10,2) DEFAULT '0.00',
  `discountReason` varchar(100) DEFAULT NULL,
  `amountTendered` double DEFAULT '0',
  PRIMARY KEY (`transactionID`),
  KEY `userID` (`userID`),
  CONSTRAINT `transaction_ibfk_1` FOREIGN KEY (`userID`) REFERENCES `user` (`userID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transaction`
--

LOCK TABLES `transaction` WRITE;
/*!40000 ALTER TABLE `transaction` DISABLE KEYS */;
/*!40000 ALTER TABLE `transaction` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transaction_details`
--

DROP TABLE IF EXISTS `transaction_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transaction_details` (
  `detailID` int NOT NULL AUTO_INCREMENT,
  `transactionID` int DEFAULT NULL,
  `productID` int DEFAULT NULL,
  `quantity` int NOT NULL,
  `subtotal` decimal(10,2) NOT NULL,
  PRIMARY KEY (`detailID`),
  KEY `transactionID` (`transactionID`),
  KEY `productID` (`productID`),
  CONSTRAINT `transaction_details_ibfk_1` FOREIGN KEY (`transactionID`) REFERENCES `transaction` (`transactionID`) ON DELETE CASCADE,
  CONSTRAINT `transaction_details_ibfk_2` FOREIGN KEY (`productID`) REFERENCES `product` (`productID`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transaction_details`
--

LOCK TABLES `transaction_details` WRITE;
/*!40000 ALTER TABLE `transaction_details` DISABLE KEYS */;
/*!40000 ALTER TABLE `transaction_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `userID` int NOT NULL AUTO_INCREMENT,
  `username` varchar(30) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('Admin','Employee') NOT NULL,
  `name` varchar(30) NOT NULL,
  `contactInfo` varchar(50) DEFAULT NULL,
  `themePreference` varchar(10) DEFAULT 'dark',
  `status` varchar(20) DEFAULT 'Active',
  PRIMARY KEY (`userID`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (4,'Admin','1234','Admin','Test','admin@gmail.com','dark','Active');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'kamotomo_db'
--

--
-- Dumping routines for database 'kamotomo_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-21  0:15:48
