package edu.uga.cs.roommateshopping;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import edu.uga.cs.roommateshopping.adapters.ShoppingBasketAdapter;
import edu.uga.cs.roommateshopping.models.ShoppingItem;
import edu.uga.cs.roommateshopping.services.ShoppingBasketServices;

public class ShoppingBasketActivity extends AppCompatActivity {

    private DatabaseReference shoppingBasketRef;
    private List<ShoppingItem> basketItems;
    private ShoppingBasketAdapter basketAdapter;
    private Button checkoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shopping_basket);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialize UI components
        RecyclerView basketRecyclerView = findViewById(R.id.basketRecyclerView);
        checkoutButton = findViewById(R.id.checkoutButton);

        // Setup RecyclerView
        basketRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize data structures
        basketItems = new ArrayList<>();
        basketAdapter = new ShoppingBasketAdapter(basketItems);
        basketRecyclerView.setAdapter(basketAdapter);

        // Firebase reference to shopping basket
        shoppingBasketRef = FirebaseDatabase.getInstance().getReference("shopping_basket");

        // Real-time Listener for Shopping Basket
        shoppingBasketRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                basketItems.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    ShoppingItem item = itemSnapshot.getValue(ShoppingItem.class);
                    if (item != null) basketItems.add(item);
                }
                basketAdapter.updateItems(basketItems);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ShoppingBasketActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Handle checkout button click
        checkoutButton.setOnClickListener(v -> handleCheckout());
    }

    /**
     * Handles the checkout process by moving items from the basket to the purchased list.
     */
    private void handleCheckout() {
        if (basketItems.isEmpty()) {
            Toast.makeText(this, "Your basket is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Call the service to perform the checkout
        ShoppingBasketServices basketService = new ShoppingBasketServices();
        basketService.checkoutBasket("CurrentUser", basketItems, 0.07, new ShoppingBasketServices.DatabaseCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(ShoppingBasketActivity.this, message, Toast.LENGTH_SHORT).show();
                basketItems.clear();
                basketAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(ShoppingBasketActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
