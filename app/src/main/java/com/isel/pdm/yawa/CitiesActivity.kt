package com.isel.pdm.yawa

import android.app.ListActivity
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.android.volley.VolleyError
import com.isel.pdm.yawa.DataContainers.CityDO
import java.util.*

class CitiesActivity : ListActivity() {
    companion object {
        private val SEARCH_TEXT_KEY : String = "searchTextKey"
    }

    // List of CityDOs returned by the search. Used to pass the weather to weatherManager
    private var citiesList : List<CityDO>? = null

    private var searching: Boolean = false
    private val txtSearchStr by lazy{ findViewById(R.id.searchText) as EditText }
    private val searchButton by lazy { findViewById(R.id.btnSearchCity) as ImageButton }
    val spinner by lazy { findViewById(R.id.spinner) as ProgressBar }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cities)
        // hide spinner
        spinner.visibility = View.GONE

        val listView = findViewById(android.R.id.list) as ListView
        listView.setOnItemClickListener { adapterView, view, position, id ->
            onListViewItemClicked(adapterView, view, position, id)
        }

        // Search Button
        searchButton.setOnClickListener {
            if(!searching) {
                this.doSearch()
            }
        }
    }

    /**
     * Called when the user press an item of ListView
     */
    private fun onListViewItemClicked(adapterView: AdapterView<*>, view: View, position: Int , id: Long) {
        val cityName: String = (adapterView.getItemAtPosition(position)
                as Map<*, *>)["city"].toString()
        val country: String = (adapterView.getItemAtPosition(position)
                as Map<*, *>)["country"].toString()

        // set the new city on Shared Preferences
        configureCity(cityName, country)
        // set the weather returned by the search
        citiesList.let{ application.weatherManager.setWeather(citiesList!![position].weatherState) }
        //
        onBackPressed()
    }

    /**
     * Configure the new city with Shared Preferences
     */
    private fun configureCity(cityName: String, country: String) {
        val settingsFileName = resources.getString(R.string.settings_filename)
        val settingsLocationStr = resources.getString(R.string.settings_location_str)
        val settings = getSharedPreferences(settingsFileName, Context.MODE_PRIVATE)
        val editor = settings.edit()

        val str: String = cityName + "," + country
        editor.putString(settingsLocationStr, str)
        editor.commit()
    }

    private fun doSearch() {
        if(txtSearchStr.text.toString().length == 0) return

        // show spinner while wait for the response
        spinner.visibility = View.VISIBLE
        searching = true
        // flush listView
        listView.adapter = null

        application.weatherManager.searchCityByName(txtSearchStr.text.toString(),
                object : ICallbackSet {
                    override fun onError(error: VolleyError) {
                        Toast.makeText(this@CitiesActivity, R.string.error1002, Toast.LENGTH_LONG).show()

                        // can make a new search
                        searching = false
                        spinner.visibility = View.GONE
                    }

                    override fun onSucceed(response: Any) {
                        citiesList = response as List<CityDO>

                        // can make a new search
                        searching = false
                        spinner.visibility = View.GONE
                        if(citiesList?.size == 0) return

                        listView.adapter = SimpleAdapter(this@CitiesActivity,
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

    override fun onSaveInstanceState(outState : Bundle) {
        outState.putString(SEARCH_TEXT_KEY, txtSearchStr.text.toString())
    }

    override fun onRestoreInstanceState(restoredState : Bundle) {
        txtSearchStr.setText(restoredState.getString(SEARCH_TEXT_KEY))
    }

}
