package edu.uga.cs.roomateshoppingapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import edu.uga.cs.roomateshoppingapp.models.User;

public class RegisterActivity extends AppCompatActivity {
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registerButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);

        // Set click listener
        registerButton.setOnClickListener(v -> attemptRegistration());
    }

    private void attemptRegistration() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.setError("Confirm password is required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            return;
        }

        // Show progress
        registerButton.setEnabled(false);

        // Attempt registration with Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    registerButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        // Registration success
                        String uid = mAuth.getCurrentUser().getUid();
                        User newUser = new User(uid, email);
                        
                        // Save user to database
                        FirebaseDatabase.getInstance().getReference()
                                .child("users")
                                .child(uid)
                                .setValue(newUser)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        Toast.makeText(RegisterActivity.this, 
                                                "Registration successful", 
                                                Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(RegisterActivity.this,
                                                "Failed to save user data: " + dbTask.getException().getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // If registration fails, display a message to the user
                        Toast.makeText(RegisterActivity.this, 
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
