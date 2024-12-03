package edu.uga.cs.roommateshopping;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.uga.cs.roommateshopping.adapters.ShoppingBasketAdapter;
import edu.uga.cs.roommateshopping.models.ShoppingItem;
import edu.uga.cs.roommateshopping.services.ShoppingBasketServices;

public class ShoppingBasketActivity extends AppCompatActivity {

    private List<ShoppingItem> basketItems;
    private ShoppingBasketAdapter basketAdapter;
    private Button checkoutButton;
    private ShoppingBasketServices basketService;

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

        // Initialize ShoppingBasketServices
        basketService = new ShoppingBasketServices();

        // Load basket items
        loadBasketItems();

        // Handle checkout button click
        checkoutButton.setOnClickListener(v -> handleCheckout());
    }


     // Loads the shopping basket items using ShoppingBasketServices
    private void loadBasketItems() {
        basketService.getBasketItems(new ShoppingBasketServices.FetchBasketCallback() {
            @Override
            public void onSuccess(List<ShoppingItem> items) {
                basketItems.clear();
                basketItems.addAll(items);
                basketAdapter.updateItems(basketItems);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(ShoppingBasketActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Handles the checkout process by moving items from the basket to the purchased list.
    private void handleCheckout() {
        if (basketItems.isEmpty()) {
            Toast.makeText(this, "Your basket is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        basketService.checkoutBasket(basketItems, 0.07, new ShoppingBasketServices.DatabaseCallback() {
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
