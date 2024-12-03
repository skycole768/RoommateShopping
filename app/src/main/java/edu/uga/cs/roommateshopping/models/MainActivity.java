package edu.uga.cs.roomateshoppingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import edu.uga.cs.roomateshoppingapp.adapters.PurchaseHistoryAdapter;
import edu.uga.cs.roomateshoppingapp.adapters.ShoppingBasketAdapter;
import edu.uga.cs.roomateshoppingapp.adapters.ShoppingListAdapter;
import edu.uga.cs.roomateshoppingapp.models.Purchase;
import edu.uga.cs.roomateshoppingapp.models.ShoppingItem;

public class MainActivity extends AppCompatActivity implements ShoppingListAdapter.OnItemSelectionListener, ShoppingBasketAdapter.OnBasketItemActionListener {
    private static final String TAG = "MainActivity";
    private EditText itemNameEditText;
    private Button addItemButton;
    private Button logoutButton;
    private Button checkoutButton;
    private Button settleCostButton;
    private Button viewPurchaseHistoryButton;
    private RecyclerView shoppingListRecyclerView;
    private RecyclerView shoppingBasketRecyclerView;
    private TextView userEmailTextView;
    
    private ShoppingListAdapter shoppingListAdapter;
    private ShoppingBasketAdapter shoppingBasketAdapter;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    
    private List<ShoppingItem> shoppingList;
    private List<ShoppingItem> shoppingBasket;
    
    private ValueEventListener shoppingListListener;
    private ValueEventListener purchaseHistoryListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        itemNameEditText = findViewById(R.id.itemNameEditText);
        addItemButton = findViewById(R.id.addItemButton);
        logoutButton = findViewById(R.id.logoutButton);
        checkoutButton = findViewById(R.id.checkoutButton);
        settleCostButton = findViewById(R.id.settleCostButton);
        viewPurchaseHistoryButton = findViewById(R.id.viewPurchaseHistoryButton);
        shoppingListRecyclerView = findViewById(R.id.shoppingListRecyclerView);
        shoppingBasketRecyclerView = findViewById(R.id.shoppingBasketRecyclerView);
        userEmailTextView = findViewById(R.id.userEmailTextView);

        // Set user email
        if (mAuth.getCurrentUser() != null) {
            userEmailTextView.setText(mAuth.getCurrentUser().getEmail());
        }

        // Initialize lists
        shoppingList = new ArrayList<>();
        shoppingBasket = new ArrayList<>();
        
        // Set up RecyclerViews
        shoppingListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        shoppingBasketRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize adapters
        shoppingListAdapter = new ShoppingListAdapter(shoppingList, this);
        shoppingBasketAdapter = new ShoppingBasketAdapter(shoppingBasket, this);
        
        // Set adapters
        shoppingListRecyclerView.setAdapter(shoppingListAdapter);
        shoppingBasketRecyclerView.setAdapter(shoppingBasketAdapter);

        // Restore shopping basket state
        if (savedInstanceState != null) {
            ArrayList<String> basketItemIds = savedInstanceState.getStringArrayList("shopping_basket_ids");
            if (basketItemIds != null) {
                for (String itemId : basketItemIds) {
                    mDatabase.child("shopping_items").child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            ShoppingItem item = dataSnapshot.getValue(ShoppingItem.class);
                            if (item != null) {
                                item.setId(dataSnapshot.getKey()); // Make sure to set the ID
                                shoppingBasket.add(item);
                                shoppingBasketAdapter.notifyItemInserted(shoppingBasket.size() - 1);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(MainActivity.this, 
                                "Failed to load shopping basket", 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        // Set click listeners
        addItemButton.setOnClickListener(v -> addNewItem());
        logoutButton.setOnClickListener(v -> logout());
        checkoutButton.setOnClickListener(v -> checkout());
        settleCostButton.setOnClickListener(v -> startSettleCost());
        viewPurchaseHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PurchaseHistoryActivity.class);
            startActivity(intent);
        });

        // Load data
        loadShoppingList();
        loadPurchaseHistory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save selected items positions
        if (shoppingListAdapter != null) {
            ArrayList<Integer> selectedPositions = shoppingListAdapter.getSelectedPositions();
            outState.putIntegerArrayList("selectedPositions", selectedPositions);
        }
        // Save shopping basket items
        if (shoppingBasket != null && !shoppingBasket.isEmpty()) {
            ArrayList<String> basketItemIds = new ArrayList<>();
            for (ShoppingItem item : shoppingBasket) {
                basketItemIds.add(item.getId());
            }
            outState.putStringArrayList("shopping_basket_ids", basketItemIds);
        }
    }

    private void addNewItem() {
        String itemName = itemNameEditText.getText().toString().trim();
        if (itemName.isEmpty()) {
            itemNameEditText.setError("Item name is required");
            return;
        }

        ShoppingItem newItem = new ShoppingItem(itemName);
        String itemId = mDatabase.child("shopping_items").push().getKey();
        newItem.setId(itemId);

        mDatabase.child("shopping_items").child(itemId).setValue(newItem)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        itemNameEditText.setText("");
                        Toast.makeText(MainActivity.this, "Item added successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to add item", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadShoppingList() {
        if (mAuth.getCurrentUser() == null) return;

        if (shoppingListListener != null) {
            mDatabase.child("shopping_items").removeEventListener(shoppingListListener);
        }

        shoppingListListener = mDatabase.child("shopping_items")
                .orderByChild("purchased")
                .equalTo(false)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        shoppingList.clear();
                        
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            ShoppingItem item = snapshot.getValue(ShoppingItem.class);
                            if (item != null) {
                                item.setId(snapshot.getKey()); // Make sure to set the ID
                                shoppingList.add(item);
                            }
                        }
                        
                        shoppingListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this, 
                            "Failed to load shopping list", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadPurchaseHistory() {
        if (purchaseHistoryListener != null) {
            mDatabase.child("purchases").removeEventListener(purchaseHistoryListener);
        }
        
        purchaseHistoryListener = mDatabase.child("purchases")
                .orderByChild("purchaseDate")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Do nothing
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this, 
                            "Failed to load purchase history", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onItemSelected(int position, boolean isSelected) {
        if (position < 0 || position >= shoppingList.size()) return;
        
        try {
            ShoppingItem item = shoppingList.get(position);
            if (item == null) return;
            
            // Remove from shopping list first
            shoppingList.remove(position);
            shoppingListAdapter.notifyItemRemoved(position);
            
            // Then add to basket
            shoppingBasket.add(item);
            shoppingBasketAdapter.notifyItemInserted(shoppingBasket.size() - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRemoveFromBasket(int position) {
        try {
            ShoppingItem item = shoppingBasket.get(position);
            shoppingBasket.remove(position);
            shoppingList.add(item);
            shoppingBasketAdapter.notifyItemRemoved(position);
            shoppingListAdapter.notifyItemInserted(shoppingList.size() - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startSettleCost() {
        startActivity(new Intent(MainActivity.this, SettleCostActivity.class));
    }

    private void checkout() {
        if (shoppingBasket.isEmpty()) {
            Toast.makeText(this, "Shopping basket is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create dialog to enter cost
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_purchase_items, null);
        EditText totalPriceEditText = dialogView.findViewById(R.id.totalPriceEditText);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Enter Total Cost")
                .setView(dialogView)
                .setPositiveButton("Confirm", (dialogInterface, i) -> {
                    String costStr = totalPriceEditText.getText().toString();
                    if (costStr.isEmpty()) {
                        Toast.makeText(this, "Please enter a cost", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        double totalAmount = Double.parseDouble(costStr);
                        savePurchaseToHistory(totalAmount);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid cost format", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private void savePurchaseToHistory(double totalAmount) {
        if (mAuth.getCurrentUser() == null) return;

        List<String> itemIds = new ArrayList<>();
        List<String> itemNames = new ArrayList<>();
        List<Task<Void>> updateTasks = new ArrayList<>();

        for (ShoppingItem item : shoppingBasket) {
            itemIds.add(item.getId());
            itemNames.add(item.getName());
            
            // Mark item as purchased in shopping_items
            item.setPurchased(true);
            item.setPurchasedBy(mAuth.getCurrentUser().getEmail());
            item.setPurchasedDate(System.currentTimeMillis());
            
            // Add update task to our list
            Task<Void> updateTask = mDatabase.child("shopping_items")
                    .child(item.getId())
                    .setValue(item);
            updateTasks.add(updateTask);
        }

        Purchase purchase = new Purchase(itemIds, itemNames, totalAmount, mAuth.getCurrentUser().getEmail());

        // First save all item updates
        Tasks.whenAllComplete(updateTasks)
                .addOnCompleteListener(task -> {
                    // Then save the purchase
                    mDatabase.child("purchases")
                            .push()
                            .setValue(purchase)
                            .addOnSuccessListener(aVoid -> {
                                // Clear the shopping basket
                                shoppingBasket.clear();
                                shoppingBasketAdapter.notifyDataSetChanged();
                                Toast.makeText(MainActivity.this, 
                                    "Purchase saved to history", 
                                    Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> 
                                Toast.makeText(MainActivity.this, 
                                    "Failed to save purchase", 
                                    Toast.LENGTH_SHORT).show()
                            );
                });
    }

    private void logout() {
        // Remove Firebase listeners
        if (shoppingListListener != null) {
            mDatabase.child("shopping_items").removeEventListener(shoppingListListener);
            shoppingListListener = null;
        }
        if (purchaseHistoryListener != null) {
            mDatabase.child("purchases").removeEventListener(purchaseHistoryListener);
            purchaseHistoryListener = null;
        }
        
        // Clear adapters
        if (shoppingListAdapter != null) {
            shoppingListAdapter.updateItems(new ArrayList<>());
        }
        if (shoppingBasketAdapter != null) {
            shoppingBasketAdapter.updateItems(new ArrayList<>());
        }
        
        // Sign out and redirect to login
        mAuth.signOut();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}
