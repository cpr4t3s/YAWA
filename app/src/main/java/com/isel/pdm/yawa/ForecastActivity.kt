package com.isel.pdm.yawa

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ImageButton
import com.isel.pdm.yawa.fragments.CitiesListFragment


class ForecastActivity : AppCompatActivity(){

    //private val refreshButton by lazy { findViewById(R.id.btnSearchCity) as ImageButton }
  //  private val listFragment by lazy { fragmentManager.findFragmentById(R.id.cities_list_frag) as CitiesListFragment }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forecast)

        // Search Button
      //  refreshButton.setOnClickListener {
           // listFragment.doSearch()
        //}
    }

    override fun onSaveInstanceState(outState : Bundle) {
       // outState.putString(CitiesListFragment.SEARCH_TEXT_KEY, listFragment.txtSearchStr.text.toString())
    }

    override fun onRestoreInstanceState(restoredState : Bundle) {
      //  listFragment.txtSearchStr.setText(restoredState.getString(CitiesListFragment.SEARCH_TEXT_KEY))
    }

}