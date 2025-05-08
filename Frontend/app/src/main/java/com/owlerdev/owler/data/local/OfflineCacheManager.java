// app/data/local/OfflineCacheManager.java
package com.owlerdev.owler.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.owlerdev.owler.model.user.User;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

/**
 * Manages offline caching of the User model using SharedPreferences.
 */
@Singleton
public class OfflineCacheManager {
    private static final String PREFS_NAME            = "user_prefs";
    private static final String KEY_USER_ID           = "user_id";
    private static final String KEY_USER_NAME         = "user_name";
    private static final String KEY_USER_EMAIL        = "user_email";
    private static final String KEY_USER_AGE          = "user_age";
    private static final String KEY_USER_GENDER       = "user_gender";
    private static final String KEY_USER_SCORE        = "user_productivity_score";

    private final SharedPreferences prefs;

    @Inject
    public OfflineCacheManager(@ApplicationContext Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Cache the provided User object to SharedPreferences.
     */
    public void cacheUser(User user) {
        prefs.edit()
                .putString(KEY_USER_ID, user.getId())
                .putString(KEY_USER_NAME, user.getName())
                .putString(KEY_USER_EMAIL, user.getEmail())
                .putInt(KEY_USER_AGE, user.getAge() != null ? user.getAge() : -1)
                .putString(KEY_USER_GENDER, user.getGender() != null ? user.getGender().toString(): null)
                .putInt(KEY_USER_SCORE, user.getProductivityScore() != null ? user.getProductivityScore() : 100)
                .apply();
        Timber.d("OfflineCacheManager: User cached successfully");
    }

    /**
     * Retrieve the cached User object, or null if none.
     */
    public User getCachedUser() {
        if (!prefs.contains(KEY_USER_ID)) {
            Timber.d("OfflineCacheManager: No cached user found");
            return null;
        }

        String id = prefs.getString(KEY_USER_ID, null);
        String name = prefs.getString(KEY_USER_NAME, null);
        String email = prefs.getString(KEY_USER_EMAIL, null);
        int age = prefs.getInt(KEY_USER_AGE, -1);
        String Sgender = prefs.getString(KEY_USER_GENDER, null);
        Character gender = Sgender != null ? Sgender.charAt(0) : null;
        int score = prefs.getInt(KEY_USER_SCORE, -1);

        User user = new User(
                id,
                email,
                name,
                age >= 0 ? age : null,
                gender,
                score >= 0 ? score : null
        );
        Timber.d("OfflineCacheManager: Retrieved cached user: %s", user.getEmail());
        return user;
    }

    /**
     * Clear the cached user data.
     */
    public void clearUserCache() {
        prefs.edit().clear().apply();
        Timber.d("OfflineCacheManager: Cleared cached user data");
    }

    /**
     * Set the productivity score in the cache.
     */
    public void setProductivityScore(int score) {
        prefs.edit()
                .putInt(KEY_USER_SCORE, score)
                .apply();
        Timber.d("OfflineCacheManager: Productivity score set to %d", score);
    }

    /**
     * Get the productivity score from the cache.
     */
    public Integer getProductivityScore() {
        int score = prefs.getInt(KEY_USER_SCORE, -1);
        return score >= 0 ? score : null;
    }
}
