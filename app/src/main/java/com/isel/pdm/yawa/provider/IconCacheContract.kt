package com.isel.pdm.yawa.provider

import android.content.ContentResolver
import android.net.Uri
import android.provider.BaseColumns


object IconCacheContract {
    val AUTHORITY = "com.isel.pdm.yawa.provider"
    val CONTENT_URI = Uri.parse("content://$AUTHORITY")
    val MEDIA_BASE_SUBTYPE = "/vnd.iconcache."

    object Icon: BaseColumns {
        val RESOURSE = "icon"
        val CONTENT_URI = Uri.withAppendedPath(IconCacheContract.CONTENT_URI, RESOURSE)
        val CONTENT_TYPE = """${ContentResolver.CURSOR_DIR_BASE_TYPE}${IconCacheContract.MEDIA_BASE_SUBTYPE}$CONTENT_URI"""
        val CONTENT_ITEM_TYPE = """${ContentResolver.CURSOR_ITEM_BASE_TYPE}${IconCacheContract.MEDIA_BASE_SUBTYPE}$CONTENT_URI"""

        val ICON_ID = "iconId"
        val EXPIRES_AT = "expiresAt"

        val SELECT_ALL = arrayOf(
                BaseColumns._ID,
                ICON_ID,
                EXPIRES_AT)

        val DEFAULT_SORT_ORDER = DbSchema.COL_ID + " ASC"
    }
}