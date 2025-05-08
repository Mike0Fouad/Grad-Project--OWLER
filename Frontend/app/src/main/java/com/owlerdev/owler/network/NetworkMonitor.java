// app/network/NetworkMonitor.java
package com.owlerdev.owler.network;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

/**
 * Monitors network connectivity status to gate backend sync operations.
 */
@Singleton
public class NetworkMonitor {
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>();

    /**
     * @param context Application context injected by Hilt
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    @Inject
    public NetworkMonitor(@ApplicationContext Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        initNetworkCallback();
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private void initNetworkCallback() {
        // Build a request for internet-capable networks
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        // Register callback to listen for changes
        connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                isConnected.postValue(true);
                Timber.d("NetworkMonitor: Network available");
            }

            @Override
            public void onLost(@NonNull Network network) {
                isConnected.postValue(false);
                Timber.d("NetworkMonitor: Network lost");
            }
        });

        // Set initial connectivity status
        isConnected.setValue(checkConnection());
        Timber.d("NetworkMonitor: Initial connectivity = %s", isConnected.getValue());
    }

    /**
     * LiveData that emits connectivity status updates.
     */
    public LiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    /**
     * Synchronous check of current connectivity.
     */
    public boolean isConnected() {
        return Boolean.TRUE.equals(isConnected.getValue());
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private boolean checkConnection() {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
