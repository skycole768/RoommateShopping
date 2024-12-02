package edu.uga.cs.roommateshopping;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Toast;
import android.widget.Button;
import edu.uga.cs.roommateshopping.services.AuthService;


public class MainActivity extends AppCompatActivity {
    private AuthService authService;
    private Button loginLogoutButton;
    private Button viewShoppingBasketButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authService = new AuthService();
        loginLogoutButton = findViewById(R.id.logoutButton);
        viewShoppingBasketButton = findViewById(R.id.viewShoppingBasket);
        // Set initial button state
        updateButtonState();

        loginLogoutButton.setOnClickListener(v -> {
            if (authService.isLoggedIn()) {
                // User is signed in
                authService.logoutUser();
                Toast.makeText(MainActivity.this, "Logged out successfully!", Toast.LENGTH_SHORT).show();
                updateButtonState();
            } else {
                // User is not signed in
                Intent loginIntent = new Intent(MainActivity.this, edu.uga.cs.roommateshopping.RegisterActivity.class);
               startActivity(loginIntent);
            }
        });

        viewShoppingBasketButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ShoppingBasketActivity.class);
            startActivity(intent);
        });


}

    // Change button text depending on whether or not
    private void updateButtonState() {
        if (authService.isLoggedIn()) {
            loginLogoutButton.setText("logout");
        } else {
            loginLogoutButton.setText("login");
        }
    }
}
