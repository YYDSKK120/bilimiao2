package com.a10miaomiao.bilimiao.page.setting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import cn.a10miaomiao.miao.binding.android.view._bottomPadding
import cn.a10miaomiao.miao.binding.android.view._leftPadding
import cn.a10miaomiao.miao.binding.android.view._rightPadding
import cn.a10miaomiao.miao.binding.android.view._topPadding
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.*
import com.a10miaomiao.bilimiao.comm.mypage.MyPage
import com.a10miaomiao.bilimiao.comm.mypage.myPageConfig
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.store.WindowStore
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import splitties.dimensions.dip
import splitties.views.backgroundColor
import splitties.views.dsl.core.*
import splitties.views.horizontalPadding
import splitties.views.imageResource
import splitties.views.verticalPadding

class AboutFragment : Fragment(), DIAware, MyPage {

    override val pageConfig = myPageConfig {
        title = "关于"
    }

    private val statement = """
        1、本程序为哔哩哔哩动画的第三方助手类工具，资源来自哔哩哔哩动画
        2、如果侵犯您的合法权益，请及时联系本人以第一时间删除
    """.trimIndent()

    override val di: DI by lazyUiDi(ui = { ui })

    private val windowStore by instance<WindowStore>()

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
        lifecycle.coroutineScope.launch {
            windowStore.connectUi(ui)
        }
    }

    private fun openUri(uriString: String){
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(uriString)
        startActivity(intent)
    }

    fun MiaoUI.lineView (): View {
        return frameLayout {
            backgroundColor = config.lineColor
        }
    }
    fun MiaoUI.itemView (
        title: String,
        subTitle: String,
        onClickListener: ((View) -> Unit)? = null
    ): View {
        return frameLayout {
            backgroundColor = config.blockBackgroundColor

            views {
                +horizontalLayout {
                    horizontalPadding = dip(10)
                    verticalPadding = dip(10)
                    setBackgroundResource(config.selectableItemBackground)
                    if (onClickListener != null)
                        setOnClickListener(onClickListener)

                    views {
                        +textView {
                            text = title
                            textSize = 16f
                            setTextColor(config.foregroundColor)
                        }

                        +textView {
                            textSize = 16f
                            text = subTitle
                            textAlignment = TextView.TEXT_ALIGNMENT_TEXT_END
                        }..lParams {
                            leftMargin = dip(5)
                            weight = 1f
                            width = matchParent
                        }
                    }
                }
            }
        }
    }

    val ui = miaoBindingUi {
        val insets = windowStore.getContentInsets(parentView)
        verticalLayout {
            _leftPadding = insets.left
            _topPadding = insets.top
            _rightPadding = insets.right
            _bottomPadding = insets.bottom

            views {
                +verticalLayout {
                    gravity = Gravity.CENTER

                    views {
                        +imageView {
                            imageResource = R.mipmap.ic_launcher
                        }..lParams {
                            height = dip(64)
                            width = dip(64)
                            bottomMargin = dip(2)
                        }
                        +textView {
                            text = "bilimiao 2.1"
                            setTextColor(config.foregroundColor)
                            textSize = 16f
                        }..lParams(wrapContent, wrapContent)
                    }

                }..lParams {
                    height = dip(120)
                    width = matchParent
                }

                +lineView()..lParams(matchParent, 2)
                val version = requireContext().run {
                    packageManager.getPackageInfo(packageName, 0).versionName
                }
                +itemView("版本", "v$version")
                +lineView()..lParams(matchParent, 2)
                +frameLayout()..lParams(height = dip(10))

                +lineView()..lParams(matchParent, 2)
                +itemView("作者", "by 10miaomiao.cn") { view ->
                    openUri("https://10miaomiao.cn/")
                }
                +lineView()..lParams(matchParent, 2)
                +itemView("酷安", "_10喵喵") { view ->
                    openUri("http://www.coolapk.com/u/602470")
                }
                +lineView()..lParams(matchParent, 2)
                +itemView("b站", "10喵喵") { view ->
                    openUri("https://space.bilibili.com/6789810")
                }
                +lineView()..lParams(matchParent, 2)
                +itemView("Github", "10miaomiao") { view ->
                    openUri("https://github.com/10miaomiao")
                }
                +lineView()..lParams(matchParent, 2)
                +itemView("Gitee", "10miaomiao") { view ->
                    openUri("https://gitee.com/10miaomiao")
                }
                +lineView()..lParams(matchParent, 2)
                +frameLayout()..lParams(height = dip(10))


                +lineView()..lParams(matchParent, 2)
                +itemView("项目地址(Github)", "github.com/10miaomiao/bilimiao2") { view ->
                    openUri("https://github.com/10miaomiao/bilimiao2")
                }
                +lineView()..lParams(matchParent, 2)
                +itemView("项目地址(Gitee)", "gitee.com/10miaomiao/bilimiao2") { view ->
                    openUri("https://gitee.com/10miaomiao/bilimiao2")
                }
                +lineView()..lParams(matchParent, 2)
                +itemView("使用声明", statement)
                +lineView()..lParams(matchParent, 2)
                +frameLayout()..lParams(height = dip(10))
            }
        }.wrapInNestedScrollView()
    }

}