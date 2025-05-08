// app/data/remote/ApiService.java
package com.owlerdev.owler.data.remote;

import com.google.gson.JsonObject;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import retrofit2.http.Path;

public interface ApiService {

    // --- AUTH ---
    @POST("auth/register")
    Call<JsonObject> register(@Body JsonObject payload);

    @POST("auth/login")
    Call<JsonObject> login(@Body JsonObject payload);

    @GET("auth/google")
    Call<JsonObject> getGoogleAuthUrl();

    @GET("auth/google/callback")
    Call<JsonObject> handleGoogleCallback(
            @Query("code") String authCode
    );

    @POST("/auth/google-signin")
    Call<JsonObject> googleSignin(@Body JsonObject authCode);

    @POST("auth/refresh")
    Call<JsonObject> refreshToken();



    // --- CALENDAR ---
    @POST("calendar/upload-days")
    Call<JsonObject> uploadDays( @Body JsonObject payload);

    @GET("calendar/get-user-data")
    Call<JsonObject> getUserData(
            @Query("date") String date
    );

    @GET("calendar/get-days")
    Call<JsonObject> getDays(
            @Query("date") String date
    );



    // --- USER PROFILE ---
    @PUT("users/profile")
    Call<JsonObject> updateProfile(@Body JsonObject payload);

    @POST("users/change-password")
    Call<JsonObject> changePassword(@Body JsonObject payload);

    @DELETE("users/delete")
    Call<JsonObject> deleteAccount(@Body JsonObject payload);
    
}
