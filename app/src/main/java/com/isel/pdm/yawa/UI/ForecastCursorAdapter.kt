package com.isel.pdm.yawa.UI

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.TextView
import com.android.volley.VolleyError
import com.isel.pdm.yawa.ICallbackSet
import com.isel.pdm.yawa.R
import com.isel.pdm.yawa.WeatherManager
import com.isel.pdm.yawa.YAWA
import com.isel.pdm.yawa.provider.DbSchema
import com.isel.pdm.yawa.tools.DateConverter
import java.text.SimpleDateFormat
import java.util.*


class ForecastCursorAdapter(activity: Activity, cursor: Cursor?,
                            flags: Int, val imageGetter: WeatherManager): CursorAdapter(activity, cursor, flags) {

    companion object {
        val TAG: String = ForecastCursorAdapter::class.java.simpleName
    }

    private val cursorInflater: LayoutInflater by lazy {
        activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater }


    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return cursorInflater.inflate(R.layout.forecast_row_layout, parent, false)
    }

    override fun bindView(parent: View?, context: Context?, cursor: Cursor?) {
        val maxTemp = parent?.findViewById(R.id.forecast_max_temp) as TextView // title
        val minTemp = parent?.findViewById(R.id.forecast_min_temp) as TextView // artist name
        val dateTextView = parent?.findViewById(R.id.forecast_date) as TextView // duration
        val thumb_image = parent?.findViewById(R.id.forecast_row_image) as ImageView // thumb image

        maxTemp.text = cursor?.getString(DbSchema.Weather.COLUMNS_ID.COL_TEMPERATURE_MAX.ordinal)
        minTemp.text = cursor?.getString(DbSchema.Weather.COLUMNS_ID.COL_TEMPERATURE_MIN.ordinal)
        val forecastDate = cursor?.getLong(DbSchema.Weather.COLUMNS_ID.COL_FORECAST_DATE.ordinal)
        val date = DateConverter.unixSecondsToDateString(forecastDate!!,
                TimeZone.getDefault(), SimpleDateFormat("yyyy-MM-dd, E"))
        dateTextView.text = date
        // TODO: fazer quando estiver definido como sao as caches
        val iconId = cursor?.getString(DbSchema.Weather.COLUMNS_ID.COL_ICON_ID.ordinal)
        imageGetter.getWeatherIcon(iconId!!, object :ICallbackSet {
            val imageView: ImageView = thumb_image

            override fun onError(error: VolleyError) {
                Log.e(TAG, error.message)
            }
            override fun onSucceed(response: Any?) {
                Log.e(TAG, "fora: ${response.toString()}")
                if(response != null) {
                    Log.e(TAG, "dentro: ${response.toString()}")
                    val bitmap: Bitmap? = response as Bitmap
                    imageView.setImageBitmap(bitmap)
                }
            }
        })
//        weather[IWeatherManager.WEATHER_ICON_KEY]?.let {
//            thumb_image.setImageBitmap(weather[IWeatherManager.WEATHER_ICON_KEY] as Bitmap)
//        }
    }
}

