package com.a10miaomiao.bilimiao.page.search.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.a10miaomiao.miao.binding.android.view._bottomPadding
import cn.a10miaomiao.miao.binding.android.view._leftPadding
import cn.a10miaomiao.miao.binding.android.view._rightPadding
import cn.a10miaomiao.miao.binding.android.view._topPadding
import com.a10miaomiao.bilimiao.MainNavGraph
import com.a10miaomiao.bilimiao.comm.*
import com.a10miaomiao.bilimiao.comm.entity.search.SearchUpperInfo
import com.a10miaomiao.bilimiao.comm.entity.search.SearchVideoInfo
import com.a10miaomiao.bilimiao.comm.mypage.MyPage
import com.a10miaomiao.bilimiao.comm.mypage.myPageConfig
import com.a10miaomiao.bilimiao.comm.recycler.*
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import com.a10miaomiao.bilimiao.commponents.loading.ListState
import com.a10miaomiao.bilimiao.commponents.loading.listStateView
import com.a10miaomiao.bilimiao.commponents.upper.upperItem
import com.a10miaomiao.bilimiao.commponents.video.videoItem
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.store.WindowStore
import com.a10miaomiao.bilimiao.template.TemplateViewModel
import com.a10miaomiao.bilimiao.widget.comm.MenuItemView
import com.chad.library.adapter.base.listener.OnItemClickListener
import org.kodein.di.DI
import org.kodein.di.DIAware
import splitties.dimensions.dip
import splitties.views.backgroundColor
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.verticalLayout
import splitties.views.dsl.core.wrapContent
import splitties.views.dsl.recyclerview.recyclerView

class UpperResultFragment : BaseResultFragment(), DIAware {

    companion object {
        fun newInstance(text: String?): UpperResultFragment {
            val fragment = UpperResultFragment()
            val bundle = Bundle()
            bundle.putString(MainNavGraph.args.text, text)
            fragment.arguments = bundle
            return fragment
        }
    }

    override val title = "UP主"

    override val menus get() = listOf<MenuItemView.MenuItemPropInfo>(

    )

    override fun onMenuItemClick(view: MenuItemView) {

    }

    override val di: DI by lazyUiDi(ui = { ui })

    private val viewModel by diViewModel<UpperResultViewModel>(di)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui.parentView = container
        return ui.root
    }


    private val handleRefresh = SwipeRefreshLayout.OnRefreshListener {
        viewModel.refreshList()
    }

    private val handleItemClick = OnItemClickListener { adapter, view, position ->
        val item = viewModel.list.data[position]
        val args = bundleOf(
            MainNavGraph.args.id to item.param
        )
        Navigation.findNavController(view)
            .navigate(MainNavGraph.action.searchResult_to_user, args)
    }

    val itemUi = miaoBindingItemUi<SearchUpperInfo> { item, index ->
        upperItem (
            name = item.title,
            face = item.cover,
            remarks = "粉丝：${NumberUtil.converString(item.fans)}   视频数：${NumberUtil.converString(item.archives)}",
            sign = item.sign,
        ).apply {
            layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
        }
    }

    val ui = miaoBindingUi {
        val windowStore = miaoStore<WindowStore>(viewLifecycleOwner, di)
        val contentInsets = windowStore.getContentInsets(parentView)
        verticalLayout {
            views {
                +recyclerView {
                    backgroundColor = config.windowBackgroundColor
                    _miaoLayoutManage(
                        GridAutofitLayoutManager(requireContext(), requireContext().dip(300))
                    )

                    val footerView = listStateView(
                        when {
                            viewModel.triggered -> ListState.NORMAL
                            viewModel.list.loading -> ListState.LOADING
                            viewModel.list.fail -> ListState.FAIL
                            viewModel.list.finished -> ListState.NOMORE
                            else -> ListState.NORMAL
                        }
                    )
                    footerView..lParams(matchParent, wrapContent) {
                        bottomMargin = contentInsets.bottom
                    }

                    _miaoAdapter(
                        items = viewModel.list.data,
                        itemUi = itemUi,
                    ) {
                        stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                        setOnItemClickListener(handleItemClick)
                        loadMoreModule.setOnLoadMoreListener {
                            viewModel.loadMode()
                        }
                        addFooterView(footerView)
                    }
                }.wrapInSwipeRefreshLayout {
                    setColorSchemeResources(config.themeColorResource)
                    setOnRefreshListener(handleRefresh)
                    _isRefreshing = viewModel.triggered
                }..lParams(matchParent, matchParent)
            }
        }
    }

}