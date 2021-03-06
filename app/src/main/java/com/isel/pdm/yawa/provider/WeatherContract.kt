package com.isel.pdm.yawa.provider

import android.content.ContentResolver
import android.net.Uri
import android.provider.BaseColumns


object WeatherContract {

    val AUTHORITY = "com.isel.pdm.yawa.provider"

    val CONTENT_URI = Uri.parse("content://$AUTHORITY")

    val MEDIA_BASE_SUBTYPE = "/vnd.weather."

    object Weather : BaseColumns {
        val RESOURCE = "weather"

        val CONTENT_URI = Uri.withAppendedPath(
                WeatherContract.CONTENT_URI,
                RESOURCE)

        val CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE +
                        MEDIA_BASE_SUBTYPE + RESOURCE

        val CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE +
                        MEDIA_BASE_SUBTYPE + RESOURCE


        val CITY_ID = "cityId"
        val CURRENT = "current"
        val LAST_UPDATE = "lastUpdate"
        val FORECAST_DATE = "forecastDate"
        val DESCRIPTION = "description"
        val TEMPERATURE = "temp"
        val TEMPERATURE_MAX = "temp_max"
        val TEMPERATURE_MIN = "temp_min"
        val MAIN_STATE = "mainState"
        val ICON_ID = "iconId"
        val PRESSURE = "pressure"
        val SEA_LEVEL = "seaLevel"
        val GROUND_LEVEL = "groundLevel"
        val HUMIDITY = "humidity"
        val WIND_SPEED = "windSpeed"
        val WIND_DEGREES = "windDegrees"

        val SELECT_ALL = arrayOf(
                DbSchema.COL_ID,
                CITY_ID,
                CURRENT,
                LAST_UPDATE,
                FORECAST_DATE,
                DESCRIPTION,
                TEMPERATURE,
                TEMPERATURE_MAX,
                TEMPERATURE_MIN,
                MAIN_STATE,
                ICON_ID,
                PRESSURE,
                SEA_LEVEL,
                GROUND_LEVEL,
                HUMIDITY,
                WIND_SPEED,
                WIND_DEGREES)

        val DEFAULT_SORT_ORDER = DbSchema.COL_ID + " ASC"
    }

}