package com.example.myapplication;

import android.util.Log;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsRepository {
    private static final String TAG ="ApiTest";
    private IApiService apiService;
    private String apiKey = "5fe6e7f68f5940ac8dac631efefc2b4a";

    public NewsRepository() {
        apiService = ApiClient.getClient().create(IApiService.class);
    }

    public void getArticlesByDomains(String domains, Callback<NewsResponse> callback) {
        Log.d(TAG, "Fetching articles for domains: " + domains);
        Call<NewsResponse> call = apiService.getNewsByDomains(domains, apiKey);
        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG, "Successfully fetched articles. Total articles: " + response.body().getArticles().size());
                } else {
                    Log.w(TAG, "Failed to fetch articles. Response code: " + response.code());
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching articles: " + t.getMessage(), t);
                callback.onFailure(call, t);
            }
        });
    }
}
