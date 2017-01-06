package com.isel.pdm.yawa.tools

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.BaseColumns
import android.util.LruCache
import com.isel.pdm.yawa.provider.DbSchema
import com.isel.pdm.yawa.provider.IconCacheContract
import com.isel.pdm.yawa.provider.WeatherContract
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream


/**
 * 'cacheSize' unit is MB
 *
 * if 'cacheSize' == 0, use default size
 */
class CacheResolver<T>(cacheSize: Int, private val l2Cache: ContentResolver, private val uri: Uri, val context: Context): ICacheSystem<T> {
    companion object {
        val MEGABYTE_UNIT: Int = 1024
        val DEFAULT_MEMORY_CACHE_SIZE: Int = 2
        //
        val CACHE_DIR_NAME = "icon"
        val FILE_EXTENTION = ".png"
    }

    init {
        // init dirs
        val path = File(context.cacheDir.toString(), CACHE_DIR_NAME)
        if(!path.exists())
            path.mkdirs()
    }


    private val l1Cache by lazy {
        val size = if (cacheSize == 0) DEFAULT_MEMORY_CACHE_SIZE else cacheSize
        LruCache<String, T>(size * MEGABYTE_UNIT) }


    override fun getItem(key: String): ICacheSystem.CacheEntry<T> {
        val item = l1Cache.get(key)
        // found on L1
        if(item != null) {
            return ICacheSystem.CacheEntry(key, item, 0, true)
        }
        // found on L2
        if(exists(key).size != 0) {
            return ICacheSystem.CacheEntry(key, getFromFs(key), 0, true)
        }

        // not found
        return ICacheSystem.CacheEntry("", item, 0, false)
    }

    override fun setItem(item: ICacheSystem.CacheEntry<T>) {
        // L1
        l1Cache.put(item.key, item.item)
        // L2
        if(exists(item.key).size != 0) return
        //
        if(putOnFS(item))
            putOnDb(item)
    }

    private fun putOnDb(item: ICacheSystem.CacheEntry<T>) {
        val newValues: ContentValues = ContentValues()
        newValues.put(IconCacheContract.Icon.ICON_ID, item.key)
        newValues.put(IconCacheContract.Icon.EXPIRES_AT, item.expiresAt)
        l2Cache.insert(uri, newValues)
    }
    private fun putOnFS(item: ICacheSystem.CacheEntry<T>): Boolean {
        val fileUri = Uri.withAppendedPath(uri, "${item.key}$FILE_EXTENTION")
        val outStream = l2Cache.openOutputStream(fileUri, "w")
        val img = item.item as Bitmap

        val success = img.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        outStream.flush()
        outStream.close()

        return success
    }

    private fun getFromFs(key: String): T {
        val fileUri = Uri.withAppendedPath(uri, "$key$FILE_EXTENTION")
        // Verify if the file still exist. If not, delete the DB entry for it
        val root = context.cacheDir
        val cacheFile = File(root, fileUri.encodedPath)
        if(!cacheFile.exists()) {
            removeItemEntry(key)
            throw FileNotFoundException()
        }

        val inStream = l2Cache.openInputStream(fileUri)

        val byteBuffer = ByteArrayOutputStream()
        // this is storage overwritten on each iteration with bytes
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        // we need to know how may bytes were read to write them to the byteBuffer
        var len: Int = 0
        var size: Int = 0

        while ({len = inStream.read(buffer); len}() != -1) {
            byteBuffer.write(buffer, 0, len)
            size += len
        }

        val img: Bitmap? = BitmapFactory.decodeByteArray(byteBuffer.toByteArray(), 0, size)

        return img as T
    }

    private fun exists(key: String): List<Int> {
        val existingIds: MutableList<Int> = mutableListOf<Int>()
        val projection = arrayOf<String>(
                BaseColumns._ID,
                IconCacheContract.Icon.ICON_ID
        )
        val selectionClause: String = "${IconCacheContract.Icon.ICON_ID} = ?"
        val selectionArgs = arrayOf(key)
        //
        val cursor = l2Cache.query(
                uri,
                projection,
                selectionClause,
                selectionArgs,
                null)

        while(cursor.moveToNext()) {
            existingIds.add(cursor.getInt(DbSchema.Icon.COLUMNS_ID.COL_ID.ordinal))
        }
        cursor.close()

        return existingIds
    }

    private fun removeItemEntry(key: String) {
        val whereClause = "${IconCacheContract.Icon.ICON_ID} = ?"
        val selectionArgs = arrayOf(key)
        l2Cache.delete(IconCacheContract.Icon.CONTENT_URI, whereClause, selectionArgs)
    }
}