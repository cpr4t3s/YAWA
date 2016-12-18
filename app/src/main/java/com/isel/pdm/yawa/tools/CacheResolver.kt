package com.isel.pdm.yawa.tools

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.BaseColumns
import android.util.LruCache
import com.isel.pdm.yawa.provider.DbSchema
import com.isel.pdm.yawa.provider.IconCacheContract
import java.io.ByteArrayOutputStream


/**
 * 'cacheSize' unit is MB
 *
 * if 'cacheSize' == 0, use default size
 */
class CacheResolver<T>(cacheSize: Int, private val l2Cache: ContentResolver, private val uri: Uri): ICacheSystem<T> {
    companion object {
        val MEGABYTE_UNIT: Int = 1024
        // size in KB
        val DEFAULT_MEMORY_CACHE_SIZE: Int = 2 // 2KB
        //
        val FILE_EXTENTION = ".png"
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
        // TODO: desconmentar
        //putOnDb(item)
        //putOnFS(item)
    }

    private fun putOnDb(item: ICacheSystem.CacheEntry<T>) {
        val newValues: ContentValues = ContentValues()
        newValues.put(IconCacheContract.Icon.ICON_ID, item.key)
        newValues.put(IconCacheContract.Icon.EXPIRES_AT, item.expiresAt)
        l2Cache.insert(uri, newValues)
    }
    private fun putOnFS(item: ICacheSystem.CacheEntry<T>) {
        val fileUri = Uri.withAppendedPath(uri, "${item.key}$FILE_EXTENTION")
        val outStream = l2Cache.openOutputStream(fileUri, "w")

        val img = item.item as Bitmap
//        val size = img.byteCount
//
//        val buffer: ByteBuffer = ByteBuffer.allocate(size)
//        img.copyPixelsToBuffer(buffer)
//        outStream.write(buffer.array())
//        outStream.flush()
//
//        outStream.close()



        img.compress(Bitmap.CompressFormat.PNG, 100, outStream) // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
        outStream.flush() // Not really required
        outStream.close() // do not forget to close the stream
    }

    private fun getFromFs(key: String): T {
        val fileUri = Uri.withAppendedPath(uri, "$key$FILE_EXTENTION")
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


        //val img: Bitmap = BitmapFactory.decodeStream(inStream)
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
}