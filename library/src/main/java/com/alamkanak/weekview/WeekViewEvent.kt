package com.alamkanak.weekview

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.graphics.ColorUtils.setAlphaComponent
import java.util.Calendar

fun Paint.withBorderColor(event: WeekViewEvent): Paint {
    val pastEvent = event.endTime < Calendar.getInstance()
    when (event.status) {
        WeekViewEvent.Status.CONFIRMED, WeekViewEvent.Status.TENTATIVE -> {
            this.color =
                if (pastEvent) setAlphaComponent(event.borderColors[0], 0x66)
                else event.borderColors[0]
        }
        WeekViewEvent.Status.CANCELED -> {
            this.color =
                if (pastEvent) setAlphaComponent(event.borderColors[1], 0x66)
                else event.borderColors[1]
        }
    }
    return this
}

fun Paint.withBgColor(event: WeekViewEvent): Paint {
    when (event.status) {
        WeekViewEvent.Status.CONFIRMED -> {
            this.shader = null
            this.color = event.bgColors[0]
        }
        WeekViewEvent.Status.CANCELED -> {
            this.shader = null
            this.color = event.bgColors[1]
        }
        WeekViewEvent.Status.TENTATIVE -> {
            this.shader = LinearGradient(
                20f, 20f, 0f, 0f, intArrayOf(
                    event.baseBgColor, event.baseBgColor, event.bgColors[0], event.bgColors[0]
                ), floatArrayOf(0f, 0.5f, 0.5f, 1f), Shader.TileMode.REPEAT
            )
        }
    }
    return this
}

fun Paint.withTextColor(event: WeekViewEvent): Paint {
    val pastEvent = event.endTime < Calendar.getInstance()
    when (event.status) {
        WeekViewEvent.Status.CONFIRMED, WeekViewEvent.Status.TENTATIVE -> {
            this.color =
                if (pastEvent) setAlphaComponent(event.textColors[0], 0x66)
                else event.textColors[0]
            this.isStrikeThruText = false
        }
        WeekViewEvent.Status.CANCELED -> {
            this.color =
                if (pastEvent) setAlphaComponent(event.textColors[1], 0x66)
                else event.textColors[1]
            this.isStrikeThruText = true
        }
    }
    return this
}

class WeekViewEvent {

    enum class Status {
        CONFIRMED,
        CANCELED,
        TENTATIVE
    }

    var id: Long = 0
    lateinit var startTime: Calendar
    lateinit var endTime: Calendar
    var name: String = ""
    var location: String = ""
    var isAllDay = false

    var status: Status = Status.TENTATIVE
    var baseBgColor = 0
    var borderColors = intArrayOf(0, 0)
    var bgColors = intArrayOf(0, 0)
    var textColors = intArrayOf(0, 0)

    constructor()

    /**
     * Initializes the event for week view.
     *
     * @param id          The id of the event.
     * @param name        Name of the event.
     * @param startYear   Year when the event starts.
     * @param startMonth  Month when the event starts.
     * @param startDay    Day when the event starts.
     * @param startHour   Hour (in 24-hour format) when the event starts.
     * @param startMinute Minute when the event starts.
     * @param endYear     Year when the event ends.
     * @param endMonth    Month when the event ends.
     * @param endDay      Day when the event ends.
     * @param endHour     Hour (in 24-hour format) when the event ends.
     * @param endMinute   Minute when the event ends.
     */
    constructor(
        id: Long,
        name: String,
        startYear: Int,
        startMonth: Int,
        startDay: Int,
        startHour: Int,
        startMinute: Int,
        endYear: Int,
        endMonth: Int,
        endDay: Int,
        endHour: Int,
        endMinute: Int
    ) {
        this.id = id
        startTime = Calendar.getInstance().apply {
            set(Calendar.YEAR, startYear)
            set(Calendar.MONTH, startMonth - 1)
            set(Calendar.DAY_OF_MONTH, startDay)
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
        }
        endTime = Calendar.getInstance().apply {
            set(Calendar.YEAR, endYear)
            set(Calendar.MONTH, endMonth - 1)
            set(Calendar.DAY_OF_MONTH, endDay)
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
        }
        this.name = name
    }

    /**
     * Initializes the event for week view.
     *
     * @param id        The id of the event.
     * @param name      Name of the event.
     * @param location  The location of the event.
     * @param startTime The time when the event starts.
     * @param endTime   The time when the event ends.
     * @param allDay    Is the event an all day event.
     */
    @JvmOverloads
    constructor(
        id: Long,
        name: String,
        location: String?,
        startTime: Calendar,
        endTime: Calendar,
        allDay: Boolean = false
    ) {
        this.id = id
        this.name = name
        this.location = location ?: ""
        this.startTime = startTime
        this.endTime = endTime
        isAllDay = allDay
    }

    /**
     * Initializes the event for week view.
     *
     * @param id        The id of the event.
     * @param name      Name of the event.
     * @param startTime The time when the event starts.
     * @param endTime   The time when the event ends.
     */
    constructor(id: Long, name: String, startTime: Calendar, endTime: Calendar) : this(
        id,
        name,
        null,
        startTime,
        endTime
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as WeekViewEvent
        return id == that.id
    }

    override fun hashCode(): Int {
        return (id xor (id ushr 32)).toInt()
    }

    // This function splits the WeekViewEvent in WeekViewEvents by day
    fun splitWeekViewEvents(): List<WeekViewEvent> {
        val events = mutableListOf<WeekViewEvent>()
        // The first millisecond of the next day is still the same day. (no need to split events for this).
        var endTime = (endTime.clone() as Calendar).apply {
            add(Calendar.MILLISECOND, -1)
        }
        if (!WeekViewUtil.isSameDay(startTime, endTime)) {
            endTime = (startTime.clone() as Calendar).apply {
                this[Calendar.HOUR_OF_DAY] = 23
                this[Calendar.MINUTE] = 59
            }
            val event1 = WeekViewEvent(id, name, location, startTime, endTime, isAllDay).apply {
                baseBgColor = this@WeekViewEvent.baseBgColor
                borderColors = this@WeekViewEvent.borderColors
                bgColors = this@WeekViewEvent.bgColors
                textColors = this@WeekViewEvent.textColors
            }
            events.add(event1)

            // Add other days.
            val otherDay = (startTime.clone() as Calendar).apply {
                add(Calendar.DATE, 1)
            }
            while (!WeekViewUtil.isSameDay(otherDay, this.endTime)) {
                val overDay = (otherDay.clone() as Calendar).apply {
                    this[Calendar.HOUR_OF_DAY] = 0
                    this[Calendar.MINUTE] = 0
                }
                val endOfOverDay = (overDay.clone() as Calendar).apply {
                    this[Calendar.HOUR_OF_DAY] = 23
                    this[Calendar.MINUTE] = 59
                }
                val eventMore = WeekViewEvent(
                    id, name, null, overDay, endOfOverDay, isAllDay
                ).apply {
                    baseBgColor = this@WeekViewEvent.baseBgColor
                    borderColors = this@WeekViewEvent.borderColors
                    bgColors = this@WeekViewEvent.bgColors
                    textColors = this@WeekViewEvent.textColors
                }
                events.add(eventMore)

                // Add next day.
                otherDay.add(Calendar.DATE, 1)
            }

            // Add last day.
            val startTime = (this.endTime.clone() as Calendar).apply {
                this[Calendar.HOUR_OF_DAY] = 0
                this[Calendar.MINUTE] = 0
            }
            val event2 = WeekViewEvent(
                id, name, location, startTime, this.endTime, isAllDay
            ).apply {
                baseBgColor = this@WeekViewEvent.baseBgColor
                borderColors = this@WeekViewEvent.borderColors
                bgColors = this@WeekViewEvent.bgColors
                textColors = this@WeekViewEvent.textColors
            }
            events.add(event2)
        } else {
            events.add(this)
        }
        return events
    }
}
