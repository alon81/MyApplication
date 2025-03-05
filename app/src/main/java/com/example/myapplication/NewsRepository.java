package com.example.myapplication;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsRepository {
    private static final String TAG = "ApiTest";
    private IApiService apiService;


    public NewsRepository() {
        apiService = ApiClient.getClient().create(IApiService.class);
    }
    private static final String API_KEY = "5fe6e7f68f5940ac8dac631efefc2b4a"; // Your NewsAPI key

    public void getSources(Callback<SourcesResponse> callback) {
        Call<SourcesResponse> call = apiService.getNewsSources(API_KEY); // Use constant API key
        call.enqueue(callback);
    }


    public void getArticlesByDomains(String domains, String sortBy, ApiCallBack<NewsResponse> callback) {
        // Create the query parameters including the 'sortBy' parameter
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("domains", domains);
        queryParams.put("sortBy", sortBy); // Passing the sortBy parameter

        // Make the API request with the query parameters
        apiService.getArticles(queryParams).enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.OnSucces(response.body());
                } else {
                    callback.OnFail();
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                callback.OnFail();
            }
        });
    }
}
