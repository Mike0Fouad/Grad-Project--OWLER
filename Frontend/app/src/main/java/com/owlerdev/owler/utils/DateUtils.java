// app/utils/DateUtils.java
package com.owlerdev.owler.utils;

import android.annotation.SuppressLint;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import timber.log.Timber;

/**
 * Utility methods for parsing and formatting dates consistently across the app.
 * Uses Timber for logging parse errors.
 */
public class DateUtils {

    @SuppressLint("ThreadSafe")
    private static final ThreadLocal<SimpleDateFormat> ISO_DATE_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    );

    /**
     * Formats a timestamp (milliseconds since epoch) into an ISO date string (yyyy-MM-dd).
     *
     * @param millis timestamp in milliseconds since epoch
     * @return formatted date string
     */
    public static String formatTimestamp(long millis) {
        Date date = new Date(millis);
        return Objects.requireNonNull(ISO_DATE_FORMAT.get()).format(date);
    }

    /**
     * Returns today's date as an ISO date string (yyyy-MM-dd).
     *
     * @return formatted date string for today
     */
    public static String getTodayDate() {
        return formatTimestamp(System.currentTimeMillis());
    }

    /**
     * Parses an ISO date string (yyyy-MM-dd) into milliseconds since epoch.
     *
     * @param dateString date string in yyyy-MM-dd format
     * @return timestamp in milliseconds, or -1 if parsing fails
     */
    public static long parseDate(String dateString) {
        try {
            Date date = Objects.requireNonNull(ISO_DATE_FORMAT.get()).parse(dateString);
            return date != null ? date.getTime() : -1;
        } catch (ParseException e) {
            Timber.e(e, "DateUtils: Failed to parse date string '%s'", dateString);
            return -1;
        }
    }
}
