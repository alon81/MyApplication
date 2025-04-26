package com.example.myapplication.api;
public interface IApiCallBack<T> {
    void OnSucces(T response);
    void OnFail();
}
