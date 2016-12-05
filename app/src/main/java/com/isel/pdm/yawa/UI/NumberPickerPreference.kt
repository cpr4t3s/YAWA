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
import com.isel.pdm.yawa.R


class NumberPickerPreference(context: Context, attrs: AttributeSet?) : DialogPreference(context, attrs) {
    // allowed range
    val MAX_VALUE = 100
    val MIN_VALUE = 0

    val minValue: Int
    val maxValue: Int
    // enable or disable the 'circular behavior'
    val WRAP_SELECTOR_WHEEL = true

    private var picker: NumberPicker? = null
    private var value: Int = 0

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference)
        minValue = a.getInt(R.styleable.NumberPickerPreference_number_picker_min_val, MIN_VALUE)
        maxValue = a.getInt(R.styleable.NumberPickerPreference_number_picker_max_val, MAX_VALUE)
    }

    override fun onCreateDialogView(): View {
        val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.gravity = Gravity.CENTER

        picker = NumberPicker(context)
        picker!!.layoutParams = layoutParams

        val dialogView = FrameLayout(context)
        dialogView.addView(picker)

        return dialogView
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        picker!!.minValue = minValue
        picker!!.maxValue = maxValue
        picker!!.wrapSelectorWheel = WRAP_SELECTOR_WHEEL
        picker!!.value = getValue()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            picker!!.clearFocus()
            val newValue = picker!!.value
            if (callChangeListener(newValue)) {
                setValue(newValue)
            }
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInt(index, minValue)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        setValue(if (restorePersistedValue) getPersistedInt(minValue) else defaultValue as Int)
    }

    fun setValue(value: Int) {
        this.value = value
        persistInt(this.value)
    }

    fun getValue(): Int {
        return this.value
    }
}