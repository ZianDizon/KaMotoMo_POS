package com.kamotomo.pos.models;

public class TransactionRecord {
    private int id;
    private String date;
    private double total;
    private String paymentMethod;
    private String status;

    public TransactionRecord(int id, String date, double total, String paymentMethod, String status) {
        this.id = id;
        this.date = date;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.status = status;
    }

    public int getId() { return id; }
    public String getDate() { return date; }
    public double getTotal() { return total; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStatus() { return status; }
}