// app/di/AppModule.java
package com.owlerdev.owler.di;

import android.content.Context;
import com.google.gson.Gson;
import com.owlerdev.owler.data.local.OfflineCacheManager;
import com.owlerdev.owler.data.local.TokenManager;
import com.owlerdev.owler.data.local.JsonStorageManager;
import androidx.credentials.CredentialManager;
import  androidx.credentials.CredentialManager;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

/**
 * Provides application-level dependencies such as TokenManager,
 * OfflineCacheManager, JsonStorageManager, and Gson.
 */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    /**
     * Provides a singleton TokenManager for secure token storage.
     */
    @Provides
    @Singleton
    public static TokenManager provideTokenManager(@ApplicationContext Context context) {
        return new TokenManager(context);
    }

    /**
     * Provides a singleton OfflineCacheManager for user model caching.
     */
    @Provides
    @Singleton
    public static OfflineCacheManager provideOfflineCacheManager(@ApplicationContext Context context) {
        return new OfflineCacheManager(context);
    }

    /**
     * Provides a singleton JsonStorageManager for calendar data persistence.
     */
    @Provides
    @Singleton
    public static JsonStorageManager provideJsonStorageManager(@ApplicationContext Context context) {
        return new JsonStorageManager(context);
    }

    /**
     * Provides a singleton Gson instance for JSON parsing.
     */
    @Provides
    @Singleton
    public static Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton
    public static CredentialManager provideCredentialManager(@ApplicationContext Context context) {
        return CredentialManager.create(context);

    }
}
