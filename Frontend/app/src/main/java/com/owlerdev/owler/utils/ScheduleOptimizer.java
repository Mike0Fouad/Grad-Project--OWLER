// app/utils/ScheduleOptimizer.java
package com.owlerdev.owler.utils;

import android.annotation.SuppressLint;

import com.owlerdev.owler.model.calendar.MLData;
import com.owlerdev.owler.model.calendar.Schedule;
import com.owlerdev.owler.model.calendar.Task;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class ScheduleOptimizer {

    private ScheduleOptimizer() {}

    public static Schedule suggestSchedule(List<MLData> mlDataList, Schedule currentSchedule) {

        Function<String, Integer> toMins = t -> {
            String[] p = t.split(":");
            return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
        };
        @SuppressLint("DefaultLocale") Function<Integer, String> toTime = mins ->
                String.format("%02d:%02d", mins / 60, mins % 60);

        int dayStart = toMins.apply(currentSchedule.getStart());
        int dayEnd   = toMins.apply(currentSchedule.getEnd());
        boolean[] occupied = new boolean[dayEnd - dayStart];


        List<Task> tasks = new ArrayList<>(currentSchedule.getTasks());
        tasks.forEach(t -> t.setPriority(
                (int)(computePriority(t, mlDataList, dayStart, dayEnd) * 1_000)
        ));
        tasks.sort(Comparator.comparingInt(Task::getPriority).reversed());


        List<Task> arranged = new ArrayList<>();
        for (Task task : tasks) {
            int duration = toMins.apply(task.getEnd()) - toMins.apply(task.getStart());
            int bestStart = findBestSlot(task, duration, mlDataList, dayStart, dayEnd, occupied, toMins);

            if (bestStart >= 0) {
                markOccupied(occupied, bestStart - dayStart, duration);
                arranged.add(new Task(
                        task.getName(),
                        toTime.apply(bestStart),
                        toTime.apply(bestStart + duration),
                        task.isDone(),
                        task.getMental(),
                        task.getPhysical(),
                        task.getExhaustion(),
                        task.getPriority(),
                        task.getDeadline()
                ));
            } else {
                arranged.add(task);
            }
        }

        // Build new Schedule
        Schedule opt = new Schedule();
        opt.setStart(currentSchedule.getStart());
        opt.setEnd(currentSchedule.getEnd());
        opt.addTasks(arranged);
        opt.setDone();
        opt.setExhaustion(currentSchedule.getExhaustion());
        opt.setDailyScore(currentSchedule.getDailyScore());
        return opt;
    }

    private static double computePriority(Task task, List<MLData> ml, int dayStart, int dayEnd) {
        double urgency = computeDeadlineUrgency(task.getDeadline());
        double mlScore = computeMLScore(task, ml, dayStart, dayEnd);
        double effort  = task.getMental() + task.getPhysical();
        return urgency * 0.6 + mlScore * 0.3 - effort * 0.1;
    }

    private static double computeDeadlineUrgency(String iso) {
        try {
            long secs = Duration.between(Instant.now(), Instant.parse(iso)).getSeconds();
            return secs > 0 ? 1.0 / secs : Double.MAX_VALUE;
        } catch (Exception e) {
            return 0;
        }
    }

    private static double computeMLScore(Task task, List<MLData> ml,
                                         int dayStart, int dayEnd) {
        Function<String, Integer> toMins = t -> {
            String[] p = t.split(":");
            return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
        };
        double score = 0;
        int dur = toMins.apply(task.getEnd()) - toMins.apply(task.getStart());
        for (MLData m : ml) {
            String[] slots = m.getTime_slot().split("-");
            int s = toMins.apply(slots[0]), e = toMins.apply(slots[1]);
            int overlap = Math.max(0, Math.min(dur, e - s));
            score += overlap * (m.getPredicted_CP() + m.getPredicted_PE());
        }
        return score;
    }

    private static int findBestSlot(Task task, int dur, List<MLData> ml,
                                    int dayStart, int dayEnd, boolean[] occ,
                                    Function<String, Integer> toMins) {
        double best = -1; int idx = -1;
        for (int t = dayStart; t <= dayEnd - dur; t++) {
            if (!isFree(occ, t - dayStart, dur)) continue;
            double sc = computeMLScore(task, ml, dayStart, dayEnd);
            if (sc > best) { best = sc; idx = t; }
        }
        return idx;
    }

    private static boolean isFree(boolean[] occ, int offset, int dur) {
        for (int i = 0; i < dur; i++) if (occ[offset + i]) return false;
        return true;
    }

    private static void markOccupied(boolean[] occ, int offset, int dur) {
        for (int i = 0; i < dur; i++) occ[offset + i] = true;
    }
}
