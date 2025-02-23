package com.a10miaomiao.bilimiao.page.search.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.a10miaomiao.miao.binding.android.view._bottomPadding
import cn.a10miaomiao.miao.binding.android.view._leftPadding
import cn.a10miaomiao.miao.binding.android.view._rightPadding
import cn.a10miaomiao.miao.binding.android.view._topPadding
import com.a10miaomiao.bilimiao.MainNavGraph
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.*
import com.a10miaomiao.bilimiao.comm.entity.region.RegionInfo
import com.a10miaomiao.bilimiao.comm.entity.region.RegionTypeDetailsInfo
import com.a10miaomiao.bilimiao.comm.entity.search.SearchVideoInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.mypage.MyPage
import com.a10miaomiao.bilimiao.comm.mypage.myMenuItem
import com.a10miaomiao.bilimiao.comm.mypage.myPageConfig
import com.a10miaomiao.bilimiao.comm.recycler.*
import com.a10miaomiao.bilimiao.commponents.loading.ListState
import com.a10miaomiao.bilimiao.commponents.loading.listStateView
import com.a10miaomiao.bilimiao.commponents.video.videoItem
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.page.region.RankOrderPopupMenu
import com.a10miaomiao.bilimiao.page.region.RegionDetailsFragment
import com.a10miaomiao.bilimiao.store.WindowStore
import com.a10miaomiao.bilimiao.template.TemplateViewModel
import com.a10miaomiao.bilimiao.widget.comm.MenuItemView
import com.a10miaomiao.bilimiao.widget.menu.CheckPopupMenu
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

class VideoResultFragment : BaseResultFragment(), DIAware {

    companion object {
        fun newInstance(text: String?): VideoResultFragment {
            val fragment = VideoResultFragment()
            val bundle = Bundle()
            bundle.putString(MainNavGraph.args.text, text)
            fragment.arguments = bundle
            return fragment
        }
    }

    override val title: String = "视频"
    override val menus get() = listOf<MenuItemView.MenuItemPropInfo>(
        myMenuItem {
            key = MenuKeys.region
            title = viewModel.regionName
            iconResource = R.drawable.ic_baseline_grid_on_gray_24
        },
        myMenuItem {
            key = MenuKeys.filter
            title = viewModel.rankOrder.title
            iconResource = R.drawable.ic_baseline_filter_list_grey_24
        },
        myMenuItem {
            key = MenuKeys.time
            title = viewModel.duration.title
            iconResource = R.drawable.ic_access_time_gray_24dp
        },
    )

    override fun onMenuItemClick(view: MenuItemView) {
        when (view.prop.key) {
            MenuKeys.region -> {
                val nav = requireActivity().findNavController(R.id.nav_bottom_sheet_fragment)
                val args = bundleOf(
                    MainNavGraph.args.id to viewModel.regionId.toString()
                )
                nav.navigate(MainNavGraph.action.global_to_videoRegion, args)
            }
            MenuKeys.filter -> {
                val pm = CheckPopupMenu(
                    activity = requireActivity(),
                    anchor = view,
                    menus = viewModel.rankOrdersMenus,
                    value = viewModel.rankOrder.value,
                )
                pm.onMenuItemClick = {
                    viewModel.rankOrder = it
                    viewModel.refreshList()
                    notifyConfigChanged()
                }
                pm.show()
            }
            MenuKeys.time -> {
                val pm = CheckPopupMenu<Int>(
                    activity = requireActivity(),
                    anchor = view,
                    menus = viewModel.durationMenus,
                    value = viewModel.duration.value,
                )
                pm.onMenuItemClick = {
                    viewModel.duration = it
                    viewModel.refreshList()
                    notifyConfigChanged()
                }
                pm.show()
            }
        }
    }

    override val di: DI by lazyUiDi(ui = { ui })

    private val viewModel by diViewModel<VideoResultViewModel>(di)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui.parentView = container
        return ui.root
    }

    fun changeVideoRegion(region: RegionInfo) {
        viewModel.regionId = region.tid
        viewModel.regionName = region.name
        viewModel.refreshList()
        notifyConfigChanged()
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
            .navigate(MainNavGraph.action.searchResult_to_videoInfo, args)
    }

    val itemUi = miaoBindingItemUi<SearchVideoInfo> { item, index ->
        videoItem (
            title = item.title,
            pic = item.cover,
            upperName = item.author,
            playNum = item.play,
            damukuNum = item.danmaku,
        )
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