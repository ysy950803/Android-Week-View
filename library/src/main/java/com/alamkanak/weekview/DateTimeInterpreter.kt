package com.alamkanak.weekview

import java.util.Calendar

interface DateTimeInterpreter {
    fun interpretDate(date: Calendar): List<String>
    fun interpretTime(hour: Int, minute: Int): String
}
