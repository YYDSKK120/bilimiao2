package com.a10miaomiao.bilimiao.template

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import cn.a10miaomiao.miao.binding.android.view._bottomPadding
import cn.a10miaomiao.miao.binding.android.view._leftPadding
import cn.a10miaomiao.miao.binding.android.view._rightPadding
import cn.a10miaomiao.miao.binding.android.view._topPadding
import cn.a10miaomiao.miao.binding.android.widget._text
import com.a10miaomiao.bilimiao.comm.*
import com.a10miaomiao.bilimiao.comm.mypage.MyPage
import com.a10miaomiao.bilimiao.comm.mypage.myPageConfig
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.store.WindowStore
import kotlinx.coroutines.launch
import org.kodein.di.*
import splitties.dimensions.dip
import splitties.views.dsl.core.*

class TemplateFragment : Fragment(), DIAware, MyPage {

    override val pageConfig = myPageConfig {
        title = "bilimiao2"
    }

    override val di: DI by lazyUiDi(ui = { ui })

    private val viewModel by diViewModel<TemplateViewModel>(di)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui.parentView = container
        return ui.root
    }

    val ui = miaoBindingUi {
        val windowStore = miaoStore<WindowStore>(viewLifecycleOwner, di)
        val contentInsets = windowStore.getContentInsets(parentView)

        verticalLayout {
            _leftPadding = contentInsets.left + config.pagePadding
            _rightPadding = contentInsets.right + config.pagePadding
            _topPadding = contentInsets.top + config.pagePadding
            _bottomPadding = contentInsets.bottom

        }
    }

}