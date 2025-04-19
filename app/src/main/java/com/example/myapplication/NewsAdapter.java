package com.example.myapplication;

import static android.app.PendingIntent.getActivity;
import static androidx.core.content.ContentProviderCompat.requireContext;
import static androidx.core.content.ContextCompat.startActivity;
import static java.security.AccessController.getContext;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ArticleViewHolder> {

    private List<Article> articleList;
    private TextToSpeech textToSpeech;
    private FirebaseFirestore db;
    private String userId;
    private Context context;  // Store the context for Toast

    // In-memory storage for favorited URLs
    private Set<String> favoriteUrls = new HashSet<>();

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
            holder.urlTextView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl));
                v.getContext().startActivity(intent);
            });

            // Text-to-Speech functionality
            holder.titleTextView.setOnClickListener(v -> {
                if (textToSpeech != null) {
                    textToSpeech.speak(article.getTitle(), TextToSpeech.QUEUE_FLUSH, null, null);
                }
            });

            // Star icon: toggle favorite state
            boolean isFavorited = article.isFavorited();  // Check the favorited state from the Article object
            holder.starImageView.setImageResource(isFavorited ? R.drawable.ic_star_filled : R.drawable.ic_star_border);
            holder.starImageView.setColorFilter(isFavorited ? Color.YELLOW : Color.GRAY);

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
            holder.sendEmailButton.setOnClickListener(v -> {
                showEmailDialog(article);
            });
        }
    }

    // Method to show the email dialog
    // Method to show the email dialog
    // Method to show the email dialog
    // Method to show the email dialog
    private void showEmailDialog(Article article) {
        // Inflate your custom layout directly from XML
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.custom_email_dialog, null);

        // Get reference to the EditText and Send button in the custom layout
        EditText emailEditText = dialogView.findViewById(R.id.emailEditText);
        Button sendButton = dialogView.findViewById(R.id.sendButton);

        // Build the AlertDialog with the custom layout
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);  // Set the custom XML view here

        // Remove the default title by setting a null title (no title bar)
        builder.setTitle(null);

        // Set the Send button click listener
        sendButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (!email.isEmpty()) {
                if (isValidEmail(email)) {
                    sendEmail(email, article);  // Call the send email method
                } else {
                    Toast.makeText(context, "Invalid email format.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Please enter an email address.", Toast.LENGTH_SHORT).show();
            }
        });

        // Show the dialog without a cancel button or title
        builder.create().show();
    }




    // Method to validate email format
    private boolean isValidEmail(String email) {
        // Regular expression for valid email format
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailPattern);
    }


    // Method to send the email
    // Method to send the email
    private void sendEmail(String recipientEmail, Article article) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));  // Only email apps should handle this

        // Set the email recipient, subject, and body
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipientEmail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this article: " + article.getTitle());
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Here's an interesting article:\n\n" + article.getTitle() + "\n" + article.getUrl());

        try {
            context.startActivity(Intent.createChooser(emailIntent, "Send email..."));
        } catch (android.content.ActivityNotFoundException ex) {
            showToast("No email client installed");
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
        ImageView starImageView, sendEmailButton;  // Add the button to send email


        public ArticleViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.txtArticleTitle);
            sourceTextView = itemView.findViewById(R.id.txtArticleSource);
            urlTextView = itemView.findViewById(R.id.txtArticleUrl);
            starImageView = itemView.findViewById(R.id.imgFavoriteStar);
            sendEmailButton = itemView.findViewById(R.id.imgSendEmail);  // Initialize the send email button
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
