package com.isel.pdm.yawa.fragments

import android.app.ListFragment
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.VolleyError
import com.isel.pdm.yawa.*
import com.isel.pdm.yawa.DataContainers.CityDO
import com.isel.pdm.yawa.service.WeatherService
import java.io.Serializable
import java.net.URLEncoder
import java.util.*


class CitiesListFragment : ListFragment() {
    companion object {
        val SEARCH_TEXT_KEY : String = "searchTextKey"
        val CITIES_LIST_TAG: String = "citieslisttag"
    }

    // List of CityDOs returned by the search. Used to pass the weather to weatherManager
    private var citiesList : List<CityDO>? = null

    private var searching: Boolean = false
    val txtSearchStr by lazy{ activity.findViewById(R.id.searchText) as EditText }
    private val spinner by lazy { activity.findViewById(R.id.spinner) as ProgressBar }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.cities_list_fragment, container, false)

    }

    override fun onActivityCreated (savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // hide spinner
        spinner.visibility = View.GONE

        val listView = activity.findViewById(android.R.id.list) as ListView
        listView.setOnItemClickListener { adapterView, view, position, id ->
            onListViewItemClicked(adapterView, view, position, id)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        if(outState != null && citiesList != null) {
            outState.putSerializable(CITIES_LIST_TAG, citiesList as Serializable)
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            citiesList = savedInstanceState.getSerializable(CITIES_LIST_TAG) as List<CityDO>?

            citiesList?.let {
                listView.adapter = SimpleAdapter(activity,
                        buildListViewDataSet(citiesList!!),
                        android.R.layout.simple_list_item_2,
                        arrayOf("city", "country"),
                        intArrayOf(android.R.id.text1, android.R.id.text2))
            }
        }
    }

    private fun updateCurrentWeather() {
        val updateWeatherIntent: Intent = Intent(activity, WeatherService::class.java)
        updateWeatherIntent.action = YAWA.UPDATE_CURRENT_WEATHER_ACTION
        activity.startService(updateWeatherIntent)
    }

    /**
     * Called when the user press an item of ListView
     */
    private fun onListViewItemClicked(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
        val cityName: String = (adapterView.getItemAtPosition(position)
                as Map<*, *>)["city"].toString()
        val country: String = (adapterView.getItemAtPosition(position)
                as Map<*, *>)["country"].toString()

        // set the new city on Shared Preferences
        configureCity(cityName, country)
        // updates the weather for the new city
        updateCurrentWeather()
        //
        activity.onBackPressed()
    }

    /**
     * Configure the new city with Shared Preferences
     */
    private fun configureCity(cityName: String, country: String) {
        val settingsLocationStr = resources.getString(R.string.settings_location_str)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        val editor = sharedPref.edit()

        val str: String =cityName + "," + country
        editor.putString(settingsLocationStr, str)
        editor.commit()
    }

    fun doSearch() {
        if(txtSearchStr.text.toString().length == 0 || searching) return

        // show spinner while wait for the response
        spinner.visibility = View.VISIBLE
        searching = true
        // flush listView
        listView.adapter = null

        activity.application.weatherManager.searchCityByName(URLEncoder.encode(txtSearchStr.text.trim().toString(),"UTF-8"),
                object : ICallbackSet {
                    override fun onError(error: VolleyError) {
                        Toast.makeText(activity, R.string.error1002, Toast.LENGTH_LONG).show()

                        // can make a new search
                        searching = false
                        spinner.visibility = View.GONE
                    }

                    override fun onSucceed(response: Any?) {
                        citiesList = response as List<CityDO>

                        // can make a new search
                        searching = false
                        spinner.visibility = View.GONE
                        if(citiesList?.size == 0) return

                        listView.adapter = SimpleAdapter(activity,
                                buildListViewDataSet(citiesList!!),
                                android.R.layout.simple_list_item_2,
                                arrayOf("city", "country"),
                                intArrayOf(android.R.id.text1, android.R.id.text2))
                    }
                }
        )
    }

    private fun buildListViewDataSet(citiesList: List<CityDO>) : ArrayList<Map<String, Any>> {
        val retList = ArrayList<Map<String, Any>>()
        for(cityDO: CityDO in citiesList) {
            retList.add(
                    mapOf(
                            Pair("city", cityDO.cityName),
                            Pair("country", cityDO.cityCountry)
                    )
            )
        }
        return retList
    }

}