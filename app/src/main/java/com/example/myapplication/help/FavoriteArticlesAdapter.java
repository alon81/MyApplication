package com.example.myapplication.help;
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

import java.util.List;
import java.util.Locale;

public class FavoriteArticlesAdapter extends RecyclerView.Adapter<FavoriteArticlesAdapter.FavoriteViewHolder> {

    private List<Article> favoriteArticles;
    private FirebaseFirestore db;
    private String userId;
    private Context context;
    private TextToSpeech textToSpeech;

    public FavoriteArticlesAdapter(Context context, List<Article> favoriteArticles, String userId, FirebaseFirestore db, TextToSpeech textToSpeech) {
        this.context = context;
        this.favoriteArticles = favoriteArticles;
        this.userId = userId;
        this.db = db;
        this.textToSpeech = textToSpeech;
    }


    @Override
    public FavoriteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FavoriteViewHolder holder, int position) {
        Article article = favoriteArticles.get(position);
        holder.titleTextView.setText(article.getTitle());
        holder.sourceTextView.setText(article.getSource().getName());

        holder.sourceTextView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl()));
            context.startActivity(intent);
        });

        holder.starImageView.setImageResource(R.drawable.ic_fav);
        holder.starImageView.setColorFilter(Color.RED);

        holder.starImageView.setOnClickListener(v -> {
            removeFromFavorites(article.getUrl());
            favoriteArticles.remove(position);
            notifyItemRemoved(position);
        });

        holder.titleTextView.setOnClickListener(v -> {
            if (textToSpeech != null) {
                String title = article.getTitle();
                boolean isHebrew = title.matches(".*\\p{InHebrew}.*");
                Locale targetLocale = isHebrew ? new Locale("he") : Locale.US;

                int result = textToSpeech.setLanguage(targetLocale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "Selected language not supported.", Toast.LENGTH_SHORT).show();
                } else {
                    textToSpeech.speak(title, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        });

        holder.shareButton.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareMessage = "Check out this article:\n" + article.getTitle() + "\n" + article.getUrl();
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

            try {
                context.startActivity(Intent.createChooser(shareIntent, "Share article via"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(context, "No app available to share the article.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return favoriteArticles.size();
    }

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
                                .delete();
                    }
                });
    }

    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, sourceTextView;
        ImageView starImageView, shareButton;

        public FavoriteViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.txtArticleTitle);
            sourceTextView = itemView.findViewById(R.id.txtArticleSource);
            starImageView = itemView.findViewById(R.id.imgFavoriteStar);
            shareButton = itemView.findViewById(R.id.imgSendEmail);
        }
    }
}
