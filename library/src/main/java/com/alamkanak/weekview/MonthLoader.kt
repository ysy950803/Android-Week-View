package com.alamkanak.weekview

import java.util.Calendar

class MonthLoader(var onMonthChangeListener: MonthChangeListener) : WeekViewLoader {

    override fun toWeekViewPeriodIndex(instance: Calendar): Int {
        return instance[Calendar.YEAR] * 12 + instance[Calendar.MONTH] + 1
    }

    override fun onLoad(periodIndex: Int) {
        onMonthChangeListener.onMonthChange(
            periodIndex,
            (periodIndex - 1) / 12, (periodIndex - 1) % 12,
            periodIndex / 12, periodIndex % 12,
            (periodIndex + 1) / 12, (periodIndex + 1) % 12,
        )
    }

    interface MonthChangeListener {
        /**
         * Very important interface, it's the base to load events in the calendar.
         * This method is called three times: once to load the previous month, once to load the next month and once to load the current month.<br></br>
         * **That's why you can have three times the same event at the same place if you mess up with the configuration**
         *
         * @param newYear  : year of the events required by the view.
         * @param newMonth : month of the events required by the view <br></br>**1 based (not like JAVA API) --> January = 1 and December = 12**.
         */
        fun onMonthChange(
            periodToFetch: Int,
            preNewYear: Int, preNewMonth: Int,
            newYear: Int, newMonth: Int,
            nextNewYear: Int, nextNewMonth: Int
        )
    }
}
