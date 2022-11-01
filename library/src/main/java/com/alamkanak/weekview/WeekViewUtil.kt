package com.alamkanak.weekview

import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.HapticFeedbackConstants
import android.view.View
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil

fun View.performPressVibrate() {
    this.performHapticFeedback(
        HapticFeedbackConstants.LONG_PRESS,
        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    )
}

fun Calendar.trimOfDay() {
    this[Calendar.HOUR_OF_DAY] = 0
    this[Calendar.MINUTE] = 0
    this[Calendar.SECOND] = 0
    this[Calendar.MILLISECOND] = 0
}

fun Calendar.containsAllDay(event: WeekViewEvent) = event.isAllDay
    && this.timeInMillis < event.drawEndTime.timeInMillis
    && event.drawStartTime.timeInMillis <= this.timeInMillis

object WeekViewUtil {

    @JvmStatic
    fun calculateDiffDay(a: Calendar, b: Calendar): Int = kotlin.runCatching {
        val diff = (b.timeInMillis + b.timeZone.getOffset(b.timeInMillis)
            - (a.timeInMillis + a.timeZone.getOffset(a.timeInMillis)))
        val day = diff / TimeUnit.DAYS.toMillis(1)
        day.toInt()
    }.getOrDefault(-1)

    /**
     * Checks if two times are on the same day.
     *
     * @param dayOne The first day.
     * @param dayTwo The second day.
     * @return Whether the times are on the same day.
     */
    @JvmStatic
    fun isSameDay(dayOne: Calendar, dayTwo: Calendar): Boolean {
        return dayOne[Calendar.YEAR] == dayTwo[Calendar.YEAR]
            && dayOne[Calendar.DAY_OF_YEAR] == dayTwo[Calendar.DAY_OF_YEAR]
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

    // 忽略float误差，避免UI联动滑动细微晃动
    @JvmStatic
    fun isFloatEqual(a: Float, b: Float) = abs(a - b) < 1f

    @JvmStatic
    fun obtainStaticLayout(
        cs: CharSequence, textPaint: TextPaint, availableWidth: Int,
        truncateAt: TextUtils.TruncateAt? = null, maxLines: Int = 0
    ) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        StaticLayout.Builder.obtain(cs, 0, cs.length, textPaint, availableWidth).apply {
            if (truncateAt != null) setEllipsize(truncateAt).setMaxLines(maxLines)
        }.build()
    } else {
        StaticLayout(
            if (truncateAt != null) TextUtils.ellipsize(
                cs,
                textPaint,
                (availableWidth * maxLines).toFloat(),
                truncateAt
            ) else cs,
            textPaint,
            availableWidth,
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,
            0.0f,
            false
        )
    }
}
