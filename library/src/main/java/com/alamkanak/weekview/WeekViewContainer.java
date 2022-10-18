package com.alamkanak.weekview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

public class WeekViewContainer extends NestedScrollView {

    private boolean scrollable = true;
    private final int mScaledTouchSlop;
    private float startX;
    private float startY;

    public WeekViewContainer(@NonNull Context context) {
        this(context, null);
    }

    public WeekViewContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeekViewContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!scrollable) return false;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float distanceX = Math.abs(ev.getX() - startX);
                float distanceY = Math.abs(ev.getY() - startY);
                if (distanceX > mScaledTouchSlop && distanceX * 2f > distanceY) {
                    // 判断为横向滑动
                    return false;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    public void setScrollable(boolean enabled) {
        scrollable = enabled;
    }
}
