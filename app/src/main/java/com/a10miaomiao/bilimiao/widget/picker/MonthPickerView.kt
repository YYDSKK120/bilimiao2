package com.a10miaomiao.bilimiao.widget.picker

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.a10miaomiao.bilimiao.R
import java.util.*

class MonthPickerView(context: Context) : FrameLayout(context) {

    private val mYearPicker: PickerView by lazy {
        findViewById(R.id.mYearPicker)
    }
    private val mMonthPicker: PickerView by lazy {
        findViewById(R.id.mMonthPicker)
    }

    var date = DateModel()
        set(value) {
            if (value.year > 2008) mYearPicker?.value = value.year - 2008
            mMonthPicker?.value = value.month
            field = value
        }

    var onChanged: ((date: DateModel) -> Unit)? = null

    init {
        View.inflate(context, R.layout.layout_month_picker, this)

        val now = Date()
        val yearSize = now.year - 108
        mYearPicker.displayedValues = Array(yearSize, { (it + 2009).toString() })
        mYearPicker.minValue = 1
        mYearPicker.maxValue = yearSize
        mMonthPicker.minValue = 1
        mMonthPicker.maxValue = 12


        mYearPicker.setOnValueChangedListener(::onValueChange)
        mMonthPicker.setOnValueChangedListener(::onValueChange)
    }

    private fun onValueChange(picker: PickerView, oldVal: Int, newVal: Int) {
        when (picker) {
            mYearPicker -> {
                date.year = newVal + 2008
            }
            mMonthPicker -> {
                date.month = newVal
            }
        }
        onChanged?.invoke(date)
    }

}