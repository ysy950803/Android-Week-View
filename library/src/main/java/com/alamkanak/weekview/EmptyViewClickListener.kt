package com.alamkanak.weekview

import java.util.Calendar

interface EmptyViewClickListener {
    /**
     * Triggered when the users clicks on a empty space of the calendar.
     *
     * @param time: [Calendar] object set with the date and time of the clicked position on the view.
     */
    fun onEmptyViewClicked(time: Calendar)
}
