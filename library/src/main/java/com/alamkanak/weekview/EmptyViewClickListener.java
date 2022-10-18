package com.alamkanak.weekview;

import java.util.Calendar;

public interface EmptyViewClickListener {
    /**
     * Triggered when the users clicks on a empty space of the calendar.
     *
     * @param time: {@link Calendar} object set with the date and time of the clicked position on the view.
     */
    void onEmptyViewClicked(Calendar time);
}
