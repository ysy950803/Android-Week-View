package com.alamkanak.weekview

import java.util.Calendar

class WeekViewEvent {

    var id: Long = 0
    lateinit var startTime: Calendar
    lateinit var endTime: Calendar
    var name: String = ""
    var location: String = ""
    var color = 0
    var isAllDay = false

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
                this.color = this@WeekViewEvent.color
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
                    this.color = this@WeekViewEvent.color
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
                this.color = this@WeekViewEvent.color
            }
            events.add(event2)
        } else {
            events.add(this)
        }
        return events
    }
}
