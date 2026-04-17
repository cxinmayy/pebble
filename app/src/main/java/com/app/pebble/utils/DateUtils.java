package com.app.pebble.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {

    private DateUtils() {}

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault());

    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    public static String formatDateTime(long timestamp) {
        return DATE_TIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Returns epoch millis for the start of the current month (midnight, day 1).
     */
    public static long getStartOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Returns epoch millis for the end of the current month (23:59:59.999 on last day).
     */
    public static long getEndOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }
}
