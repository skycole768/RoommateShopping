package edu.uga.cs.roommateshopping.models;

public class ShoppingItem {
    private String id;          // Unique identifier for the item
    private String name;        // Name of the item
    private int quantity;       // Quantity of the item
    private double price;       // Price of the item
    private boolean purchased;  // Indicates if the item has been purchased
    private boolean selected;   // Indicates if the item is selected in the UI

    // Empty constructor for Firebase
    public ShoppingItem() {}

    // Constructor
    public ShoppingItem(String id, String name, int quantity, double price, boolean purchased) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.purchased = purchased;
        this.selected = false;  // Default value for selection
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return "ShoppingItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", purchased=" + purchased +
                ", selected=" + selected +
                '}';
    }
}

