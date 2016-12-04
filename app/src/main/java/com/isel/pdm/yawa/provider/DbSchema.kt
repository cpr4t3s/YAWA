package com.isel.pdm.yawa.provider

import android.provider.BaseColumns


object DbSchema {

    val DB_NAME = "weather.db"
    val DB_VERSION = 1

    val COL_ID = BaseColumns._ID

    object Weather {
        val TBL_NAME = "weather"

        val COL_CITY_ID = "cityId"
        val COL_CURRENT = "current"
        val COL_LAST_UPDATE = "lastUpdate"
        val COL_DESCRIPTION = "description"
        val COL_TEMPERATURE = "temp"
        val COL_TEMPERATURE_MAX = "temp_max"
        val COL_TEMPERATURE_MIN = "temp_min"

        val DDL_CREATE_TABLE =
                "CREATE TABLE " + TBL_NAME + "(" +
                        COL_ID + " INTEGER PRIMARY KEY, " +
                        COL_CITY_ID + " TEXT, " +
                        COL_CURRENT + " INTEGER, " +
                        COL_LAST_UPDATE + " TEXT, " +
                        COL_DESCRIPTION + " TEXT, " +
                        COL_TEMPERATURE + " REAL, " +
                        COL_TEMPERATURE_MAX + " REAL, " +
                        COL_TEMPERATURE_MIN + " REAL)"

        val DDL_DROP_TABLE = "DROP TABLE IF EXISTS " + TBL_NAME
    }
}
