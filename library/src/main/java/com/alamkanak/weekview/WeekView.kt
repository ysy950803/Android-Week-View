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
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
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
import com.alamkanak.weekview.WeekViewUtil.isFloatEqual
import com.alamkanak.weekview.WeekViewUtil.isSameDay
import com.alamkanak.weekview.WeekViewUtil.obtainStaticLayout
import com.alamkanak.weekview.WeekViewUtil.performPressVibrate
import com.alamkanak.weekview.WeekViewUtil.today
import com.blankj.utilcode.util.ConvertUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

@Suppress("unused")
class WeekView : View {

    private lateinit var mTimeTextPaint: Paint
    private var mTimeTextWidth = 0f
    private var mTimeTextHeight = 0f
    private lateinit var mGestureDetector: GestureDetectorCompat
    private lateinit var mScroller: OverScroller
    private var mCurrentOrigin = PointF(0f, 0f)
    private var mCurrentScrollDirection = Direction.NONE
    private var mWidthPerDay = 0f
    private lateinit var mDayBackgroundPaint: Paint
    private lateinit var mHourSeparatorPaint: Paint
    private var mHeaderMarginBottom = 0f
    private lateinit var mTodayBackgroundPaint: Paint
    private lateinit var mFutureBackgroundPaint: Paint
    private lateinit var mPastBackgroundPaint: Paint
    private lateinit var mFutureWeekendBackgroundPaint: Paint
    private lateinit var mPastWeekendBackgroundPaint: Paint
    private lateinit var mNowLinePaint: Paint
    private lateinit var mNowLineOtherPaint: Paint
    private lateinit var mEventBackgroundPaint: Paint
    private lateinit var mEventBorderPaint: Paint
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
    private var mHourHeight = 50
    private var mColumnGap = 10
    private var mFirstDayOfWeek = Calendar.MONDAY
    private var mTextSize = 12
    private var mHeaderColumnPadding = 10
    private var mNumberOfVisibleDays = 3
    private var mDayBackgroundColor = Color.rgb(245, 245, 245)
    private var mPastBackgroundColor = Color.rgb(227, 227, 227)
    private var mFutureBackgroundColor = Color.rgb(245, 245, 245)
    private var mPastWeekendBackgroundColor = 0
    private var mFutureWeekendBackgroundColor = 0
    private var mNowLineColor = Color.rgb(102, 102, 102)
    private var mNowLineThickness = 5
    private var mHourSeparatorColor = Color.rgb(230, 230, 230)
    private var mTodayBackgroundColor = Color.rgb(239, 247, 254)
    private var mHourSeparatorHeight = 2
    private var mEventTextSize = 12
    private var mEventTextColor = Color.BLACK
    private var mEventVPadding = 0
    private var mEventHPadding = 0
    private var mIsFirstDraw = true
    private var mAreDimensionsInvalid = true
    private var mOverlappingEventGap = 0
    private var mEventMarginVertical = 0
    private var mScrollToDay: Calendar? = null
    private var mScrollToHour = -1.0
    private var mShowDistinctWeekendColor = false
    private var mShowNowLine = false
    private var mShowDistinctPastFutureColor = false

    // 绘制过程中标记某天是否有日程
    val dayHasEvents = hashMapOf<Int, Boolean>()

    // 选择区域位置，即点击“新建日程”区域
    private enum class SelectType {
        NONE, CREATE, MOVE, TOP_BUBBLE, BOTTOM_BUBBLE
    }

    private var mCreateView: CreateEventView? = null
    private var mSelectType = SelectType.NONE
    private var mSelectedRectF: RectF? = null
    private var mSelectedBubbleMargin = 0
    private var mSelectedBubbleClickPadding = 0
    private lateinit var mSelectTextPaint: Paint
    private lateinit var mSelectConflictTextPaint: Paint
    private var mTopEventRectY = Float.MAX_VALUE
    private var mBottomEventRectY = Float.MIN_VALUE
    private var mCurrentDate: Calendar = today()
        get() = field.clone() as Calendar

    private val mGestureListener: SimpleOnGestureListener = object : SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            mSelectedRectF?.let { rectF ->
                val topBubble = RectF(
                    rectF.right - mSelectedBubbleMargin - mSelectedBubbleClickPadding,
                    rectF.top + getFirstHourLineY() - mSelectedBubbleClickPadding,
                    rectF.right - mSelectedBubbleMargin + mSelectedBubbleClickPadding,
                    rectF.top + getFirstHourLineY() + mSelectedBubbleClickPadding
                )
                val bottomBubble = RectF(
                    rectF.left + mSelectedBubbleMargin - mSelectedBubbleClickPadding,
                    rectF.bottom + getFirstHourLineY() - mSelectedBubbleClickPadding,
                    rectF.left + mSelectedBubbleMargin + mSelectedBubbleClickPadding,
                    rectF.bottom + getFirstHourLineY() + mSelectedBubbleClickPadding
                )
                val selectedRectF = RectF(
                    rectF.left, rectF.top + getFirstHourLineY(),
                    rectF.right, rectF.bottom + getFirstHourLineY()
                )
                mSelectType = if (topBubble.contains(e.x, e.y)) {
                    performPressVibrate(this@WeekView)
                    SelectType.TOP_BUBBLE
                } else if (bottomBubble.contains(e.x, e.y)) {
                    performPressVibrate(this@WeekView)
                    SelectType.BOTTOM_BUBBLE
                } else if (selectedRectF.contains(e.x, e.y)) {
                    SelectType.MOVE
                } else {
                    SelectType.NONE
                }
                if (mSelectType != SelectType.NONE) getContainer()?.setScrollable(false)
            } ?: run {
                mSelectType = SelectType.NONE
                goToNearestOrigin()
            }
            return true
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val disX = if (isOneVisibleDay()) 0f else distanceX
            when (mSelectType) {
                SelectType.MOVE -> onSelectMove(disX, distanceY)
                SelectType.TOP_BUBBLE, SelectType.BOTTOM_BUBBLE -> onSelectBubble(distanceY)
                else -> {
                    val horizontal = abs(disX) > abs(distanceY)
                    when (mCurrentScrollDirection) {
                        Direction.NONE -> {
                            // Allow scrolling only in one direction.
                            mCurrentScrollDirection = if (horizontal) {
                                if (disX > 0) {
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
                            if (horizontal && disX < -mScaledTouchSlop) {
                                mCurrentScrollDirection = Direction.RIGHT
                            }
                        }
                        Direction.RIGHT -> {
                            // Change direction if there was enough change.
                            if (horizontal && disX > mScaledTouchSlop) {
                                mCurrentScrollDirection = Direction.LEFT
                            }
                        }
                        else -> {}
                    }
                    when (mCurrentScrollDirection) {
                        Direction.LEFT, Direction.RIGHT -> {
                            clearSelected(false)
                            getContainer()?.setScrollable(false)
                            mCurrentOrigin.x -= disX * xScrollingSpeed
                            ViewCompat.postInvalidateOnAnimation(this@WeekView)
                        }
                        Direction.VERTICAL ->
                            if (mScroller.isFinished) getContainer()?.setScrollable(true)
                        else -> {}
                    }
                }
            }
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (isOneVisibleDay()
                || mCurrentFlingDirection == Direction.LEFT && !horizontalFlingEnabled
                || mCurrentFlingDirection == Direction.RIGHT && !horizontalFlingEnabled
            ) {
                return true
            }
            mScroller.forceFinished(true)
            mCurrentFlingDirection = mCurrentScrollDirection
            when (mCurrentFlingDirection) {
                Direction.LEFT, Direction.RIGHT -> {
                    getContainer()?.setScrollable(false)
                    mScroller.fling(
                        mCurrentOrigin.x.toInt(),
                        0,
                        (velocityX * xScrollingSpeed).toInt(),
                        0,
                        Int.MIN_VALUE,
                        Int.MAX_VALUE,
                        -(mHourHeight * 24 + getFirstHourLineY() - height).toInt(),
                        0
                    )
                }
                else -> {}
            }
            ViewCompat.postInvalidateOnAnimation(this@WeekView)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (isClickSelected(e)) {
                eventClickListener?.let { listener ->
                    // TODO 点击“新建日程”区域
                }
            } else {
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

                // If the tap was on in an empty space, then trigger the callback.
                emptyViewClickListener?.let { listener ->
                    if (e.x > mHeaderColumnWidth && e.y > mHeaderMarginBottom) {
                        getTimeFromPoint(e.x, e.y)?.let { selectedTime ->
                            playSoundEffect(SoundEffectConstants.CLICK)
                            listener.onEmptyViewClicked(selectedTime)
                        }
                    }
                }

                // 点击空白区域
                if (mSelectedRectF == null) {
                    reLocationSelectedRectF(e, true)
                    performPressVibrate(this@WeekView)
                    invalidate()
                } else {
                    clearSelected(true)
                    getContainer()?.setScrollable(true)
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

            // If the tap was on in an empty space, then trigger the callback.
            emptyViewLongPressListener?.let { listener ->
                if (e.x > mHeaderColumnWidth && e.y > mHeaderMarginBottom) {
                    getTimeFromPoint(e.x, e.y)?.let { selectedTime ->
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        listener.onEmptyViewLongPress(selectedTime)
                    }
                }
            }
        }

        private fun isClickSelected(e: MotionEvent) = mSelectedRectF?.contains(e.x, e.y) == true

        private fun onSelectMove(distanceX: Float, distanceY: Float) {
            val rectF = mSelectedRectF ?: return
            distanceX.let { rectF.left -= it; rectF.left }
            distanceX.let { rectF.right -= it; rectF.right }
            val top = distanceY.let { rectF.top -= it; rectF.top }
            val bottom = distanceY.let { rectF.bottom -= it; rectF.bottom }
            val height = rectF.height()
            if (0 < top && bottom < mHourHeight * 24) {
                rectF.top = top
                rectF.bottom = bottom
            } else {
                if (top <= 0) {
                    rectF.top = 0f
                    rectF.bottom = height
                }
                if (bottom >= mHourHeight * 24) {
                    rectF.top = mHourHeight * 24 - height
                    rectF.bottom = (mHourHeight * 24).toFloat()
                }
            }
            scrollWhenEdge(distanceY)
            invalidate()
        }

        private fun onSelectBubble(distanceY: Float) {
            val rectF = mSelectedRectF ?: return
            val topBubbleSelected = mSelectType == SelectType.TOP_BUBBLE
            val bottomBubbleSelected = mSelectType == SelectType.BOTTOM_BUBBLE
            val top = (if (topBubbleSelected) -distanceY else 0f).let {
                rectF.top += it; rectF.top
            }
            val bottom = (if (bottomBubbleSelected) -distanceY else 0f).let {
                rectF.bottom += it; rectF.bottom
            }
            if (0 < top && bottom < mHourHeight * 24) {
                rectF.top = top
                rectF.bottom = bottom
            } else {
                if (top <= 0) rectF.top = 0f
                if (bottom >= mHourHeight * 24) rectF.bottom = (mHourHeight * 24).toFloat()
            }
            if (topBubbleSelected && top > bottom - mHourHeight / 2f) {
                rectF.top = bottom - mHourHeight / 2f
            } else if (bottomBubbleSelected && bottom < top + mHourHeight / 2f) {
                rectF.bottom = top + mHourHeight / 2f
            }
            scrollWhenEdge(distanceY)
            invalidate()
        }

        private var mEdgeDelaying = false
        private val mEdgeDelayRunnable = Runnable { mEdgeDelaying = false }

        private fun scrollWhenEdge(distanceY: Float) {
            val rectF = mSelectedRectF ?: return
            // 上下边界，触发垂直滚动
            val scrollView = getContainer() ?: return
            val layoutScrollY = scrollView.scrollY
            val layoutHeight = scrollView.height
            val boundaryDistance = 100f
            if (distanceY > 0 && layoutScrollY > 0 && rectF.top - layoutScrollY <= boundaryDistance) {
                scrollView.smoothScrollTo(0, (layoutScrollY - distanceY).toInt())
            } else if (distanceY < 0 && layoutScrollY + layoutHeight - rectF.bottom <= boundaryDistance) {
                scrollView.smoothScrollTo(0, (layoutScrollY + abs(distanceY)).toInt())
            }
            // 左右边界，触发水平滚动（日期切换）
            if (!mEdgeDelaying && !isOneVisibleDay()) {
                if (rectF.left < mHeaderColumnWidth - mSelectedBubbleMargin) {
                    mEdgeDelaying = true
                    postDelayed(mEdgeDelayRunnable, TimeUnit.SECONDS.toMillis(2))
                    mFirstVisibleDay?.let {
                        val preDay = (it.clone() as Calendar).apply {
                            add(Calendar.HOUR_OF_DAY, -24)
                        }
                        goToDate(preDay, true)
                    }
                } else if (rectF.right > width + mSelectedBubbleMargin) {
                    mEdgeDelaying = true
                    postDelayed(mEdgeDelayRunnable, TimeUnit.SECONDS.toMillis(2))
                    mFirstVisibleDay?.let {
                        val nextDay = (it.clone() as Calendar).apply {
                            add(Calendar.HOUR_OF_DAY, 24)
                        }
                        goToDate(nextDay, true)
                    }
                }
            }
        }
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        // Get the attribute values (if any).
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.WeekView, 0, 0)
        try {
            mFirstDayOfWeek = a.getInteger(R.styleable.WeekView_firstDayOfWeek, mFirstDayOfWeek)
            mHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_hourHeight, mHourHeight)
            mTextSize = a.getDimensionPixelSize(
                R.styleable.WeekView_textSize,
                ConvertUtils.sp2px(mTextSize.toFloat())
            )
            mHeaderColumnPadding = a.getDimensionPixelSize(
                R.styleable.WeekView_headerColumnPadding,
                mHeaderColumnPadding
            )
            mColumnGap = a.getDimensionPixelSize(R.styleable.WeekView_columnGap, mColumnGap)
            mNumberOfVisibleDays =
                a.getInteger(R.styleable.WeekView_noOfVisibleDays, mNumberOfVisibleDays)
            showFirstDayOfWeekFirst =
                a.getBoolean(R.styleable.WeekView_showFirstDayOfWeekFirst, showFirstDayOfWeekFirst)
            mDayBackgroundColor =
                a.getColor(R.styleable.WeekView_dayBackgroundColor, mDayBackgroundColor)
            mFutureBackgroundColor =
                a.getColor(R.styleable.WeekView_futureBackgroundColor, mFutureBackgroundColor)
            mPastBackgroundColor =
                a.getColor(R.styleable.WeekView_pastBackgroundColor, mPastBackgroundColor)
            mFutureWeekendBackgroundColor = a.getColor(
                R.styleable.WeekView_futureWeekendBackgroundColor,
                mFutureBackgroundColor
            )
            // If not set, use the same color as in the week
            mPastWeekendBackgroundColor =
                a.getColor(R.styleable.WeekView_pastWeekendBackgroundColor, mPastBackgroundColor)
            mNowLineColor = a.getColor(R.styleable.WeekView_nowLineColor, mNowLineColor)
            mNowLineThickness = a.getDimensionPixelSize(
                R.styleable.WeekView_nowLineThickness,
                ConvertUtils.dp2px(mNowLineThickness.toFloat())
            )
            this.mHourSeparatorColor =
                a.getColor(
                    R.styleable.WeekView_hourSeparatorColor,
                    this.mHourSeparatorColor
                )
            this.mTodayBackgroundColor =
                a.getColor(
                    R.styleable.WeekView_todayBackgroundColor,
                    this.mTodayBackgroundColor
                )
            this.mHourSeparatorHeight = a.getDimensionPixelSize(
                R.styleable.WeekView_hourSeparatorHeight,
                this.mHourSeparatorHeight
            )
            mEventTextSize = a.getDimensionPixelSize(
                R.styleable.WeekView_eventTextSize,
                ConvertUtils.sp2px(this.mEventTextSize.toFloat())
            )
            mEventTextColor = a.getColor(R.styleable.WeekView_eventTextColor, mEventTextColor)
            mEventVPadding =
                a.getDimensionPixelSize(R.styleable.WeekView_eventPadding, mEventVPadding)
            // TODO 日程文本内容水平padding
            mEventHPadding = ConvertUtils.dp2px(6f)
            this.mOverlappingEventGap = a.getDimensionPixelSize(
                R.styleable.WeekView_overlappingEventGap,
                this.mOverlappingEventGap
            )
            mEventMarginVertical = a.getDimensionPixelSize(
                R.styleable.WeekView_eventMarginVertical,
                mEventMarginVertical
            )
            xScrollingSpeed = a.getFloat(R.styleable.WeekView_xScrollingSpeed, xScrollingSpeed)
            eventCornerRadius =
                a.getDimensionPixelSize(R.styleable.WeekView_eventCornerRadius, eventCornerRadius)
            mShowDistinctPastFutureColor = a.getBoolean(
                R.styleable.WeekView_showDistinctPastFutureColor,
                mShowDistinctPastFutureColor
            )
            mShowDistinctWeekendColor = a.getBoolean(
                R.styleable.WeekView_showDistinctWeekendColor,
                mShowDistinctWeekendColor
            )
            mShowNowLine = a.getBoolean(R.styleable.WeekView_showNowLine, mShowNowLine)
            horizontalFlingEnabled =
                a.getBoolean(R.styleable.WeekView_horizontalFlingEnabled, horizontalFlingEnabled)
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
            textAlign = Paint.Align.LEFT
            // 时间轴文本大小
            textSize = ConvertUtils.sp2px(10f).toFloat()
            color = Color.parseColor("#FF888888")
        }
        val rect = Rect()
        mTimeTextPaint.getTextBounds("00 PM", 0, "00 PM".length, rect)
        mTimeTextHeight = rect.height().toFloat()
        mHeaderMarginBottom = mTimeTextHeight / 2
        initTextTimeWidth()

        // Prepare day background color paint.
        mDayBackgroundPaint = Paint().apply { color = mDayBackgroundColor }
        mFutureBackgroundPaint = Paint().apply { color = mFutureBackgroundColor }
        mPastBackgroundPaint = Paint().apply { color = mPastBackgroundColor }
        mFutureWeekendBackgroundPaint = Paint().apply { color = mFutureWeekendBackgroundColor }
        mPastWeekendBackgroundPaint = Paint().apply { color = mPastWeekendBackgroundColor }

        // Prepare hour separator color paint.
        mHourSeparatorPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = this@WeekView.mHourSeparatorHeight.toFloat()
            color = this@WeekView.mHourSeparatorColor
        }

        // Prepare the "now" line color paint
        mNowLinePaint = Paint().apply {
            strokeWidth = mNowLineThickness.toFloat()
            color = mNowLineColor
        }
        mNowLineOtherPaint = Paint().apply {
            strokeWidth = mNowLineThickness / 2f
            color = mNowLineColor
            alpha = 128
        }

        // Prepare today background color paint.
        mTodayBackgroundPaint = Paint().apply { color = mTodayBackgroundColor }

        // Prepare event background color.
        mEventBackgroundPaint = Paint().apply { color = Color.rgb(174, 208, 238) }
        // TODO 日程左侧装饰边界线
        mEventBorderPaint = Paint().apply { color = Color.parseColor("#FFFF8628") }

        // Prepare event text size and color.
        mEventTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            textAlign = Paint.Align.LEFT
            style = Paint.Style.FILL
            color = mEventTextColor
            textSize = this@WeekView.mEventTextSize.toFloat()
        }

        mSelectedBubbleMargin = ConvertUtils.dp2px(18f)
        mSelectedBubbleClickPadding = ConvertUtils.dp2px(14f)
        mSelectTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.LEFT
            textSize = ConvertUtils.sp2px(10f).toFloat()
            color = Color.parseColor("#FF00B98F")
            isFakeBoldText = true
        }
        mSelectConflictTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = ConvertUtils.sp2px(10f).toFloat()
            color = Color.parseColor("#FFFD7443")
            isFakeBoldText = true
        }
    }

    /**
     * Initialize time column width. Calculate value with all possible hours (supposed widest text).
     */
    private fun initTextTimeWidth() {
        mTimeTextWidth = 0f
        for (i in 0..23) {
            // Measure time string and get max width.
            val time: String = dateTimeInterpreter.interpretTime(i, 0)
            mTimeTextWidth = mTimeTextWidth.coerceAtLeast(mTimeTextPaint.measureText(time))
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // fix rotation changes
        mAreDimensionsInvalid = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(widthMeasureSpec, (getFirstHourLineY() + mHourHeight * 24).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isOneVisibleDay()) mCurrentOrigin.x = 0f
        drawAllEvents(canvas)
        val overlapping = checkSelectOverlapping()
        // Draw create event view.
        drawSelected(canvas, overlapping)
        // Draw the time column and all the axes/separators.
        drawTimeColumnAndAxes(canvas, overlapping)
        checkCascadeScrollListener()
    }

    private fun drawAllEvents(canvas: Canvas) {
        // Calculate the available width for each day.
        mHeaderColumnWidth = mTimeTextWidth + mHeaderColumnPadding * 2
        mWidthPerDay = width - mHeaderColumnWidth - mColumnGap * (mNumberOfVisibleDays - 1)
        mWidthPerDay /= mNumberOfVisibleDays

        val curDate = mCurrentDate
        val today = today()

        if (mAreDimensionsInvalid) {
            mAreDimensionsInvalid = false
            mScrollToDay?.let { goToDate(it, false) }

            mAreDimensionsInvalid = false
            if (mScrollToHour >= 0) goToHour(mScrollToHour)

            mScrollToDay = null
            mScrollToHour = -1.0
            mAreDimensionsInvalid = false
        }
        if (mIsFirstDraw) {
            mIsFirstDraw = false

            // If the week view is being drawn for the first time, then consider the first day of the week.
            if (mNumberOfVisibleDays >= 7 && curDate[Calendar.DAY_OF_WEEK] != mFirstDayOfWeek && showFirstDayOfWeekFirst) {
                val difference = curDate[Calendar.DAY_OF_WEEK] - mFirstDayOfWeek
                mCurrentOrigin.x += (mWidthPerDay + mColumnGap) * difference
            }
        }

        // Consider scroll offset.
        val leftDaysWithGaps = -(mCurrentOrigin.x / (mWidthPerDay + mColumnGap)).roundToInt()
        var startPixel =
            mCurrentOrigin.x + (mWidthPerDay + mColumnGap) * leftDaysWithGaps + mHeaderColumnWidth

        // Prepare to iterate for each hour to draw the hour lines.
        var lineCount = ((height - mHeaderMarginBottom) / mHourHeight).toInt() + 1
        lineCount *= (mNumberOfVisibleDays + 1)
        val hourLines = FloatArray(lineCount * 4)

        // Clear the cache for event rectangles.
        mEventRects.forEach { it.rectF = null }

        // Clip to paint events only.
        canvas.save()
        canvas.clipRect(
            mHeaderColumnWidth - ConvertUtils.dp2px(3f),
            0f,
            width.toFloat(),
            height.toFloat()
        )

        // Iterate through each day.
        val oldFirstVisibleDay = mFirstVisibleDay
        mFirstVisibleDay = (curDate.clone() as Calendar).apply {
            add(
                Calendar.DATE,
                -(mCurrentOrigin.x / (mWidthPerDay + mColumnGap)).roundToInt()
            )
        }

        dayHasEvents.clear()
        resetTopBottomEventRectY()
        val nowLineY = getNowLineY()
        var day: Calendar // Prepare to iterate for each day.
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + 1
            + if (isOneVisibleDay()) 0 else mNumberOfVisibleDays
        ) {
            // Check if the day is today.
            day = curDate.clone() as Calendar
            mLastVisibleDay = day.clone() as Calendar
            day.add(Calendar.DATE, dayNumber - 1)
            mLastVisibleDay?.add(Calendar.DATE, dayNumber - 2)
            val sameDay = isSameDay(day, curDate)

            // Get more events if necessary. We want to store the events 3 months beforehand. Get
            // events only when it is the first iteration of the loop.
            if (dayNumber == leftDaysWithGaps + 1) {
                getMoreEvents(day)
            }

            // Draw background color for each day.
            val start = startPixel.coerceAtLeast(mHeaderColumnWidth)
            if (mWidthPerDay + startPixel - start > 0) {
                if (mShowDistinctPastFutureColor) {
                    val isWeekend =
                        day[Calendar.DAY_OF_WEEK] == Calendar.SATURDAY || day[Calendar.DAY_OF_WEEK] == Calendar.SUNDAY
                    val pastPaint =
                        if (isWeekend && mShowDistinctWeekendColor) mPastWeekendBackgroundPaint else mPastBackgroundPaint
                    val futurePaint =
                        if (isWeekend && mShowDistinctWeekendColor) mFutureWeekendBackgroundPaint else mFutureBackgroundPaint
                    val startY = getFirstHourLineY()
                    if (sameDay) {
                        val now = Calendar.getInstance()
                        val beforeNow =
                            (now[Calendar.HOUR_OF_DAY] + now[Calendar.MINUTE] / 60.0f) * mHourHeight
                        canvas.drawRect(
                            start,
                            startY,
                            startPixel + mWidthPerDay,
                            startY + beforeNow,
                            pastPaint
                        )
                        canvas.drawRect(
                            start,
                            startY + beforeNow,
                            startPixel + mWidthPerDay,
                            height.toFloat(),
                            futurePaint
                        )
                    } else if (day.before(curDate)) {
                        canvas.drawRect(
                            start,
                            startY,
                            startPixel + mWidthPerDay,
                            height.toFloat(),
                            pastPaint
                        )
                    } else {
                        canvas.drawRect(
                            start,
                            startY,
                            startPixel + mWidthPerDay,
                            height.toFloat(),
                            futurePaint
                        )
                    }
                } else {
                    // 今天背景色
                    canvas.drawRect(
                        start, 0f, startPixel + mWidthPerDay, height.toFloat(),
                        (if (sameDay && !isOneVisibleDay()) mTodayBackgroundPaint else mDayBackgroundPaint)
                    )
                }
            }

            // Prepare the separator lines for hours.
            var i = 0
            for (hourNumber in 0..23) {
                val top = mHourHeight * hourNumber + getFirstHourLineY()
                if (top > getFirstHourLineY() - this.mHourSeparatorHeight && top < height && startPixel + mWidthPerDay - start > 0) {
                    hourLines[i * 4] = start
                    hourLines[i * 4 + 1] = top
                    hourLines[i * 4 + 2] = startPixel + mWidthPerDay
                    hourLines[i * 4 + 3] = top
                    i++
                }
            }

            // Draw the lines for hours.
            canvas.drawLines(hourLines, mHourSeparatorPaint)

            // Draw the events.
            dayHasEvents[dayNumber] = drawEvents(
                day, startPixel, canvas,
                dayNumber == leftDaysWithGaps + 1 + mNumberOfVisibleDays
            )

            // Draw the line at the current time.
            if (mShowNowLine && isSameDay(day, today)) {
                canvas.drawLine(
                    start,
                    nowLineY,
                    startPixel + mWidthPerDay,
                    nowLineY,
                    mNowLinePaint
                )
                // 时间游标圆点
                if (startPixel >= mHeaderColumnWidth) {
                    val radius = ConvertUtils.dp2px(3f).toFloat()
                    canvas.drawCircle(start + radius, nowLineY, radius, mNowLinePaint)
                }
            }

            // In the next iteration, start from the next day.
            startPixel += mWidthPerDay + mColumnGap
        }
        if (!isOneVisibleDay()) canvas.drawLine(
            mHeaderColumnWidth,
            nowLineY,
            mHeaderColumnWidth + mWidthPerDay * mNumberOfVisibleDays,
            nowLineY,
            mNowLineOtherPaint
        )
        canvas.restore()

        if (mFirstVisibleDay != oldFirstVisibleDay) mFirstVisibleDay?.let {
            scrollListener?.onFirstVisibleDayChanged(it, oldFirstVisibleDay)
        }
    }

    /**
     * Draw all the events of a particular day.
     *
     * @param date           The day.
     * @param startFromPixel The left position of the day area. The events will never go any left from this value.
     * @param canvas         The canvas to draw upon.
     * @param preDraw        正在绘制可见范围外的未来一天
     * @return 此日是否有日程（包括全天）
     */
    private fun drawEvents(
        date: Calendar,
        startFromPixel: Float,
        canvas: Canvas,
        preDraw: Boolean
    ): Boolean {
        var hasEvent = false
        mEventRects.filter {
            isSameDay(it.event.startTime, date) && !it.event.isAllDay
        }.also {
            if (it.isNotEmpty()) hasEvent = true
        }.forEach { eventRect ->
            // Calculate top.
            val top =
                mHourHeight * 24 * eventRect.top / 1440 + getFirstHourLineY() + mEventMarginVertical
            // Calculate bottom.
            val bottom =
                mHourHeight * 24 * eventRect.bottom / 1440 + getFirstHourLineY() - mEventMarginVertical

            // Calculate left and right.
            var left = startFromPixel + eventRect.left * mWidthPerDay
            if (left < startFromPixel) left += this.mOverlappingEventGap.toFloat()
            var right = left + eventRect.width * mWidthPerDay
            if (right < startFromPixel + mWidthPerDay) right -= this.mOverlappingEventGap.toFloat()

            // Draw the event and the event name on top of it.
            if (left < right && left < width && top < height && right > mHeaderColumnWidth && bottom > getFirstHourLineY()) {
                eventRect.rectF = RectF(left, top, right, bottom).also { rectF ->
                    canvas.drawRoundRect(
                        rectF,
                        eventCornerRadius.toFloat(),
                        eventCornerRadius.toFloat(),
                        mEventBackgroundPaint.withBgStyle(eventRect.originalEvent)
                    )
                    // 日程左侧装饰边界线
                    canvas.drawRect(
                        rectF.left,
                        rectF.top,
                        rectF.left + ConvertUtils.dp2px(2f),
                        rectF.bottom,
                        mEventBorderPaint.withBorderStyle(eventRect.originalEvent)
                    )
                    drawEventTitle(eventRect.event, rectF, canvas)
                }

                if (!preDraw) {
                    mTopEventRectY = mTopEventRectY.coerceAtMost(top)
                    mBottomEventRectY = mBottomEventRectY.coerceAtLeast(bottom)
                }
            } else eventRect.rectF = null
        }
        return hasEvent
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

        val availableHeight = (rect.bottom - rect.top - mEventVPadding * 2).toInt()
        val availableWidth = (rect.right - rect.left - mEventHPadding * 2).toInt()
        val onlyTitle = rect.height() < mHourHeight
        mEventTextPaint.withTextStyle(event)

        // Prepare the name of the event.
        val title = SpannableStringBuilder(event.name.replace("\\s+", " ")).apply {
            setSpan(
                AbsoluteSizeSpan(ConvertUtils.sp2px(14f)),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
        }
        // Prepare the location of the event.
        val location = SpannableStringBuilder(event.location.replace("[\n\r]", " ")).apply {
            setSpan(
                AbsoluteSizeSpan(ConvertUtils.sp2px(12f)),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        val allText = SpannableStringBuilder(title).apply {
            if (!onlyTitle) append("\n").append(location)
        }

        // Get text dimensions.
        val allTextLayout = obtainStaticLayout(allText, mEventTextPaint, availableWidth)
        val lineHeight = allTextLayout.height / allTextLayout.lineCount

        if (availableHeight >= lineHeight) {
            // Calculate available number of line counts.
            var availableLineCount = availableHeight / lineHeight

            var titleLayout = obtainStaticLayout(
                title, mEventTextPaint, availableWidth, TextUtils.TruncateAt.END,
                if (onlyTitle) 1 else availableLineCount - if (location.isNotEmpty()) 1 else 0
            )
            // 空余给地址的line
            availableLineCount -= titleLayout.lineCount

            val ellipsisCount = titleLayout.getEllipsisCount(titleLayout.lineCount - 1)
            if (ellipsisCount > 0) {
                val subTitle = title.subSequence(0, title.length - ellipsisCount)
                titleLayout = obtainStaticLayout(subTitle, mEventTextPaint, availableWidth)
            }
            // Draw text.
            canvas.save()
            canvas.translate(rect.left + mEventHPadding, rect.top + mEventVPadding)
            titleLayout.draw(canvas)
            canvas.restore()

            if (location.isNotEmpty() && availableLineCount > 0) {
                var locationLayout = obtainStaticLayout(
                    location, mEventTextPaint, availableWidth,
                    TextUtils.TruncateAt.END, availableLineCount
                )
                val locEllipsisCount = locationLayout.getEllipsisCount(locationLayout.lineCount - 1)
                if (locEllipsisCount > 0) {
                    val subLocation = location.subSequence(0, location.length - locEllipsisCount)
                    locationLayout =
                        obtainStaticLayout(subLocation, mEventTextPaint, availableWidth)
                }
                canvas.save()
                canvas.translate(
                    rect.left + mEventHPadding,
                    rect.top + mEventVPadding + titleLayout.height
                )
                locationLayout.draw(canvas)
                canvas.restore()
            }
        }
    }

    private fun checkSelectOverlapping(): Boolean {
        return false // TODO checkSelectOverlapping
    }

    private fun drawSelected(canvas: Canvas, overlapping: Boolean) {
        val rectF = mSelectedRectF ?: return
        rectF.top = adjustSelectLine(rectF.top)
        rectF.bottom = adjustSelectLine(rectF.bottom)
        if (mCreateView == null) {
            mCreateView = CreateEventView(context)
        }
        mCreateView?.apply {
            val showWidth = (rectF.right - rectF.left).toInt()
            val showHeight = (rectF.bottom - rectF.top).toInt()
            val width = MeasureSpec.makeMeasureSpec(showWidth, MeasureSpec.EXACTLY)
            val height = MeasureSpec.makeMeasureSpec(showHeight, MeasureSpec.EXACTLY)
            this.setWidthHeight(showWidth, showHeight)
            this.measure(width, height)
            this.layout(0, 0, showWidth, showHeight)
            this.setInfo(
                overlapping,
                true,
                mSelectType == SelectType.BOTTOM_BUBBLE || mSelectType == SelectType.TOP_BUBBLE || mSelectType == SelectType.MOVE,
                0
            )
            canvas.save()
            canvas.translate(rectF.left, rectF.top + getFirstHourLineY())
            this.draw(canvas)
            canvas.restore()
            drawSelectedBubble(canvas, overlapping)
        }
        drawSelectedTime(canvas, overlapping)
    }

    private fun adjustSelectLine(y: Float): Float {
        return if (mSelectType == SelectType.NONE || mSelectType == SelectType.CREATE) {
            // 按splitNodeCount调整位置（避免无级调节）
            val minHeight = mHourHeight * 1f / if (mSelectType == SelectType.CREATE) 2 else 4
            var count = floor((y / minHeight).toDouble()).toInt()
            val remainder = y % minHeight
            if (remainder >= minHeight / 2f) {
                count += 1
            }
            minHeight * count
        } else {
            y
        }
    }

    private fun drawSelectedBubble(canvas: Canvas, overlapping: Boolean) {
        val rectF = mSelectedRectF ?: return
        // 绘制下方圆点
        canvas.drawCircle(
            rectF.left + mSelectedBubbleMargin,
            rectF.bottom + getFirstHourLineY(), 10f,
            if (overlapping) mSelectConflictTextPaint else mSelectTextPaint
        )
        canvas.drawCircle(
            rectF.left + mSelectedBubbleMargin,
            rectF.bottom + getFirstHourLineY(), 7f, mDayBackgroundPaint
        )
        // 绘制上方圆点
        canvas.drawCircle(
            rectF.right - mSelectedBubbleMargin,
            rectF.top + getFirstHourLineY(), 10f,
            if (overlapping) mSelectConflictTextPaint else mSelectTextPaint
        )
        canvas.drawCircle(
            rectF.right - mSelectedBubbleMargin,
            rectF.top + getFirstHourLineY(), 7f, mDayBackgroundPaint
        )
    }

    private fun drawTimeColumnAndAxes(canvas: Canvas, overlapping: Boolean) {
        // Clip to paint in left column only.
        canvas.save()
        canvas.clipRect(0f, 0f, mHeaderColumnWidth, height.toFloat())
        canvas.drawRect(0f, 0f, mHeaderColumnWidth, height.toFloat(), mDayBackgroundPaint)
        for (i in 0..23) {
            val top = mHourHeight * i + mHeaderMarginBottom

            // Draw the text if its y position is not outside of the visible area. The pivot point of the text is the point at the bottom-right corner.
            val time = dateTimeInterpreter.interpretTime(i, 0)
            if (top < height) canvas.drawText(
                time,
                mHeaderColumnWidth - (mTimeTextWidth + mHeaderColumnPadding),
                top + mTimeTextHeight,
                mTimeTextPaint
            )
        }
        drawSelectedTime(canvas, overlapping)
        canvas.restore()
    }

    private fun drawSelectedTime(canvas: Canvas, overlapping: Boolean) {
        val rectF = mSelectedRectF ?: return
        val top = getTimeFromPoint(
            (rectF.left + rectF.right) / 2,
            rectF.top + getFirstHourLineY()
        )
        val bottom = getTimeFromPoint(
            (rectF.left + rectF.right) / 2,
            rectF.bottom + getFirstHourLineY()
        )
        if (top == null || bottom == null) return
        val topTime = dateTimeInterpreter.interpretTime(
            top[Calendar.HOUR_OF_DAY], top[Calendar.MINUTE]
        )
        val bottomTime = dateTimeInterpreter.interpretTime(
            bottom[Calendar.HOUR_OF_DAY], bottom[Calendar.MINUTE]
        )
        mSelectTextPaint.color = if (overlapping) -0x28bbd else -0xff4671
        // y = getFirstHourLineY() - mHeaderMarginBottom + mTimeTextHeight = mTimeTextHeight * 3 / 2f
        canvas.drawText(
            topTime, mHeaderColumnWidth - (mTimeTextWidth + mHeaderColumnPadding),
            rectF.top + mTimeTextHeight * 3 / 2f, mSelectTextPaint
        )
        canvas.drawText(
            bottomTime, mHeaderColumnWidth - (mTimeTextWidth + mHeaderColumnPadding),
            rectF.bottom + mTimeTextHeight * 3 / 2f, mSelectTextPaint
        )
    }

    /**
     * Get the time and date where the user clicked on.
     *
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     * @return The time and date at the clicked position.
     */
    private fun getTimeFromPoint(x: Float, y: Float): Calendar? {
        val leftDaysWithGaps = -(mCurrentOrigin.x / (mWidthPerDay + mColumnGap)).roundToInt()
        var startPixel = mCurrentOrigin.x + (mWidthPerDay + mColumnGap) * leftDaysWithGaps +
            mHeaderColumnWidth
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + 1
            + if (isOneVisibleDay()) 0 else mNumberOfVisibleDays
        ) {
            val start = startPixel.coerceAtLeast(mHeaderColumnWidth)
            if (mWidthPerDay + startPixel - start > 0 && x > start && x < startPixel + mWidthPerDay) {
                val day = mCurrentDate.apply {
                    add(Calendar.DATE, dayNumber - 1)
                    val pixelsFromZero = y - getFirstHourLineY()
                    val hour = (pixelsFromZero / mHourHeight).toInt()
                    val minute = (60 * (pixelsFromZero - hour * mHourHeight) / mHourHeight).toInt()
                    add(Calendar.HOUR, hour)
                    this[Calendar.MINUTE] = minute
                }
                return day
            }
            startPixel += mWidthPerDay + mColumnGap
        }
        return null
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
        // Iterate through each day with events to calculate the position of the events.
        while (tempEvents.size > 0) {
            val eventRects = ArrayList<EventRect>(tempEvents.size)
            // Get first event for a day.
            val eventRect1 = tempEvents.removeAt(0)
            eventRects.add(eventRect1)
            var i = 0
            while (i < tempEvents.size) {
                // Collect all other events for same day.
                val eventRect2 = tempEvents[i]
                if (isSameDay(eventRect1.event.startTime, eventRect2.event.startTime)) {
                    tempEvents.removeAt(i)
                    eventRects.add(eventRect2)
                } else {
                    i++
                }
            }
            computePositionOfEvents(eventRects)
        }
        invalidate()
    }

    /**
     * Cache the event for smooth scrolling functionality.
     *
     * @param event The event to cache.
     */
    private fun cacheEvent(event: WeekViewEvent) {
        if (event.startTime >= event.endTime) return
        event.splitWeekViewEvents().forEach {
            mEventRects.add(EventRect(it, event, null))
        }
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
        val columns = mutableListOf(mutableListOf<EventRect>())
        for (eventRect in collisionGroup) {
            var isPlaced = false
            for (column in columns) {
                if (column.size == 0) {
                    column.add(eventRect)
                    isPlaced = true
                } else if (!isEventsCollide(eventRect.event, column[column.size - 1].event)) {
                    column.add(eventRect)
                    isPlaced = true
                    break
                }
            }
            if (!isPlaced) {
                val newColumn = mutableListOf(eventRect)
                columns.add(newColumn)
            }
        }

        // Calculate left and right position for all the events.
        // Get the maxRowCount by looking in all columns.
        var maxRowCount = 0
        for (column in columns) {
            maxRowCount = maxRowCount.coerceAtLeast(column.size)
        }
        for (i in 0 until maxRowCount) {
            // Set the left and right values of the event.
            for ((j, column) in columns.withIndex()) {
                if (column.size >= i + 1) {
                    val eventRect = column[i]
                    eventRect.width = 1f / columns.size
                    eventRect.left = j * 1f / columns.size
                    if (!eventRect.event.isAllDay) {
                        eventRect.top =
                            (eventRect.event.startTime[Calendar.HOUR_OF_DAY] * 60 + eventRect.event.startTime[Calendar.MINUTE]).toFloat()
                        eventRect.bottom =
                            (eventRect.event.endTime[Calendar.HOUR_OF_DAY] * 60 + eventRect.event.endTime[Calendar.MINUTE]).toFloat()
                    }
                    mEventRects.add(eventRect)
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

    var emptyViewClickListener: EmptyViewClickListener? = null

    var emptyViewLongPressListener: EmptyViewLongPressListener? = null

    var scrollListener: ScrollListener? = null

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

    fun getHourHeight(): Int = mHourHeight

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

    fun getDayBackgroundColor(): Int = mDayBackgroundColor

    fun setDayBackgroundColor(dayBackgroundColor: Int) {
        mDayBackgroundColor = dayBackgroundColor
        mDayBackgroundPaint.color = mDayBackgroundColor
        invalidate()
    }

    fun getHourSeparatorColor(): Int = mHourSeparatorColor

    fun setHourSeparatorColor(hourSeparatorColor: Int) {
        mHourSeparatorColor = hourSeparatorColor
        mHourSeparatorPaint.color = mHourSeparatorColor
        invalidate()
    }

    fun getTodayBackgroundColor(): Int = mTodayBackgroundColor

    fun setTodayBackgroundColor(todayBackgroundColor: Int) {
        mTodayBackgroundColor = todayBackgroundColor
        mTodayBackgroundPaint.color = mTodayBackgroundColor
        invalidate()
    }

    fun getHourSeparatorHeight(): Int = mHourSeparatorHeight

    fun setHourSeparatorHeight(hourSeparatorHeight: Int) {
        mHourSeparatorHeight = hourSeparatorHeight
        mHourSeparatorPaint.strokeWidth = mHourSeparatorHeight.toFloat()
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

    fun getOverlappingEventGap(): Int = mOverlappingEventGap

    /**
     * Set the gap between overlapping events.
     *
     * @param overlappingEventGap The gap between overlapping events.
     */
    fun setOverlappingEventGap(overlappingEventGap: Int) {
        mOverlappingEventGap = overlappingEventGap
        invalidate()
    }

    /**
     * Get/Set corner radius for event rect. (in px)
     */
    var eventCornerRadius = 0

    fun getEventMarginVertical(): Int = mEventMarginVertical

    /**
     * Set the top and bottom margin of the event. The event will release this margin from the top
     * and bottom edge. This margin is useful for differentiation consecutive events.
     *
     * @param eventMarginVertical The top and bottom margin.
     */
    fun setEventMarginVertical(eventMarginVertical: Int) {
        this.mEventMarginVertical = eventMarginVertical
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
     * Get whether "now" line should be displayed. "Now" line is defined by the attributes
     * `nowLineColor` and `nowLineThickness`.
     *
     * @return True if "now" line should be displayed.
     */
    fun isShowNowLine(): Boolean = mShowNowLine

    /**
     * Set whether "now" line should be displayed. "Now" line is defined by the attributes
     * `nowLineColor` and `nowLineThickness`.
     *
     * @param showNowLine True if "now" line should be displayed.
     */
    fun setShowNowLine(showNowLine: Boolean) {
        this.mShowNowLine = showNowLine
        invalidate()
    }

    /**
     * Get the "now" line color.
     *
     * @return The color of the "now" line.
     */
    fun getNowLineColor(): Int = mNowLineColor

    /**
     * Set the "now" line color.
     *
     * @param nowLineColor The color of the "now" line.
     */
    fun setNowLineColor(nowLineColor: Int) {
        this.mNowLineColor = nowLineColor
        invalidate()
    }

    /**
     * Get the "now" line thickness.
     *
     * @return The thickness of the "now" line.
     */
    fun getNowLineThickness(): Int = mNowLineThickness

    /**
     * Set the "now" line thickness.
     *
     * @param nowLineThickness The thickness of the "now" line.
     */
    fun setNowLineThickness(nowLineThickness: Int) {
        mNowLineThickness = nowLineThickness
        invalidate()
    }

    /**
     * Get/Set whether the week view should fling horizontally.
     */
    var horizontalFlingEnabled = true

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

        if (event.action == MotionEvent.ACTION_CANCEL
            || event.action == MotionEvent.ACTION_UP
        ) {
            if (mSelectedRectF != null && mSelectType != SelectType.NONE && mSelectType != SelectType.CREATE) {
                if (mSelectType == SelectType.MOVE) {
                    reLocationSelectedRectF(event, false)
                }
                mSelectType = SelectType.NONE
                performPressVibrate(this)
                invalidate()
            }
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
            mScroller.startScroll(
                mCurrentOrigin.x.toInt(),
                0,
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
            ViewCompat.postInvalidateOnAnimation(this@WeekView)
        } else {
            mCurrentOrigin.x = newX
            invalidate()
        }
    }

    /**
     * Vertically scroll to a specific hour in the week view.
     *
     * @param hour The hour to scroll to in 24-hour format. Supported values are 0-24.
     */
    fun goToHour(hour: Double) {
        val container = getContainer() ?: return
        if (mAreDimensionsInvalid) {
            mScrollToHour = hour
            return
        }
        val verticalOffset = when {
            hour > 24 -> mHourHeight * 24
            hour > 0 -> (mHourHeight * hour).toInt()
            else -> 0
        }
        verticalOffset.coerceAtMost((getFirstHourLineY() + mHourHeight * 24 - container.height).toInt())
        container.smoothScrollTo(0, verticalOffset)
        invalidate()
    }

    /**
     * Get the first hour that is visible on the screen.
     *
     * @return The first hour that is visible.
     */
    fun getFirstVisibleHour(): Double = getContainer()?.run {
        (scrollY * 1f / mHourHeight).toDouble()
    } ?: 0.0

    /////////////////////////////////////////////////////////////////
    //
    //      Custom
    //
    /////////////////////////////////////////////////////////////////

    fun goToTopEventRect() {
        getContainer()?.smoothScrollTo(0, mTopEventRectY.toInt())
        invalidate()
    }

    fun goToBottomEventRect() {
        getContainer()?.run {
            smoothScrollTo(0, (mBottomEventRectY - this.height).toInt())
            invalidate()
        }
    }

    fun isTopEventRectVisible(): Boolean = getContainer()?.run {
        scrollY <= mTopEventRectY.toInt()
    } ?: false

    fun isBottomEventRectVisible(): Boolean = getContainer()?.run {
        scrollY >= (mBottomEventRectY - this.height).toInt()
    } ?: false

    private fun resetTopBottomEventRectY() {
        mTopEventRectY = Float.MAX_VALUE
        mBottomEventRectY = Float.MIN_VALUE
    }

    var cascadeScrollListener: CascadeScrollListener? = null

    fun setCurrentOriginX(currentOriginX: Float) {
        if (!isFloatEqual(currentOriginX, mCurrentOrigin.x)) {
            mCurrentOrigin.x = currentOriginX
            clearSelected(false)
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

    private fun getNowLineY(): Float {
        val startY = getFirstHourLineY()
        val now = Calendar.getInstance()
        val beforeNow = (now[Calendar.HOUR_OF_DAY] + now[Calendar.MINUTE] / 60.0f) * mHourHeight
        return startY + beforeNow
    }

    private fun getFirstHourLineY(): Float {
        return mHeaderMarginBottom + mTimeTextHeight / 2f
    }

    private fun getContainer(): WeekViewContainer? = parent as? WeekViewContainer

    private fun reLocationSelectedRectF(e: MotionEvent, create: Boolean) {
        val x = e.x - mHeaderColumnWidth
        val y = e.y - getFirstHourLineY()
        val hours = floor(y / mHourHeight).toInt().coerceAtLeast(0)
        var minNode = if (y % mHourHeight >= mHourHeight * 1f / 2) mHourHeight * 1f / 2 else 0f
        // 23:30-24:00与23:00-23:30区域点击效果相同
        if (hours >= 23 && minNode > 0) {
            minNode = 0f
        }
        val left =
            mHeaderColumnWidth + (mWidthPerDay + mColumnGap) * (x / (mWidthPerDay + mColumnGap)).toInt()
        val top = hours * mHourHeight + minNode + getFirstHourLineY()
        val right = left + mWidthPerDay
        val bottom = top + mHourHeight
        if (create) {
            mSelectedRectF = RectF(left, top, right, bottom)
            mSelectType = SelectType.CREATE
        } else {
            mSelectedRectF?.apply {
                this.left = left
                this.right = right
            }
        }
    }

    private fun clearSelected(invalidate: Boolean) {
        if (mSelectedRectF != null) {
            mSelectedRectF = null
            if (invalidate) invalidate()
        }
    }

    fun updateCurrentDate(currentDate: Calendar, invalidate: Boolean = false) {
        mCurrentDate = currentDate
        if (invalidate) invalidate()
    }

    private fun isOneVisibleDay() = mNumberOfVisibleDays == 1
}
