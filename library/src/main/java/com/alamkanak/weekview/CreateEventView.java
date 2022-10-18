package com.alamkanak.weekview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class CreateEventView extends LinearLayout {

    private TextView createTextView;
    private LinearLayout createBg;
    private int width = -1;
    private int height = -1;
    public boolean showCreateText = true;
    public boolean conflict = true;

    public CreateEventView(Context context) {
        super(context);
        init();
    }

    public CreateEventView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CreateEventView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CreateEventView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void init() {
        View.inflate(getContext(), R.layout.select_layout, this);
        createTextView = findViewById(R.id.create_text);
        createBg = findViewById(R.id.create_bg);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (width != -1 && height != -1) {
            setMeasuredDimension(width, height);
        }
    }

    public void setWidthHeight(int width, int height) {
        this.width = width;
        this.height = height;
        requestLayout();
    }

    public void calculate() {
        if ((getHeight() / createTextView.getLineHeight()) > 1) {
            createTextView.setVisibility(VISIBLE);
        } else {
            createTextView.setVisibility(GONE);
        }
    }

    public void setCreateBg(int resId) {
        createBg.setBackgroundResource(resId);
    }

    public boolean isConflict() {
        return conflict;
    }

    public void setInfo(boolean conflict, boolean showCreateText, boolean touching, int textType) {
        this.conflict = conflict;
        this.showCreateText = showCreateText;
        createTextView.setVisibility(VISIBLE);
        if (this.conflict) {
            createTextView.setText("该时段有人冲突");
            createTextView.setTextColor(0xFFFD7443);
            setCreateBg(touching ? R.drawable.create_event_bg_red_m : R.drawable.create_event_bg_red);
        } else {
            String str = textType == 0 ? "点击新建" : "所有人都有时间";
            createTextView.setText(showCreateText ? str : "");
            createTextView.setTextColor(0xFF00B38B);
            setCreateBg(touching ? R.drawable.create_event_bg_m : R.drawable.create_event_bg);
        }
    }
}
