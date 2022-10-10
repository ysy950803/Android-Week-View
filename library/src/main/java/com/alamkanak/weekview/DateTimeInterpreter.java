package com.alamkanak.weekview;

import java.util.Calendar;
import java.util.List;

/**
 * Created by Raquib on 1/6/2015.
 */
public interface DateTimeInterpreter {
    List<String> interpretDate(Calendar date);

    String interpretTime(int hour);
}
