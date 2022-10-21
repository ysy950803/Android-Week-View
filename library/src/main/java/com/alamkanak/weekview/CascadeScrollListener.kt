package com.alamkanak.weekview

import java.util.Calendar

interface CascadeScrollListener {
    fun onScrolling(currentOriginX: Float)
    fun onScrollEnd(newFirstVisibleDay: Calendar)
}
