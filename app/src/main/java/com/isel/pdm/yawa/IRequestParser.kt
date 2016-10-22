package com.isel.pdm.yawa

import com.android.volley.Request
import com.android.volley.toolbox.ImageLoader


interface IRequestParser {
    fun <T> addRequest(req: Request<T>)
    fun cancelRequest(requestTag: String)
    fun cancelAllRequests()
    fun getImgLoader(): ImageLoader
}