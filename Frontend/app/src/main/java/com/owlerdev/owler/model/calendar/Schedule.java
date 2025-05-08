// app/model/Schedule.java
package com.owlerdev.owler.model.calendar;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

public class Schedule {
    private String start;
    private String end;
    private float done;
    private int exhaustion;
    private int dailyScore;
    private List<Task> tasks = new ArrayList<>();

    public Schedule() {
        this.tasks = new ArrayList<>();
    }

    public Schedule(String start,
                    String end,
                    float done,
                    int exhaustion,
                    int dailyScore,
                    List<Task> tasks) {
        this.start = start;
        this.end = end;
        this.done = done;
        this.exhaustion = exhaustion;
        this.dailyScore = dailyScore;
        this.tasks = tasks;
    }

    public String getStart() {
        return start;
    }
    public void setStart(String start) {

        if (checkTime(start))
            this.start = start;
        else
            throw new IllegalArgumentException("Invalid start time format. Must be HH:MM.");
    }
    public String getEnd() {
        return end;
    }
    public void setEnd(String end) {

        if (checkTime(end))
            this.end = end;
        else
            throw new IllegalArgumentException("Invalid end time format. Must be HH:MM.");
    }

    public float getDone() {
        return done;
    }
    public void setDone() {
        float totalDone = 0;
        for (Task task : tasks) {
            if (task.isDone()) {
                totalDone += 1;
            }
        }
        this.done = totalDone / tasks.size();

    }

    public int getExhaustion() {
        return exhaustion;
    }
    public void setExhaustion(int exhaustion) {
        if (!tasks.isEmpty()) {
            if (exhaustion==0){
                int exhaustionLevel = 0;
                for (Task task : tasks) {
                    if (task.getExhaustion() > 0) {
                        exhaustionLevel += task.getExhaustion();
                    }
                }
                this.exhaustion = (exhaustionLevel / tasks.size())* 10;
            }
            else{
                this.exhaustion = exhaustion;
            }
        }else {
            this.exhaustion = 0;
        }

    }

    public int getDailyScore() {
        return dailyScore;
    }
    public void setDailyScore(int dailyScore) {
        this.dailyScore = dailyScore;
    }

    public List<Task> getTasks() {
        this.arrangeTasks();
        return this.tasks;
    }
    public void addTasks(List<Task> newTasks) {

        if (tasks == null) {
            tasks = newTasks;
        } else {
            this.tasks.addAll(newTasks);
        }
        this.arrangeTasks();
        this.setDone();
        this.setExhaustion(0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Schedule schedule = (Schedule) o;
        return Float.compare(done, schedule.done) == 0 && exhaustion == schedule.exhaustion && dailyScore == schedule.dailyScore && Objects.equals(start, schedule.start) && Objects.equals(end, schedule.end) && Objects.equals(tasks, schedule.tasks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, done, exhaustion, dailyScore, tasks);
    }

    public boolean checkTime(String time){
        if (start == null || start.isEmpty()) {
            throw new IllegalArgumentException("Start time cannot be null or empty");
        }
        if (!time.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("Time must be in the format HH:MM");
        }
        String[] parts = start.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        if (hours < 0 || hours > 12 || minutes < 0 || minutes > 59) {
            throw new IllegalArgumentException("Invalid time values. Hours must be between 01 and 12, " +
                    "minutes must be between 00 and 59.");

        }
        return false;
    }
    public int findTaskIndex(Task task) {
        if (tasks == null) return -1;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).equals(task)) {
                return i;

            }
        }
        return -1;
    }
    public void arrangeTasks() {
        if (tasks == null || tasks.isEmpty()) return;

        tasks.sort((task1, task2) -> {
            String time1 = task1.getStart();
            String time2 = task2.getStart();
            return compareTasksTimes(time1, time2);
        });
    }
    public int compareTasksTimes(String start1, String start2){
        String[] parts1 = start1.split(":");
        String[] parts2 = start2.split(":");
        int hours1 = Integer.parseInt(parts1[0]);
        int minutes1 = Integer.parseInt(parts1[1]);
        int hours2 = Integer.parseInt(parts2[0]);
        int minutes2 = Integer.parseInt(parts2[1]);

        if (hours1 != hours2) {
            return Integer.compare(hours1, hours2);
        } else {
            return Integer.compare(minutes1, minutes2);
        }


    }


    public boolean isEmpty() {
        return tasks == null || tasks.isEmpty();
    }



}
