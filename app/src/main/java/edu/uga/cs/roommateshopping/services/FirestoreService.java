package edu.uga.cs.roommateshopping.services;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirestoreService {
    private final FirebaseFirestore mfirestore;

    public FirestoreService() {
        mfirestore = FirebaseFirestore.getInstance();
    }

    // Add a new user to Firestore after registration
    public void addUserToFirestore(String userId, String email, FirestoreCallback callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());

        mfirestore.collection("users").document(userId).set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess("User added successfully"))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Callback interface for handling success/failure
    public interface FirestoreCallback {
        void onSuccess(String message);

        void onFailure(String errorMessage);
    }
}
