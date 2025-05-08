package com.owlerdev.owler;

import android.app.Application;


import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class OwlerApp extends Application {

    private static final String TAG = "OwlerApp"; // Log tag

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.tag(TAG).d("onCreate: Initializing application (DEBUG mode)");
            Timber.plant(new Timber.DebugTree());
        }

        // Example of using Android Log directly
        Timber.tag(TAG).d("onCreate: Global initialization completed");
    }
}