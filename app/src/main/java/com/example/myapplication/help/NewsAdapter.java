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

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.objects.Article;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ArticleViewHolder> {

    private final List<Article> articleList;
    private TextToSpeech textToSpeech;
    private final FirebaseFirestore db;
    private final String userId;
    private final Context context;
    private final Set<String> favoriteUrls = new HashSet<>();

    public NewsAdapter(List<Article> articleList, Context context) {
        this.articleList = articleList;
        this.context = context;
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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

            // Load article image using Glide
            String imageUrl = article.getUrlToImage();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
//                        .placeholder(R.drawable.ic_logo_background)
//                        .error(R.drawable.ic_logo_background)
                        .into(holder.articleImageView);
                holder.articleImageView.setVisibility(View.VISIBLE);
            } else {
                holder.articleImageView.setVisibility(View.GONE);
            }

            String articleUrl = article.getUrl();

            holder.sourceTextView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl));
                v.getContext().startActivity(intent);
            });

            holder.titleTextView.setOnClickListener(v -> {
                if (textToSpeech != null) {
                    String title = article.getTitle();
                    boolean isHebrew = title.matches(".*\\p{InHebrew}.*");
                    Locale targetLocale = isHebrew ? new Locale("he") : Locale.US;
                    int result = textToSpeech.setLanguage(targetLocale);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(context, "Selected language not supported on this device.", Toast.LENGTH_SHORT).show();
                    } else {
                        textToSpeech.speak(title, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
            });

            boolean isFavorited = article.isFavorited();
            holder.starImageView.setImageResource(isFavorited ? R.drawable.ic_fav_fill : R.drawable.ic_fav);
            holder.starImageView.setColorFilter(isFavorited ? Color.RED : Color.GRAY);

            holder.starImageView.setOnClickListener(v -> {
                if (isFavorited) {
                    article.setFavorited(false);
                    favoriteUrls.remove(articleUrl);
                    removeFromFavorites(articleUrl);
                } else {
                    article.setFavorited(true);
                    favoriteUrls.add(articleUrl);
                    addToFavorites(article);
                }
                notifyItemChanged(position);
            });

            holder.shareButton.setOnClickListener(v -> shareArticle(article));
        }
    }

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

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return (articleList != null) ? articleList.size() : 0;
    }

    public static class ArticleViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView, sourceTextView;
        ImageView starImageView, shareButton, articleImageView;

        public ArticleViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.txtArticleTitle);
            sourceTextView = itemView.findViewById(R.id.txtArticleSource);
            starImageView = itemView.findViewById(R.id.btnFavorite);
            shareButton = itemView.findViewById(R.id.btnShare);
            articleImageView = itemView.findViewById(R.id.imgArticleImage);  // ← Added image view
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

    private void addToFavorites(Article article) {
        db.collection("user")
                .document(userId)
                .collection("favorites")
                .whereEqualTo("url", article.getUrl())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        db.collection("user")
                                .document(userId)
                                .collection("favorites")
                                .add(article);
                    } else {
                        showToast("This article is already favorited.");
                    }
                })
                .addOnFailureListener(e -> showToast("Error checking favorites."));
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

}
