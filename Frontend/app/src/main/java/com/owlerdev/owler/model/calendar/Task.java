// app/model/Task.java
package com.owlerdev.owler.model.calendar;

public class Task {
    private String name;
    private String start;      // e.g. "08:00"
    private String end;        // e.g. "09:00"
    private String deadline;   // e.g. "09:00"
    private boolean done;
    private int mental;        // 1–10
    private int physical;      // 1–10
    private int exhaustion;    // 1–10
    private int priority;      // 1 = highest


    public Task() { }
    public Task(String name){this.name =name;}
    public Task(String name, String start, String end,
                boolean done, int mental, int physical,
                int exhaustion, int priority, String deadline) {
        this.name = name;
        this.start = start;
        this.end = end;
        this.done = done;
        this.mental = mental;
        this.physical = physical;
        this.exhaustion = exhaustion;
        this.deadline = deadline;
        this.priority = priority;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
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

    public String getDeadline() {return deadline;    }

    public void setDeadline(String deadline) {
        if (checkTime(deadline))
            this.deadline = deadline;
        else
            throw new IllegalArgumentException("Invalid deadline time format. Must be HH:MM.");
    }

    public boolean checkTime(String time){
        if (time == null || time.isEmpty()) {
            throw new IllegalArgumentException("Time cannot be null or empty");
        }
        if (!time.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("Time must be in the format HH:MM");
        }
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
            throw new IllegalArgumentException("Invalid time values. Hours must be between 00 and 23, minutes must be between 00 and 59.");
        }
        return true;
    }

    public boolean isDone() {
        return this.done;
    }
    public void setDone(boolean done) {
        this.done = done;
    }

    public int getMental() {
        return mental;
    }
    public void setMental(int mental) {
        this.mental = mental;
    }

    public int getPhysical() {
        return physical;
    }
    public void setPhysical(int physical) {
        this.physical = physical;
    }

    public int getExhaustion() {
        return exhaustion;
    }
    public void setExhaustion(int exhaustion) {
        this.exhaustion = exhaustion;
    }

    public int getPriority() {
        return priority;
    }
    public void setPriority(int priority) {
        this.priority = priority;
    }




    public boolean equals(Task other){
        int var = 0;
        var += this.name.equals(other.getName()) ? 0 : 1;
        var += this.start.equals(other.getStart()) ? 0 : 1;
        var += this.end.equals(other.getEnd()) ? 0 : 1;
        var += this.done == other.isDone() ? 0 : 1;
        var += this.mental == other.getMental() ? 0 : 1;
        var += this.physical == other.getPhysical() ? 0 : 1;
        var += this.exhaustion == other.getExhaustion() ? 0 : 1;
        var += this.priority == other.getPriority() ? 0 : 1;
        var += this.deadline.equals(other.getDeadline()) ? 0 : 1;
        return (var < 2);
    }
}
