package com.isel.pdm.yawa.tools

import java.text.SimpleDateFormat
import java.util.*


object DateConverter {
    private val MILLI = 1000L

    fun unixSecondsToDateString(seconds: Long, timeZone: TimeZone, format: SimpleDateFormat): String {
        val date = Date(seconds * MILLI)
        val sdf = format
        sdf.timeZone = timeZone
        val formattedDate = sdf.format(date)

        return formattedDate
    }
}
