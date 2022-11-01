package com.alamkanak.weekview.oneday

import android.content.Context
import android.graphics.RectF
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
import com.alamkanak.weekview.EmptyViewClickListener
import com.alamkanak.weekview.EventClickListener
import com.alamkanak.weekview.R
import com.alamkanak.weekview.WeekArrowView
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewContainer
import com.alamkanak.weekview.WeekViewEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class OneDayWeekView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), LifecycleObserver, LifecycleOwner {

    companion object {
        var curScrollY = 0
    }

    var viewEventClickListener: ((
        weekViewEvent: WeekViewEvent?,
        createSTime: Calendar?, createETime: Calendar?
    ) -> Unit)? = null
    val weekView: WeekView
    val weekViewContainer: WeekViewContainer

    private val allDayEventsLayout: View
    private val allDayEventsListView: AllDayEventsListView
    private val weekArrowView: WeekArrowView
    private var currentDate: Calendar? = null
    private var eventsLoader: ((
        newYear: Int,
        newMonth: Int,
        newDayOfMonth: Int
    ) -> MutableList<WeekViewEvent>)? = null

    init {
        inflate(context, R.layout.wv_layout_one_day_week_view, this)

        allDayEventsLayout = findViewById(R.id.all_day_events_layout)
        allDayEventsListView = findViewById<AllDayEventsListView>(R.id.all_day_events_list_view).apply {
            itemClickListener = {
                viewEventClickListener?.invoke(it, null, null)
            }
        }

        weekView = findViewById<WeekView>(R.id.week_view).apply {
            eventClickListener = object : EventClickListener {
                override fun onEventClick(event: WeekViewEvent, eventRect: RectF) {
                    viewEventClickListener?.invoke(event, null, null)
                }
            }
            emptyViewClickListener = object : EmptyViewClickListener {
                override fun onEmptyViewClicked(starTime: Calendar, endTime: Calendar) {
                    viewEventClickListener?.invoke(null, starTime, endTime)
                }
            }
        }
        weekViewContainer = findViewById<WeekViewContainer>(R.id.week_view_container).apply {
            setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                updateArrowVisible()
                curScrollY = scrollY
            })
        }
        weekArrowView = findViewById<WeekArrowView>(R.id.week_arrow_view).apply {
            setUpArrowClickListener { weekView.goToTopEventRect() }
            setDownArrowClickListener { weekView.goToBottomEventRect() }
        }
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
        this.currentDate = currentDate
        this.eventsLoader = eventsLoader
        updateData()
    }

    private fun updateData() {
        val curDate = currentDate ?: return
        weekView.currentDate = curDate
        lifecycleScope.launchWhenStarted {
            val datas = withContext(Dispatchers.IO) {
                val data = eventsLoader?.invoke(
                    curDate[Calendar.YEAR],
                    curDate[Calendar.MONTH] + 1,
                    curDate[Calendar.DAY_OF_MONTH]
                ) ?: mutableListOf()
                Pair(
                    data.filter { !it.isAllDay }.toMutableList(),
                    data.filter { it.isAllDay }.toMutableList()
                )
            }
            allDayEventsListView.setData(datas.second)
            allDayEventsLayout.isVisible = datas.second.isNotEmpty()
            weekView.notifyDatasetChanged(datas.first)
            delay(TimeUnit.SECONDS.toMillis(1))
            updateArrowVisible()
        }
    }

    private fun updateArrowVisible() {
        weekArrowView.setArrowsVisible(
            !weekView.isTopEventRectVisible(),
            !weekView.isBottomEventRectVisible()
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycle.addObserver(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycle.removeObserver(this)
        eventsLoader = null
        viewEventClickListener = null
    }

    override fun getLifecycle(): Lifecycle = (context as LifecycleOwner).lifecycle
}