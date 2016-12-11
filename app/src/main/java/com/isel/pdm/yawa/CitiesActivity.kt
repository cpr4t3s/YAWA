package com.isel.pdm.yawa

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.*
import com.isel.pdm.yawa.fragments.CitiesListFragment
import com.isel.pdm.yawa.service.WeatherService

class CitiesActivity : AppCompatActivity() {
    private val searchButton by lazy { findViewById(R.id.btnSearchCity) as ImageButton }
    private val listFragment by lazy { fragmentManager.findFragmentById(R.id.cities_list_frag) as CitiesListFragment }
    private var forAction: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        forAction = intent.action

        setContentView(R.layout.activity_cities)
        // Search Button
        searchButton.setOnClickListener {
            listFragment.doSearch()
        }

    }

    override fun onStop() {
        super.onStop()

        // on activity's state changes we need to cancel possible requests
        application.weatherManager.cancelAllRequests()
    }

    override fun onSaveInstanceState(outState : Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CitiesListFragment.SEARCH_TEXT_KEY, listFragment.txtSearchStr.text.toString())
    }

    override fun onRestoreInstanceState(restoredState : Bundle) {
        super.onRestoreInstanceState(restoredState)
        listFragment.txtSearchStr.setText(restoredState.getString(CitiesListFragment.SEARCH_TEXT_KEY))
    }

    private fun updateCurrentWeather() {
        val updateWeatherIntent: Intent = Intent(this, WeatherService::class.java)
        updateWeatherIntent.action = YAWA.UPDATE_CURRENT_WEATHER_ACTION
        startService(updateWeatherIntent)
    }

    fun onFragmentListViewItemClicked(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
        when(forAction) {
            YAWA.SEARCH_LOCATION_ACTION -> { setNewCity(adapterView, position) }
            YAWA.ADD_NEW_LOCATION_ACTION -> {
                addNewCity(adapterView, position)
                setNewCity(adapterView, position)
            }
        }

        //
        onBackPressed()
    }


    /**
     * just configure a new city as active
     */
    private fun setNewCity(adapterView: AdapterView<*>, position: Int) {
        val cityName
                : String = (adapterView.getItemAtPosition(position)
                as Map<*, *>)["city"].toString()
        val country
                : String = (adapterView.getItemAtPosition(position)
                as Map<*, *>)["country"].toString()

        // set the new city on Shared Preferences
        selectCity(cityName, country)
        // updates the weather for the new city
        updateCurrentWeather()
    }

    /**
     * Configure the new city with Shared Preferences
     */
    private fun selectCity(cityName: String, country: String) {
        val settingsLocationStr = resources.getString(R.string.settings_location_str)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPref.edit()

        val str: String =cityName + "," + country
        editor.putString(settingsLocationStr, str)
        editor.apply()
    }

    /**
     * Add a new city to our favorites and set it as active
     */
    private fun addNewCity(adapterView: AdapterView<*>, position: Int) {
        val cityName
                : String = (adapterView.getItemAtPosition(position)
                as Map<*, *>)["city"].toString()
        val country
                : String = (adapterView.getItemAtPosition(position)
                as Map<*, *>)["country"].toString()

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPref.edit()
        val newCityDesc: String =cityName + "," + country
        // contador das cidades configuradas actualmente
        var citiesCounter = sharedPref.getInt("citiesCounter", 0)

        // check for duplicated cities
        for (i in 0 until citiesCounter) {
            val city = sharedPref.getString("city" + i, "--")
            if(city == newCityDesc) return
        }

        // save the new city
        editor.putString("city" + citiesCounter++, newCityDesc)
        // update cities counter
        editor.putInt("citiesCounter", citiesCounter)

        editor.apply()
    }

}
