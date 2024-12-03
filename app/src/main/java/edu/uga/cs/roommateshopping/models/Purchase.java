package edu.uga.cs.roommateshopping.models;

import java.util.List;

public class Purchase {
    private String id;
    private List<String> itemIds;  // References to shopping items
    private double totalAmount;
    private String purchasedBy;
    private long purchaseDate;
    private List<String> itemNames;  // For easy display without fetching items

    // Required empty constructor for Firebase
    public Purchase() {}

    public Purchase(List<String> itemIds, List<String> itemNames, double totalAmount, String purchasedBy) {
        this.itemIds = itemIds;
        this.itemNames = itemNames;
        this.totalAmount = totalAmount;
        this.purchasedBy = purchasedBy;
        this.purchaseDate = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<String> itemIds) {
        this.itemIds = itemIds;
    }

    public List<String> getItemNames() {
        return itemNames;
    }

    public void setItemNames(List<String> itemNames) {
        this.itemNames = itemNames;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPurchasedBy() {
        return purchasedBy;
    }

    public void setPurchasedBy(String purchasedBy) {
        this.purchasedBy = purchasedBy;
    }

    public long getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(long purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
}
