package com.alamkanak.weekview

import android.graphics.RectF

interface EventClickListener {
    /**
     * Triggered when clicked on one existing event
     *
     * @param event:     event clicked.
     * @param eventRect: view containing the clicked event.
     */
    fun onEventClick(event: WeekViewEvent, eventRect: RectF)
}
