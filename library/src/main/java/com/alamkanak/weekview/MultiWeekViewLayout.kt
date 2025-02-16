package com.alamkanak.weekview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

@SuppressLint("ClickableViewAccessibility")
class MultiWeekViewLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), LifecycleObserver, LifecycleOwner {

    var eventsLoader: ((newYear: Int, newMonth: Int) -> MutableList<WeekViewEvent>)? = null

    private lateinit var weekView: WeekView
    private lateinit var weekHeaderView: WeekHeaderView
    private lateinit var weekArrowView: WeekArrowView
    private var touchHeader = false

    private var mPreviousPeriodEvents: MutableList<WeekViewEvent>? = null
    private var mCurrentPeriodEvents: MutableList<WeekViewEvent>? = null
    private var mNextPeriodEvents: MutableList<WeekViewEvent>? = null
    private var mFetchedPeriod = -1 // the middle period the calendar has fetched.

    init {
        inflate(context, R.layout.wv_layout_multi_week_view, this)
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

    override fun onFinishInflate() {
        super.onFinishInflate()
        weekHeaderView = findViewById(R.id.week_header_view)
        weekView = findViewById(R.id.week_view)
        weekArrowView = findViewById<WeekArrowView>(R.id.week_arrow_view).apply {
            setUpArrowClickListener { weekView.goToTopEventRect() }
            setDownArrowClickListener { weekView.goToBottomEventRect() }
        }

        weekHeaderView.apply {
            setOnTouchListener { _, _ ->
                touchHeader = true
                false
            }
            cascadeScrollListener = object : CascadeScrollListener {
                override fun onScrolling(currentOriginX: Float) {
                    if (touchHeader) weekView.setCurrentOriginX(currentOriginX)
                }

                override fun onScrollEnd(newFirstVisibleDay: Calendar) {
                    if (touchHeader) weekView.goFirstVisibleDay(newFirstVisibleDay)
                }
            }
            dayHasEvents = weekView.dayHasEvents
        }
        weekView.apply {
            monthChangeListener = object : MonthLoader.MonthChangeListener {
                override fun onMonthChange(
                    periodToFetch: Int,
                    preNewYear: Int,
                    preNewMonth: Int,
                    newYear: Int,
                    newMonth: Int,
                    nextNewYear: Int,
                    nextNewMonth: Int
                ) {
                    (context as LifecycleOwner).lifecycleScope.launch {
                        if (mFetchedPeriod >= 0 && mFetchedPeriod == periodToFetch) return@launch

                        val datas = withContext(Dispatchers.IO) {
                            var previousPeriodEvents: MutableList<WeekViewEvent>? = null
                            var currentPeriodEvents: MutableList<WeekViewEvent>? = null
                            var nextPeriodEvents: MutableList<WeekViewEvent>? = null
                            if (mPreviousPeriodEvents != null && mCurrentPeriodEvents != null && mNextPeriodEvents != null) {
                                when (periodToFetch) {
                                    mFetchedPeriod - 1 -> {
                                        currentPeriodEvents = mPreviousPeriodEvents
                                        nextPeriodEvents = mCurrentPeriodEvents
                                    }
                                    mFetchedPeriod -> {
                                        previousPeriodEvents = mPreviousPeriodEvents
                                        currentPeriodEvents = mCurrentPeriodEvents
                                        nextPeriodEvents = mNextPeriodEvents
                                    }
                                    mFetchedPeriod + 1 -> {
                                        previousPeriodEvents = mCurrentPeriodEvents
                                        currentPeriodEvents = mNextPeriodEvents
                                    }
                                }
                            }
                            if (currentPeriodEvents == null) currentPeriodEvents =
                                eventsLoader?.invoke(newYear, newMonth)
                            if (previousPeriodEvents == null) previousPeriodEvents =
                                eventsLoader?.invoke(preNewYear, preNewMonth)
                            if (nextPeriodEvents == null) nextPeriodEvents =
                                eventsLoader?.invoke(nextNewYear, nextNewMonth)
                            mPreviousPeriodEvents = previousPeriodEvents
                            mCurrentPeriodEvents = currentPeriodEvents
                            mNextPeriodEvents = nextPeriodEvents
                            mFetchedPeriod = periodToFetch

                            val data = mutableListOf<WeekViewEvent>().apply {
                                previousPeriodEvents?.let { addAll(it) }
                                currentPeriodEvents?.let { addAll(it) }
                                nextPeriodEvents?.let { addAll(it) }
                            }
                            Pair(
                                data.filter { !it.isAllDay }.toMutableList(),
                                data.filter { it.isAllDay }.toMutableList()
                            )
                        }

                        weekView.notifyDatasetChanged(datas.first)
                        weekHeaderView.notifyDatasetChanged(datas.second)
                        delay(TimeUnit.SECONDS.toMillis(1))
                        updateArrowVisible()
                    }
                }
            }

            setOnTouchListener { _, _ ->
                touchHeader = false
                false
            }
            cascadeScrollListener = object : CascadeScrollListener {
                override fun onScrolling(currentOriginX: Float) {
                    if (!touchHeader) weekHeaderView.setCurrentOriginX(currentOriginX)
                }

                override fun onScrollEnd(newFirstVisibleDay: Calendar) {
                    if (!touchHeader) weekHeaderView.goFirstVisibleDay(newFirstVisibleDay)
                }
            }
            scrollListener = object : ScrollListener {
                override fun onFirstVisibleDayChanged(
                    newFirstVisibleDay: Calendar,
                    oldFirstVisibleDay: Calendar?
                ) {
                    updateArrowVisible()
                }
            }
        }

        findViewById<WeekViewContainer>(
            R.id.week_view_container
        )?.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            updateArrowVisible()
        })
    }

    private fun updateArrowVisible() {
        weekArrowView.setArrowsVisible(
            !weekView.isTopEventRectVisible(),
            !weekView.isBottomEventRectVisible()
        )
    }

    override fun getLifecycle(): Lifecycle = (context as LifecycleOwner).lifecycle
}
