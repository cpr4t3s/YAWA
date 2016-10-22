package com.isel.pdm.yawa

import com.android.volley.VolleyError

interface ICallbackSet {
    fun onError(error: VolleyError)
    fun onSucceed(response: Any)
}
