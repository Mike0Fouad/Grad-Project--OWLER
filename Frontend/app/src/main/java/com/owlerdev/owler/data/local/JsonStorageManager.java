// app/data/local/JsonStorageManager.java
package com.owlerdev.owler.data.local;

import android.content.Context;
import com.owlerdev.owler.model.calendar.Calendar;
import com.owlerdev.owler.model.calendar.Day;
import com.owlerdev.owler.model.calendar.Schedule;
import com.owlerdev.owler.model.calendar.UserData;
import com.owlerdev.owler.model.calendar.MLData;
import com.owlerdev.owler.model.calendar.GoogleFitData;
import com.owlerdev.owler.utils.JsonUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
//import java.util.Iterator;
import java.util.List;

import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

/**
 * Offline JSON storage manager for calendar data.
 * Handles all local persistence using JsonUtils.
 */
@Singleton
public class JsonStorageManager {
    private static final String FILE_NAME = "calendar_data.json";
    private final Context context;

    @Inject
    public JsonStorageManager(@ApplicationContext Context context) {
        this.context = context;
    }

    /** Load the entire calendar from disk. Returns null if not found. */
    public Calendar loadCalendar() {
        return JsonUtils.readJsonFromFile(context, FILE_NAME, Calendar.class);
    }

    /** Save the entire calendar to disk. */
    public void saveCalendar(Calendar calendar) {
        JsonUtils.writeJsonToFile(context, FILE_NAME, calendar);
    }
    /** Create a new calendar file. */
    public void createCalendar() {
        Calendar calendar = new Calendar(new ArrayList<>());
        saveCalendar(calendar);
    }
    /** Delete the calendar file. */
    public void clearCalendar() {
        if (!context.deleteFile(FILE_NAME)) {
            Timber.e("Failed to delete calendar file");
        }
    }

    /** Add or update a Day in the calendar. */
    public void addOrUpdateDay(Day day) {
        Calendar cal = loadCalendar();
        if (cal == null) {
            cal = new Calendar(new ArrayList<>());
        }
        List<Day> days = cal.getDays();
        boolean updated = false;
        for (int i = 0; i < days.size(); i++) {
            if (days.get(i).getDate().equals(day.getDate())) {
                days.set(i, day);
                updated = true;
                break;
            }
        }
        if (!updated) {
            days.add(day);
        }
        saveCalendar(cal);
    }

    // public void removeDay(String date) {
    //     Calendar cal = loadCalendar();
    //     if (cal == null || cal.getDays() == null) return;
    //
    //     Iterator<Day> it = cal.getDays().iterator();
    //     while (it.hasNext()) {
    //         if (it.next().getDate().equals(date)) {
    //             it.remove();
    //             break;
    //         }
    //     }
    //     saveCalendar(cal);
    // }

    /** Load a specific Day by date, or null if not found. */
    public Day loadDay(String date) {
        Calendar cal = loadCalendar();
        if (cal == null || cal.getDays() == null) return null;
        for (Day d : cal.getDays()) {
            if (d.getDate().equals(date)) {
                return d;
            }
        }
        return null;
    }

    // /** Load all days, or return empty list if none. */
    //public List<Day> loadAllDays() {
    //    Calendar cal = loadCalendar();
    //    return cal != null && cal.getDays() != null
    //            ? cal.getDays()
    //            : new ArrayList<>();
    //}

    /** Get the Schedule for a specific date, or null. */
    public Schedule getSchedule(String date) {
        Day day = loadDay(date);
        return day != null ? day.getSchedule() : null;
    }

    /** Add or update Schedule for a specific date. */
    public void addOrUpdateSchedule(String date, Schedule schedule) {
        Day day = loadDay(date);
        if (day == null) {
            day = new Day(date, schedule);
        } else {
            day.setSchedule(schedule);
        }
        addOrUpdateDay(day);
    }

    /** Get UserData for a specific date, or null. */
    public UserData getUserData(String date) {
        Day day = loadDay(date);
        return day != null ? day.getUserData() : null;
    }

    /** Add or update UserData for a specific date. */
    public void addOrUpdateUserData(String date, UserData userData) {
        Day day = loadDay(date);
        if (day == null) {
            day = new Day(date, userData);
        } else {
            day.setUserData(userData);
        }
        addOrUpdateDay(day);
    }

    /** Get MLData list for a specific date, or null. */
    public List<MLData> getMlData(String date) {
        Day day = loadDay(date);
        return day != null ? day.getMlData() : null;
    }

    // /** Save MLData list for a specific date. */
    //public void saveMlData(String date, List<MLData> mlData) {
    //    Day day = loadDay(date);
    //    if (day == null) {
    //        day = new Day(date, null, null, null);
    //    }
    //    day.setMlData(mlData);
    //    addOrUpdateDay(day);
    //}

    /** Get GoogleFitData for a specific date, or null. */
    public GoogleFitData getGoogleFitData(String date) {
        Day day = loadDay(date);
        return day != null ? day.getGoogleFitData() : null;
    }

    // /** Save GoogleFitData for a specific date. */
    //public void saveGoogleFitData(String date, GoogleFitData googleFitData) {
    //    Day day = loadDay(date);
    //    if (day == null) {
    //        day = new Day(date, null, null, null);
    //    }
    //    day.setGoogleFitData(googleFitData);
    //    addOrUpdateDay(day);
    //}
}