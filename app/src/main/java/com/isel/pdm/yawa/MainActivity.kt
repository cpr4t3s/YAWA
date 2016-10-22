package com.isel.pdm.yawa

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

import com.android.volley.VolleyError
import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import com.isel.pdm.yawa.fragments.WeatherDetailsFragment

// TODO:
// Versoes anteriores a 21 nao aparece o menu na toolbar - No meu so aparecem os menus quando carrego no botão de opções
// ver isto: https://guides.codepath.com/android/Using-the-App-ToolBar

class MainActivity : AppCompatActivity() {
    private val BACK_PRESS_INTERVAL: Long = 2000 // 2 seconds

    private var lastBackTime: Long = 0
    private val txtTitleCity by lazy { findViewById(R.id.txtTitleCity) as TextView }
    private val weatherFragment by lazy { fragmentManager.findFragmentById(R.id.weather_detail)
            as WeatherDetailsFragment }
    private val callbackSet : ICallbackSet by lazy {
        object : ICallbackSet {
            override fun onError(error: VolleyError) {
                Toast.makeText(this@MainActivity,
                        resources.getString(R.string.error1001), Toast.LENGTH_SHORT).show()
            }

            override fun onSucceed(response: Any) {
                val weatherState = response as WeatherStateDO
                weatherFragment.updateUI(weatherState)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather__main_menu_)

        val imgButtonRefresh = findViewById(R.id.imageButtonRefresh) as ImageButton
        imgButtonRefresh.setOnClickListener { application.weatherManager.refreshCurrentWeather(callbackSet) }

    }

    override fun onStop() {
        super.onStop()

        // on activity's state changes we need to cancel possible requests
        application.weatherManager.cancelAllRequests()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        println("MainActivity.onSaveInstanceState")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        println("MainActivity.onRestoreInstanceState")
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - this.lastBackTime < this.BACK_PRESS_INTERVAL) {
            super.onBackPressed()
        }

        this.lastBackTime = System.currentTimeMillis()
        Toast.makeText(this, R.string.message_on_exit, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item : MenuItem) : Boolean {
        when(item.itemId) {
            R.id.settings_addCity -> {
                val i = Intent(this, CitiesActivity::class.java)
                startActivity(i)
            }

            R.id.settings_forecast -> startActivity(Intent(this, ForecastActivity::class.java))

            //TODO activity com os dados
            R.id.settings_about -> Toast.makeText(this, "About!!!!!!!!!", Toast.LENGTH_SHORT).show()


        }
        return true
    }

    override fun onResume() {
        super.onResume()

        // update title
        setCityOnTitle()
        // update the weather. The weather may change when the user select a different city
        application.weatherManager.getCurrentWeather(callbackSet)
    }

    private fun setCityOnTitle() {
        val settings = getSharedPreferences(application.settingsFileName, Context.MODE_PRIVATE)
        val title = settings.getString(application.settingsLocationStr, application.defaultLocation)
        txtTitleCity.text = title
    }
}
