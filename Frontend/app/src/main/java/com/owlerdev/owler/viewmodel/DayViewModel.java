// app/ui/viewmodel/DayViewModel.java
package com.owlerdev.owler.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;

import com.owlerdev.owler.model.calendar.Task;
import com.owlerdev.owler.model.calendar.Schedule;
import com.owlerdev.owler.service.CalendarService;
import com.owlerdev.owler.utils.Result;


import javax.inject.Inject;

@HiltViewModel
public class DayViewModel extends ViewModel {
    private final CalendarService calendarService;

    @Inject
    public DayViewModel(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    public LiveData<Result<Schedule>> getSchedule(String date) {
        return calendarService.getSchedule(date);
    }
    public LiveData<Result<Schedule>> suggestSchedule(String date) {
        return calendarService.suggestSchedule(date);
    }

    public LiveData<Result<Void>> changeSchedule(String date, Schedule schedule) {
        //notificationViewModel.rescheduleAllReminders(schedule);
        return calendarService.addSchedule(date, schedule);
    }



    //  Tasks operations
    public LiveData<Result<Void>> addTask(String date, Task task) {
        task.setDone(false);

        return calendarService.addTask(date, task);
    }

    public LiveData<Result<Void>> updateTaskStartTime(String date, Task task, String value) {
        task.setStart(value);

        return calendarService.editTask(date,task);
    }
    public LiveData<Result<Void>> updateTaskEndTime(String date, Task task, String value) {
        task.setEnd(value);
        return calendarService.editTask(date,task);
    }
    public LiveData<Result<Void>> updateTaskDeadline(String date, Task task, String value) {
        task.setDeadline(value);
        return calendarService.editTask(date,task);
    }
    public LiveData<Result<Void>> updateTaskMental(String date, Task task, int value) {
        task.setMental(value);
        return calendarService.editTask(date,task);
    }

    public LiveData<Result<Void>> updateTaskPhysical(String date, Task task, int value) {
        task.setPhysical(value);
        return calendarService.editTask(date,task);
    }

    public LiveData<Result<Void>> updateTaskExhaustion(String date, Task task, int value) {
        task.setExhaustion(value);
        return calendarService.editTask(date,task);
    }

    public LiveData<Result<Void>> renameTask(String date, Task task, String newName) {
        task.setName(newName);
        return calendarService.editTask(date,task);
    }
    public LiveData<Result<Void>> deleteTask(String date, Task task) {
        return calendarService.removeTask(date,task);
    }
    public LiveData<Result<Void>> checkTask(String date, Task task) {
        task.setDone(true);
        return calendarService.editTask(date, task);
    }
    public LiveData<Result<Void>> uncheckTask(String date, Task task) {
        task.setDone(false);
        //notificationViewModel.scheduleReminder(task);
        return calendarService.editTask(date, task);
    }
}
