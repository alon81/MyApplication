package com.example.myapplication.api;
public interface ApiCallBack<T> {
    void OnSucces(T response);
    void OnFail();
}
