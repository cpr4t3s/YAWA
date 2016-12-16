package com.isel.pdm.yawa


import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast


class EditCitiesActivity  : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_cities)
        val lv = findViewById(android.R.id.list) as ListView

        val itemsAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, favouriteCities)
        lv.adapter = itemsAdapter

        var city : String = ""
        var position : Int = 0
        lv.setOnItemClickListener { adapterView, view, pos, id ->
            position = pos
            city = favouriteCities[pos]
        }

        val setPref = findViewById(R.id.setPref)
        setPref.setOnClickListener {
            //Toast.makeText(this, "setPref" + city, Toast.LENGTH_SHORT).show()
            setPrefCity(city)

        }
        //TODO remove city not working
        val removeCity = findViewById(R.id.removeCity)
        removeCity.setOnClickListener {
            Toast.makeText(this, "remove" + city, Toast.LENGTH_SHORT).show()
            removeCityFromList(position)
            favouriteCities.removeAt(position)
        }


    }
    //TODO remove city not working
    private fun removeCityFromList(position: Int) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPref.edit().remove("city" + position)
    }


    private fun  setPrefCity(city: String) {
        val settingsLocationStr = resources.getString(R.string.settings_location_str)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPref.edit().putString(settingsLocationStr, city).apply()
    }

}
