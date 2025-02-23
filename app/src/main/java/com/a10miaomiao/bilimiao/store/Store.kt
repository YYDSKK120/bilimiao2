package com.a10miaomiao.bilimiao.store

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cn.a10miaomiao.download.DownloadService
import com.a10miaomiao.bilimiao.MainActivity
import com.a10miaomiao.bilimiao.comm.diViewModel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.LazyDI
import org.kodein.di.bindSingleton

class Store (
        private val activity: AppCompatActivity,
        override val di: DI,
): DIAware {

        val windowStore: WindowStore by activity.diViewModel(di)
        val playerStore: PlayerStore by activity.diViewModel(di)
        val downloadStore: DownloadStore by activity.diViewModel(di)
        val userStore: UserStore by activity.diViewModel(di)
        val timeSettingStore: TimeSettingStore by activity.diViewModel(di)
        val filterStore: FilterStore by activity.diViewModel(di)
        val regionStore: RegionStore by activity.diViewModel(di)

        fun loadStoreModules(diBuilder: DI.Builder) = diBuilder.run{
                bindSingleton { windowStore }
                bindSingleton { playerStore }
                bindSingleton { downloadStore }
                bindSingleton { userStore }
                bindSingleton { timeSettingStore }
                bindSingleton { filterStore }
                bindSingleton { regionStore }
        }

        fun onCreate(savedInstanceState: Bundle?) {
                windowStore.init(activity)
                playerStore.init(activity)
                downloadStore.init(activity)
                userStore.init(activity)
                timeSettingStore.init(activity)
                filterStore.init(activity)
                regionStore.init(activity)
        }

        fun onDestroy() {

        }

}