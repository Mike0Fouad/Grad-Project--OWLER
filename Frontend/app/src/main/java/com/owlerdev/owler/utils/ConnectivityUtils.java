// app/utils/ConnectivityUtils.java
package com.owlerdev.owler.utils;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;


import androidx.annotation.RequiresPermission;

import timber.log.Timber;

/**
 * Utility class to check network connectivity status.
 */
public class ConnectivityUtils {

    /**
     * Checks if the device currently has an active internet connection.
     *
     * @param context Application or Activity context
     * @return true if connected to the internet, false otherwise
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                Timber.w("ConnectivityUtils: ConnectivityManager is null");
                return false;
            }

            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (Exception e) {
            Timber.e(e, "ConnectivityUtils: Error checking network status");
            return false;
        }
    }
}