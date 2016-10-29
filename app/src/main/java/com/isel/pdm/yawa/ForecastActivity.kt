package com.isel.pdm.yawa

import android.app.Fragment
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.isel.pdm.yawa.fragments.ForecastFragment


class ForecastActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forecast)

        // We only add fragment on the first time activity is created
        if(savedInstanceState == null) {
            val forecastFragment = ForecastFragment()
            fragmentManager.beginTransaction()
                    .add(R.id.forecast_data_holder, forecastFragment)
                    .commit()
        }
    }

    override fun onStop() {
        super.onStop()

        // on activity's state changes we need to cancel possible requests
        application.weatherManager.cancelAllRequests()
    }

    override fun onSaveInstanceState(outState : Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(restoredState : Bundle) {
      //  listFragment.txtSearchStr.setText(restoredState.getString(CitiesListFragment.SEARCH_TEXT_KEY))
    }

}