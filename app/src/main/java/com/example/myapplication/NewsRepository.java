package com.example.myapplication;

import android.util.Log;
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


    public void getArticlesByDomains(String domains, final ApiCallBack<NewsResponse> apiCallBack) {
        Log.d(TAG, "Fetching articles for domains: " + domains);
        Call<NewsResponse> call = apiService.getNewsByDomains(domains, API_KEY);
        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG, "Articles fetched: " + response.body().getArticles().size());
                    apiCallBack.OnSucces(response.body());  // Call OnSuccess with the response
                } else {
                    Log.w(TAG, "Failed to fetch articles: " + response.code());
                    apiCallBack.OnFail();  // Call OnFail in case of failure
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching articles: " + t.getMessage(), t);
                apiCallBack.OnFail();  // Call OnFail in case of an error
            }
        });
    }
}
