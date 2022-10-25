package com.alamkanak.weekview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@SuppressLint("ClickableViewAccessibility")
class WeekViewLayout : LinearLayout, LifecycleObserver, LifecycleOwner {

    var eventsLoader: ((newYear: Int, newMonth: Int) -> MutableList<WeekViewEvent>)? = null

    private lateinit var weekView: WeekView
    private lateinit var weekHeaderView: WeekHeaderView
    private var upArrow: ImageView? = null
    private var downArrow: ImageView? = null
    private var arrowAnimSet: AnimatorSet? = null
    private var touchHeader = false

    private var mPreviousPeriodEvents: MutableList<WeekViewEvent>? = null
    private var mCurrentPeriodEvents: MutableList<WeekViewEvent>? = null
    private var mNextPeriodEvents: MutableList<WeekViewEvent>? = null
    private var mFetchedPeriod = -1 // the middle period the calendar has fetched.

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        LayoutInflater.from(context).inflate(R.layout.layout_weekview, this, true)
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
        startArrowAnim()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        clearArrowAnim()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        weekHeaderView = findViewById(R.id.week_header_view)
        weekView = findViewById(R.id.week_view)
        upArrow = findViewById(R.id.iv_remind_up_arrow)
        downArrow = findViewById(R.id.iv_remind_down_arrow)
        upArrow?.setOnClickListener { weekView.goToTopEventRect() }
        downArrow?.setOnClickListener { weekView.goToBottomEventRect() }

        weekHeaderView.apply {
            dateTimeInterpreter = createDateTimeInterpreter()

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
            dateTimeInterpreter = createDateTimeInterpreter()

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
            R.id.weekview_container
        )?.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            updateArrowVisible()
        })
    }

    private fun createDateTimeInterpreter() = object : DateTimeInterpreter {
        override fun interpretDate(date: Calendar): List<String> = runCatching {
            val sdf = SimpleDateFormat("EEE", Locale.getDefault())
            listOf(
                if (WeekViewUtil.isSameDay(date, Calendar.getInstance())) "今天" // TODO 翻译
                else sdf.format(date.time).toUpperCase(Locale.getDefault()),
                "${date.get(Calendar.DAY_OF_MONTH)}"
            )
        }.getOrDefault(listOf("", ""))

        override fun interpretTime(hour: Int, minute: Int): String = runCatching {
            val calendar = Calendar.getInstance().apply {
                this[Calendar.HOUR_OF_DAY] = hour
                this[Calendar.MINUTE] = minute
            }
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(calendar.time)
        }.getOrDefault("")
    }

    private fun updateArrowVisible() {
        upArrow?.isVisible = !weekView.isTopEventRectVisible()
        downArrow?.isVisible = !weekView.isBottomEventRectVisible()
    }

    private fun startArrowAnim() {
        val height = ConvertUtils.dp2px(6f).toFloat()
        val u1 = ObjectAnimator.ofFloat(upArrow, View.TRANSLATION_Y, 0f, height)
        val u2 = ObjectAnimator.ofFloat(upArrow, View.ALPHA, 0.4f, 1f)
        val u3 = ObjectAnimator.ofFloat(upArrow, View.TRANSLATION_Y, height, 0f)
        val u4 = ObjectAnimator.ofFloat(upArrow, View.ALPHA, 1f, 0.4f)
        val b1 = ObjectAnimator.ofFloat(downArrow, View.TRANSLATION_Y, 0f, height)
        val b2 = ObjectAnimator.ofFloat(downArrow, View.ALPHA, 0.4f, 1f)
        val b3 = ObjectAnimator.ofFloat(downArrow, View.TRANSLATION_Y, height, 0f)
        val b4 = ObjectAnimator.ofFloat(downArrow, View.ALPHA, 1f, 0.4f)
        val up1 = AnimatorSet().apply { play(u3).with(u2) }
        val up2 = AnimatorSet().apply { play(u1).with(u4) }
        val upSet = AnimatorSet().apply { playSequentially(up1, up2) }
        val bottom1 = AnimatorSet().apply { play(b1).with(b2) }
        val bottom2 = AnimatorSet().apply { play(b3).with(b4) }
        val bottomSet = AnimatorSet().apply { playSequentially(bottom1, bottom2) }
        arrowAnimSet = AnimatorSet().apply {
            play(upSet).with(bottomSet)
            addListener(object : AnimatorListenerAdapter() {
                private var canceled = false
                override fun onAnimationEnd(animation: Animator) {
                    if (!canceled) {
                        animation.start()
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    canceled = true
                }
            })
            duration = 600L
            start()
        }
    }

    private fun clearArrowAnim() {
        upArrow?.clearAnimation()
        downArrow?.clearAnimation()
        arrowAnimSet?.run {
            removeAllListeners()
            cancel()
        }
        arrowAnimSet = null
    }

    override fun getLifecycle(): Lifecycle = (context as LifecycleOwner).lifecycle
}
