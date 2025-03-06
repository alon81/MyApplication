package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ArticleViewHolder> {

    private List<Article> articleList;

    // Constructor to initialize the list of articles
    public NewsAdapter(List<Article> articleList) {
        this.articleList = articleList;
    }

    // Create the ViewHolder for each article item
    @Override
    public ArticleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    // Bind the data to the ViewHolder (populate the views)
    @Override
    public void onBindViewHolder(ArticleViewHolder holder, int position) {
        // Make sure the position is valid
        if (position >= 0 && position < articleList.size()) {
            Article article = articleList.get(position);
            holder.titleTextView.setText(article.getTitle());
            holder.sourceTextView.setText(article.getSource().getName());  // Adjust based on your Article object

            // Set the URL dot and make it clickable
            String articleUrl = article.getUrl();
            holder.urlTextView.setOnClickListener(v -> {
                // Open the article URL in a browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl));
                v.getContext().startActivity(intent);
            });
        }
    }

    // Return the number of articles in the list
    @Override
    public int getItemCount() {
        if (articleList != null) {
            return articleList.size();
        } else {
            return 0;
        }
    }

    // ViewHolder to hold the views for each article item
    public static class ArticleViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView, sourceTextView, urlTextView;

        public ArticleViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.txtArticleTitle);
            sourceTextView = itemView.findViewById(R.id.txtArticleSource);
            urlTextView = itemView.findViewById(R.id.txtArticleUrl);
        }
    }

    // Method to update the list of articles
    public void updateArticles(List<Article> articles) {
        if (articles != null && !articles.isEmpty()) {
            Log.d("NewsAdapter", "Updating articles. New size: " + articles.size());
            this.articleList = articles;
            notifyDataSetChanged();
        } else {
            Log.d("NewsAdapter", "No articles to display.");
        }
    }
}
