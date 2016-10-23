package com.isel.pdm.yawa.UI

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.isel.pdm.yawa.IWeatherManager
import com.isel.pdm.yawa.R
import java.util.*


class ForecastListAdapter (activity: Activity,
                           val data: ArrayList<Map<String, Any?>>): BaseAdapter() {

    private val inflater: LayoutInflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return data.size
    }

    // TODO: deve estar mal
    override fun getItem(position: Int): Any {
        return data[position]
    }

    /**
     * Returns the self index of the element
     */
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var vi = convertView
        if(convertView == null)
            vi = inflater.inflate(R.layout.forecast_row_layout, null)

        val maxTemP = vi?.findViewById(R.id.forecast_max_temp) as TextView // title
        val minTemp = vi?.findViewById(R.id.forecast_min_temp) as TextView // artist name
        val date = vi?.findViewById(R.id.forecast_date) as TextView // duration
        val thumb_image = vi?.findViewById(R.id.forecast_row_image) as ImageView // thumb image

        val weather: Map<String, Any?>
        weather = data[position]

        // Setting all values in listview
        maxTemP.text = weather[IWeatherManager.MAX_TEMP_KEY] as String
        minTemp.text = weather[IWeatherManager.MIN_TEMP_KEY] as String
        date.text = weather[IWeatherManager.DATE_KEY] as String
        weather[IWeatherManager.WEATHER_ICON_KEY]?.let {
            thumb_image.setImageBitmap(weather[IWeatherManager.WEATHER_ICON_KEY] as Bitmap)
        }

        return vi
    }
}

