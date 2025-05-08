// app/model/calendar/Day.java
package com.owlerdev.owler.model.calendar;



import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single day in the calendar, including its Schedule and user data.
 */
public class Day {

    private String date;                    // e.g., "2025-02-16"
    private Schedule schedule;
    private UserData UserData;             // holds MLData, aggregated task data

    /**
     * Default constructor for JSON serialization.
     */
    public Day() {}

    /**
     * Constructs a Day with Date, Schedule, and UserData.
     */
    public Day(String date, Schedule schedule, UserData userData) {
        this.date = date;
        this.schedule = schedule;
        this.UserData = userData;
    }



    public Day(String date, Schedule schedule) {
        this.date = date;
        this.schedule = schedule;
    }
    public Day(String date, UserData userData) {
        this.date = date;
        this.UserData = userData;
    }
    // --------------------
    // Getters & Setters
    // --------------------

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        if (date == null || date.isEmpty()) {
            throw new IllegalArgumentException("Date cannot be null or empty");
        }
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Date must be in the format YYYY-MM-DD");
        }

        this.date = date;
    }

    public Schedule getSchedule() {
        if (schedule == null) {
            schedule = new Schedule();
        }
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public UserData getUserData() {
        return UserData;
    }

    public void setUserData(UserData userData) {
        this.UserData = userData;
    }

    // --------------------
    // Task Helpers
    // --------------------

    /**
     * Returns the list of tasks for this day, or an empty list if none.
     */
    public List<Task> getTasks() {
        if (schedule == null || schedule.getTasks() == null) {
            return new ArrayList<>();
        }
        return schedule.getTasks();
    }

    /**
     * Adds or updates a task in the day's Schedule.
     */
    public void addOrUpdateTask(Task task) {
        if (schedule == null) {
            schedule = new Schedule();
        }
        int index = schedule.findTaskIndex(task);
        if (index == -1) {
            List<Task> tasks = new ArrayList<>();
            tasks.add(task);
            schedule.addTasks(tasks);
        }
        else{
            schedule.getTasks().set(index, task);

        }

    }

    /**
     * Removes a task by its name from the day's Schedule.
     */
    public void removeTask(Task task) {
        if (schedule == null || schedule.getTasks() == null) return;
        int index = schedule.findTaskIndex(task);
        if (index != -1)
            schedule.getTasks().remove(index);

    }

    // --------------------
    // ML Data Helpers
    // --------------------

    /**
     * Returns ML prediction data for this day, or null.
     */
    public List<MLData> getMlData() {
        return UserData != null ? UserData.getMlData() : null;
    }

    /**
     * Sets ML prediction data for this day.
     */
    public void setMlData(List<MLData> mlData) {
        if (UserData == null) {
            UserData = new UserData();
        }
        UserData.setMlData(mlData);
    }

    /**
     * Returns Google Fit data for this day, or null.
     */
    public GoogleFitData getGoogleFitData() {
        return UserData != null ? UserData.getGoogleFitData() : null;
    }

    /**
     * Sets Google Fit data for this day.
     */
    public void setGoogleFitData(GoogleFitData googleFitData) {
        if (UserData == null) {
            UserData = new UserData();
        }
        UserData.setGoogleFitData(googleFitData);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Day day = (Day) o;
        return Objects.equals(date, day.date) && Objects.equals(schedule, day.schedule) && Objects.equals(UserData, day.UserData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, schedule, UserData);
    }
}
