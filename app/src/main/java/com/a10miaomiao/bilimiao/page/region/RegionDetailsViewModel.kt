package com.a10miaomiao.bilimiao.page.region

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a10miaomiao.bilimiao.comm.MiaoBindingUi
import com.a10miaomiao.bilimiao.comm.entity.ResultListInfo
import com.a10miaomiao.bilimiao.comm.entity.ResultListInfo2
import com.a10miaomiao.bilimiao.comm.entity.comm.PaginationInfo
import com.a10miaomiao.bilimiao.comm.entity.region.RegionInfo
import com.a10miaomiao.bilimiao.comm.entity.region.RegionTypeDetailsInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.a10miaomiao.bilimiao.comm.recycler.GridAutofitLayoutManager
import com.a10miaomiao.bilimiao.comm.utils.DebugMiao
import com.a10miaomiao.bilimiao.store.FilterStore
import com.a10miaomiao.bilimiao.store.TimeSettingStore
import com.a10miaomiao.bilimiao.widget.picker.DateModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import splitties.dimensions.dip
import splitties.toast.toast

class RegionDetailsViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    val context: Context by instance()
    val ui: MiaoBindingUi by instance()
    val fragment: Fragment by instance()
    val timeSettingStore: TimeSettingStore by instance()
    val filterStore: FilterStore by instance()

    var timeFrom = DateModel()
    var timeTo = DateModel()

    val rid by lazy { fragment.requireArguments().getInt(RegionDetailsFragment.TID) }
    var rankOrder = "click"  //排行依据
    var triggered = false
    var list = PaginationInfo<RegionTypeDetailsInfo>()


    init {
        val timeSettingState = timeSettingStore.state
        timeFrom = timeSettingState.timeFrom
        timeTo = timeSettingState.timeTo
        rankOrder = timeSettingState.rankOrder
        loadData()
    }

    private fun loadData(
        pageNum: Int = list.pageNum
    ) = viewModelScope.launch(Dispatchers.IO){
        try {
            ui.setState {
                list.loading = true
            }

            val res = BiliApiService.regionAPI
                .regionVideoList(
                    rid = rid,
                    rankOrder = rankOrder,
                    pageNum = pageNum,
                    pageSize = list.pageSize,
                    timeFrom = timeFrom.getValue(),
                    timeTo = timeTo.getValue(),
                )
                .call()
                .gson<ResultListInfo2<RegionTypeDetailsInfo>>()
            if (res.code == 0) {
                var result = res.result
                var totalCount = 0 // 屏蔽前数量
                if (result.size < list.pageSize) {
                    ui.setState { list.finished = true }
                }
                totalCount = result.size
                result = result.filter {
                    filterStore.filterWord(it.title)
                            && filterStore.filterUpper(it.mid.toLong())
                }
                ui.setState {
                    if (pageNum == 1) {
                        list.data = arrayListOf()
                    }
                    list.data.addAll(result)
                }
                list.pageNum = pageNum
                if (list.data.size < 10 && totalCount != result.size) {
                    _loadData(pageNum + 1)
                }
            } else {
                context.toast(res.msg)
                throw Exception(res.msg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ui.setState {
                list.fail = true
            }
        } finally {
            ui.setState {
                list.loading = false
                triggered = false
            }
        }
    }

    private fun _loadData(pageNum: Int = list.pageNum) {
        loadData(pageNum)
    }

    fun loadMode () {
        val (loading, finished, pageNum) = this.list
        if (!finished && !loading) {
            loadData(
                pageNum = pageNum + 1
            )
        }
    }

    fun refreshList() {
        ui.setState {
            list = PaginationInfo()
            triggered = true
            loadData()
        }
    }


}