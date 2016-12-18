package com.isel.pdm.yawa.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


class WeatherProvider : ContentProvider() {

    private val WEATHER_TABLE = 1
    private val WEATHER_ROW = 2
    //
    private val CACHE_TABLE = 4
    private val CACHE_ENTRY = 8

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
        // Icons Cache
        URI_MATCHER.addURI(
                IconCacheContract.AUTHORITY,
                IconCacheContract.Icon.RESOURSE,
                CACHE_TABLE
        )
        URI_MATCHER.addURI(
                IconCacheContract.AUTHORITY,
                "${IconCacheContract.Icon.RESOURSE}/#",
                CACHE_ENTRY
        )

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
            CACHE_TABLE -> return IconCacheContract.Icon.CONTENT_TYPE
            CACHE_ENTRY -> return IconCacheContract.Icon.CONTENT_ITEM_TYPE
            else -> throw badUri(uri!!)
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        var sort = sortOrder

        val qbuilder = SQLiteQueryBuilder()
        when (URI_MATCHER.match(uri)) {
            WEATHER_TABLE -> {
                qbuilder.tables = DbSchema.Weather.TBL_NAME
                if (TextUtils.isEmpty(sort)) {
                    sort = WeatherContract.Weather.DEFAULT_SORT_ORDER
                }
            }
            CACHE_TABLE -> {
                qbuilder.tables = DbSchema.Icon.TBL_NAME
                if (TextUtils.isEmpty(sort)) {
                    sort = IconCacheContract.Icon.DEFAULT_SORT_ORDER
                }
            }
            CACHE_ENTRY -> {
                qbuilder.tables = DbSchema.Icon.TBL_NAME
                if (!TextUtils.isEmpty(sort)) {
                    throw IllegalArgumentException("sort not supported")
                }
                sort = null
                qbuilder.appendWhere(DbSchema.COL_ID + "=" + uri.lastPathSegment)
            }
            else -> badUri(uri)
        }

        val db = dbHelper!!.readableDatabase
        val cursor = qbuilder.query(db, projection, selection, selectionArgs, null, null, sort)
        cursor.setNotificationUri(context.contentResolver, uri)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val table: String
        when (URI_MATCHER.match(uri)) {
            WEATHER_TABLE -> table = DbSchema.Weather.TBL_NAME
            CACHE_TABLE -> table = DbSchema.Icon.TBL_NAME
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
            CACHE_TABLE -> table = DbSchema.Icon.TBL_NAME
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
            WEATHER_TABLE -> table = DbSchema.Weather.TBL_NAME
            CACHE_TABLE -> table = DbSchema.Icon.TBL_NAME
            else -> throw badUri(uri)
        }

        val db = dbHelper!!.writableDatabase
        val ndel = db.delete(table, null, null)

        context.contentResolver.notifyChange(uri, null)
        return ndel
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
//        val root = context.cacheDir
//        val file = File(root, uri.encodedPath)
//        //root.mkdirs()
//        //val file = File(path, uri.lastPathSegment)
        val root = context.cacheDir
        val path = File(root, uri.getEncodedPath())
        path.mkdirs()
        val file = File(path, "file_"+uri.getLastPathSegment())

        var imode = 0
        if (mode.contains("w")) {
            imode = imode or ParcelFileDescriptor.MODE_WRITE_ONLY
            if (!file.exists()) {
                try {
                    file.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            else {
                try {
                    file.delete()
                    file.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        if (mode.contains("r"))
            imode = imode or ParcelFileDescriptor.MODE_READ_ONLY
        if (mode.contains("+"))
            imode = imode or ParcelFileDescriptor.MODE_APPEND

        return ParcelFileDescriptor.open(file, imode)
    }

    private fun badUri(uri: Uri): Exception {
        throw IllegalArgumentException("Unsupported URI: " + uri)
    }
}