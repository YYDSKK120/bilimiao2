package com.a10miaomiao.bilimiao.page.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import bilibili.app.card.v1.CardOuterClass
import bilibili.app.card.v1.Single
import bilibili.app.show.v1.PopularOuterClass
import cn.a10miaomiao.miao.binding.android.view._bottomPadding
import cn.a10miaomiao.miao.binding.android.view._leftPadding
import cn.a10miaomiao.miao.binding.android.view._rightPadding
import cn.a10miaomiao.miao.binding.android.view._topPadding
import cn.a10miaomiao.miao.binding.android.widget._text
import com.a10miaomiao.bilimiao.MainNavGraph
import com.a10miaomiao.bilimiao.comm.*
import com.a10miaomiao.bilimiao.comm.delegate.theme.ThemeDelegate
import com.a10miaomiao.bilimiao.comm.entity.search.SearchVideoInfo
import com.a10miaomiao.bilimiao.comm.recycler.*
import com.a10miaomiao.bilimiao.commponents.loading.ListState
import com.a10miaomiao.bilimiao.commponents.loading.listStateView
import com.a10miaomiao.bilimiao.commponents.video.videoItem
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.store.WindowStore
import com.a10miaomiao.bilimiao.template.TemplateViewModel
import com.chad.library.adapter.base.listener.OnItemClickListener
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import splitties.dimensions.dip
import splitties.views.backgroundColor
import splitties.views.dsl.core.*
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.gravityCenter
import splitties.views.verticalPadding

class PopularFragment: Fragment(), DIAware {

    companion object {
        fun newFragmentInstance(): PopularFragment {
            val fragment = PopularFragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    override val di: DI by lazyUiDi(ui = { ui })

    private val viewModel by diViewModel<PopularViewModel>(di)

    private val themeDelegate by instance<ThemeDelegate>()

    private var themeId = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (themeDelegate.getThemeResId() != themeId) {
            ui.cleanCacheView()
            themeId = themeDelegate.getThemeResId()
        }
        ui.parentView = container
        return ui.root
    }

    private val handleTopEntranceItemClick = OnItemClickListener { adapter, view, position ->
        val item = viewModel.topEntranceList[position]
        val url = item.uri
        if (url.indexOf("bilibili://") == 0) {
            BiliNavigation.navigationTo(view, url)
        } else {
            val args = bundleOf(
                MainNavGraph.args.url to url,
            )
            findNavController().navigate(
                MainNavGraph.action.global_to_web,
                args,
            )
        }
    }

    private val handleRefresh = SwipeRefreshLayout.OnRefreshListener {
        viewModel.refreshList()
    }

    private val handleItemClick = OnItemClickListener { adapter, view, position ->
        val item = viewModel.list.data[position]
        val args = bundleOf(
            MainNavGraph.args.id to item.base.param
        )
        Navigation.findNavController(view)
            .navigate(MainNavGraph.action.global_to_videoInfo, args)
    }

    val topEntranceItemUi = miaoBindingItemUi<PopularOuterClass.EntranceShow> { item, index ->
        verticalLayout {
            layoutParams = ViewGroup.MarginLayoutParams(
                dip(100), wrapContent
            )
            verticalPadding = config.pagePadding
            gravity = gravityCenter
            setBackgroundResource(config.selectableItemBackground)
            views {
                +imageView {
                    _network(item.icon)
                }..lParams {
                    width = dip(40)
                    height = dip(40)
                    bottomMargin = config.dividerSize
                }
                +textView {
                    _text = item.title
                    gravity = gravityCenter
                }
            }
        }
    }

    val itemUi = miaoBindingItemUi<Single.SmallCoverV5> { item, index ->
        videoItem (
            title = item.base.title,
            pic =item.base.cover,
            upperName = item.rightDesc1,
            remark = item.rightDesc2,
        )
    }

    val ui = miaoBindingUi {
        val windowStore = miaoStore<WindowStore>(viewLifecycleOwner, di)
        val contentInsets = windowStore.getContentInsets(parentView)

        verticalLayout {
            _leftPadding = contentInsets.left
            _rightPadding = contentInsets.right
//            _topPadding = config.pagePadding
//            _bottomPadding = contentInsets.bottom

            views {
                +recyclerView {
                    backgroundColor = config.windowBackgroundColor
                    _miaoLayoutManage(
                        GridAutofitLayoutManager(requireContext(), requireContext().dip(300))
                    )

                    val mAdapter = _miaoAdapter(
                        items = viewModel.list.data,
                        itemUi = itemUi,
                    ) {
                        stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                        setOnItemClickListener(handleItemClick)
                        loadMoreModule.setOnLoadMoreListener {
                            viewModel.loadMode()
                        }
                    }
                    headerViews(mAdapter) {
                        +recyclerView {
                            layoutManager = LinearLayoutManager(requireContext()).apply {
                                orientation = LinearLayoutManager.HORIZONTAL
                            }
                            _miaoAdapter(
                                items = viewModel.topEntranceList,
                                itemUi = topEntranceItemUi,
                            ) {
                                setOnItemClickListener(handleTopEntranceItemClick)
                            }
                        }..lParams(matchParent, wrapContent)
                    }
                    footerViews(mAdapter) {
                        +listStateView(
                            when {
                                viewModel.triggered -> ListState.NORMAL
                                viewModel.list.loading -> ListState.LOADING
                                viewModel.list.fail -> ListState.FAIL
                                viewModel.list.finished -> ListState.NOMORE
                                else -> ListState.NORMAL
                            },
                            viewModel::tryAgainLoadData
                        )..lParams(matchParent, wrapContent) {
                            bottomMargin = contentInsets.bottom
                        }
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