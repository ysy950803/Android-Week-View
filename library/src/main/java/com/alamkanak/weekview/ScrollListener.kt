package com.alamkanak.weekview

import java.util.Calendar

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
