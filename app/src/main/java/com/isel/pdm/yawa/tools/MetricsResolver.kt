package com.isel.pdm.yawa.tools

object MetricsResolver {
    fun getMetricSymbol(metric: String): String {
        when(metric) {
            "metric" -> return "ºC"
            "imperial" -> return "K"
            "" -> return "F"
            else -> return "ºC"
        }
    }
}