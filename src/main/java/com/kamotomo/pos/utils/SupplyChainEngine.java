package com.kamotomo.pos.utils;

import com.kamotomo.pos.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SupplyChainEngine {

    /**
     * Calculates the Reorder Point (ROP)
     * ROP = (Average Daily Sales * Lead Time) + Safety Stock
     */
    public static int calculateDynamicROP(double averageDailySales, int leadTime, int safetyStock) {
        return (int) Math.ceil((averageDailySales * leadTime) + safetyStock);
    }

    /**
     * Calculates Economic Order Quantity (EOQ)
     * EOQ = √ (2 * Annual Demand * Order Cost / Holding Cost)
     */
    public static int calculateEOQ(double annualDemand, double orderCost, double holdingCost) {
        if (holdingCost <= 0) return 0; // Prevent divide-by-zero errors
        return (int) Math.ceil(Math.sqrt((2 * annualDemand * orderCost) / holdingCost));
    }

    /**
     * Master method: Analyzes real database history to generate a restock recommendation.
     * Returns an integer array: [0] = Recommended ROP, [1] = Recommended EOQ
     */
    public static int[] getRestockRecommendations(int productId) {
        int[] results = new int[]{0, 0}; // Default fallback

        try (Connection conn = DatabaseConnection.getConnection()) {

            // 1. Get supply chain variables from the product
            String prodSql = "SELECT supplierLeadTimeDays, safetyStock, orderCost, holdingCost FROM PRODUCT WHERE productID = ?";
            PreparedStatement prodStmt = conn.prepareStatement(prodSql);
            prodStmt.setInt(1, productId);
            ResultSet prodRs = prodStmt.executeQuery();

            if (!prodRs.next()) return results; // Product not found

            int leadTime = prodRs.getInt("supplierLeadTimeDays");
            int safetyStock = prodRs.getInt("safetyStock");
            double orderCost = prodRs.getDouble("orderCost");
            double holdingCost = prodRs.getDouble("holdingCost");

            // 2. Calculate actual sales velocity from the last 30 days
            String salesSql = "SELECT SUM(td.quantity) AS totalSold " +
                    "FROM TRANSACTION_DETAILS td " +
                    "JOIN TRANSACTION t ON td.transactionID = t.transactionID " +
                    "WHERE td.productID = ? AND t.transactionDate >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
            PreparedStatement salesStmt = conn.prepareStatement(salesSql);
            salesStmt.setInt(1, productId);
            ResultSet salesRs = salesStmt.executeQuery();

            int soldLast30Days = 0;
            if (salesRs.next()) {
                soldLast30Days = salesRs.getInt("totalSold");
            }

            // 3. Crunch the numbers
            double averageDailySales = soldLast30Days / 30.0;
            double projectedAnnualDemand = averageDailySales * 365.0;

            int dynamicROP = calculateDynamicROP(averageDailySales, leadTime, safetyStock);
            int recommendedEOQ = calculateEOQ(projectedAnnualDemand, orderCost, holdingCost);

            results[0] = dynamicROP;
            results[1] = recommendedEOQ;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
}