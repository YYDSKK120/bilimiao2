package com.a10miaomiao.bilimiao2.comm

import android.view.View
import android.view.ViewGroup
import splitties.views.dsl.core.Ui

abstract class MiaoUI : Ui {

    companion object {
        @PublishedApi internal val parentAndViews = arrayListOf<ViewsInfo>()
        @PublishedApi internal var isRecordViews = false
    }



    class ViewsInfo(
        private val viewGroup: ViewGroup,
        private val isRecord: Boolean,
    ) {
        private val views = arrayListOf<View>()

        operator fun View.unaryPlus(): View {
            if (isRecord) {
                views.add(this)
            }
            return this
        }

        operator fun View.rangeTo(lParams: ViewGroup.LayoutParams) {
            if (isRecord) {
                this.layoutParams = lParams
            }
        }

        fun bindViews() {
            views.forEach {
                viewGroup.addView(it)
            }
        }

    }


    /**
     * 不知道起啥名
     */
    inline fun miao(block: () -> View): View {
        isRecordViews = true
        val view = block()
        parentAndViews.forEach {
            it.bindViews()
        }
        parentAndViews.clear()
        isRecordViews = false
        return view
    }

}