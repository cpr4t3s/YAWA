package com.isel.pdm.yawa.openweather_tools


import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley
import com.isel.pdm.yawa.IRequestParser

class OpenWeatherRequester constructor(context: Context) : IRequestParser {
    val requestQueue: RequestQueue
    val imageLoader: ImageLoader

    init {
        this.requestQueue = Volley.newRequestQueue(context.applicationContext)

        imageLoader = ImageLoader(requestQueue,
                object : ImageLoader.ImageCache {
                    private val cache = LruCache<String, Bitmap>(10)

                    override fun getBitmap(url: String): Bitmap? {
                        return cache.get(url)
                    }

                    override fun putBitmap(url: String, bitmap: Bitmap) {
                        cache.put(url, bitmap)
                    }
                })
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
