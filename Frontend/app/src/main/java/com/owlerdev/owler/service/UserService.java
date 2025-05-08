// app/service/UserService.java
package com.owlerdev.owler.service;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.owlerdev.owler.data.repository.UserRepository;
import com.owlerdev.owler.model.user.User;
import com.owlerdev.owler.utils.Result;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service layer for user profile operations: caching and backend sync.
 */
@Singleton
public class UserService {
    private final UserRepository userRepository;

    @Inject
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieve the cached user, or null if none.
     */
    public LiveData<Result<User>> getCachedUser() {

        return userRepository.getCachedUser();
    }

    /**
     * Clear the locally cached user.
     */
    public MutableLiveData<Result<Void>> clearCache() {

        return userRepository.clearCache();
    }

    /**
     * Update the user profile on the backend and cache locally.
     * @param updates Map of profile fields to update (e.g., name, age).
     */
    public LiveData<Result<User>> updateProfile(Map<String, Object> updates) {
        return userRepository.updateProfile(updates);


    }

    /**
     * Change the user's password.
     * @param oldPassword The current password.
     * @param newPassword The new password to set.
     */
    public MutableLiveData<Result<Void>> changePassword(String oldPassword, String newPassword) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("oldPassword", oldPassword);
        payload.put("newPassword", newPassword);
        return userRepository.changePassword(payload);
    }

    /**
     * Delete the user account on the backend and clear local cache.
     */
    public MutableLiveData<Result<Void>> deleteAccount(String Password) {
        return userRepository.deleteAccount(Password);
    }

    /**
     * Get the user's productivity score from cache.
     */
    public LiveData<Result<Integer>> getProductivityScore() {
        return userRepository.getProductivityScore();
    }

    /**
     * Set the user's productivity score in cache.
     */
    public MutableLiveData<Result<Void>> setProductivityScore(int score) {
        return userRepository.setProductivityScore(score);
    }
}
