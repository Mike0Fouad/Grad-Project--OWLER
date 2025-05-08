// app/service/CalendarService.java
package com.owlerdev.owler.service;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.owlerdev.owler.data.repository.CalendarRepository;
import com.owlerdev.owler.model.calendar.GoogleFitData;
import com.owlerdev.owler.model.calendar.MLData;
import com.owlerdev.owler.model.calendar.Schedule;
import com.owlerdev.owler.model.calendar.Task;
import com.owlerdev.owler.utils.Result;
import com.owlerdev.owler.utils.ScheduleOptimizer;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * Service layer for calendar operations: schedules, tasks, and optimized suggestions.
 */
@Singleton
public class CalendarService {
    private final CalendarRepository calendarRepository;
    private final UserService userService;


    @Inject
    public CalendarService(CalendarRepository calendarRepository, UserService userService) {
        this.calendarRepository = calendarRepository;
        this.userService = userService;
    }

    /**
     * Retrieve the schedule for a given user and date.
     */
    public LiveData<Result<Schedule>> getSchedule(String date) {
        return calendarRepository.getSchedule(date);
    }

    /**
     * update Schedule daily score
     */
    public void updateDailyScore(String date) {
        LiveData<Result<Schedule>> scheduleLiveData = this.getSchedule(date);
        Result<Schedule> result = scheduleLiveData.getValue();
        
        if (result == null || !result.isSuccess() || result.getData() == null) {
            // If schedule is not available, create a new one
            Schedule schedule = new Schedule();
            calendarRepository.addSchedule(date, schedule);
            return;
        }

        Schedule schedule = result.getData();
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        int dailyScore = 0;
        
        if (schedule != null) {
            List<Task> tasks = schedule.getTasks();
            if (tasks != null && !tasks.isEmpty()) {
                for (Task task : tasks) {
                    if (task.isDone()) {
                        dailyScore += 1;
                    }
                }
                // daily score out of 10
                dailyScore = (int) ((dailyScore / (float) tasks.size()) * 10);
            }
            
            // update schedule with new daily score
            schedule.setDailyScore(dailyScore);
            live.setValue(Result.success(null));

            // Get current productivity score, defaulting to 0 if null
            int productivityScore = 0;
            Result<Integer> currentScoreResult = userService.getProductivityScore().getValue();
            if (currentScoreResult != null && currentScoreResult.isSuccess() && currentScoreResult.getData() != null) {
                productivityScore = currentScoreResult.getData();
            }
                    
            // update schedule done status and productivity score
            if (dailyScore == 10) {
                schedule.setDone();
                userService.setProductivityScore(productivityScore + 5);
                live.setValue(Result.success(null));
            } else {
                if (schedule.getDone() == 1.0f) {
                    userService.setProductivityScore(Math.max(0, productivityScore - 5));
                }
                schedule.setDone();
            }
        }
        calendarRepository.addSchedule(date, schedule);
        live.setValue(Result.success(null));
    }

    /**
     * /**
     * Add or update a schedule for a given user and date.
     */
    public LiveData<Result<Void>> addSchedule(String date, Schedule schedule) {
        return calendarRepository.addSchedule(date, schedule);
    }

    /**
     * Add a task to a specific day and sync the schedule.
     */
    public LiveData<Result<Void>> addTask(String date, Task task) {
        return calendarRepository.addTask(date, task);


    }

    /**
     * Remove a task by name from a specific day and sync the schedule.
     */
    public LiveData<Result<Void>> removeTask(String date, Task task) {
        LiveData<Result<Void>> live = calendarRepository.removeTask(date, task);
        this.updateDailyScore(date);
        return live;
    }

    /**
     * Edit (add/update) a task for a specific day and sync the schedule.
     */
    public LiveData<Result<Void>> editTask(String date, Task task) {
                LiveData<Result<Void>> live = calendarRepository.editTask(date, task);
                this.updateDailyScore(date);
        return live;
    }

    //    public LiveData<Result<Void>> checkTask(String date, Task task) {
//        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
//        Schedule schedule = Objects.requireNonNull(this.getSchedule(date).getValue()).getData();
//        if (schedule != null) {
//            Task foundTask = schedule.getTasks().get(schedule.findTaskIndex(task));
//
//            if (foundTask.equals(task)) {
//                task.setDone(!task.isDone());
//
//                calendarRepository.addSchedule(date, schedule);
//
//                this.updateDailyScore(date);
//                live.setValue(Result.success(null));
//
//            }
//
//        }
//        return live;
//    }

    //    public LiveData<Result<UserData>> getUserData(String date) {
//        return calendarRepository.getUserData(date);
//    }

    /**
     * Retrieve ML prediction data for a given day.
     */
    public LiveData<Result<List<MLData>>> getMlData(String date) {
        return calendarRepository.getMlData(date);
    }

    /**
     * Retrieve Google Fit data for a given day.
     */
    public LiveData<Result<GoogleFitData>> getGoogleFitData(String date) {
        return calendarRepository.getGoogleFitData(date);
    }

//    /**
//     * Save or update a full Day locally without syncing.
//     */
//    public LiveData<Result<Boolean>> saveLocalDay(Day day) {
//        calendarRepository.saveLocalDay(day);
//    }

    /**
     * Suggests an optimized schedule by delegating to ScheduleOptimizer.
     */
    public LiveData<Result<Schedule>> suggestSchedule(String date) {
        MutableLiveData<Result<Schedule>> result = new MutableLiveData<>();
        
        // Get ML data
        LiveData<Result<List<MLData>>> mlDataLive = this.getMlData(date);
        Result<List<MLData>> mlDataResult = mlDataLive.getValue();
        
        if (mlDataResult == null || !mlDataResult.isSuccess() || mlDataResult.getData() == null || mlDataResult.getData().isEmpty()) {
            result.setValue(Result.error("No ML data available for schedule suggestions"));
            return result;
        }

        // Get current schedule
        LiveData<Result<Schedule>> scheduleLive = this.getSchedule(date);
        Result<Schedule> scheduleResult = scheduleLive.getValue();
        
        if (scheduleResult == null || !scheduleResult.isSuccess() || scheduleResult.getData() == null) {
            result.setValue(Result.error("No schedule available for optimization"));
            return result;
        }

        try {
            Schedule suggestedSchedule = ScheduleOptimizer.suggestSchedule(mlDataResult.getData(), scheduleResult.getData());
            result.setValue(Result.success(suggestedSchedule));
        } catch (Exception e) {
            Timber.e(e, "Error suggesting schedule");
            result.setValue(Result.error("Failed to generate schedule suggestions: " + e.getMessage()));
        }
        
        return result;
    }

}