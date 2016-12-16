package com.isel.pdm.yawa


import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import java.util.*


class EditCitiesActivity  : AppCompatActivity() {
    var favouriteCities : ArrayList<String> = ArrayList()

    private fun loadPrefCities() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        // TODO: meter o zero com constante nas resourses
        val citiesCounter = sharedPref.getInt("citiesCounter", 0)

        for (i in 0 until citiesCounter) {
            val city = sharedPref.getString("city" + i, "--")
            favouriteCities.add(city)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_cities)
        val lv = findViewById(android.R.id.list) as ListView

        loadPrefCities()
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
            removeCityFromList(city)
            val itemsAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, favouriteCities)
            lv.adapter = itemsAdapter
        }
    }

    //TODO remove city not working
    private fun removeCityFromList(city: String) {
        flushFavoriteCities()
        favouriteCities.remove(city)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPref.edit()
        for (i in 0 until favouriteCities.size) {
            editor.putString("city" + i, favouriteCities[i])
        }
        editor.putInt("citiesCounter",favouriteCities.size )
        editor.apply()
    }
    private fun flushFavoriteCities() {
       val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
      // TODO: meter o zero com constante nas resourses
      val citiesCounter = sharedPref.getInt("citiesCounter", 0)
      val editor = sharedPref.edit()
      for (i in 0 until citiesCounter) {
         editor.remove("city" + i)
      }
      editor.apply()
}

    private fun  setPrefCity(city: String) {
        val settingsLocationStr = resources.getString(R.string.settings_location_str)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPref.edit().putString(settingsLocationStr, city).apply()
    }

}
