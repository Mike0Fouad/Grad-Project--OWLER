package com.owlerdev.owler.ui.activity;

import androidx.core.splashscreen.SplashScreen;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.owlerdev.owler.R;
import com.owlerdev.owler.data.local.TokenManager;
import com.owlerdev.owler.databinding.ActivityMainBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen.installSplashScreen(this);
        // Inflate layout via View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Find NavHostFragment and its NavController
        NavHostFragment navHost =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            navController = navHost.getNavController();
            // Hook up BottomNavigationView with NavController
            NavigationUI.setupWithNavController(binding.bottomNavigationView, navController);
        }

        // Use a post to make sure NavController is fully set up
        binding.getRoot().post(() -> {
            if (navController != null) {
                navController.navigate(R.id.navigation_profile); // Ensure correct ID
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle Up button
        return navController != null && navController.navigateUp()
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
