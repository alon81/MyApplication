package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FirestoreHelper {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public FirestoreHelper(Context context) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void loadFollowedSources(OnFollowedSourcesLoadedListener listener) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("user")
                .document(userId)
                .collection("followedSources")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> followedSources = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            followedSources.add(document.getString("source"));
                        }
                        listener.onFollowedSourcesLoaded(followedSources);
                    } else {
                        listener.onFollowedSourcesLoaded(new ArrayList<>());
                    }
                });
    }

    // Add a new followed source for the current user
    public void addFollowedSource(String source) {
        String userId = auth.getCurrentUser().getUid();

        // Chec source is already followed
        db.collection("user")
                .document(userId)
                .collection("followedSources")
                .whereEqualTo("source", source)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().isEmpty()) {
                        db.collection("user")
                                .document(userId)
                                .collection("followedSources")
                                .add(new HashMap<String, Object>() {{
                                    put("source", source);
                                }})
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("FirestoreHelper", "Source added: " + source);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w("FirestoreHelper", "Error adding source: ", e);
                                });
                    }
                });
    }

    // Remove a source
    public void removeFollowedSource(String source) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("user")
                .document(userId)
                .collection("followedSources")
                .whereEqualTo("source", source)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String documentId = task.getResult().getDocuments().get(0).getId();
                        db.collection("user")
                                .document(userId)
                                .collection("followedSources")
                                .document(documentId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FirestoreHelper", "Source removed: " + source);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w("FirestoreHelper", "Error removing source: ", e);
                                });
                    }
                });
    }

    public interface OnFollowedSourcesLoadedListener {
        void onFollowedSourcesLoaded(List<String> followedSources);
    }
}
