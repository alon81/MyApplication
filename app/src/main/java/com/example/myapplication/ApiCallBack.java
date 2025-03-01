package com.example.myapplication;

public interface ApiCallBack<T> {
    void OnSucces(T response);

    void OnFail();
}
