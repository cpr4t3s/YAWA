package com.isel.pdm.yawa.provider

import android.provider.BaseColumns


object DbSchema {

    val DB_NAME = "weather.db"
    val DB_VERSION = 2

    val COL_ID = BaseColumns._ID

    object Weather {
        val TBL_NAME = "weather"

        /**
         * It MUST be in the same order of 'DDL_CREATE_TABLE'
         */
        enum class COLUMNS_ID {
            COL_ID,
            COL_CITY_ID,
            COL_CURRENT,
            COL_LAST_UPDATE,
            COL_FORECAST_DATE,
            COL_DESCRIPTION,
            COL_TEMPERATURE,
            COL_TEMPERATURE_MAX,
            COL_TEMPERATURE_MIN,
            COL_MAIN_STATE,
            COL_ICON_ID,
            COL_PRESSURE,
            COL_SEA_LEVEL,
            COL_GROUND_LEVEL,
            COL_HUMIDITY,
            COL_WIND_SPEED,
            COL_WIND_DEGREES,
        }

        val COL_CITY_ID = "cityId"
        val COL_CURRENT = "current"
        val COL_LAST_UPDATE = "lastUpdate"
        val COL_FORECAST_DATE = "forecastDate"
        val COL_DESCRIPTION = "description"
        val COL_TEMPERATURE = "temp"
        val COL_TEMPERATURE_MAX = "temp_max"
        val COL_TEMPERATURE_MIN = "temp_min"
        val COL_MAIN_STATE = "mainState"
        val COL_ICON_ID = "iconId"
        val COL_PRESSURE = "pressure"
        val COL_SEA_LEVEL = "seaLevel"
        val COL_GROUND_LEVEL = "groundLevel"
        val COL_HUMIDITY = "humidity"
        val COL_WIND_SPEED = "windSpeed"
        val COL_WIND_DEGREES = "windDegrees"

        /**
         * It MUST be in the same order of 'COLUMNS_ID'
         */
        val DDL_CREATE_TABLE =
                "CREATE TABLE " + TBL_NAME + "(" +
                        COL_ID + " INTEGER PRIMARY KEY, " +
                        COL_CITY_ID + " TEXT, " +
                        COL_CURRENT + " INTEGER, " +
                        COL_LAST_UPDATE + " TEXT, " +
                        COL_FORECAST_DATE + " TEXT, " +
                        COL_DESCRIPTION + " TEXT, " +
                        COL_TEMPERATURE + " REAL, " +
                        COL_TEMPERATURE_MAX + " REAL, " +
                        COL_TEMPERATURE_MIN + " REAL, " +
                        COL_MAIN_STATE + " TEXT, " +
                        COL_ICON_ID + " TEXT, " +
                        COL_PRESSURE + " REAL, " +
                        COL_SEA_LEVEL + " REAL, " +
                        COL_GROUND_LEVEL + " REAL, " +
                        COL_HUMIDITY + " REAL, " +
                        COL_WIND_SPEED + " REAL, " +
                        COL_WIND_DEGREES + " REAL)"

        val DDL_DROP_TABLE = "DROP TABLE IF EXISTS " + TBL_NAME
    }
}
