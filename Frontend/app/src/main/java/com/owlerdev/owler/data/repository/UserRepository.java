// app/data/repository/UserRepository.java
package com.owlerdev.owler.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.owlerdev.owler.data.local.OfflineCacheManager;
import com.owlerdev.owler.data.remote.ApiService;
import com.owlerdev.owler.model.user.User;
import com.owlerdev.owler.utils.Result;

import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.inject.Named;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Repository for user profile operations: caching locally and syncing with backend.
 */
@Singleton
public class UserRepository {
    private final ApiService authApiService;
    private final OfflineCacheManager cacheManager;
    private final Gson gson;

    @Inject
    public UserRepository(@Named("authApiService") ApiService authApiService,
                          OfflineCacheManager cacheManager,
                          Gson gson) {
        this.authApiService = authApiService;
        this.cacheManager = cacheManager;
        this.gson = gson;
    }

    /**
     * Retrieve the cached user or null if none.
     */
    public LiveData<Result<User>> getCachedUser() {
        MutableLiveData<Result<User>> live = new MutableLiveData<>();
        User user = cacheManager.getCachedUser();
        if (user != null) {
            live.postValue(Result.success(user));
        } else {
            live.postValue(Result.error("No cached user found"));
        }
        return live;
    }

    /**
     * Clear the cached user data.
     */
    public MutableLiveData<Result<Void>> clearCache() {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        cacheManager.clearUserCache();
        if (cacheManager.getCachedUser() == null) {
            live.postValue(Result.success(null));
        } else {
            live.postValue(Result.error("Failed to clear cache"));
        }
        return live;
    }

    /**
     * Update user profile remotely and cache the updated user locally.
     *
     * @param payload Map of fields to update (e.g., name, age)
     */

    public LiveData<Result<User>> updateProfile(Map<String, Object> payload) {
        MutableLiveData<Result<User>> live = new MutableLiveData<>();
        JsonObject jsonPayload = new JsonObject();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            jsonPayload.addProperty(entry.getKey(), String.valueOf(entry.getValue()));
        }
        authApiService.updateProfile(jsonPayload)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> res) {
                        if (res.isSuccessful() && res.body() != null) {
                            try {
                                User user = gson.fromJson(res.body(), User.class);
                                cacheManager.cacheUser(user);
                                live.postValue(Result.success(user));
                            } catch (Exception e) {
                                Timber.e(e, "UserRepository: parse updateProfile");
                                live.postValue(Result.error(Objects.requireNonNull(e.getMessage())));
                            }
                        } else {
                            live.postValue(Result.error("Update failed: " + res.code()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        Timber.e(t, "UserRepository: updateProfile error");
                        live.postValue(Result.error(Objects.requireNonNull(t.getMessage())));
                    }
                });
        return live;
    }

    /**
     * Change user password on the backend.
     *
     * @param payload Map with 'oldPassword' and 'newPassword'.
     */
    public MutableLiveData<Result<Void>> changePassword(Map<String, Object> payload) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        JsonObject passwords = new JsonObject();
        passwords.addProperty("oldPassword", (String) payload.get("oldPassword"));
        passwords.addProperty("newPassword", (String) payload.get("newPassword"));
        authApiService.changePassword(passwords)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> res) {
                        if (res.isSuccessful()) {
                            live.postValue(Result.success(null));
                        } else {
                            live.postValue(Result.error("Change password failed: " + res.code()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        Timber.e(t, "UserRepository: changePassword error");
                        live.postValue(Result.error(Objects.requireNonNull(t.getMessage())));
                    }
                });
        return live;
    }

    /**
     * Delete user account on backend and clear local cache.
     */
    public MutableLiveData<Result<Void>> deleteAccount(String password) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        JsonObject payload = new JsonObject();
        payload.addProperty("password", password);
        authApiService.deleteAccount(payload)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> res) {
                        if (res.isSuccessful()) {
                            cacheManager.clearUserCache();
                            live.postValue(Result.success(null));
                        } else {
                            live.postValue(Result.error("Delete account failed: " + res.code()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        Timber.e(t, "UserRepository: deleteAccount error");
                        live.postValue(Result.error(Objects.requireNonNull(t.getMessage())));
                    }
                });
        return live;
    }

    /**
     * Get the productivity score from the cache.
     */
    public LiveData<Result<Integer>> getProductivityScore() {
        MutableLiveData<Result<Integer>> live = new MutableLiveData<>();
        Integer UserProductivity =cacheManager.getProductivityScore();
        if (UserProductivity != null) {
            live.postValue(Result.success(UserProductivity));
        } else {
            live.postValue(Result.error("No productivity score found"));


        }
        return live;
    }

    /**
     * Set the productivity score in the cache.
     */
    public MutableLiveData<Result<Void>> setProductivityScore(int score) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
         cacheManager.setProductivityScore(score);
         if (cacheManager.getProductivityScore() == score)
             live.postValue(Result.success(null));
         else
                live.postValue(Result.error("Failed to set productivity score"));
        return live;
    }
}