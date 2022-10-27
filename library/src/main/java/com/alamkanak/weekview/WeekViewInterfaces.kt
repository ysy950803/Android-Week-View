package com.alamkanak.weekview

import android.graphics.RectF
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

interface EmptyViewClickListener {
    /**
     * Triggered when the users clicks on a empty space of the calendar.
     *
     * @param time: [Calendar] object set with the date and time of the clicked position on the view.
     */
    fun onEmptyViewClicked(time: Calendar)
}

interface EmptyViewLongPressListener {
    /**
     * Similar to [com.alamkanak.weekview.EmptyViewClickListener] but with long press.
     *
     * @param time: [Calendar] object set with the date and time of the long pressed position on the view.
     */
    fun onEmptyViewLongPress(time: Calendar)
}

interface EventClickListener {
    /**
     * Triggered when clicked on one existing event
     *
     * @param event:     event clicked.
     * @param eventRect: view containing the clicked event.
     */
    fun onEventClick(event: WeekViewEvent, eventRect: RectF)
}

interface EventLongPressListener {
    /**
     * Similar to [com.alamkanak.weekview.EventClickListener] but with a long press.
     *
     * @param event:     event clicked.
     * @param eventRect: view containing the clicked event.
     */
    fun onEventLongPress(event: WeekViewEvent, eventRect: RectF)
}

interface ScrollListener {
    /**
     * Called when the first visible day has changed.
     *
     * (this will also be called during the first draw of the weekview)
     *
     * @param newFirstVisibleDay The new first visible day
     * @param oldFirstVisibleDay The old first visible day (is null on the first call).
     */
    fun onFirstVisibleDayChanged(newFirstVisibleDay: Calendar, oldFirstVisibleDay: Calendar?)
}

interface WeekViewLoader {
    /**
     * Convert a date into a double that will be used to reference when you're loading data.
     *
     *
     * All periods that have the same integer part, define one period. Dates that are later in time
     * should have a greater return value.
     *
     * @param instance the date
     * @return The period index in which the date falls (floating point number).
     */
    fun toWeekViewPeriodIndex(instance: Calendar): Int

    /**
     * Load the events within the period
     *
     * @param periodIndex the period to load
     */
    fun onLoad(periodIndex: Int)
}

interface DateTimeInterpreter {
    fun interpretDate(date: Calendar): List<String>
    fun interpretTime(hour: Int, minute: Int): String
}

interface CascadeScrollListener {
    fun onScrolling(currentOriginX: Float)
    fun onScrollEnd(newFirstVisibleDay: Calendar)
}

/**
 * Get the interpreter which provides the text to show in the header column and the header row.
 */
internal val dateTimeInterpreter: DateTimeInterpreter = object : DateTimeInterpreter {
    override fun interpretDate(date: Calendar): List<String> = runCatching {
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        listOf(
            if (WeekViewUtil.isSameDay(date, Calendar.getInstance())) "今天" // TODO 翻译
            else sdf.format(date.time).toUpperCase(Locale.getDefault()),
            "${date.get(Calendar.DAY_OF_MONTH)}"
        )
    }.getOrDefault(listOf("", ""))

    override fun interpretTime(hour: Int, minute: Int): String = runCatching {
        val calendar = Calendar.getInstance().apply {
            this[Calendar.HOUR_OF_DAY] = hour
            this[Calendar.MINUTE] = minute
        }
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(calendar.time)
    }.getOrDefault("")
}
