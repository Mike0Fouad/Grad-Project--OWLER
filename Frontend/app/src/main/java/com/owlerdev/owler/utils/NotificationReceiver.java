package com.owlerdev.owler.utils;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.owlerdev.owler.R;

import timber.log.Timber;

/**
 * Receives the alarm intent and shows the notification.
 */
public class NotificationReceiver extends BroadcastReceiver {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @Override
    public void onReceive(Context context, Intent intent) {
        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Timber.w("NotificationReceiver: POST_NOTIFICATIONS permission not granted");
            return;
        }

        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        int notifId = intent.getIntExtra("notifId", 0);

        int iconRes = R.drawable.ic_launcher_foreground; // replace with your notification icon

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build());
            Timber.d("NotificationReceiver: Displayed notification '%s'", message);
        } catch (SecurityException se) {
            Timber.e(se, "NotificationReceiver: Notification permission error");
        }
    }
}