package com.owlerdev.owler.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.owlerdev.owler.data.local.TokenManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LauncherActivity extends AppCompatActivity {

    @Inject
    TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String token = tokenManager.getAccessToken();

        // Check if the token is null, empty, or expired
        Intent intent;
        if (token == null || token.isEmpty() || isTokenExpired(token)) {
            // Redirect to AuthActivity if no token or token is invalid/expired
            intent = new Intent(this, AuthActivity.class);
        } else {
            // Redirect to MainActivity (or ProfileFragment directly depending on the flow)
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        finish(); // Prevent going back to LauncherActivity
    }

    /**
     * Dummy token expiry check (implement your own logic here)
     */
    private boolean isTokenExpired(String token) {

        return false;
    }
}
