package com.alamkanak.weekview.oneday

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import com.alamkanak.weekview.R
import com.alamkanak.weekview.WeekArrowView
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewContainer
import com.alamkanak.weekview.WeekViewEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class OneDayWeekView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), LifecycleObserver, LifecycleOwner {

    private val weekView: WeekView
    private val allDayEventsLayout: View
    private val allDayEventsListView: AllDayEventsListView
    private val weekArrowView: WeekArrowView

    init {
        inflate(context, R.layout.wv_layout_one_day_week_view, this)

        weekView = findViewById(R.id.week_view)
        allDayEventsLayout = findViewById(R.id.all_day_events_layout)
        allDayEventsListView = findViewById(R.id.all_day_events_list_view)
        weekArrowView = findViewById<WeekArrowView>(R.id.week_arrow_view).apply {
            setUpArrowClickListener { weekView.goToTopEventRect() }
            setDownArrowClickListener { weekView.goToBottomEventRect() }
        }

        findViewById<WeekViewContainer>(
            R.id.week_view_container
        )?.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            updateArrowVisible()
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycle.addObserver(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycle.removeObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onStart() {
        weekArrowView.startArrowAnim()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        weekArrowView.clearArrowAnim()
    }

    fun updateCurrentDate(
        currentDate: Calendar,
        eventsLoader: ((
            newYear: Int,
            newMonth: Int,
            newDayOfMonth: Int
        ) -> MutableList<WeekViewEvent>)?
    ) {
        weekView.updateCurrentDate(currentDate)
        (context as LifecycleOwner).lifecycleScope.launch {
            val datas = withContext(Dispatchers.IO) {
                val data = eventsLoader?.invoke(
                    currentDate[Calendar.YEAR],
                    currentDate[Calendar.MONTH] + 1,
                    currentDate[Calendar.DAY_OF_MONTH]
                ) ?: mutableListOf()
                Pair(
                    data.filter { !it.isAllDay }.toMutableList(),
                    data.filter { it.isAllDay }.toMutableList()
                )
            }
            allDayEventsListView.setData(datas.second)
            allDayEventsLayout.isVisible = datas.second.isNotEmpty()
            weekView.notifyDatasetChanged(datas.first)
        }
    }

    private fun updateArrowVisible() {
        weekArrowView.setArrowsVisible(
            !weekView.isTopEventRectVisible(),
            !weekView.isBottomEventRectVisible()
        )
    }

    override fun getLifecycle(): Lifecycle = (context as LifecycleOwner).lifecycle
}
