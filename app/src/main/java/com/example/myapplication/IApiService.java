package com.example.myapplication;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface IApiService {
    @GET("v2/everything")
    Call<NewsResponse> getNewsByDomains(
            @Query("domains") String domains,
            @Query("apiKey") String apiKey
    );
}


