// app/data/repository/AuthRepository.java
package com.owlerdev.owler.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.owlerdev.owler.data.local.JsonStorageManager;
import com.owlerdev.owler.data.local.OfflineCacheManager;
import com.owlerdev.owler.data.local.TokenManager;
import com.owlerdev.owler.data.remote.ApiService;
import com.owlerdev.owler.model.user.User;

import com.owlerdev.owler.utils.Result;


import javax.inject.Inject;
import javax.inject.Singleton;
import javax.inject.Named;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Repository for handling authentication: email/password, Google OAuth2,
 * token refresh, and logout. Caches user data and tokens locally.
 */
@Singleton
public class AuthRepository {
    private final ApiService noAuthApiService;
    private final TokenManager tokenManager;
    private final OfflineCacheManager cacheManager;
    private final Gson gson;
    private final JsonStorageManager jsonStorageManager;

    @Inject
    public AuthRepository(@Named("noAuthApiService") ApiService noAuthApiService,
                          TokenManager tokenManager,
                          OfflineCacheManager cacheManager,
                          Gson gson,
                          JsonStorageManager jsonStorageManager) {
        this.noAuthApiService = noAuthApiService;
        this.tokenManager = tokenManager;
        this.cacheManager = cacheManager;
        this.gson = gson;
        this.jsonStorageManager = jsonStorageManager;
    }

    public LiveData<Result<User>> register(String email,
                                           String password,
                                           String name,
                                           int age,
                                           String gender) {
        MutableLiveData<Result<User>> live = new MutableLiveData<>();
        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);
        payload.addProperty("name", name);
        payload.addProperty("age", age);
        payload.addProperty("gender", gender);
        jsonStorageManager.createCalendar();
        noAuthApiService.register(payload)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject body = response.body();
                            try {
                                String access = body.get("access_token").getAsString();
                                String refresh = body.get("refresh_token").getAsString();
                                tokenManager.saveTokens(access, refresh);

                                JsonObject userJson = body.getAsJsonObject("user");
                                User user = gson.fromJson(userJson, User.class);
                                cacheManager.cacheUser(user);
                                live.postValue(Result.success(user));
                            } catch (Exception e) {
                                Timber.e(e, "AuthRepository.register parse error");
                                live.postValue(Result.error("Registration parsing failed"));
                            }
                        } else {
                            live.postValue(Result.error("Registration failed: " + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        Timber.e(t, "AuthRepository.register network error");
                        live.postValue(Result.error("Network error during registration"));
                    }
                });
        return live;
    }

    public LiveData<Result<User>> login(String email, String password) {
        MutableLiveData<Result<User>> live = new MutableLiveData<>();

        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);

        jsonStorageManager.createCalendar();
        noAuthApiService.login(payload)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject body = response.body();
                            try {
                                String access = body.get("access_token").getAsString();
                                String refresh = body.get("refresh_token").getAsString();
                                tokenManager.saveTokens(access, refresh);

                                JsonObject userJson = body.getAsJsonObject("user");
                                User user = gson.fromJson(userJson, User.class);
                                cacheManager.cacheUser(user);
                                live.postValue(Result.success(user));
                            } catch (Exception e) {
                                Timber.e(e, "AuthRepository.login parse error");
                                live.postValue(Result.error("Login parsing failed"));
                            }
                        } else {
                            live.postValue(Result.error("Login failed: " + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        Timber.e(t, "AuthRepository.login network error");
                        live.postValue(Result.error("Network error during login"));
                    }
                });
        return live;
    }


    public LiveData<Result<User>> googleSignin(String idToken) {
        MutableLiveData<Result<User>> live = new MutableLiveData<>();
        JsonObject payload = new JsonObject();
        payload.addProperty("id_token", idToken);
        noAuthApiService.googleSignin(payload).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        String access = body.get("access_token").getAsString();
                        String refresh = body.get("refresh_token").getAsString();
                        tokenManager.saveTokens(access, refresh);

                        JsonObject userJson = body.getAsJsonObject("user");
                        User user = gson.fromJson(userJson, User.class);
                        cacheManager.cacheUser(user);
                        live.postValue(Result.success(user));
                    } catch (Exception e) {
                        Timber.e(e, "AuthRepository.googleSignin parse error");
                        live.postValue(Result.error("Google sign-in parsing failed"));
                    }
                } else {
                    live.postValue(Result.error("Google sign-in failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Timber.e(t, "AuthRepository.googleSignin network error");
                live.postValue(Result.error("Network error during Google sign-in"));
            }
        });
        return live;
    }



    public MutableLiveData<Result<Void>> logout() {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();

        tokenManager.clearTokens();
        cacheManager.clearUserCache();
        jsonStorageManager.clearCalendar();
        

        if (tokenManager.getAccessToken() == null && 
            tokenManager.getRefreshToken() == null && 
            cacheManager.getCachedUser() == null) {
            live.postValue(Result.success(null));
        } else {
            live.postValue(Result.error("Logout failed: Could not clear all data"));
        }
        return live;
    }
    // /**
}
