package com.isel.pdm.yawa.openweather_tools


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley
import com.isel.pdm.yawa.IRequestParser
import com.isel.pdm.yawa.R
import com.isel.pdm.yawa.tools.ICacheSystem
import java.io.FileNotFoundException

class OpenWeatherRequester constructor(context: Context, val cache: ICacheSystem<Bitmap>) : IRequestParser {
    val requestQueue: RequestQueue
    val imageLoader: ImageLoader

    init {
        this.requestQueue = Volley.newRequestQueue(context.applicationContext)

        imageLoader = ImageLoader(requestQueue,
                object : ImageLoader.ImageCache {
                    override fun getBitmap(url: String): Bitmap? {
                        try {
                            return cache.getItem(convertIconUrlToIconId(url)).item
                        } catch (e: FileNotFoundException) {
                            return BitmapFactory.decodeResource(context.resources, R.drawable.icon_not_available)
                        }
                    }

                    override fun putBitmap(url: String, bitmap: Bitmap) {
                        cache.setItem(ICacheSystem.CacheEntry(convertIconUrlToIconId(url), bitmap, 0))
                    }
                })
    }

    private fun convertIconUrlToIconId(url: String): String {
        val uri = Uri.parse(url)
        return uri.lastPathSegment.split(".")[0]
    }

    override fun <T> addRequest(req: Request<T>) {
        requestQueue.add(req)
    }

    override fun cancelRequest(requestTag: String) {
        requestQueue.cancelAll(requestTag)
    }

    override fun cancelAllRequests() {
        requestQueue.cancelAll(RequestQueue.RequestFilter { true })
    }

    override fun getImgLoader() : ImageLoader {
        return this.imageLoader
    }
}
