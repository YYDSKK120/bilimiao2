package com.a10miaomiao.bilimiao.page.video

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.marginRight
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.navigation.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.a10miaomiao.miao.binding.android.view.*
import cn.a10miaomiao.miao.binding.android.widget._text
import cn.a10miaomiao.miao.binding.android.widget._textColorResource
import cn.a10miaomiao.miao.binding.miaoEffect
import com.a10miaomiao.bilimiao.MainNavGraph
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.*
import com.a10miaomiao.bilimiao.comm.delegate.player.PlayerDelegate
import com.a10miaomiao.bilimiao.comm.entity.video.VideoPageInfo
import com.a10miaomiao.bilimiao.comm.entity.video.VideoRelateInfo
import com.a10miaomiao.bilimiao.comm.entity.video.VideoStaffInfo
import com.a10miaomiao.bilimiao.comm.mypage.MyPage
import com.a10miaomiao.bilimiao.comm.mypage.MyPageConfig
import com.a10miaomiao.bilimiao.comm.mypage.myMenuItem
import com.a10miaomiao.bilimiao.comm.mypage.myPageConfig
import com.a10miaomiao.bilimiao.comm.recycler.*
import com.a10miaomiao.bilimiao.comm.utils.BiliUrlMatcher
import com.a10miaomiao.bilimiao.comm.utils.DebugMiao
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import com.a10miaomiao.bilimiao.commponents.loading.ListState
import com.a10miaomiao.bilimiao.commponents.loading.listStateView
import com.a10miaomiao.bilimiao.commponents.video.videoItem
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.page.region.RankOrderPopupMenu
import com.a10miaomiao.bilimiao.page.video.comment.SortOrderPopupMenu
import com.a10miaomiao.bilimiao.store.PlayerStore
import com.a10miaomiao.bilimiao.store.UserStore
import com.a10miaomiao.bilimiao.store.WindowStore
import com.a10miaomiao.bilimiao.template.TemplateViewModel
import com.a10miaomiao.bilimiao.widget._setContent
import com.a10miaomiao.bilimiao.widget.comm.MenuItemView
import com.a10miaomiao.bilimiao.widget.comm.getAppBarView
import com.a10miaomiao.bilimiao.widget.expandableTextView
import com.a10miaomiao.bilimiao.widget.expandabletext.ExpandableTextView
import com.a10miaomiao.bilimiao.widget.expandabletext.app.LinkType
import com.a10miaomiao.bilimiao.widget.rcImageView
import com.chad.library.adapter.base.listener.OnItemClickListener
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.FlowCollector
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import splitties.dimensions.dip
import splitties.toast.toast
import splitties.views.*
import splitties.views.dsl.core.*
import splitties.views.dsl.core.lParams
import splitties.views.dsl.recyclerview.recyclerView
import kotlin.properties.Delegates

class VideoInfoFragment: Fragment(), DIAware, MyPage {

    override val di: DI by lazyUiDi(ui = { ui })

    private val viewModel by diViewModel<VideoInfoViewModel>(di)

    private val windowStore by instance<WindowStore>()

    private val userStore by instance<UserStore>()

    private val playerStore by instance<PlayerStore>()

    private val playerDelegate by instance<PlayerDelegate>()

    override val pageConfig = myPageConfig {
        val info = viewModel.info
        title = info?.let {
            "${it.bvid} /\nAV${it.aid}"
        } ?: "视频详情"
        menus = listOf(
            myMenuItem {
                key = 0
                iconResource = R.drawable.ic_more_vert_grey_24dp
                title = "更多"
            },
            myMenuItem {
                key = 1
                iconResource = R.drawable.ic_column_comm
                title = NumberUtil.converString(info?.stat?.reply?.toString() ?: "评论")
            },
//            myMenuItem {
//                key = 2
//                iconResource = R.drawable.ic_column_share
//                title = NumberUtil.converString(info?.stat?.share?.toString() ?: "分享")
//            },
            myMenuItem {
                key = 3
                iconResource = if (info?.req_user?.favorite == null) {
                    R.drawable.ic_column_unstar
                } else {
                    R.drawable.ic_column_star
                }
                title = NumberUtil.converString(info?.stat?.favorite?.toString() ?: "收藏")
            },
            myMenuItem {
                key = 4
                iconResource = if (info?.req_user?.coin == null) {
                    R.drawable.ic_column_uncoin
                } else {
                    R.drawable.ic_column_coin
                }
                title = NumberUtil.converString(info?.stat?.coin?.toString() ?: "投币")
            },
            myMenuItem {
                key = 5
                iconResource = if (info?.req_user?.like == null) {
                    R.drawable.ic_column_unlike
                } else {
                    R.drawable.ic_column_like
                }
                title = NumberUtil.converString(info?.stat?.like?.toString() ?: "点赞")
            },
        )
    }

    override fun onMenuItemClick(view: MenuItemView) {
        super.onMenuItemClick(view)
        val info = viewModel.info
        if (!userStore.isLogin() && (view.prop.key ?: 0) >= 3) {
            toast("请先登录")
            return
        }
        when (view.prop.key) {
            0 -> {
                // 更多
                val pm = VideoMorePopupMenu(
                    activity = requireActivity(),
                    anchor = view,
                    viewModel = viewModel
                )
                pm.show()
            }
            1 -> {
                // 评论
                if (info != null) {
                    val nav = requireActivity().findNavController(R.id.nav_host_fragment)
                    val args = bundleOf(
                        MainNavGraph.args.id to info.aid
                    )
                    nav.navigate(MainNavGraph.action.videoInfo_to_videoCommentList, args)
                }
            }
            2 -> {
                // 分享
                if (info == null) {
                    toast("视频信息未加载完成，请稍后再试")
                    return
                }
                var shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "bilibili视频分享")
                    putExtra(Intent.EXTRA_TEXT, "${info.title} https://www.bilibili.com/video/${info.bvid}")
                }
                requireActivity().startActivity(Intent.createChooser(shareIntent, "分享"))
            }
            3 -> {
                // 收藏
                if (info != null) {
                    val nav = Navigation.findNavController(requireActivity(), R.id.nav_bottom_sheet_fragment)
                    val args = bundleOf(
                        MainNavGraph.args.id to info.aid
                    )
                    nav.navigate(MainNavGraph.action.global_to_videoAddFavorite, args)
                }
            }
            4 -> {
                // 投币
                if (info != null) {
                    val nav = Navigation.findNavController(requireActivity(), R.id.nav_bottom_sheet_fragment)
                    val args = bundleOf(
                        MainNavGraph.args.num to if (info.copyright == 2) { 1 } else { 2 }
                    )
                    nav.navigate(MainNavGraph.action.global_to_videoCoin, args)
                }
            }
            5 -> {
                // 点赞
                viewModel.requestLike()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ui.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.info?.let {
            if (it.season?.is_jump == 1) {
                findNavController().popBackStack()
            }
        }
    }

    fun confirmCoin(num: Int) {
        viewModel.requestCoin(num)
    }

    fun confirmFavorite(favIds: List<String>, addIds: List<String>, delIds: List<String>) {
        viewModel.requestFavorite(favIds, addIds, delIds)
    }

    private fun playVideo(cid: String, title: String) {
        val info = viewModel.info
        if (info != null) {
            playerDelegate.playVideo(info.aid.toString(), cid, title)
        }
    }

    private fun toSelfLink (view: View, url: String) {
        val urlInfo = BiliUrlMatcher.findIDByUrl(url)
        val urlType = urlInfo[0]
        var urlId = urlInfo[1]
        if (urlType == "BV") {
            urlId = "BV$urlId"
        }
        val args = bundleOf(
            MainNavGraph.args.id to urlId
        )
        when(urlType){
            "AV", "BV" -> {
                args.putString(MainNavGraph.args.type, urlType)
                Navigation.findNavController(view)
                    .navigate(MainNavGraph.action.videoInfo_to_videoInfo, args)
            }
            else -> {
                BiliUrlMatcher.toUrlLink(view, url)
            }
        }
    }

    private fun toUser(view: View, mid: String) {
        val args = bundleOf(
            MainNavGraph.args.id to mid
        )
        Navigation.findNavController(view)
            .navigate(MainNavGraph.action.videoInfo_to_user, args)
    }

    private val handleUpperClick = View.OnClickListener {
        viewModel.info?.let { info ->
            toUser(it, info.owner.mid)
        }
    }

    private val handleMorePageClick = View.OnClickListener {
        viewModel.info?.let { info ->
            val nav = Navigation.findNavController(requireActivity(), R.id.nav_bottom_sheet_fragment)
            val args = bundleOf(
                MainNavGraph.args.id to info.aid,
                MainNavGraph.args.pages to viewModel.pages,
            )
            nav.navigate(MainNavGraph.action.global_to_videoPages, args)
        }
    }

    private val handleUpperItemClick = OnItemClickListener { adapter, view, position ->
        val item = viewModel.staffs[position]
        toUser(view, item.mid)
    }

    private val handlePageItemClick = OnItemClickListener { adapter, view, position ->
        val item = viewModel.pages[position]
        playVideo(item.cid, item.part)
    }

    private val handleRelateItemClick = OnItemClickListener { adapter, view, position ->
        val item = viewModel.relates[position]
        if (item.goto == "av") {
            val args = bundleOf(
                MainNavGraph.args.id to item.aid
            )
            Navigation.findNavController(view)
                .navigate(MainNavGraph.action.videoInfo_to_videoInfo, args)
        } else {
            val url = item.uri
            val re = BiliNavigation.navigationTo(view, url)
            if (!re) {
                if (url.indexOf("bilibili://") == 0) {
                    toast("不支持打开的链接：$url")
                } else {
                    BiliUrlMatcher.toUrlLink(view, url)
                }
            }
        }
    }

    private val handleRefresh = SwipeRefreshLayout.OnRefreshListener {
        viewModel.loadData()
    }

    private val handleLinkClickListener = ExpandableTextView.OnLinkClickListener { view, linkType, content, selfContent -> //根据类型去判断
        when (linkType) {
            LinkType.LINK_TYPE -> {
                val url = content
                val re = BiliNavigation.navigationTo(view, url)
                if (!re) {
                    if (url.indexOf("bilibili://") == 0) {
                        toast("不支持打开的链接：$url")
                    } else {
                        BiliUrlMatcher.toUrlLink(view, url)
                    }
                }
            }
            LinkType.MENTION_TYPE -> {
//                toast("你点击了@用户 内容是：$content")
            }
            LinkType.SELF -> {
                toSelfLink(view, selfContent)
            }
        }
    }

    val pageItemUi = miaoBindingItemUi<VideoPageInfo> { item, index ->
        frameLayout {
            layoutParams = lParams {
                width = wrapContent
                height = matchParent
                rightMargin = dip(5)
            }
            val enabled = playerStore.state.info.cid != item.cid
            setBackgroundResource(R.drawable.shape_corner)
            _isEnabled = enabled

            views {
                +textView {
                    horizontalPadding = dip(10)
                    verticalPadding = dip(5)
                    textColorResource = R.color.text_black
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    maxWidth = dip(120)
                    minWidth = dip(60)
                    maxLines = 1
                    gravity = Gravity.LEFT
                    ellipsize = TextUtils.TruncateAt.END
                    textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
                    _textColorResource = if (enabled) {
                        R.color.text_black
                    } else {
                        config.themeColorResource
                    }
                    _text = item.part
                }..lParams {
                    gravity = Gravity.CENTER
                }
            }
        }
    }

    fun MiaoUI.pageView(): View {
        return horizontalLayout {
            views {
                +frameLayout {

                    views {
                        +recyclerView {
                            val lm = LinearLayoutManager(context)
                            lm.orientation = LinearLayoutManager.HORIZONTAL
                            _miaoLayoutManage(lm)

                            _miaoAdapter(
                                items = viewModel.pages,
                                itemUi = pageItemUi,
                                depsAry = arrayOf(playerStore.state.info.cid),
                            ) {
                                setOnItemClickListener(handlePageItemClick)
                            }
                        }..lParams {
                            width = matchParent
                            height = matchParent
                        }

                        +imageView {
                            scaleType = ImageView.ScaleType.FIT_XY
                            imageResource = R.drawable.shape_gradient
                        }..lParams {
                            gravity = Gravity.RIGHT
                            width = dip(10)
                            height = matchParent
                        }
                    }
                }..lParams {
                    width = matchParent
                    height = matchParent
                    weight = 1f
                }

                +imageView {
                    setImageResource(R.drawable.ic_navigate_next_black_24dp)
                    setBackgroundResource(config.selectableItemBackgroundBorderless)
                    setOnClickListener(handleMorePageClick)
//                    setOnClickListener {
//                        val fragment = PagesFragment.newInstance(viewModel.id, viewModel.pages, 0)
//                        MainActivity.of(context)
//                            .showBottomSheet(fragment)
//                    }
                    _show = viewModel.pages.size > 2
                }..lParams(dip(24), dip(24)) {
                    gravity = Gravity.CENTER
                }
            }
        }
    }

    val upperItemUi = miaoBindingItemUi<VideoStaffInfo> { item, index ->
        verticalLayout {
            layoutParams = ViewGroup.LayoutParams(dip(64), wrapContent)
            topPadding = dip(10)
            bottomPadding = dip(5)
            gravity = Gravity.CENTER
            setBackgroundResource(config.selectableItemBackground)

            views {
                +rcImageView {
                    isCircle = true
                    _network(item.face)
                }..lParams {
                    height = dip(40)
                    width = dip(40)
                }

                +textView {
                    setTextColor(config.foregroundColor)
                    gravity = Gravity.CENTER
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    _text = item.name
                }
                +textView {
                    setTextColor(config.themeColor)
                    textSize = 12f
                    gravity = Gravity.CENTER
                    maxLines = 1
                    _text = item.title
                }
            }
        }
    }

    fun MiaoUI.upperView(): View {
        return verticalLayout {

            val isMutableUpper = viewModel.info?.staff?.isEmpty() == false

            views {
                // 单个up主信息
                +horizontalLayout {
                    setBackgroundResource(config.selectableItemBackground)
                    setOnClickListener(handleUpperClick)
                    _show = !isMutableUpper

                    views {
                        +rcImageView {
                            isCircle = true
                            _network(viewModel.info?.owner?.face)
                        }..lParams {
                            height = dip(40)
                            width = dip(40)
                        }

                        +verticalLayout {

                            views {
                                +textView {
                                    setTextColor(config.foregroundColor)
                                    _text = viewModel.info?.owner?.name ?: ""
                                }
                                +textView {
                                    textSize = 12f
                                    setTextColor(config.foregroundAlpha45Color)
                                    _text = "发表于 " + NumberUtil.converCTime(viewModel.info?.pubdate)
                                }
                            }

                        }..lParams {
                            leftMargin = dip(8)
                        }
                    }
                }..lParams {
                    width = matchParent
                }

                // 多个up主信息
                +recyclerView {
                    _show = isMutableUpper
                    _miaoLayoutManage(
                        LinearLayoutManager(context).apply {
                            orientation = LinearLayoutManager.HORIZONTAL
                        }
                    )
                    _miaoAdapter(
                        itemUi = upperItemUi,
                        items = viewModel.staffs
                    ) {
                        setOnItemClickListener(handleUpperItemClick)
                    }
                }

                +textView {
                    _show = isMutableUpper
                    textSize = 12f
                    setTextColor(config.foregroundAlpha45Color)
                    _text = "发表于 " + NumberUtil.converCTime(viewModel.info?.pubdate)
                }..lParams {
                    topMargin = dip(10)
                }
            }

        }
    }

    fun MiaoUI.headerView(): View {
        val videoInfo = viewModel.info
        return verticalLayout {
            views {
                +horizontalLayout {
                    views {
                        +rcImageView {
                            radius = dip(5)
                            _network(videoInfo?.pic)
                        }..lParams {
                            width = dip(150)
                            height = dip(100)
                            rightMargin = dip(10)
                        }

                        +verticalLayout {

                            views {
                                // 标题
                                +textView {
                                    textSize = 16f
                                    ellipsize = TextUtils.TruncateAt.END
                                    maxLines = 2
                                    setTextColor(config.foregroundColor)
                                    _text = videoInfo?.title ?: ""
                                }..lParams(weight = 1f)

                                // 播放量
                                +horizontalLayout {

                                    views {

                                        +imageView {
                                            imageTintList = ColorStateList.valueOf(config.foregroundAlpha45Color)
                                            setImageResource(R.drawable.ic_info_views)
                                        }..lParams(dip(14), dip(14)) {
                                            gravity = Gravity.CENTER
                                        }
                                        +textView {
                                            textSize = 12f
                                            setTextColor(config.foregroundAlpha45Color)
                                            _text = NumberUtil.converString(videoInfo?.stat?.view ?: "")
                                        }..lParams {
                                            leftMargin = dip(3)
                                            rightMargin = dip(16)
                                        }

                                        +imageView {
                                            imageTintList = ColorStateList.valueOf(config.foregroundAlpha45Color)
                                            setImageResource(R.drawable.ic_info_danmakus)
                                        }..lParams(dip(14), dip(14)) {
                                            gravity = Gravity.CENTER
                                        }
                                        +textView {
                                            textSize = 12f
                                            setTextColor(config.foregroundAlpha45Color)
                                            _text = NumberUtil.converString(videoInfo?.stat?.danmaku ?: "")
                                        }..lParams {
                                            leftMargin = dip(3)
                                            rightMargin = dip(16)
                                        }
                                    }

                                }..lParams {
                                    width = matchParent
                                    height = wrapContent
                                    gravity = Gravity.CENTER_VERTICAL
                                }
                            }
                        }..lParams(matchParent, matchParent)


                    }
                }

                +upperView()..lParams {
                    width = matchParent
                    height = wrapContent
                    topMargin = dip(10)
                }
                +pageView()..lParams {
                    width = matchParent
                    height = dip(48)
                    topMargin = dip(10)
                }

                +expandableTextView {
                    setLineSpacing(dip(4).toFloat(), 1.0f)
                    textSize = 14f
                    setMaxLine(2)
                    isNeedContract = true
                    isNeedExpend = true
                    setNeedMention(false)
                    isNeedSelf = true
                    setNeedConvertUrl(false)
                    _setContent(viewModel.info?.desc ?: "")
                    linkClickListener = handleLinkClickListener
                }..lParams {
                    width = matchParent
                    height = wrapContent
                    topMargin = dip(10)
                }
            }

        }
    }

    val relateItemUi = miaoBindingItemUi<VideoRelateInfo> { item, index ->
        videoItem(
            title = item.title,
            pic = item.pic,
            upperName = item.owner?.name,
            playNum = item.stat?.view,
            damukuNum = item.stat?.danmaku
        )
    }

    val ui = miaoBindingUi {
        connectStore(viewLifecycleOwner, windowStore)
        connectStore(viewLifecycleOwner, playerStore)
        val contentInsets = windowStore.getContentInsets(parentView)
        val info = viewModel.info
        // 监听info改变，修改页面标题
        miaoEffect(listOf(info, info?.req_user, info?.staff)) {
            pageConfig.notifyConfigChanged()
        }
        recyclerView {
            _leftPadding = contentInsets.left
            _rightPadding = contentInsets.right
            backgroundColor = config.windowBackgroundColor
            _miaoLayoutManage(
                GridAutofitLayoutManager(requireContext(), requireContext().dip(300))
            )

            val mAdapter = _miaoAdapter(
                items = viewModel.relates,
                itemUi = relateItemUi,
            ) {
                setOnItemClickListener(handleRelateItemClick)
            }

            headerViews(mAdapter) {
                +headerView().apply {
                    horizontalPadding = config.pagePadding
                    _topPadding = contentInsets.top + config.pagePadding
                }..lParams(matchParent, wrapContent)
            }
            footerViews(mAdapter) {
                +frameLayout {
                }..lParams {
                    _height = contentInsets.bottom
                }
            }
        }.wrapInSwipeRefreshLayout {
            setColorSchemeResources(config.themeColorResource)
            setOnRefreshListener(handleRefresh)
            _isRefreshing = viewModel.loading
        }
    }

}