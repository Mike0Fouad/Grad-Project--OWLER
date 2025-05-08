package com.owlerdev.owler.viewmodel;

import android.Manifest;

import androidx.annotation.RequiresPermission;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import dagger.hilt.android.lifecycle.HiltViewModel;

import com.owlerdev.owler.model.calendar.Schedule;
import com.owlerdev.owler.model.calendar.Task;
import com.owlerdev.owler.utils.NotificationHelper;
import com.owlerdev.owler.utils.Result;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

@HiltViewModel
public class NotificationViewModel extends ViewModel {
    private final NotificationHelper notificationHelper;

    @Inject
    public NotificationViewModel(NotificationHelper notificationHelper) {
        this.notificationHelper = notificationHelper;
    }

    /**
     * Schedule a reminder notification
     * <p>
     * @ param task The task to schedule a reminder for
     */


    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    public void scheduleReminder(Task task) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        try {
            if (Boolean.TRUE.equals(Objects.requireNonNull(notificationHelper.canScheduleExactAlarms().getValue()).getData())) {
                notificationHelper.scheduleTaskReminder(task);
            } else{
                live.setValue(Result.error("Exact alarms are not permitted"));
            }
        } catch (SecurityException e) {
            live.setValue(Result.error("Failed to schedule exact alarm: " + e.getMessage()));
        }
    }

    /**
     * Cancel a reminder notification
     * <p>
     * @ param task The task to cancel the reminder for
     */
    public void cancelReminder(Task task) {
        notificationHelper.cancelTaskReminder(task);
    }

    /**
     * Reschedule a reminder notification
     *
     * @param task The task to reschedule the reminder for
     */
    public void rescheduleReminder(Task task) {
        notificationHelper.RescheduleTaskReminder(task);
    }


    /**
     * Schedule all reminder notifications
     *
     * @param schedule The schedule containing the tasks to schedule reminders for
     */


    public LiveData<Result<Void>> scheduleAllReminders(Schedule schedule) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        List<Task> tasks = schedule.getTasks();
        if (Boolean.TRUE.equals(Objects.requireNonNull(notificationHelper.canScheduleExactAlarms().getValue()).getData())) {
            for (Task task : tasks) {
                LiveData<Result<Void>> result = notificationHelper.scheduleTaskReminder(task);
                if (result.getValue() != null && result.getValue().isSuccess())
                    live.setValue(Result.success(null));
            }
        } else {
            live.setValue(Result.error("Exact alarms are not permitted"));
        }
        if (live.getValue() == null) {
            live.setValue(Result.error("Failed to schedule all reminders"));
        }
        return live;
    }


    /**
     * Cancel all reminder notifications
     *
     * @param schedule The schedule containing the tasks to cancel reminders for
     */

    public LiveData<Result<Void>> cancelAllReminders(Schedule schedule) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        List<Task> tasks = schedule.getTasks();
        for (Task task : tasks) {
            if (Objects.requireNonNull(notificationHelper.cancelTaskReminder(task).getValue()).isSuccess())
                live.setValue(Result.success(null));
        }
        if (live.getValue() == null) {
            live.setValue(Result.error("Failed to cancel all reminders"));
        }
        return live;
    }

    /**
     * Reschedule all reminder notifications
     *
     * @param schedule The schedule containing the tasks to reschedule reminders for
     */

    public void rescheduleAllReminders(Schedule schedule) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        List<Task> tasks = schedule.getTasks();
        for (Task task : tasks) {
            if (Objects.requireNonNull(notificationHelper.RescheduleTaskReminder(task).getValue()).isSuccess())
                live.setValue(Result.success(null));
        }
        if (live.getValue() == null) {
            live.setValue(Result.error("Failed to reschedule all reminders"));
        }
    }

    public LiveData<Result<Boolean>> canScheduleExactAlarms() {
        return notificationHelper.canScheduleExactAlarms();
    }

}
