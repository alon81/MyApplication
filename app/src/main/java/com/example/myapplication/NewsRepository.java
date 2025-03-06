package com.example.myapplication;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsRepository {
    private static final String TAG = "NewsRepository";
    private IApiService apiService;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public NewsRepository() {
        apiService = ApiClient.getClient().create(IApiService.class);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private static final String API_KEY = "176ca19806d8486eb27058d1ed8fc3f9";

    // Fetch sources from NewsAPI
    public void getSources(Callback<SourcesResponse> callback) {
        Call<SourcesResponse> call = apiService.getNewsSources(API_KEY);
        call.enqueue(callback);
    }

    // Fetch articles by specific sources, ensuring they are sorted only by newest (publishedAt)
    public void getArticlesBySources(String sources, String sortBy, int pageSize, ApiCallBack<NewsResponse> callback) {
        if (sources == null || sources.isEmpty()) {
            Log.e(TAG, "Sources parameter is empty or null");
            callback.OnFail();
            return;
        }
        if (sortBy == null || sortBy.isEmpty()) {
            Log.e(TAG, "SortBy parameter is empty or null");
            callback.OnFail();
            return;
        }

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("sources", sources);  // Use "sources" instead of "domains"
        queryParams.put("sortBy", sortBy);
        queryParams.put("pageSize", String.valueOf(pageSize)); // Added pageSize parameter to get more than 20 articles
        queryParams.put("apiKey", API_KEY);

        Log.d(TAG, "Sending API request with parameters: " + queryParams);

        // Fetch articles sorted only by publishedAt in descending order
        apiService.getArticles(queryParams).enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    NewsResponse newsResponse = response.body();
                    Log.d(TAG, "Received articles: " + newsResponse.getArticles().size());

                    // Pass the articles to the callback, which will update the RecyclerView adapter in the activity
                    callback.OnSucces(newsResponse);
                } else {
                    Log.e(TAG, "API error: " + response.code() + " - " + response.message());
                    callback.OnFail();
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                Log.e(TAG, "Network failure: " + t.getMessage(), t);
                callback.OnFail();
            }
        });
    }



    // Fetch followed sources from Firebase Firestore and get articles
    // Fetch followed sources from Firebase Firestore and get articles
    public void getArticlesByFollowedSources(String sortBy, ApiCallBack<NewsResponse> callback) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.e(TAG, "User not logged in");
            callback.OnFail();
            return;
        }

        db.collection("user").document(userId).collection("followedSources")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> sourcesList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String source = document.getString("source");
                            if (source != null && !source.isEmpty()) {
                                sourcesList.add(source);
                            }
                        }

                        // Log the sources to verify
                        Log.d(TAG, "Followed sources: " + sourcesList);

                        if (!sourcesList.isEmpty()) {
                            String sources = String.join(",", sourcesList);
                            Log.d(TAG, "Sources for API: " + sources);

                            // Adjusted to fetch more articles (pageSize = 100) and sorted by newest
                            getArticlesBySources(sources, sortBy, 100, callback); // Fetch up to 100 articles sorted by newest
                        } else {
                            Log.e(TAG, "No followed sources found");
                            callback.OnFail();
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch followed sources", task.getException());
                        callback.OnFail();
                    }
                });
    }


}
