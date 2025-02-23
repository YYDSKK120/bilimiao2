package com.a10miaomiao.bilimiao.page.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.navigation.findNavController
import androidx.viewpager.widget.ViewPager
import cn.a10miaomiao.miao.binding.android.view._bottomPadding
import cn.a10miaomiao.miao.binding.android.view._leftPadding
import cn.a10miaomiao.miao.binding.android.view._rightPadding
import cn.a10miaomiao.miao.binding.android.view._topPadding
import com.a10miaomiao.bilimiao.MainNavGraph
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.*
import com.a10miaomiao.bilimiao.comm.entity.region.RegionInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.mypage.MyPage
import com.a10miaomiao.bilimiao.comm.mypage.myMenuItem
import com.a10miaomiao.bilimiao.comm.mypage.myPageConfig
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.page.region.RegionDetailsFragment
import com.a10miaomiao.bilimiao.page.search.result.VideoRegionFragment
import com.a10miaomiao.bilimiao.page.search.result.VideoResultFragment
import com.a10miaomiao.bilimiao.page.search.result.VideoResultViewModel
import com.a10miaomiao.bilimiao.store.WindowStore
import com.a10miaomiao.bilimiao.template.TemplateViewModel
import com.a10miaomiao.bilimiao.widget.comm.MenuItemView
import com.google.android.material.tabs.TabLayout
import org.kodein.di.DI
import org.kodein.di.DIAware
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.verticalLayout
import splitties.views.dsl.core.wrapContent

class SearchResultFragment : Fragment(), DIAware, MyPage, ViewPager.OnPageChangeListener {

    override val pageConfig = myPageConfig {
        title = "搜索\n-\n${viewModel.keyword ?: "无关键字"}"
        menus = mutableListOf(
            myMenuItem {
                key = MenuKeys.search
                title = "继续搜索"
                iconResource = R.drawable.ic_search_gray
            }
        ).apply {
            viewModel.curFragment?.let { addAll(it.menus) }
        }
    }

    override fun onMenuItemClick(view: MenuItemView) {
        super.onMenuItemClick(view)
        when (view.prop.key) {
            MenuKeys.search -> {
                val bsNav = requireActivity().findNavController(R.id.nav_bottom_sheet_fragment)
                val args = bundleOf(
                    MainNavGraph.args.text to viewModel.keyword
                )
                bsNav.navigate(MainNavGraph.action.global_to_searchStart, args)
            }
            else -> {
                viewModel.curFragment?.onMenuItemClick(view)
            }
        }
    }

    override val di: DI by lazyUiDi(ui = { ui })

    private val viewModel by diViewModel<SearchResultViewModel>(di)

    private val ID_viewPager = View.generateViewId()
    private val ID_tabLayout = View.generateViewId()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui.parentView = container
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    private fun initView(view: View) {
        val tabLayout = view.findViewById<TabLayout>(ID_tabLayout)
        val viewPager = view.findViewById<ViewPager>(ID_viewPager)
        if  (viewPager.adapter == null) {
            val mAdapter = object : FragmentStatePagerAdapter(childFragmentManager) {
                override fun getItem(p0: Int): Fragment {
                    return viewModel.fragments[p0]
                }
                override fun getCount() = viewModel.fragments.size
                override fun getPageTitle(position: Int) = viewModel.fragments[position].title
            }
            viewPager.adapter = mAdapter
            tabLayout.setTabsFromPagerAdapter(mAdapter)
            tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
            tabLayout.setupWithViewPager(viewPager)
            viewPager.addOnPageChangeListener(this)
            viewPager.post {
                viewModel.position = 0
                pageConfig.notifyConfigChanged()
            }
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        viewModel.position = position
        pageConfig.notifyConfigChanged()
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    fun changeVideoRegion(region: RegionInfo) {
        val curFragment = viewModel.curFragment
        if (curFragment is VideoResultFragment) {
            curFragment.changeVideoRegion(region)
        }
    }

    val ui = miaoBindingUi {
        val windowStore = miaoStore<WindowStore>(viewLifecycleOwner, di)
        val contentInsets = windowStore.getContentInsets(parentView)
        verticalLayout {
            views {
                +tabLayout(ID_tabLayout) {
                    _topPadding = contentInsets.top
                    _leftPadding = contentInsets.left
                    _rightPadding = contentInsets.right
                }..lParams(matchParent, wrapContent)
                +viewPager(ID_viewPager) {
                    _leftPadding = contentInsets.left
                    _rightPadding = contentInsets.right
                }..lParams(matchParent, matchParent) {
                    weight = 1f
                }
            }
        }
    }

}