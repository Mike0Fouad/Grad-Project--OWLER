package com.owlerdev.owler.network;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.owlerdev.owler.data.local.TokenManager;
import com.owlerdev.owler.data.remote.ApiService;
import com.owlerdev.owler.ui.activity.AuthActivity;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * Intercepts requests to add the Bearer token and handles 401 refresh logic.
 */
@Singleton
public class AuthInterceptor implements Interceptor {
    private final TokenManager tokenManager;
    private final Context context;
    private final ApiService noAuthApiService;
    private final Gson gson;

    @Inject
    public AuthInterceptor(
            TokenManager tokenManager,
            Context context,
            @Named("noAuthApiService") ApiService noAuthApiService,
            Gson gson
    ) {
        this.tokenManager = tokenManager;
        this.context = context;
        this.noAuthApiService = noAuthApiService;
        this.gson = gson;
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();


        String url = originalRequest.url().toString();
        String accessToken = tokenManager.getAccessToken();
        Timber.d("AuthInterceptor: Intercepting request: %s", chain.request().url());
        Timber.d("AuthInterceptor: Token used: %s", accessToken);
        
        if (url.contains("/auth/login") || url.contains("/auth/register") || url.contains("/auth/refresh")) {
            Timber.d("AuthInterceptor: Skipping token for auth endpoint");
            return chain.proceed(originalRequest);
        }
        
        if (accessToken == null || accessToken.isEmpty()) {
            Timber.e("AuthInterceptor: No access token available");
            redirectToLogin();
            return chain.proceed(originalRequest);
        }

        // Add access token to request
        Request modifiedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .build();
        Timber.d("AuthInterceptor: Added token to request");

        Response response = chain.proceed(modifiedRequest);

        // Handle 401 Unauthorized (token expired)
        if (response.code() == 401) {
            synchronized (this) {
                // Attempt refresh only once per request
                String refreshToken = tokenManager.getRefreshToken();
                if (refreshToken == null || refreshToken.isEmpty()) {
                    redirectToLogin();
                    return response;
                }

                try {
                    // Refresh token endpoint call
                    retrofit2.Response<JsonObject> refreshResponse =
                            noAuthApiService.refreshToken().execute();

                    if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                        JsonObject jsonObject = refreshResponse.body();
                        String newAccess = jsonObject.get("access_token").getAsString();
                        String newRefresh = jsonObject.get("refresh_token").getAsString();
                        tokenManager.saveTokens(newAccess, newRefresh);

                        // Retry original request with new token
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Bearer " + newAccess)
                                .build();
                        response.close();
                        return chain.proceed(newRequest);
                    } else {
                        redirectToLogin();
                        return response;
                    }
                } catch (Exception e) {
                    Timber.e(e, "Token refresh failed");
                    redirectToLogin();
                    return response;
                }
            }
        }
        return response;
    }

    private void redirectToLogin() {
        Intent intent = new Intent(context, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
