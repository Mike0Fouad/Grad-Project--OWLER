// app/model/Calendar.java
package com.owlerdev.owler.model.calendar;

import java.util.List;

/**
 * Container for a collection of Day objects, e.g. a month or week view.
 */
public class Calendar {
    /** List of days in this calendar view */
    private List<Day> days;

    public Calendar() { }

    /**
     * @param days the list of Day objects to display/manage
     */
    public Calendar(List<Day> days) {
        this.days = days;
    }

    public List<Day> getDays() {
        return days;
    }

//    public void setDays(List<Day> days) {
//        this.days = days;
//    }
}
