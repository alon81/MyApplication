package com.example.myapplication.api;

import com.example.myapplication.objects.NewsResponse;
import com.example.myapplication.objects.SourcesResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface IApiService {

    @GET("v2/sources")
    Call<SourcesResponse> getNewsSources(@Query("apikey") String apiKey);
    @GET("v2/top-headlines")
    Call<NewsResponse> getTopHeadlinesByCategory(
            @Query("country") String country,
            @Query("category") String category,
            @Query("pageSize") int pageSize,
            @Query("apiKey") String apiKey
    );

    //    @GET("top-headlines")
//    Call<NewsResponse> getTopHeadlinesByCategory(
//            @QueryMap String country,
//           String category,
//             int pageSize,
//             String apiKey
//
//    );
    @GET("v2/top-headlines")
    Call<NewsResponse> getArticles(@QueryMap Map<String, String> options);
}


