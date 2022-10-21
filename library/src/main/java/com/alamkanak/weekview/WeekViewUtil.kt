package com.alamkanak.weekview

import android.view.HapticFeedbackConstants
import android.view.View
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil

object WeekViewUtil {

    /**
     * Checks if two times are on the same day.
     *
     * @param dayOne The first day.
     * @param dayTwo The second day.
     * @return Whether the times are on the same day.
     */
    @JvmStatic
    fun isSameDay(dayOne: Calendar, dayTwo: Calendar): Boolean {
        return dayOne[Calendar.YEAR] == dayTwo[Calendar.YEAR] && dayOne[Calendar.DAY_OF_YEAR] == dayTwo[Calendar.DAY_OF_YEAR]
    }

    /**
     * Returns a calendar instance at the start of this day
     *
     * @return the calendar instance
     */
    @JvmStatic
    fun today(): Calendar {
        val today = Calendar.getInstance()
        today[Calendar.HOUR_OF_DAY] = 0
        today[Calendar.MINUTE] = 0
        today[Calendar.SECOND] = 0
        today[Calendar.MILLISECOND] = 0
        return today
    }

    // 只适用于全天日程
    @JvmStatic
    fun allDaysBetween(dayOne: Calendar, dayTwo: Calendar) = ceil(
        ((dayTwo.timeInMillis - dayOne.timeInMillis) * 1f
            / TimeUnit.DAYS.toMillis(1)).toDouble()
    ).toInt()

    @JvmStatic
    fun isContainsAllDay(day: Calendar, event: WeekViewEvent) =
        day.timeInMillis <= event.endTime.timeInMillis && event.startTime.timeInMillis <= day.timeInMillis && event.isAllDay

    // 忽略float误差，避免UI联动滑动细微晃动
    @JvmStatic
    fun isFloatEqual(a: Float, b: Float) = abs(a - b) < 1f

    @JvmStatic
    fun performPressVibrate(view: View) {
        view.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
}
