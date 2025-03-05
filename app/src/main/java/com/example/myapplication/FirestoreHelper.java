package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirestoreHelper {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public FirestoreHelper(Context context) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // Load followed domains from Firestore for the current user
    public void loadFollowedDomains(OnFollowedDomainsLoadedListener listener) {
        String userId = auth.getCurrentUser().getUid(); // Get the current user's ID
        db.collection("users")
                .document(userId)
                .collection("followedDomains")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> followedDomains = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            followedDomains.add(document.getString("domain"));
                        }
                        listener.onFollowedDomainsLoaded(followedDomains);
                    } else {
                        listener.onFollowedDomainsLoaded(new ArrayList<>());
                    }
                });
    }

    // Add a new followed domain for the current user
    public void addFollowedDomain(String domain) {
        String userId = auth.getCurrentUser().getUid(); // Get the current user's ID

        // Check if the domain is already followed by this user
        db.collection("users")
                .document(userId)
                .collection("followedDomains")
                .whereEqualTo("domain", domain)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().isEmpty()) {
                        // If the domain is not already followed, add it
                        db.collection("users")
                                .document(userId)
                                .collection("followedDomains")
                                .add(new java.util.HashMap<String, Object>() {{
                                    put("domain", domain); // Store domain as a string
                                }})
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("FirestoreHelper", "Domain added: " + domain);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w("FirestoreHelper", "Error adding domain: ", e);
                                });
                    } else {
                        // If the domain is already followed, log a message or show a toast
                        Log.d("FirestoreHelper", "Domain already followed: " + domain);
                    }
                });
    }

    // Remove a followed domain from Firestore for the current user
    public void removeFollowedDomain(String domain) {
        String userId = auth.getCurrentUser().getUid(); // Get the current user's ID

        db.collection("users")
                .document(userId)
                .collection("followedDomains")
                .whereEqualTo("domain", domain)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String documentId = task.getResult().getDocuments().get(0).getId();
                        db.collection("users")
                                .document(userId)
                                .collection("followedDomains")
                                .document(documentId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FirestoreHelper", "Domain removed: " + domain);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w("FirestoreHelper", "Error removing domain: ", e);
                                });
                    }
                });
    }

    // Interface for callback when followed domains are loaded
    public interface OnFollowedDomainsLoadedListener {
        void onFollowedDomainsLoaded(List<String> followedDomains);
    }
}
