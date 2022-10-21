package com.alamkanak.weekview

import android.graphics.RectF

interface EventLongPressListener {
    /**
     * Similar to [com.alamkanak.weekview.EventClickListener] but with a long press.
     *
     * @param event:     event clicked.
     * @param eventRect: view containing the clicked event.
     */
    fun onEventLongPress(event: WeekViewEvent, eventRect: RectF)
}
