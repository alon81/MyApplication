package com.example.myapplication.help;

import static android.app.PendingIntent.getActivity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.objects.Article;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ArticleViewHolder> {

    private final List<Article> articleList;
    private TextToSpeech textToSpeech;
    private final FirebaseFirestore db;
    private final String userId;
    private final Context context;  // Store the context for Toast

    // In-memory storage for favorited URLs
    private final Set<String> favoriteUrls = new HashSet<>();

    // Update constructor to accept Context as a parameter
    public NewsAdapter(List<Article> articleList, Context context) {
        this.articleList = articleList;
        this.context = context;  // Store the context
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();  // Get userId here
    }


    @Override
    public ArticleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }
    @Override
    public void onBindViewHolder(ArticleViewHolder holder, int position) {
        if (position >= 0 && position < articleList.size()) {
            Article article = articleList.get(position);
            holder.titleTextView.setText(article.getTitle());
            holder.sourceTextView.setText(article.getSource().getName());

            String articleUrl = article.getUrl();

            // Set URL dot clickable
            holder.sourceTextView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl));
                v.getContext().startActivity(intent);
            });

            holder.titleTextView.setOnClickListener(v -> {
                if (textToSpeech != null) {
                    String title = article.getTitle();

                    // Detect Hebrew (basic check based on Unicode range)
                    boolean isHebrew = title.matches(".*\\p{InHebrew}.*");

                    // Set language accordingly
                    Locale targetLocale = isHebrew ? new Locale("he") : Locale.US;
                    int result = textToSpeech.setLanguage(targetLocale);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(context, "Selected language not supported on this device.", Toast.LENGTH_SHORT).show();
                    } else {
                        textToSpeech.speak(title, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
            });



            // Star icon: toggle favorite state
            boolean isFavorited = article.isFavorited();  // Check the favorited state from the Article object
            holder.starImageView.setImageResource(R.drawable.ic_fav);
            holder.starImageView.setColorFilter(isFavorited ? Color.RED : Color.GRAY);

            holder.starImageView.setOnClickListener(v -> {
                if (isFavorited) {
                    article.setFavorited(false);  // Unfavorite the article
                    favoriteUrls.remove(articleUrl);
                    removeFromFavorites(articleUrl); // Remove from Firebase
                } else {
                    article.setFavorited(true);  // Favorite the article
                    favoriteUrls.add(articleUrl);
                    addToFavorites(article); // Add to Firebase
                }
                notifyItemChanged(position);  // Notify that the item has changed (star icon)
            });

            // Email button to send the article via email
            holder.shareButton.setOnClickListener(v -> {
                shareArticle(article);
            });
        }
    }

    // Method to show the email dialog
    // Method to show the email dialog
    // Method to show the email dialog
    // Method to show the email dialog
    private void shareArticle(Article article) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        String shareMessage = "Check out this article:\n" +
                article.getTitle() + "\n" + article.getUrl();

        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share article via"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No app available to share the article.", Toast.LENGTH_SHORT).show();
        }
    }



    // Method to show the toast message
    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }


    @Override
    public int getItemCount() {
        return (articleList != null) ? articleList.size() : 0;
    }

    public static class ArticleViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView, sourceTextView, urlTextView;
        ImageView starImageView, shareButton;  // Add the button to send email


        public ArticleViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.txtArticleTitle);
            //sourceTextView = itemView.findViewById(R.id.txtArticleSource);
            sourceTextView = itemView.findViewById(R.id.txtArticleSource);
            starImageView = itemView.findViewById(R.id.imgFavoriteStar);
            shareButton = itemView.findViewById(R.id.imgSendEmail);  // Initialize the send email button
        }
    }

    public void updateArticles(List<Article> articles) {
        articleList.clear();
        articleList.addAll(articles);
        notifyDataSetChanged();
    }

    public void setTextToSpeech(TextToSpeech tts) {
        this.textToSpeech = tts;
    }

    // Add to Firebase favorites
    private void addToFavorites(Article article) {
        // Query to check if the article already exists in the favorites collection
        db.collection("user")
                .document(userId)
                .collection("favorites")
                .whereEqualTo("url", article.getUrl())  // Assuming `getUrl()` is a unique identifier for the article
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Article is not in favorites, so we add it
                        db.collection("user")
                                .document(userId)
                                .collection("favorites")
                                .add(article)  // Use the complete Article object
                                .addOnSuccessListener(documentReference -> {
                                    // Success, favorite added
                                    // Optionally show a success toast or update UI
                                })
                                .addOnFailureListener(e -> {
                                    // Handle error
                                    // Optionally show a toast with the error message
                                });
                    } else {
                        // Article is already in favorites, handle accordingly (e.g., show a toast)
                        showToast("This article is already favorited.");
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error
                    // Optionally show a toast with the error message
                    showToast("Error checking favorites.");
                });
    }

    // Show Toast message (you can use this to notify the user)



    // Remove from Firebase favorites
    private void removeFromFavorites(String articleUrl) {
        db.collection("user")
                .document(userId)
                .collection("favorites")
                .whereEqualTo("url", articleUrl)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String documentId = task.getResult().getDocuments().get(0).getId();
                        db.collection("user")
                                .document(userId)
                                .collection("favorites")
                                .document(documentId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Success, favorite removed
                                })
                                .addOnFailureListener(e -> {
                                    // Handle error
                                });
                    }
                });
    }

    // Optional getter for next step (Firebase storage)
    public Set<String> getFavoriteUrls() {
        return favoriteUrls;
    }
}
