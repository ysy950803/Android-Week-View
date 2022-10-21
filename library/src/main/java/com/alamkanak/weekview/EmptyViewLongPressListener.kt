package com.alamkanak.weekview

import java.util.Calendar

interface EmptyViewLongPressListener {
    /**
     * Similar to [com.alamkanak.weekview.EmptyViewClickListener] but with long press.
     *
     * @param time: [Calendar] object set with the date and time of the long pressed position on the view.
     */
    fun onEmptyViewLongPress(time: Calendar)
}