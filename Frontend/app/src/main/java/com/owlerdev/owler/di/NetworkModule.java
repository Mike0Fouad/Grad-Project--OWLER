package com.owlerdev.owler.di;

import android.content.Context;

import com.owlerdev.owler.BuildConfig;
import com.google.gson.Gson;

import com.owlerdev.owler.data.local.TokenManager;
import com.owlerdev.owler.data.remote.ApiService;
import com.owlerdev.owler.network.AuthInterceptor;
import javax.inject.Named;
import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    @Provides
    @Singleton
    static HttpLoggingInterceptor provideLoggingInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(BuildConfig.DEBUG ?
                HttpLoggingInterceptor.Level.BODY :
                HttpLoggingInterceptor.Level.NONE);
        return interceptor;
    }

    @Provides
    @Singleton
    static AuthInterceptor provideAuthInterceptor(
            TokenManager tokenManager,
            @ApplicationContext Context context,
            @Named("noAuthApiService") ApiService noAuthApiService,
            Gson gson
    ) {
        return new AuthInterceptor(tokenManager, context, noAuthApiService, gson);
    }

    // Authenticated client (with AuthInterceptor)
    @Provides
    @Named("authClient")
    @Singleton
    static OkHttpClient provideAuthClient(
            HttpLoggingInterceptor loggingInterceptor,
            AuthInterceptor authInterceptor
    ) {
        return new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    // Non-authenticated client (no AuthInterceptor)
    @Provides
    @Named("noAuthClient")
    @Singleton
    static OkHttpClient provideNoAuthClient(
            HttpLoggingInterceptor loggingInterceptor
    ) {
        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    // Main Retrofit instance (authenticated)
    @Provides
    @Singleton
    @Named("authRetrofit")
    static Retrofit provideAuthRetrofit(
            @Named("authClient") OkHttpClient client,
            Gson gson
    ) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    // Non-authenticated Retrofit
    @Provides
    @Named("noAuthRetrofit")
    @Singleton
    static Retrofit provideNoAuthRetrofit(
            @Named("noAuthClient") OkHttpClient client,
            Gson gson
    ) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    // Main API service
    @Provides
    @Singleton
    @Named("authApiService")
    static ApiService provideAuthApiService(@Named("authRetrofit") Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }

    // Non-authenticated API service
    @Provides
    @Named("noAuthApiService")
    @Singleton
    static ApiService provideNoAuthApiService(
            @Named("noAuthRetrofit") Retrofit retrofit
    ) {
        return retrofit.create(ApiService.class);
    }
}