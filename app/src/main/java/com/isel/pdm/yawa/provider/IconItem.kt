package com.isel.pdm.yawa.provider

import android.content.ContentResolver
import android.net.Uri


object IconItem {

    val AUTHORITY = "com.isel.pdm.yawa.provider"

    val RESOURCE = "icon"

    val CONTENT_URI = Uri.parse("content://$AUTHORITY/$RESOURCE")

    val MEDIA_BASE_SUBTYPE = "/vnd.weathericon."

    val CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + MEDIA_BASE_SUBTYPE + RESOURCE

    val CONTENT_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + MEDIA_BASE_SUBTYPE + RESOURCE
}