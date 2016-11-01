package com.isel.pdm.yawa.UI

import android.content.Context
import android.content.res.TypedArray

import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.NumberPicker


class NumberPickerPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {
    // allowed range
    val MAX_VALUE = 16
    val MIN_VALUE = 1
    // enable or disable the 'circular behavior'
    val WRAP_SELECTOR_WHEEL = true

    private var picker: NumberPicker? = null
    private var value: Int = 0


    constructor (context: Context, attrs: AttributeSet, defStyleAttr: Int): this(context, attrs){
    }

    override fun onCreateDialogView(): View {
        val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.gravity = Gravity.CENTER

        picker = NumberPicker(context)
        picker!!.setLayoutParams(layoutParams)

        val dialogView = FrameLayout(context)
        dialogView.addView(picker)

        return dialogView
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        picker!!.setMinValue(MIN_VALUE)
        picker!!.setMaxValue(MAX_VALUE)
        picker!!.setWrapSelectorWheel(WRAP_SELECTOR_WHEEL)
        picker!!.setValue(getValue())
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            picker!!.clearFocus()
            val newValue = picker!!.getValue()
            if (callChangeListener(newValue)) {
                setValue(newValue)
            }
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInt(index, MIN_VALUE)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any) {
        setValue(if (restorePersistedValue) getPersistedInt(MIN_VALUE) else defaultValue as Int)
    }

    fun setValue(value: Int) {
        this.value = value
        persistInt(this.value)
    }

    fun getValue(): Int {
        return this.value
    }
}