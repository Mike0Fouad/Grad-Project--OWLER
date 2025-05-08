// app/utils/NotificationHelper.java
package com.owlerdev.owler.utils;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.owlerdev.owler.model.calendar.Task;

import java.util.Calendar;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

@Singleton
public class NotificationHelper {
    public static final String CHANNEL_ID = "task_reminders";
    private final Context context;
    private final AlarmManager alarmManager;

    @Inject
    public NotificationHelper(@ApplicationContext Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Task Reminder Notifications",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notifications to remind you of scheduled tasks");
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
            Timber.d("NotificationHelper: Channel '%s' created", CHANNEL_ID);
        }
    }

    /**
     * Schedules a reminder at the task's start time.
     * Falls back to inexact alarm if exact alarms aren't allowed.
     */
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    public LiveData<Result<Void>> scheduleTaskReminder(Task task) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        String[] parts = task.getStart().split(":");
        int hour   = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("title",   "Task Reminder");
        intent.putExtra("message", task.getName());
        intent.putExtra("notifId", task.getPriority());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getPriority(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        boolean useExact = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            useExact = alarmManager.canScheduleExactAlarms();
            if (!useExact) {
                Timber.w("Exact alarms not permitted; scheduling inexact alarm instead");
            }
        }

        try {
            if (useExact) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );

                Timber.d("Scheduled EXACT reminder for '%s' at %s", task.getName(), task.getStart());
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );

                Timber.d("Scheduled inexact reminder for '%s' at %s", task.getName(), task.getStart());
            }
            live.postValue(Result.success(null));
        } catch (SecurityException se) {
            // fallback if we somehow lack the permission at runtime
            Timber.e(se, "Lacks SCHEDULE_EXACT_ALARM permission, falling back to inexact");
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } catch (Exception e) {
            Timber.e(e, "Failed to schedule reminder for '%s'", task.getName());
        }
        return live;
    }

    /** Cancels a previously scheduled reminder. */
    public LiveData<Result<Void>> cancelTaskReminder(Task task) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getPriority(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
        Timber.d("Cancelled reminder for task '%s'", task.getName());
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()){
            NotificationManagerCompat.from(context).cancel(task.getPriority());
            Timber.d("Cancelled notification for task '%s'", task.getName());
            live.postValue(Result.success(null));
            return live;
        }
        return live;
    }
    public LiveData<Result<Void>> RescheduleTaskReminder(Task task) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        cancelTaskReminder(task);
        Boolean isThereStillNotification = Objects.requireNonNull(IsNotification(task).getValue()).getData();
        if (Boolean.FALSE.equals(isThereStillNotification)) {
            scheduleTaskReminder(task);
            live.postValue(Result.success(null));
        }else
            live.postValue(Result.error("Failed to reschedule reminder"));
        return live;
    }
    public LiveData<Result<Boolean>> IsNotification(Task task) {
        MutableLiveData<Result<Boolean>> live = new MutableLiveData<>();
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getPriority(),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            live.postValue(Result.success(true));
        } else {
            live.postValue(Result.success(false));
        }
        return live;
    }

    /**
     * Checks if the app has permission to schedule exact alarms.
     * @return True if the app can schedule exact alarms, False otherwise
     */
    public LiveData<Result<Boolean>> canScheduleExactAlarms() {
        MutableLiveData<Result<Boolean>> live = new MutableLiveData<>();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                boolean canSchedule = alarmManager.canScheduleExactAlarms();
                live.postValue(Result.success(canSchedule));
            } else {
                live.postValue(Result.success(true));
            }
        } catch (Exception e) {
            Timber.e(e, "Failed to check exact alarm permission");
            live.postValue(Result.error("Failed to check exact alarm permission"));
        }
        return live;
    }


}
