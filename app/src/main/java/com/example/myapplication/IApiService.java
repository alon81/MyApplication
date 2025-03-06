package com.example.myapplication;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface IApiService {
    @GET("v2/everything")
    Call<NewsResponse> getNewsByDomains(
            @Query("domains") String domains,
            @Query("5fe6e7f68f5940ac8dac631efefc2b4a") String apiKey
    );
    @GET("v2/sources")
    Call<SourcesResponse> getNewsSources(@Query("5fe6e7f68f5940ac8dac631efefc2b4a") String apiKey);

        @GET("v2/sources")
        Call<SourcesResponse> getSources(@Query("language") String language);

    @GET("v2/top-headlines")
    Call<NewsResponse> getArticles(@QueryMap Map<String, String> options);
    @GET("v2/everything")
    Call<NewsResponse> getArticlesByDomains(
            @Query("domains") String domains,
            @Query("apiKey") String apiKey
    );

}



