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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ArticleViewHolder> {

    private final List<Article> articleList;
    private final Context context;
    private final Set<String> favoriteUrls = new HashSet<>();
    private TextToSpeech textToSpeech;
    private final boolean isFavoritesMode;
    private final FirestoreHelper firestoreHelper;


    public NewsAdapter(List<Article> articles, Context context, boolean isFavoritesMode) {
        this.articleList = articles;
        this.context = context;
        this.isFavoritesMode = isFavoritesMode;
        this.firestoreHelper = new FirestoreHelper(context);

    }

    @Override
    public ArticleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ArticleViewHolder holder, int position) {
        Article article = articleList.get(position);


        holder.titleTextView.setText(article.getTitle());
        holder.sourceTextView.setText(article.getSource().getName());

        String imageUrl = article.getUrlToImage();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.mipmap.ic_launcher)
                    .into(holder.articleImageView);
        } else {
            Glide.with(context)
                    .load(R.drawable.ic_error) // Default image
                    .into(holder.articleImageView);
        }

        holder.articleImageView.setVisibility(View.VISIBLE);

        holder.sourceTextView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl()));
            context.startActivity(intent);
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

        if (isFavoritesMode) {
            holder.starImageView.setImageResource(R.drawable.ic_fav_fill);
            holder.starImageView.setColorFilter(Color.RED);
        } else {
            boolean isFavorited = article.getFavorited();
            holder.starImageView.setImageResource(isFavorited ? R.drawable.ic_fav_fill : R.drawable.ic_fav);
            holder.starImageView.setColorFilter(isFavorited ? Color.RED : Color.GRAY);
        }

        holder.starImageView.setOnClickListener(v -> {
            if (isFavoritesMode) {
                firestoreHelper.removeFromFavorites(article.getUrl());
                articleList.remove(position);
                notifyItemRemoved(position);
            } else {
                if (article.getFavorited()) {
                    article.setFavorited(false);
                    favoriteUrls.remove(article.getUrl());
                    firestoreHelper.removeFromFavorites(article.getUrl());
                } else {
                    article.setFavorited(true);
                    favoriteUrls.add(article.getUrl());
                    firestoreHelper.addToFavorites(article);
                }
                notifyItemChanged(position);
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
        return articleList.size();
    }

    public void updateArticles(List<Article> articles) {
        articleList.clear();
        articleList.addAll(articles);
        notifyDataSetChanged();
    }

    public void setTextToSpeech(TextToSpeech tts) {
        this.textToSpeech = tts;
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
            articleImageView = itemView.findViewById(R.id.imgArticleImage);
        }
    }
}

