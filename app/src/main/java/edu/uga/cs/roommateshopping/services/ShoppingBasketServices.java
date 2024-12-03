package edu.uga.cs.roommateshopping.services;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
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
    private final String userId;

    public ShoppingBasketServices() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid(); // Get the user ID if authenticated
            shoppingListRef = database.getReference("shopping_items");
            basketRef = database.getReference("users").child(userId).child("shopping_basket");
            purchaseRef = database.getReference("purchases");
        } else {
            userId = null; // User is not authenticated
            shoppingListRef = null;
            basketRef = null;
            purchaseRef = null;
        }
    }

    // Move an item to the shopping list from the basket
    public void moveItemToShoppingList(String itemId, ShoppingItem item, DatabaseCallback callback) {
        if (userId == null || shoppingListRef == null || basketRef == null) {
            callback.onFailure("User not authenticated or database references are null.");
            return;
        }
        shoppingListRef.child(itemId).setValue(item)
                .addOnSuccessListener(aVoid -> basketRef.child(itemId).removeValue()
                        .addOnSuccessListener(aVoid1 -> callback.onSuccess("Item moved to shopping list"))
                        .addOnFailureListener(e -> callback.onFailure("Failed to remove item from basket: " + e.getMessage())))
                .addOnFailureListener(e -> callback.onFailure("Failed to add item to shopping list: " + e.getMessage()));
    }

    // Move an item to the shopping basket from the shopping list
    public void moveItemToBasket(String itemId, ShoppingItem item, DatabaseCallback callback) {
        if (userId == null) {
            callback.onFailure("User not authenticated.");
            return;
        }

        // Check if basketRef exists
        basketRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailure("Failed to check basket existence: " + task.getException().getMessage());
                return;
            }

            // If the basket doesn't exist, create it
            if (!task.getResult().exists()) {
                basketRef.setValue(new HashMap<>()) // Create an empty basket node
                        .addOnSuccessListener(aVoid -> {
                            // After creating the basket, add the item
                            addItemToBasket(itemId, item, callback);
                        })
                        .addOnFailureListener(e -> callback.onFailure("Failed to create basket: " + e.getMessage()));
            } else {
                // Basket already exists, proceed to add the item
                addItemToBasket(itemId, item, callback);
            }
        });
    }

    // Helper method to add an item to the basket and remove it from the shopping list
    private void addItemToBasket(String itemId, ShoppingItem item, DatabaseCallback callback) {
        basketRef.child(itemId).setValue(item)
                .addOnSuccessListener(aVoid -> {
                    // Remove item from the shopping list
                    shoppingListRef.child(itemId).removeValue()
                            .addOnSuccessListener(aVoid1 -> callback.onSuccess("Item moved to basket"))
                            .addOnFailureListener(e -> callback.onFailure("Failed to remove item from shopping list: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to add item to basket: " + e.getMessage()));
    }

    // Check out the basket and move items to the purchased list
    public void checkoutBasket(List<ShoppingItem> basketItems, double taxRate, DatabaseCallback callback) {
        if (userId == null || basketRef == null) {
            callback.onFailure("User not authenticated or database references are null.");
            return;
        }
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

        Purchase purchase = new Purchase(
                itemsMap.keySet().stream().collect(Collectors.toList()),
                itemsMap.values().stream().map(ShoppingItem::getName).collect(Collectors.toList()),
                totalAmount,
                userId
        );

        purchaseRef.push().setValue(purchase)
                .addOnSuccessListener(aVoid -> basketRef.removeValue()
                        .addOnSuccessListener(aVoid1 -> callback.onSuccess("Checkout successful!"))
                        .addOnFailureListener(e -> callback.onFailure("Failed to clear basket: " + e.getMessage())))
                .addOnFailureListener(e -> callback.onFailure("Failed to save purchase: " + e.getMessage()));
    }

    // Fetch all items in the shopping basket
    public void getBasketItems(FetchBasketCallback callback) {
        basketRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ShoppingItem> basketItems = new ArrayList<>();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    ShoppingItem item = itemSnapshot.getValue(ShoppingItem.class);
                    if (item != null) {
                        basketItems.add(item);
                    }
                }
                callback.onSuccess(basketItems);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure("Failed to fetch basket items: " + error.getMessage());
            }
        });
    }

    // Callback for database actions
    public interface DatabaseCallback {
        void onSuccess(String message);

        void onFailure(String errorMessage);
    }

    // Callback for fetching basket items
    public interface FetchBasketCallback {
        void onSuccess(List<ShoppingItem> basketItems);

        void onFailure(String errorMessage);
    }
}

