package com.alamkanak.weekview;

import androidx.annotation.NonNull;

import java.util.Calendar;

public interface CascadeScrollListener {
    void onScrolling(float currentOriginX);

    void onScrollEnd(@NonNull Calendar newFirstVisibleDay);
}
