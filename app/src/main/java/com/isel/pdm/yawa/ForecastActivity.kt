package com.isel.pdm.yawa

import android.app.FragmentTransaction
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.isel.pdm.yawa.fragments.ForecastFragment


class ForecastActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forecast)

        val forecastFragment = ForecastFragment()
        fragmentManager.beginTransaction()
                .add(R.id.forecast_data_holder, forecastFragment)
                .commit()
    }

    override fun onSaveInstanceState(outState : Bundle) {
       // outState.putString(CitiesListFragment.SEARCH_TEXT_KEY, listFragment.txtSearchStr.text.toString())
    }

    override fun onRestoreInstanceState(restoredState : Bundle) {
      //  listFragment.txtSearchStr.setText(restoredState.getString(CitiesListFragment.SEARCH_TEXT_KEY))
    }

}