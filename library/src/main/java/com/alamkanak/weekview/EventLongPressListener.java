package com.alamkanak.weekview;

import android.graphics.RectF;

public interface EventLongPressListener {
    /**
     * Similar to {@link com.alamkanak.weekview.WeekView.EventClickListener} but with a long press.
     *
     * @param event:     event clicked.
     * @param eventRect: view containing the clicked event.
     */
    void onEventLongPress(WeekViewEvent event, RectF eventRect);
}
