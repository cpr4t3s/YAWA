package com.isel.pdm.yawa.UI

import android.content.Context

import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.isel.pdm.yawa.R


class TimePickerPreference(context: Context, attrs: AttributeSet?) : DialogPreference(context, attrs) {
    val DEFAULT_HOUR = 12
    val DEFAULT_MINUTES = 0

    var hour: Int
    var minutes: Int

    private var picker: TimePicker? = null
    private var value: String = ""

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TimePickerPreference)
        hour = a.getInt(R.styleable.TimePickerPreference_time_picker_hour, DEFAULT_HOUR)
        minutes = a.getInt(R.styleable.TimePickerPreference_time_picker_minutes, DEFAULT_MINUTES)
    }

    override fun onCreateDialogView(): View {
        val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.gravity = Gravity.CENTER

        picker = TimePicker(context)
        picker!!.layoutParams = layoutParams

        val dialogView = LinearLayout(context)
        dialogView.addView(picker)

        return dialogView
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        picker!!.currentHour = hour
        picker!!.currentMinute = minutes
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val newValue = "${picker!!.currentHour}:${picker!!.currentMinute}"
            if (callChangeListener(newValue)) {
                setValue(newValue)
            }
        }
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        setValue(
                if (restorePersistedValue)
                    getPersistedString(value)
                else
                    defaultValue as String
        )
    }

    fun setValue(value: String) {
        persistString(value)

        val time = value
        val splitedTime = time.split(":")
        hour = splitedTime[0].toInt()
        minutes = splitedTime[1].toInt()
    }
}