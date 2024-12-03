package edu.uga.cs.roommateshopping.services;

import android.util.Patterns;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class AuthService {
    private final FirebaseAuth mAuth;

    public AuthService() {
        mAuth = FirebaseAuth.getInstance();
    }

    // Validate email format
    public boolean isEmailValid(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public boolean isPasswordValid(String password) {
        if (password == null) {
            return false;
        }

        // At least one letter/one digit/one special character and min of 7 characters
        String passwordPattern = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d#@$!%*?&]{7,}$";

        return password.matches(passwordPattern);
    }

    // Register a new user
    public void registerUser(String email, String password, AuthCallback callback) {
        if (!isEmailValid(email)) {
            callback.onFailure("Invalid email format.");
            return;
        }

        if (!isPasswordValid(password)) {
            callback.onFailure("Password must be at least 7 characters long and include a number, a letter, and a unique character.");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(mAuth.getCurrentUser());
                    } else if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        callback.onFailure("Email already exists");
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Login user
    public void loginUser(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(mAuth.getCurrentUser());
                    } else {
                        callback.onFailure("Invalid email or password");
                    }
                });
    }

    // Check if the user is logged in
    public boolean isLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    // Logout user
    public void logoutUser() {
        mAuth.signOut();
    }

    // Callback interface for handling success/failure
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);

        void onFailure(String errorMessage);
    }
}