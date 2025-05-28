package com.example.myapplication.help;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.objects.Article;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FirestoreHelper {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Context context;
    public FirestoreHelper(Context context) {
        this.context = context;
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

    //  Remove all followed sources
    public void removeAllFollowedSources() {
        String userId = auth.getCurrentUser().getUid();

        db.collection("user")
                .document(userId)
                .collection("followedSources")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        db.collection("user")
                                .document(userId)
                                .collection("followedSources")
                                .document(document.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> Log.d("FirestoreHelper", "Source removed: " + document.getId()))
                                .addOnFailureListener(e -> Log.w("FirestoreHelper", "Error removing source: ", e));
                    }
                    Toast.makeText(context, "All sources removed.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Error getting followed sources", e);
                    Toast.makeText(context, "Failed to remove sources.", Toast.LENGTH_SHORT).show();
                });
    }


    public void addFollowedSource(String source) {
        String userId = auth.getCurrentUser().getUid();

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
                                .addOnSuccessListener(documentReference -> Log.d("FirestoreHelper", "Source added: " + source))
                                .addOnFailureListener(e -> Log.w("FirestoreHelper", "Error adding source: ", e));
                    }
                });
    }

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
                                .addOnSuccessListener(aVoid -> Log.d("FirestoreHelper", "Source removed: " + source))
                                .addOnFailureListener(e -> Log.w("FirestoreHelper", "Error removing source: ", e));
                    }
                });
    }

    public void addToFavorites(Article article) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("user")
                .document(userId)
                .collection("favorites")
                .whereEqualTo("url", article.getUrl())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        db.collection("user")
                                .document(userId)
                                .collection("favorites")
                                .add(article);
                    } else {
                        Log.d("FirestoreHelper", "Article is already favorited.");
                    }
                })
                .addOnFailureListener(e -> Log.w("FirestoreHelper", "Error adding to favorites.", e));
    }

    public void removeFromFavorites(String articleUrl) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("user")
                .document(userId)
                .collection("favorites")
                .whereEqualTo("url", articleUrl)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String docId = task.getResult().getDocuments().get(0).getId();
                        db.collection("user")
                                .document(userId)
                                .collection("favorites")
                                .document(docId)
                                .delete();
                    }
                });
    }

    public void loadFavoriteArticles(Context context, RecyclerView recyclerViewFavorites, TextToSpeech tts) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("user")
                .document(userId)
                .collection("favorites")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Article> favoriteArticles = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Article article = doc.toObject(Article.class);
                        if (article != null) {
                            article.setFavorited(true);
                            favoriteArticles.add(article);
                        }
                    }

                    NewsAdapter adapter = new NewsAdapter(favoriteArticles, context, true);
                    adapter.setTextToSpeech(tts);
                    recyclerViewFavorites.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to load favorites.", Toast.LENGTH_SHORT).show();
                    Log.e("FirestoreHelper", "Error loading favorites", e);
                });
    }

    public interface OnFollowedSourcesLoadedListener {
        void onFollowedSourcesLoaded(List<String> followedSources);
    }
}
