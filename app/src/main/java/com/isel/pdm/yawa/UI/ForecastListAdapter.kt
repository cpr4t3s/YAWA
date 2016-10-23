package com.isel.pdm.yawa.UI

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.android.volley.VolleyError
import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import com.isel.pdm.yawa.ICallbackSet
import com.isel.pdm.yawa.IWeatherManager
import com.isel.pdm.yawa.R
import java.util.*


class ForecastListAdapter (activity: Activity,
                           val data: ArrayList<Map<String, String>>,
                           val imgLoader: IWeatherManager): BaseAdapter() {

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

        val weather: Map<String, String>
        weather = data[position]

        // Setting all values in listview
        maxTemP.text = weather[IWeatherManager.MAX_TEMP_KEY]
        minTemp.text = weather[IWeatherManager.MIN_TEMP_KEY]
        date.text = weather[IWeatherManager.DATE_KEY]
        imgLoader.getWeatherIcon(weather[IWeatherManager.WEATHER_ICON_KEY]!!,
                object: ICallbackSet{
                    override fun onError(error: VolleyError) {
                        // TODO: colocar uma imagem de erro
                        println("################################### On Error")
                    }

                    override fun onSucceed(response: Any) {
                        val weather = response as WeatherStateDO
                        thumb_image.setImageBitmap(weather.weatherIcon)
                    }
                }
        )

        return vi
    }
}

