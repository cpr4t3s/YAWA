package com.isel.pdm.yawa

import org.json.JSONArray
import org.json.JSONObject

operator fun JSONArray.iterator() =
        (0 until length()).asSequence().map { idx -> get(idx) as JSONObject }.iterator()