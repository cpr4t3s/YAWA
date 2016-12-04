package com.isel.pdm.yawa.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.text.TextUtils


class WeatherProvider : ContentProvider() {

    private val WEATHER_TABLE = 1
    private val WEATHER_ROW = 2

    private val URI_MATCHER: UriMatcher

    init {
        URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH)

        URI_MATCHER.addURI(
                WeatherContract.AUTHORITY,
                WeatherContract.Weather.RESOURCE,
                WEATHER_TABLE)
        URI_MATCHER.addURI(
                WeatherContract.AUTHORITY,
                WeatherContract.Weather.RESOURCE + "/#",
                WEATHER_ROW)
    }

    private var dbHelper: DbWeatherHelper? = null

    override fun onCreate(): Boolean {
        dbHelper = DbWeatherHelper(context)
        return true
    }

    override fun getType(uri: Uri?): String {
        when (URI_MATCHER.match(uri)) {
            WEATHER_TABLE -> return WeatherContract.Weather.CONTENT_TYPE
            WEATHER_ROW -> return WeatherContract.Weather.CONTENT_ITEM_TYPE
            else -> throw badUri(uri!!)
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        var sortOrder = sortOrder

        val qbuilder = SQLiteQueryBuilder()
        when (URI_MATCHER.match(uri)) {
            WEATHER_TABLE -> {
                qbuilder.tables = DbSchema.Weather.TBL_NAME
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = WeatherContract.Weather.DEFAULT_SORT_ORDER
                }
            }
//            CURRENT_WEATHER -> {
//                qbuilder.tables = DbSchema.Weather.TBL_NAME
//                qbuilder.appendWhere(DbSchema.COL_ID + "=" + uri.lastPathSegment)
//            }
            else -> badUri(uri)
        }

        val db = dbHelper!!.readableDatabase
        val cursor = qbuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder)
        cursor.setNotificationUri(context.contentResolver, uri)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val table: String
        when (URI_MATCHER.match(uri)) {
            WEATHER_TABLE -> table = DbSchema.Weather.TBL_NAME
            else -> throw badUri(uri)
        }

        val db = dbHelper!!.writableDatabase
        val newId = db.insert(table, null, values)

        context.contentResolver.notifyChange(uri, null)
        return ContentUris.withAppendedId(uri, newId)
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        val table: String
        when (URI_MATCHER.match((uri))) {
            WEATHER_TABLE -> table = DbSchema.Weather.TBL_NAME
            else -> throw badUri(uri)
        }

        val db = dbHelper!!.writableDatabase
        val affectedRows = db.update(table, values, selection, selectionArgs)

        context.contentResolver.notifyChange(uri, null)
        return affectedRows
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val table: String
        when (URI_MATCHER.match(uri)) {
            WEATHER_TABLE -> {
                table = DbSchema.Weather.TBL_NAME
                if (selection != null) {
                    throw IllegalArgumentException("selection not supported")
                }
            }
            else -> throw badUri(uri)
        }

        val db = dbHelper!!.writableDatabase
        val ndel = db.delete(table, null, null)

        context.contentResolver.notifyChange(uri, null)
        return ndel
    }

    private fun badUri(uri: Uri): Exception {
        throw IllegalArgumentException("Unsupported URI: " + uri)
    }
}