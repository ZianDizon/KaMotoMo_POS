-- KaMotoMo POS System Full Backup
-- Generated: 2026-05-22 01:18:08
SET FOREIGN_KEY_CHECKS=0;

-- Dump for table: user
INSERT INTO `user` VALUES ('4', 'Admin', '1234', 'Admin', 'Test', 'admin@gmail.com', 'light', 'Active');
INSERT INTO `user` VALUES ('5', 'abc', '1234', 'Employee', '1', '123', 'dark', 'Active');


-- Dump for table: PRODUCT
INSERT INTO `PRODUCT` VALUES ('1', '', '120.00', NULL, '25', 'Active', 'Engine Parts');
INSERT INTO `PRODUCT` VALUES ('2', 'TEST PRODUCR', '112121.00', NULL, '0', 'Active', 'Brakes');


-- Dump for table: PURCHASE_ORDER
INSERT INTO `PURCHASE_ORDER` VALUES ('1', 'General Supplier A', '2026-05-19T14:27:52', 'Received', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('2', 'General Supplier A', '2026-05-19T14:32:22', 'Received', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('3', 'General Supplier A', '2026-05-19T14:32:58', 'Received', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('4', 'General Supplier A', '2026-05-19T14:34:58', 'Received', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('5', 'General Supplier A', '2026-05-20T09:14:38', 'Cancelled', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('6', 'General Supplier A', '2026-05-20T09:20:30', 'Cancelled', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('7', 'General Supplier A', '2026-05-20T09:20:51', 'Cancelled', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('8', 'General Supplier A', '2026-05-20T09:21:21', 'Received', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('9', 'General Supplier A', '2026-05-20T09:25:40', 'Cancelled', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('10', 'General Supplier A', '2026-05-20T09:25:57', 'Cancelled', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('11', 'General Supplier A', '2026-05-20T09:26:12', 'Received', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('12', 'General Supplier A', '2026-05-20T09:26:32', 'Received', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('13', 'General Supplier A', '2026-05-20T09:26:47', 'Received', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('14', 'General Supplier A', '2026-05-20T09:27:05', 'Cancelled', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('15', 'General Supplier A', '2026-05-20T09:30:39', 'Cancelled', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('16', 'General Supplier A', '2026-05-20T09:30:49', 'Received', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('17', 'General Supplier A', '2026-05-20T09:30:59', 'Cancelled', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('18', 'General Supplier A', '2026-05-20T09:31:15', 'Received', 'System Admin');
INSERT INTO `PURCHASE_ORDER` VALUES ('19', 'General Supplier A', '2026-05-20T16:33:30', 'Cancelled', 'System Administrator');
INSERT INTO `PURCHASE_ORDER` VALUES ('20', 'General Supplier A', '2026-05-20T22:17:26', 'Cancelled', 'System Administrator');
INSERT INTO `PURCHASE_ORDER` VALUES ('21', 'General Supplier A', '2026-05-20T23:10:37', 'Received', 'System Administrator');
INSERT INTO `PURCHASE_ORDER` VALUES ('22', 'General Supplier A', '2026-05-20T23:10:49', 'Cancelled', 'System Administrator');
INSERT INTO `PURCHASE_ORDER` VALUES ('23', 'General Supplier A', '2026-05-22T00:20:15', 'Cancelled', 'Test');
INSERT INTO `PURCHASE_ORDER` VALUES ('24', 'General Supplier A', '2026-05-22T00:20:32', 'Received', 'Test');
INSERT INTO `PURCHASE_ORDER` VALUES ('25', 'General Supplier A', '2026-05-22T00:20:57', 'Cancelled', 'Test');
INSERT INTO `PURCHASE_ORDER` VALUES ('26', 'General Supplier A', '2026-05-22T00:21:10', 'Received', 'Test');
INSERT INTO `PURCHASE_ORDER` VALUES ('27', 'General Supplier A', '2026-05-22T00:21:29', 'Received', 'Test');


-- Dump for table: PO_ITEM
INSERT INTO `PO_ITEM` VALUES ('1', '4', '18');
INSERT INTO `PO_ITEM` VALUES ('1', '7', '15');
INSERT INTO `PO_ITEM` VALUES ('1', '8', '13');
INSERT INTO `PO_ITEM` VALUES ('1', '9', '15');
INSERT INTO `PO_ITEM` VALUES ('1', '10', '13');
INSERT INTO `PO_ITEM` VALUES ('1', '12', '10');
INSERT INTO `PO_ITEM` VALUES ('1', '15', '16');
INSERT INTO `PO_ITEM` VALUES ('1', '1', '10');
INSERT INTO `PO_ITEM` VALUES ('2', '1', '1');
INSERT INTO `PO_ITEM` VALUES ('3', '1', '1');
INSERT INTO `PO_ITEM` VALUES ('4', '1', '1');
INSERT INTO `PO_ITEM` VALUES ('5', '1', '10');
INSERT INTO `PO_ITEM` VALUES ('6', '1', '10');
INSERT INTO `PO_ITEM` VALUES ('7', '2', '10');
INSERT INTO `PO_ITEM` VALUES ('8', '1', '1');
INSERT INTO `PO_ITEM` VALUES ('9', '1', '10');
INSERT INTO `PO_ITEM` VALUES ('10', '1', '10');
INSERT INTO `PO_ITEM` VALUES ('11', '2', '1');
INSERT INTO `PO_ITEM` VALUES ('12', '1', '1');
INSERT INTO `PO_ITEM` VALUES ('13', '1', '1');
INSERT INTO `PO_ITEM` VALUES ('14', '2', '1');
INSERT INTO `PO_ITEM` VALUES ('15', '1', '1');
INSERT INTO `PO_ITEM` VALUES ('16', '1', '1');
INSERT INTO `PO_ITEM` VALUES ('17', '5', '1');
INSERT INTO `PO_ITEM` VALUES ('18', '7', '1');
INSERT INTO `PO_ITEM` VALUES ('19', '5', '1');
INSERT INTO `PO_ITEM` VALUES ('20', '5', '1');
INSERT INTO `PO_ITEM` VALUES ('20', '1', '1');
INSERT INTO `PO_ITEM` VALUES ('21', '5', '1');
INSERT INTO `PO_ITEM` VALUES ('21', '16', '1');
INSERT INTO `PO_ITEM` VALUES ('22', '5', '16');
INSERT INTO `PO_ITEM` VALUES ('22', '16', '12');
INSERT INTO `PO_ITEM` VALUES ('23', '1', '13');
INSERT INTO `PO_ITEM` VALUES ('24', '1', '1');
INSERT INTO `PO_ITEM` VALUES ('25', '1', '12');
INSERT INTO `PO_ITEM` VALUES ('26', '1', '10');
INSERT INTO `PO_ITEM` VALUES ('27', '1', '10');


-- Dump for table: TRANSACTION
INSERT INTO `TRANSACTION` VALUES ('1', '4', '2026-05-22T00:33:35', '120.00', 'Cash', '0.00', '', '200.0');
INSERT INTO `TRANSACTION` VALUES ('2', '4', '2026-05-22T00:41:05', '112121.00', 'Cash', '0.00', '', '1.1111111E7');


-- Dump for table: TRANSACTION_DETAILS
INSERT INTO `TRANSACTION_DETAILS` VALUES ('1', '1', '1', '1', '120.00');
INSERT INTO `TRANSACTION_DETAILS` VALUES ('2', '2', '2', '1', '112121.00');


-- Dump for table: system_log
INSERT INTO `system_log` VALUES ('1', '2026-05-22T00:33:24', 'Admin', 'System', 'Administrator truncated all system logs.');
INSERT INTO `system_log` VALUES ('2', '2026-05-22T00:33:35', 'Admin', 'Transaction', 'Sale ID: 1 | Total: ₱120.00');
INSERT INTO `system_log` VALUES ('3', '2026-05-22T00:39:37', 'Admin', 'Login', 'User successfully authenticated.');
INSERT INTO `system_log` VALUES ('4', '2026-05-22T00:40:28', 'Admin', 'Maintenance', 'Generated full system SQL backup.');
INSERT INTO `system_log` VALUES ('5', '2026-05-22T00:40:59', 'Admin', 'Inventory', 'Added new product: TEST PRODUCR');
INSERT INTO `system_log` VALUES ('6', '2026-05-22T00:41:05', 'Admin', 'Transaction', 'Sale ID: 2 | Total: ₱112121.00');
INSERT INTO `system_log` VALUES ('7', '2026-05-22T00:53:58', 'Admin', 'Maintenance', 'Admin initiated database restoration protocol.');
INSERT INTO `system_log` VALUES ('8', '2026-05-22T00:56:28', 'Admin', 'Login', 'User successfully authenticated.');
INSERT INTO `system_log` VALUES ('9', '2026-05-22T00:59:33', 'Admin', 'Login', 'User successfully authenticated.');
INSERT INTO `system_log` VALUES ('10', '2026-05-22T01:03:23', 'Admin', 'Login', 'User successfully authenticated.');
INSERT INTO `system_log` VALUES ('11', '2026-05-22T01:13:51', 'Admin', 'Login', 'User successfully authenticated.');
INSERT INTO `system_log` VALUES ('12', '2026-05-22T01:17:13', 'Admin', 'Login', 'User successfully authenticated.');


SET FOREIGN_KEY_CHECKS=1;
