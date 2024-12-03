package edu.uga.cs.roommateshopping.services;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.uga.cs.roommateshopping.models.Purchase;
import edu.uga.cs.roommateshopping.models.ShoppingItem;

public class ShoppingBasketServices {
    private final DatabaseReference shoppingListRef;
    private final DatabaseReference basketRef;
    private final DatabaseReference purchaseRef;

    public ShoppingBasketServices() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        shoppingListRef = database.getReference("shopping_items");
        basketRef = database.getReference("shopping_basket");
        purchaseRef = database.getReference("purchases");
    }

    // Move an item to the shopping list from the basket
    public void moveItemToShoppingList(String itemId, ShoppingItem item, DatabaseCallback callback) {
        shoppingListRef.child(itemId).setValue(item) // Add item to shopping list
                .addOnSuccessListener(aVoid -> basketRef.child(itemId).removeValue() // Remove item from basket
                        .addOnSuccessListener(aVoid1 -> callback.onSuccess("Item moved to shopping list"))
                        .addOnFailureListener(e -> callback.onFailure("Failed to remove item from basket: " + e.getMessage())))
                .addOnFailureListener(e -> callback.onFailure("Failed to add item to shopping list: " + e.getMessage()));
    }

    // Move an item to the shopping basket from the shopping list
    public void moveItemToBasket(String itemId, ShoppingItem item, DatabaseCallback callback) {
        basketRef.child(itemId).setValue(item) // Add item to basket
                .addOnSuccessListener(aVoid -> shoppingListRef.child(itemId).removeValue() // Remove item from shopping list
                        .addOnSuccessListener(aVoid1 -> callback.onSuccess("Item moved to basket"))
                        .addOnFailureListener(e -> callback.onFailure("Failed to remove item from shopping list: " + e.getMessage())))
                .addOnFailureListener(e -> callback.onFailure("Failed to add item to basket: " + e.getMessage()));
    }

    // Check out the basket and move items to the purchased list
    public void checkoutBasket(String purchasedBy, List<ShoppingItem> basketItems, double taxRate, DatabaseCallback callback) {
        if (basketItems.isEmpty()) {
            callback.onFailure("Basket is empty.");
            return;
        }

        double totalAmount = 0;
        Map<String, ShoppingItem> itemsMap = new HashMap<>();
        for (ShoppingItem item : basketItems) {
            totalAmount += item.getPrice() * item.getQuantity();
            itemsMap.put(item.getId(), item);
        }
        totalAmount += totalAmount * taxRate;

        // Create a new purchase object
        Purchase purchase = new Purchase(
                itemsMap.keySet().stream().collect(Collectors.toList()),
                itemsMap.values().stream().map(ShoppingItem::getName).collect(Collectors.toList()),
                totalAmount,
                purchasedBy
        );

        // Save the purchase
        purchaseRef.push().setValue(purchase)
                .addOnSuccessListener(aVoid -> basketRef.removeValue() // Clear basket after checkout
                        .addOnSuccessListener(aVoid1 -> callback.onSuccess("Checkout successful!"))
                        .addOnFailureListener(e -> callback.onFailure("Failed to clear basket: " + e.getMessage())))
                .addOnFailureListener(e -> callback.onFailure("Failed to save purchase: " + e.getMessage()));
    }

    public interface DatabaseCallback {
        void onSuccess(String message);

        void onFailure(String errorMessage);
    }
}

