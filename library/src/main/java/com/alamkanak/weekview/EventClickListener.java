package com.alamkanak.weekview;

import android.graphics.RectF;

public interface EventClickListener {
    /**
     * Triggered when clicked on one existing event
     *
     * @param event:     event clicked.
     * @param eventRect: view containing the clicked event.
     */
    void onEventClick(WeekViewEvent event, RectF eventRect);
}
