package com.alamkanak.weekview;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by jesse on 6/02/2016.
 */
public class WeekViewUtil {


    /////////////////////////////////////////////////////////////////
    //
    //      Helper methods.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * Checks if two times are on the same day.
     *
     * @param dayOne The first day.
     * @param dayTwo The second day.
     * @return Whether the times are on the same day.
     */
    public static boolean isSameDay(Calendar dayOne, Calendar dayTwo) {
        return dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR) && dayOne.get(Calendar.DAY_OF_YEAR) == dayTwo.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Returns a calendar instance at the start of this day
     *
     * @return the calendar instance
     */
    public static Calendar today() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today;
    }

    // TODO 暂时只适用于全天日程
    public static int daysBetween(Calendar dayOne, Calendar dayTwo) {
        return (int) Math.ceil((dayTwo.getTimeInMillis() - dayOne.getTimeInMillis()) * 1f / TimeUnit.DAYS.toMillis(1));
    }
    
    public static boolean isContainsAllDay(Calendar day, WeekViewEvent event) {
        return day.getTimeInMillis() <= event.getEndTime().getTimeInMillis()
                && event.getStartTime().getTimeInMillis() <= day.getTimeInMillis()
                && event.isAllDay();
    }
}
