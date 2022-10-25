package com.alamkanak.weekview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import com.alamkanak.weekview.WeekViewUtil.allDaysBetween
import com.alamkanak.weekview.WeekViewUtil.isContainsAllDay
import com.alamkanak.weekview.WeekViewUtil.isFloatEqual
import com.alamkanak.weekview.WeekViewUtil.isSameDay
import com.alamkanak.weekview.WeekViewUtil.today
import com.blankj.utilcode.util.ConvertUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

@Suppress("unused")
class WeekHeaderView : View {

    private lateinit var mTimeTextPaint: Paint
    private var mTimeTextWidth = 0f
    private var mTimeTextHeight = 0f
    private lateinit var mHeaderTextPaint: Paint
    private lateinit var mPastHeaderTextPaint: Paint
    private lateinit var mPastHasEventPaint: Paint
    private var mHeaderHeight = 0f
    private lateinit var mGestureDetector: GestureDetectorCompat
    private lateinit var mScroller: OverScroller
    private var mCurrentOrigin = PointF(0f, 0f)
    private var mCurrentScrollDirection = Direction.NONE
    private lateinit var mHeaderBackgroundPaint: Paint
    private var mWidthPerDay = 0f
    private lateinit var mDayBackgroundPaint: Paint
    private var mHeaderMarginBottom = 0f
    private lateinit var mTodayBackgroundPaint: Paint
    private lateinit var mTodayHeaderTextPaint: Paint
    private lateinit var mEventBackgroundPaint: Paint
    private lateinit var mEventBorderPaint: Paint
    private lateinit var mHeaderColumnBackgroundPaint: Paint
    private var mHeaderColumnWidth = 0f
    private var mEventRects = mutableListOf<EventRect>()
    private lateinit var mEventTextPaint: TextPaint
    private var mFetchedPeriod = -1 // the middle period the calendar has fetched.

    private var mCurrentFlingDirection = Direction.NONE
    private var mFirstVisibleDay: Calendar? = null
    private var mLastVisibleDay: Calendar? = null
    private var mMinimumFlingVelocity = 0
    private var mScaledTouchSlop = 0

    // Attributes and their default values.
    private var mColumnGap = 10
    private var mFirstDayOfWeek = Calendar.MONDAY
    private var mTextSize = 12
    private var mHeaderColumnPadding = 10
    private var mHeaderColumnTextColor = Color.BLACK
    private var mNumberOfVisibleDays = 3
    private var mHeaderRowBackgroundColor = Color.WHITE
    private var mDayBackgroundColor = Color.rgb(245, 245, 245)
    private var mTodayBackgroundColor = Color.rgb(239, 247, 254)
    private var mTodayHeaderTextColor = Color.rgb(39, 137, 228)
    private var mEventTextSize = 12
    private var mEventTextColor = Color.BLACK
    private var mEventVPadding = 0
    private var mEventHPadding = 0
    private var mHeaderColumnBackgroundColor = Color.WHITE
    private var mIsFirstDraw = true
    private var mAreDimensionsInvalid = true
    private var mScrollToDay: Calendar? = null
    private var mShowDistinctWeekendColor = false
    private var mShowDistinctPastFutureColor = false

    // 全天垂直滑动边界距离
    private var mAllDayEventMinY = 0

    // 绘制过程中标记某天是否有日程
    var dayHasEvents = hashMapOf<Int, Boolean>()

    // 除掉全天日程以外的header高度
    private var mNormalHeaderHeight = 0f

    private val mGestureListener: SimpleOnGestureListener = object : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            goToNearestOrigin()
            return true
        }

        override fun onScroll(
            e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float
        ): Boolean {
            val horizontal = abs(distanceX) > abs(distanceY)
            when (mCurrentScrollDirection) {
                Direction.NONE -> {
                    // Allow scrolling only in one direction.
                    mCurrentScrollDirection = if (horizontal) {
                        if (distanceX > 0) {
                            Direction.LEFT
                        } else {
                            Direction.RIGHT
                        }
                    } else {
                        Direction.VERTICAL
                    }
                }
                Direction.LEFT -> {
                    // Change direction if there was enough change.
                    if (horizontal && distanceX < -mScaledTouchSlop) {
                        mCurrentScrollDirection = Direction.RIGHT
                    }
                }
                Direction.RIGHT -> {
                    // Change direction if there was enough change.
                    if (horizontal && distanceX > mScaledTouchSlop) {
                        mCurrentScrollDirection = Direction.LEFT
                    }
                }
                else -> {}
            }
            when (mCurrentScrollDirection) {
                Direction.LEFT, Direction.RIGHT -> {
                    mCurrentOrigin.x -= distanceX * xScrollingSpeed
                    ViewCompat.postInvalidateOnAnimation(this@WeekHeaderView)
                }
                Direction.VERTICAL -> {
                    mCurrentOrigin.y -= distanceY
                    ViewCompat.postInvalidateOnAnimation(this@WeekHeaderView)
                }
                else -> {}
            }
            return true
        }

        override fun onFling(
            e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float
        ): Boolean {
            if (mCurrentFlingDirection == Direction.LEFT && !horizontalFlingEnabled
                || mCurrentFlingDirection == Direction.RIGHT && !horizontalFlingEnabled
                || mCurrentFlingDirection == Direction.VERTICAL && !verticalFlingEnabled
            ) {
                return true
            }
            mScroller.forceFinished(true)
            mCurrentFlingDirection = mCurrentScrollDirection
            when (mCurrentFlingDirection) {
                Direction.LEFT, Direction.RIGHT -> mScroller.fling(
                    mCurrentOrigin.x.toInt(),
                    mCurrentOrigin.y.toInt(),
                    (velocityX * xScrollingSpeed).toInt(),
                    0,
                    Int.MIN_VALUE,
                    Int.MAX_VALUE,
                    0,
                    0
                )
                Direction.VERTICAL -> mScroller.fling(
                    mCurrentOrigin.x.toInt(),
                    mCurrentOrigin.y.toInt(),
                    0,
                    velocityY.toInt(),
                    Int.MIN_VALUE,
                    Int.MAX_VALUE,
                    mAllDayEventMinY,
                    0
                )
                else -> {}
            }
            ViewCompat.postInvalidateOnAnimation(this@WeekHeaderView)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // If the tap was on an event then trigger the callback.
            eventClickListener?.let { listener ->
                for (event in mEventRects.reversed()) {
                    val rectF = event.rectF ?: continue
                    if (e.x > rectF.left
                        && e.x < rectF.right
                        && e.y > rectF.top
                        && e.y < rectF.bottom
                    ) {
                        listener.onEventClick(event.originalEvent, rectF)
                        playSoundEffect(SoundEffectConstants.CLICK)
                        return super.onSingleTapConfirmed(e)
                    }
                }
            }
            return super.onSingleTapConfirmed(e)
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            eventLongPressListener?.let { listener ->
                for (event in mEventRects.reversed()) {
                    val rectF = event.rectF ?: continue
                    if (e.x > rectF.left
                        && e.x < rectF.right
                        && e.y > rectF.top
                        && e.y < rectF.bottom
                    ) {
                        listener.onEventLongPress(event.originalEvent, rectF)
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        return
                    }
                }
            }
        }
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        // Get the attribute values (if any).
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.WeekView, 0, 0)
        try {
            mFirstDayOfWeek = a.getInteger(R.styleable.WeekView_firstDayOfWeek, mFirstDayOfWeek)
            mTextSize = a.getDimensionPixelSize(
                R.styleable.WeekView_textSize, ConvertUtils.sp2px(mTextSize.toFloat())
            )
            mHeaderColumnPadding = a.getDimensionPixelSize(
                R.styleable.WeekView_headerColumnPadding, mHeaderColumnPadding
            )
            mColumnGap = a.getDimensionPixelSize(R.styleable.WeekView_columnGap, mColumnGap)
            mHeaderColumnTextColor =
                a.getColor(R.styleable.WeekView_headerColumnTextColor, mHeaderColumnTextColor)
            mNumberOfVisibleDays =
                a.getInteger(R.styleable.WeekView_noOfVisibleDays, mNumberOfVisibleDays)
            showFirstDayOfWeekFirst =
                a.getBoolean(R.styleable.WeekView_showFirstDayOfWeekFirst, showFirstDayOfWeekFirst)
            mHeaderRowBackgroundColor =
                a.getColor(R.styleable.WeekView_headerRowBackgroundColor, mHeaderRowBackgroundColor)
            mDayBackgroundColor =
                a.getColor(R.styleable.WeekView_dayBackgroundColor, mDayBackgroundColor)
            this.mTodayBackgroundColor = a.getColor(
                R.styleable.WeekView_todayBackgroundColor, this.mTodayBackgroundColor
            )
            mTodayHeaderTextColor =
                a.getColor(R.styleable.WeekView_todayHeaderTextColor, mTodayHeaderTextColor)
            mEventTextSize = a.getDimensionPixelSize(
                R.styleable.WeekView_eventTextSize,
                ConvertUtils.sp2px(this.mEventTextSize.toFloat())
            )
            mEventTextColor = a.getColor(R.styleable.WeekView_eventTextColor, mEventTextColor)
            mEventVPadding =
                a.getDimensionPixelSize(R.styleable.WeekView_eventPadding, mEventVPadding)
            // TODO 日程文本内容水平padding
            mEventHPadding = ConvertUtils.dp2px(6f)
            mHeaderColumnBackgroundColor = a.getColor(
                R.styleable.WeekView_headerColumnBackground,
                mHeaderColumnBackgroundColor
            )
            xScrollingSpeed = a.getFloat(R.styleable.WeekView_xScrollingSpeed, xScrollingSpeed)
            mShowDistinctPastFutureColor = a.getBoolean(
                R.styleable.WeekView_showDistinctPastFutureColor, mShowDistinctPastFutureColor
            )
            mShowDistinctWeekendColor = a.getBoolean(
                R.styleable.WeekView_showDistinctWeekendColor, mShowDistinctWeekendColor
            )
            horizontalFlingEnabled =
                a.getBoolean(R.styleable.WeekView_horizontalFlingEnabled, horizontalFlingEnabled)
            verticalFlingEnabled =
                a.getBoolean(R.styleable.WeekView_verticalFlingEnabled, verticalFlingEnabled)
            allDayEventHeight =
                a.getDimensionPixelSize(R.styleable.WeekView_allDayEventHeight, allDayEventHeight)
            scrollDuration = a.getInt(R.styleable.WeekView_scrollDuration, scrollDuration)
        } finally {
            a.recycle()
        }

        init()
    }

    private fun init() {
        // Scrolling initialization.
        mGestureDetector = GestureDetectorCompat(context, mGestureListener)
        mScroller = OverScroller(context, FastOutLinearInInterpolator())

        mMinimumFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
        mScaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        // Measure settings for time column.
        mTimeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.RIGHT
            // 时间轴文本大小
            textSize = ConvertUtils.sp2px(10f).toFloat()
            color = Color.parseColor("#FF888888")
        }
        val rect = Rect()
        mTimeTextPaint.getTextBounds("00 PM", 0, "00 PM".length, rect)
        mTimeTextHeight = rect.height().toFloat()
        mHeaderMarginBottom = ConvertUtils.dp2px(2f).toFloat()
        initTextTimeWidth()

        // Measure settings for header row.
        mHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mHeaderColumnTextColor
            textAlign = Paint.Align.CENTER
            textSize = mTextSize.toFloat()
        }
        // 过去的日期header文本
        mPastHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF999999")
            textAlign = Paint.Align.CENTER
            textSize = mTextSize.toFloat()
        }
        // 过去的日期header是否有日程圆点
        mPastHasEventPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFDDDDDD")
        }

        // Prepare header background paint.
        mHeaderBackgroundPaint = Paint().apply { color = mHeaderRowBackgroundColor }

        // Prepare day background color paint.
        mDayBackgroundPaint = Paint().apply { color = mDayBackgroundColor }

        // Prepare today background color paint.
        mTodayBackgroundPaint = Paint().apply { color = mTodayBackgroundColor }

        // Prepare today header text color paint.
        mTodayHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = mTextSize.toFloat()
            typeface = Typeface.DEFAULT_BOLD
            color = mTodayHeaderTextColor
        }

        // Prepare event background color.
        mEventBackgroundPaint = Paint().apply { color = Color.rgb(174, 208, 238) }
        // TODO 日程左侧装饰边界线
        mEventBorderPaint = Paint().apply { color = Color.parseColor("#FFFF8628") }

        // Prepare event text size and color.
        mEventTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            textAlign = Paint.Align.LEFT
            style = Paint.Style.FILL
            typeface = Typeface.DEFAULT_BOLD
            color = mEventTextColor
            textSize = mEventTextSize.toFloat()
        }

        // 除掉全天日程以外的header高度
        val headerTextPaint = mHeaderTextPaint.apply {
            typeface = Typeface.DEFAULT
            textSize = mTextSize.toFloat()
        }
        val label0Height = ConvertUtils.dp2px(16f) - headerTextPaint.fontMetrics.ascent
        headerTextPaint.apply {
            typeface = Typeface.DEFAULT_BOLD
            textSize = ConvertUtils.sp2px(22f).toFloat()
        }
        val label1Height =
            label0Height + ConvertUtils.dp2px(10f) - headerTextPaint.fontMetrics.ascent
        mNormalHeaderHeight = label1Height + ConvertUtils.dp2px((6 + 2 + 14).toFloat())
    }

    /**
     * Initialize time column width. Calculate value with all possible hours (supposed widest text).
     */
    private fun initTextTimeWidth() {
        mTimeTextWidth = 0f
        for (i in 0..23) {
            // Measure time string and get max width.
            val time = dateTimeInterpreter.interpretTime(i, 0)
            mTimeTextWidth = mTimeTextWidth.coerceAtLeast(mTimeTextPaint.measureText(time))
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mAreDimensionsInvalid = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        heightSize = if (heightMode == MeasureSpec.EXACTLY) heightSize
        else mHeaderHeight.coerceAtLeast(mNormalHeaderHeight + mHeaderMarginBottom).toInt()
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawAllEvents(canvas)
        checkCascadeScrollListener()
    }

    private fun calculateHeaderHeight() {
        // Make sure the header is the right size (depends on AllDay events)
        var maxAllDayRowCount = 0
        if (mEventRects.isNotEmpty()) {
            for (dayNumber in 0 until mNumberOfVisibleDays) {
                val day = mFirstVisibleDay?.clone() as? Calendar ?: continue
                day.add(Calendar.DATE, dayNumber)
                for (rect in mEventRects) {
                    if (isContainsAllDay(day, rect.event)) {
                        maxAllDayRowCount = maxAllDayRowCount.coerceAtLeast(rect.row)
                    }
                }
            }
        }
        val visibleRowCount = 2.5f.coerceAtMost(maxAllDayRowCount * 1f)
        mHeaderHeight =
            mNormalHeaderHeight + mHeaderMarginBottom + allDayEventHeight * visibleRowCount
        mAllDayEventMinY = (allDayEventHeight * (visibleRowCount - maxAllDayRowCount)).toInt()
        if (mHeaderHeight.toInt() != measuredHeight) {
            requestLayout()
        }
    }

    private fun drawAllEvents(canvas: Canvas) {
        // Calculate the available width for each day.
        mHeaderColumnWidth = mTimeTextWidth + mHeaderColumnPadding * 2
        mWidthPerDay = width - mHeaderColumnWidth - mColumnGap * (mNumberOfVisibleDays - 1)
        mWidthPerDay /= mNumberOfVisibleDays

        calculateHeaderHeight() // Make sure the header is the right size (depends on AllDay events)

        val today = today()

        if (mAreDimensionsInvalid) {
            mAreDimensionsInvalid = false
            mScrollToDay?.let { goToDate(it, false) }
            mScrollToDay = null
            mAreDimensionsInvalid = false
        }
        if (mIsFirstDraw) {
            mIsFirstDraw = false

            // If the week view is being drawn for the first time, then consider the first day of the week.
            if (mNumberOfVisibleDays >= 7 && today[Calendar.DAY_OF_WEEK] != mFirstDayOfWeek && showFirstDayOfWeekFirst) {
                val difference = today[Calendar.DAY_OF_WEEK] - mFirstDayOfWeek
                mCurrentOrigin.x += (mWidthPerDay + mColumnGap) * difference
            }
        }

        // If the new mCurrentOrigin.y is invalid, make it valid.
        if (mCurrentOrigin.y < mAllDayEventMinY) mCurrentOrigin.y = mAllDayEventMinY.toFloat()
        // Don't put an "else if" because it will trigger a glitch when completely zoomed out and
        // scrolling vertically.
        if (mCurrentOrigin.y > 0) mCurrentOrigin.y = 0f

        // Consider scroll offset.
        val leftDaysWithGaps = -(mCurrentOrigin.x / (mWidthPerDay + mColumnGap)).roundToInt()
        var startPixel =
            mCurrentOrigin.x + (mWidthPerDay + mColumnGap) * leftDaysWithGaps + mHeaderColumnWidth

        // Clear the cache for event rectangles.
        mEventRects.forEach { it.rectF = null }

        // Iterate through each day.
        val oldFirstVisibleDay = mFirstVisibleDay
        mFirstVisibleDay = (today.clone() as Calendar).apply {
            add(
                Calendar.DATE, -(mCurrentOrigin.x / (mWidthPerDay + mColumnGap)).roundToInt()
            )
        }

        var day: Calendar // Prepare to iterate for each day.
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + 1 + mNumberOfVisibleDays) {
            // Check if the day is today.
            day = today.clone() as Calendar
            mLastVisibleDay = day.clone() as Calendar
            day.add(Calendar.DATE, dayNumber - 1)
            mLastVisibleDay?.add(Calendar.DATE, dayNumber - 2)

            // Get more events if necessary. We want to store the events 3 months beforehand. Get
            // events only when it is the first iteration of the loop.
            if (dayNumber == leftDaysWithGaps + 1) {
                getMoreEvents(day)
            }

            // Draw the events.
            dayHasEvents[dayNumber] = dayHasEvents[dayNumber] == true || hasEvents(day)
        }

        // Hide everything in the first cell (top left corner).
        canvas.save()
        canvas.clipRect(0f, 0f, mTimeTextWidth + mHeaderColumnPadding * 2, mHeaderHeight)
        canvas.drawRect(
            0f, 0f, mTimeTextWidth + mHeaderColumnPadding * 2, mHeaderHeight, mHeaderBackgroundPaint
        )
        // TODO 翻译
        canvas.drawText(
            "全天",
            mTimeTextWidth + mHeaderColumnPadding,
            mNormalHeaderHeight + ConvertUtils.dp2px(2f) - mEventTextPaint.fontMetrics.ascent,
            mTimeTextPaint
        )
        canvas.restore()

        // Clip to paint header row only.
        canvas.save()
        canvas.clipRect(mHeaderColumnWidth, 0f, width.toFloat(), mHeaderHeight)

        // Draw the header background.
        canvas.drawRect(0f, 0f, width.toFloat(), mHeaderHeight, mHeaderBackgroundPaint)

        // Draw the header row texts.
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + 1 + mNumberOfVisibleDays) {
            // Check if the day is today.
            day = today.clone() as Calendar
            day.add(Calendar.DATE, dayNumber - 1)
            val sameDay = isSameDay(day, today)

            // 今天背景颜色
            if (sameDay) canvas.drawRect(
                startPixel, 0f, startPixel + mWidthPerDay, mHeaderHeight, mTodayBackgroundPaint
            )

            // Draw the day labels.
            val dayLabel = dateTimeInterpreter.interpretDate(day)

            // eg. 周三
            val headerTextPaint = when {
                sameDay -> mTodayHeaderTextPaint
                dayNumber < 1 -> mPastHeaderTextPaint
                else -> mHeaderTextPaint
            }.apply {
                typeface = Typeface.DEFAULT
                textSize = mTextSize.toFloat()
            }
            val label0Height = ConvertUtils.dp2px(16f) - headerTextPaint.fontMetrics.ascent
            canvas.drawText(
                dayLabel[0], startPixel + mWidthPerDay / 2, label0Height, headerTextPaint
            )
            // eg. 28
            headerTextPaint.apply {
                typeface = Typeface.DEFAULT_BOLD
                textSize = ConvertUtils.sp2px(22f).toFloat()
            }
            val label1Height =
                label0Height + ConvertUtils.dp2px(10f) - headerTextPaint.fontMetrics.ascent
            canvas.drawText(
                dayLabel[1], startPixel + mWidthPerDay / 2, label1Height, headerTextPaint
            )
            // 是否有日程的圆点
            if (dayHasEvents[dayNumber] == true) {
                canvas.drawCircle(
                    startPixel + mWidthPerDay / 2,
                    label1Height + ConvertUtils.dp2px(6f),
                    ConvertUtils.dp2px(2f).toFloat(),
                    if (dayNumber < 1) mPastHasEventPaint else mTodayHeaderTextPaint
                )
            }

            // In the next iteration, start from the next day.
            startPixel += mWidthPerDay + mColumnGap
        }
        canvas.restore()

        drawAllDayEvents(canvas)

        if (mFirstVisibleDay != oldFirstVisibleDay) mFirstVisibleDay?.let {
            scrollListener?.onFirstVisibleDayChanged(it, oldFirstVisibleDay)
        }
    }

    /**
     * Draw all the events of a particular day.
     *
     * @param date The day.
     * @return 此日是否有日程（包括全天）
     */
    private fun hasEvents(date: Calendar): Boolean {
        var hasEvent = false
        if (mEventRects.isNotEmpty()) {
            for (rect in mEventRects) {
                hasEvent = isContainsAllDay(date, rect.event) || hasEvent
            }
        }
        return hasEvent
    }

    /**
     * Draw all the Allday-events of a particular day.
     *
     * @param canvas The canvas to draw upon.
     */
    private fun drawAllDayEvents(canvas: Canvas) {
        canvas.save()
        canvas.clipRect(
            mHeaderColumnWidth,
            mNormalHeaderHeight,
            width.toFloat(),
            mHeaderHeight - mHeaderMarginBottom
        )
        if (mEventRects.size > 0) {
            for (rect in mEventRects) {
                val rectF = RectF(
                    mCurrentOrigin.x + rect.left,
                    mCurrentOrigin.y + rect.top + ConvertUtils.dp2px(2f),
                    mCurrentOrigin.x + rect.left + rect.width - mColumnGap,
                    mCurrentOrigin.y + rect.bottom
                ).also {
                    rect.rectF = it
                }
                // 日程背景
                canvas.drawRect(
                    rectF.left, rectF.top, rectF.right, rectF.bottom,
                    mEventBackgroundPaint.withBgColor(rect.originalEvent)
                )
                // 日程左侧装饰边界线
                canvas.drawRect(
                    rectF.left,
                    rectF.top,
                    rectF.left + ConvertUtils.dp2px(2f),
                    rectF.bottom,
                    mEventBorderPaint.withBorderColor(rect.originalEvent)
                )
                // 日程标题
                drawEventTitle(rect.event, rectF, canvas)
            }
        }
        canvas.restore()
    }

    /**
     * Draw the name of the event on top of the event rectangle.
     *
     * @param event  The event of which the title (and location) should be drawn.
     * @param rect   The rectangle on which the text is to be drawn.
     * @param canvas The canvas to draw upon.
     */
    private fun drawEventTitle(event: WeekViewEvent, rect: RectF, canvas: Canvas) {
        if (rect.right - rect.left - mEventHPadding * 2 < 0) return
        if (rect.bottom - rect.top - mEventVPadding * 2 < 0) return
        rect.right -= mEventHPadding.toFloat()
        canvas.save()
        canvas.clipRect(rect)
        val left = rect.left.coerceAtLeast(mHeaderColumnWidth)
        canvas.drawText(
            event.name,
            left + mEventHPadding,
            rect.top + mEventVPadding - mEventTextPaint.fontMetrics.ascent,
            mEventTextPaint.withTextColor(event)
        )
        canvas.restore()
    }

    /**
     * Gets more events of one/more month(s) if necessary. This method is called when the user is
     * scrolling the week view. The week view stores the events of three months: the visible month,
     * the previous month, the next month.
     *
     * @param day The day where the user is currently is.
     */
    private fun getMoreEvents(day: Calendar) {
        weekViewLoader?.run {
            val periodToFetch = this.toWeekViewPeriodIndex(day)
            if (!isInEditMode && (mFetchedPeriod < 0 || mFetchedPeriod != periodToFetch)) {
                this.onLoad(periodToFetch)
                mFetchedPeriod = periodToFetch
            }
        }
    }

    /**
     * Refreshes the view and loads the events again.
     */
    fun notifyDatasetChanged(data: MutableList<WeekViewEvent>) {
        // Clear events.
        mEventRects.clear()
        sortAndCacheEvents(data)
        // Prepare to calculate positions of each events.
        val tempEvents = mEventRects
        mEventRects = mutableListOf()
        computePositionOfEvents(tempEvents)
        if (mEventRects.isNotEmpty()) invalidate() else calculateHeaderHeight()
    }

    /**
     * Cache the event for smooth scrolling functionality.
     *
     * @param event The event to cache.
     */
    private fun cacheEvent(event: WeekViewEvent) {
        if (event.startTime >= event.endTime) return
        mEventRects.add(EventRect(event, event, null))
    }

    /**
     * Sort and cache events.
     *
     * @param events The events to be sorted and cached.
     */
    private fun sortAndCacheEvents(events: MutableList<WeekViewEvent>?) {
        events.takeIf { !it.isNullOrEmpty() }?.run {
            sortWith { event1, event2 ->
                val start1 = event1.startTime.timeInMillis
                val start2 = event2.startTime.timeInMillis
                var comparator = start1.compareTo(start2)
                if (comparator == 0) {
                    val end1 = event1.endTime.timeInMillis
                    val end2 = event2.endTime.timeInMillis
                    comparator = end1.compareTo(end2)
                }
                comparator
            }
            forEach { cacheEvent(it) }
        }
    }

    /**
     * Calculates the left and right positions of each events. This comes handy specially if events
     * are overlapping.
     *
     * @param eventRects The events along with their wrapper class.
     */
    private fun computePositionOfEvents(eventRects: List<EventRect>) {
        // Make "collision groups" for all events that collide with others.
        val collisionGroups = mutableListOf<MutableList<EventRect>>()
        for (eventRect in eventRects) {
            var isPlaced = false
            outerLoop@ for (collisionGroup in collisionGroups) {
                for (groupEvent in collisionGroup) {
                    if (isEventsCollide(groupEvent.event, eventRect.event)
                        && groupEvent.event.isAllDay == eventRect.event.isAllDay
                    ) {
                        collisionGroup.add(eventRect)
                        isPlaced = true
                        break@outerLoop
                    }
                }
            }
            if (!isPlaced) {
                val newGroup = mutableListOf(eventRect)
                collisionGroups.add(newGroup)
            }
        }
        collisionGroups.forEach { expandEventsToMaxWidth(it) }
    }

    /**
     * Expands all the events to maximum possible width. The events will try to occupy maximum
     * space available horizontally.
     *
     * @param collisionGroup The group of events which overlap with each other.
     */
    private fun expandEventsToMaxWidth(collisionGroup: List<EventRect>) {
        // Expand the events to maximum possible width.
        val rows = mutableListOf(mutableListOf<EventRect>())
        for (eventRect in collisionGroup) {
            var isPlaced = false
            for (row in rows) {
                if (row.size == 0) {
                    row.add(eventRect)
                    isPlaced = true
                } else if (!isEventsCollide(eventRect.event, row[row.size - 1].event)) {
                    row.add(eventRect)
                    isPlaced = true
                    break
                }
            }
            if (!isPlaced) {
                val newRow = mutableListOf(eventRect)
                rows.add(newRow)
            }
        }

        // Calculate left and right position for all the events.
        // Get the maxColumnCount by looking in all rows.
        var maxColumnCount = 0
        for (row in rows) {
            maxColumnCount = maxColumnCount.coerceAtLeast(row.size)
        }
        for (i in 0 until maxColumnCount) {
            // Set the left and right values of the event.
            for ((j, row) in rows.withIndex()) {
                if (row.size >= i + 1) {
                    val eventRect = row[i]
                    eventRect.width = allDaysBetween(
                        eventRect.event.startTime, eventRect.event.endTime
                    ) * (mWidthPerDay + mColumnGap)
                    eventRect.left = mHeaderColumnWidth + allDaysBetween(
                        today(), eventRect.event.startTime
                    ) * (mWidthPerDay + mColumnGap)
                    if (eventRect.event.isAllDay) {
                        eventRect.top = mNormalHeaderHeight + j * allDayEventHeight
                        eventRect.bottom = eventRect.top + allDayEventHeight
                        eventRect.row = j + 1
                        mEventRects.add(eventRect)
                    }
                }
            }
        }
    }

    /**
     * Checks if two events overlap.
     *
     * @param event1 The first event.
     * @param event2 The second event.
     * @return true if the events overlap.
     */
    private fun isEventsCollide(event1: WeekViewEvent, event2: WeekViewEvent): Boolean {
        val start1 = event1.startTime.timeInMillis
        val end1 = event1.endTime.timeInMillis
        val start2 = event2.startTime.timeInMillis
        val end2 = event2.endTime.timeInMillis
        return !(start1 >= end2 || end1 <= start2)
    }

    override fun invalidate() {
        super.invalidate()
        mAreDimensionsInvalid = true
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Functions related to setting and getting the properties.
    //
    /////////////////////////////////////////////////////////////////

    var eventClickListener: EventClickListener? = null

    var eventLongPressListener: EventLongPressListener? = null

    var monthChangeListener: MonthLoader.MonthChangeListener?
        get() = (weekViewLoader as? MonthLoader)?.onMonthChangeListener
        set(value) {
            value?.let { weekViewLoader = MonthLoader(it) }
        }

    /**
     * Get/Set event loader in the week view. Event loaders define the  interval after which the events
     * are loaded in week view. For a MonthLoader events are loaded for every month. You can define
     * your custom event loader by extending WeekViewLoader.
     */
    var weekViewLoader: WeekViewLoader? = null

    var scrollListener: ScrollListener? = null

    /**
     * Get/Set the interpreter which provides the text to show in the header column and the header row.
     */
    var dateTimeInterpreter: DateTimeInterpreter = object : DateTimeInterpreter {
        override fun interpretDate(date: Calendar): List<String> = runCatching {
            // TODO 默认的date显示拦截器
            val flags =
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_NUMERIC_DATE
            val localizedDate = DateUtils.formatDateTime(context, date.time.time, flags)
            val sdf = SimpleDateFormat("EEE", Locale.getDefault())
            listOf(
                sdf.format(date.time).toUpperCase(Locale.getDefault()), localizedDate
            )
        }.getOrDefault(listOf("", ""))

        override fun interpretTime(hour: Int, minute: Int): String = runCatching {
            val calendar = Calendar.getInstance().apply {
                this[Calendar.HOUR_OF_DAY] = hour
                this[Calendar.MINUTE] = minute
            }
            val sdf = if (DateFormat.is24HourFormat(context)) SimpleDateFormat(
                "HH:mm", Locale.getDefault()
            ) else SimpleDateFormat("hh a", Locale.getDefault())
            sdf.format(calendar.time)
        }.getOrDefault("")
    }
        set(value) {
            field = value
            // Refresh time column width.
            initTextTimeWidth()
        }

    /**
     * Get the number of visible days in a week.
     *
     * @return The number of visible days in a week.
     */
    fun getNumberOfVisibleDays(): Int = mNumberOfVisibleDays

    /**
     * Set the number of visible days in a week.
     *
     * @param numberOfVisibleDays The number of visible days in a week.
     */
    fun setNumberOfVisibleDays(numberOfVisibleDays: Int) {
        this.mNumberOfVisibleDays = numberOfVisibleDays
        mCurrentOrigin.x = 0f
        invalidate()
    }

    fun getColumnGap(): Int = mColumnGap

    fun setColumnGap(columnGap: Int) {
        mColumnGap = columnGap
        invalidate()
    }

    fun getFirstDayOfWeek(): Int = mFirstDayOfWeek

    /**
     * Set the first day of the week. First day of the week is used only when the week view is first
     * drawn. It does not of any effect after user starts scrolling horizontally.
     *
     * **Note:** This method will only work if the week view is set to display more than 6 days at
     * once.
     *
     * @param firstDayOfWeek The supported values are [java.util.Calendar.SUNDAY],
     * [java.util.Calendar.MONDAY], [java.util.Calendar.TUESDAY],
     * [java.util.Calendar.WEDNESDAY], [java.util.Calendar.THURSDAY],
     * [java.util.Calendar.FRIDAY].
     */
    fun setFirstDayOfWeek(firstDayOfWeek: Int) {
        mFirstDayOfWeek = firstDayOfWeek
        invalidate()
    }

    var showFirstDayOfWeekFirst = false

    fun getTextSize(): Int = mTextSize

    fun setTextSize(textSize: Int) {
        mTextSize = textSize
        invalidate()
    }

    fun getHeaderColumnPadding(): Int = mHeaderColumnPadding

    fun setHeaderColumnPadding(headerColumnPadding: Int) {
        mHeaderColumnPadding = headerColumnPadding
        invalidate()
    }

    fun getHeaderColumnTextColor(): Int = mHeaderColumnTextColor

    fun setHeaderColumnTextColor(headerColumnTextColor: Int) {
        mHeaderColumnTextColor = headerColumnTextColor
        mHeaderTextPaint.color = mHeaderColumnTextColor
        invalidate()
    }

    fun getHeaderRowBackgroundColor(): Int = mHeaderRowBackgroundColor

    fun setHeaderRowBackgroundColor(headerRowBackgroundColor: Int) {
        mHeaderRowBackgroundColor = headerRowBackgroundColor
        mHeaderBackgroundPaint.color = mHeaderRowBackgroundColor
        invalidate()
    }

    fun getDayBackgroundColor(): Int = mDayBackgroundColor

    fun setDayBackgroundColor(dayBackgroundColor: Int) {
        mDayBackgroundColor = dayBackgroundColor
        mDayBackgroundPaint.color = mDayBackgroundColor
        invalidate()
    }

    fun getTodayBackgroundColor(): Int = mTodayBackgroundColor

    fun setTodayBackgroundColor(todayBackgroundColor: Int) {
        mTodayBackgroundColor = todayBackgroundColor
        mTodayBackgroundPaint.color = mTodayBackgroundColor
        invalidate()
    }

    fun getTodayHeaderTextColor(): Int = mTodayHeaderTextColor

    fun setTodayHeaderTextColor(todayHeaderTextColor: Int) {
        mTodayHeaderTextColor = todayHeaderTextColor
        mTodayHeaderTextPaint.color = mTodayHeaderTextColor
        invalidate()
    }

    fun getEventTextSize(): Int = mEventTextSize

    fun setEventTextSize(eventTextSize: Int) {
        mEventTextSize = eventTextSize
        mEventTextPaint.textSize = mEventTextSize.toFloat()
        invalidate()
    }

    fun getEventTextColor(): Int = mEventTextColor

    fun setEventTextColor(eventTextColor: Int) {
        mEventTextColor = eventTextColor
        mEventTextPaint.color = mEventTextColor
        invalidate()
    }

    fun getEventPadding(): Int = mEventVPadding

    fun setEventPadding(eventPadding: Int) {
        mEventVPadding = eventPadding
        invalidate()
    }

    fun getHeaderColumnBackgroundColor(): Int {
        return mHeaderColumnBackgroundColor
    }

    fun setHeaderColumnBackgroundColor(headerColumnBackgroundColor: Int) {
        mHeaderColumnBackgroundColor = headerColumnBackgroundColor
        mHeaderColumnBackgroundPaint.color = mHeaderColumnBackgroundColor
        invalidate()
    }

    /**
     * Returns the first visible day in the week view.
     */
    fun getFirstVisibleDay(): Calendar? = mFirstVisibleDay

    /**
     * Returns the last visible day in the week view.
     */
    fun getLastVisibleDay(): Calendar? = mLastVisibleDay

    /**
     * Get/Set the speed for horizontal scrolling.
     */
    var xScrollingSpeed = 1f

    /**
     * Whether weekends should have a background color different from the normal day background
     * color. The weekend background colors are defined by the attributes
     * `futureWeekendBackgroundColor` and `pastWeekendBackgroundColor`.
     *
     * @return True if weekends should have different background colors.
     */
    fun isShowDistinctWeekendColor(): Boolean = mShowDistinctWeekendColor

    /**
     * Set whether weekends should have a background color different from the normal day background
     * color. The weekend background colors are defined by the attributes
     * `futureWeekendBackgroundColor` and `pastWeekendBackgroundColor`.
     *
     * @param showDistinctWeekendColor True if weekends should have different background colors.
     */
    fun setShowDistinctWeekendColor(showDistinctWeekendColor: Boolean) {
        this.mShowDistinctWeekendColor = showDistinctWeekendColor
        invalidate()
    }

    /**
     * Whether past and future days should have two different background colors. The past and
     * future day colors are defined by the attributes `futureBackgroundColor` and
     * `pastBackgroundColor`.
     *
     * @return True if past and future days should have two different background colors.
     */
    fun isShowDistinctPastFutureColor(): Boolean = mShowDistinctPastFutureColor

    /**
     * Set whether weekends should have a background color different from the normal day background
     * color. The past and future day colors are defined by the attributes `futureBackgroundColor`
     * and `pastBackgroundColor`.
     *
     * @param showDistinctPastFutureColor True if past and future should have two different
     * background colors.
     */
    fun setShowDistinctPastFutureColor(showDistinctPastFutureColor: Boolean) {
        this.mShowDistinctPastFutureColor = showDistinctPastFutureColor
        invalidate()
    }

    /**
     * Get/Set whether the week view should fling horizontally.
     */
    var horizontalFlingEnabled = true

    /**
     * Get/Set whether the week view should fling vertically.
     */
    var verticalFlingEnabled = true

    /**
     * Get/Set the height of AllDay-events.
     */
    var allDayEventHeight = 100

    /**
     * Get/Set scroll duration
     */
    var scrollDuration = 250

    /////////////////////////////////////////////////////////////////
    //
    //      Functions related to scrolling.
    //
    /////////////////////////////////////////////////////////////////

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val ret = mGestureDetector.onTouchEvent(event)
        // Check after call of mGestureDetector, so mCurrentFlingDirection and mCurrentScrollDirection are set.
        if (event.action == MotionEvent.ACTION_UP && mCurrentFlingDirection == Direction.NONE) {
            if (mCurrentScrollDirection == Direction.RIGHT || mCurrentScrollDirection == Direction.LEFT) {
                goToNearestOrigin()
            }
            mCurrentScrollDirection = Direction.NONE
        }
        return ret
    }

    private fun goToNearestOrigin() {
        var leftDays = mCurrentOrigin.x / (mWidthPerDay + mColumnGap)
        leftDays = if (mCurrentFlingDirection != Direction.NONE) {
            // snap to nearest day
            round(leftDays)
        } else if (mCurrentScrollDirection == Direction.LEFT) {
            // snap to last day
            floor(leftDays)
        } else if (mCurrentScrollDirection == Direction.RIGHT) {
            // snap to next day
            ceil(leftDays)
        } else {
            // snap to nearest day
            round(leftDays)
        }

        val nearestOrigin = (mCurrentOrigin.x - leftDays * (mWidthPerDay + mColumnGap)).toInt()
        if (nearestOrigin != 0) {
            // Stop current animation.
            mScroller.forceFinished(true)
            // Snap to date.
            // Snap to date.
            mScroller.startScroll(
                mCurrentOrigin.x.toInt(),
                mCurrentOrigin.y.toInt(),
                -nearestOrigin,
                0,
                (abs(nearestOrigin) / mWidthPerDay * scrollDuration).toInt()
            )

            ViewCompat.postInvalidateOnAnimation(this)
        }
        // Reset scrolling and fling direction.
        mCurrentScrollDirection = Direction.NONE
        mCurrentFlingDirection = Direction.NONE
    }

    override fun computeScroll() {
        super.computeScroll()
        if (mScroller.isFinished) {
            if (mCurrentFlingDirection != Direction.NONE) {
                // Snap to day after fling is finished.
                goToNearestOrigin()
            }
        } else {
            if (mCurrentFlingDirection != Direction.NONE && forceFinishScroll()) {
                goToNearestOrigin()
            } else if (mScroller.computeScrollOffset()) {
                mCurrentOrigin.y = mScroller.currY.toFloat()
                mCurrentOrigin.x = mScroller.currX.toFloat()
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }
    }

    /**
     * Check if scrolling should be stopped.
     *
     * @return true if scrolling should be stopped before reaching the end of animation.
     */
    private fun forceFinishScroll(): Boolean {
        // current velocity only available since api 14
        return mScroller.currVelocity <= mMinimumFlingVelocity
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Public methods.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * Show today on the week view.
     */
    fun goToToday() {
        val today = Calendar.getInstance()
        goToDate(today, false)
    }

    fun goFirstVisibleDay(outFirstVisibleDay: Calendar) {
        goToDate(outFirstVisibleDay, false)
    }

    /**
     * Show a specific day on the week view.
     *
     * @param date The date to show.
     */
    fun goToDate(date: Calendar, smooth: Boolean) {
        mScroller.forceFinished(true)
        mCurrentFlingDirection = Direction.NONE
        mCurrentScrollDirection = mCurrentFlingDirection
        date.apply {
            this[Calendar.HOUR_OF_DAY] = 0
            this[Calendar.MINUTE] = 0
            this[Calendar.SECOND] = 0
            this[Calendar.MILLISECOND] = 0
        }
        if (mAreDimensionsInvalid) {
            mScrollToDay = date
            return
        }
        val today = Calendar.getInstance().apply {
            this[Calendar.HOUR_OF_DAY] = 0
            this[Calendar.MINUTE] = 0
            this[Calendar.SECOND] = 0
            this[Calendar.MILLISECOND] = 0
        }
        val day = 1000L * 60L * 60L * 24L
        val dateInMillis = date.timeInMillis + date.timeZone.getOffset(date.timeInMillis)
        val todayInMillis = today.timeInMillis + today.timeZone.getOffset(today.timeInMillis)
        val dateDifference = dateInMillis / day - todayInMillis / day
        val newX = -dateDifference * (mWidthPerDay + mColumnGap)
        if (smooth) {
            val dX = newX - mCurrentOrigin.x
            mScroller.startScroll(
                mCurrentOrigin.x.toInt(),
                0,
                dX.toInt(),
                0,
                (abs(dX) / mWidthPerDay * scrollDuration).toInt()
            )
            ViewCompat.postInvalidateOnAnimation(this@WeekHeaderView)
        } else {
            mCurrentOrigin.x = newX
            invalidate()
        }
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Custom
    //
    /////////////////////////////////////////////////////////////////

    var cascadeScrollListener: CascadeScrollListener? = null

    fun setCurrentOriginX(currentOriginX: Float) {
        if (!isFloatEqual(currentOriginX, mCurrentOrigin.x)) {
            mCurrentOrigin.x = currentOriginX
            invalidate()
        }
    }

    private fun checkCascadeScrollListener() {
        cascadeScrollListener?.let { listener ->
            if (mScroller.isFinished
                && isFloatEqual(mCurrentOrigin.x, mScroller.finalX.toFloat())
            ) {
                mFirstVisibleDay?.let { listener.onScrollEnd(it) }
            } else {
                listener.onScrolling(mCurrentOrigin.x)
            }
        }
    }
}
