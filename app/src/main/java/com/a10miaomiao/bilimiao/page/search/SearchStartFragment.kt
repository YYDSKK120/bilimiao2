package com.a10miaomiao.bilimiao.page.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import cn.a10miaomiao.miao.binding.android.view.*
import cn.a10miaomiao.miao.binding.android.widget._text
import com.a10miaomiao.bilimiao.MainNavGraph
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.*
import com.a10miaomiao.bilimiao.comm.delegate.helper.SupportHelper
import com.a10miaomiao.bilimiao.comm.mypage.MyPage
import com.a10miaomiao.bilimiao.comm.mypage.myPageConfig
import com.a10miaomiao.bilimiao.comm.recycler.MiaoBindingItemUi
import com.a10miaomiao.bilimiao.comm.recycler._miaoAdapter
import com.a10miaomiao.bilimiao.comm.recycler._miaoLayoutManage
import com.a10miaomiao.bilimiao.comm.recycler.miaoBindingItemUi
import com.a10miaomiao.bilimiao.config.ViewStyle
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.store.WindowStore
import com.a10miaomiao.bilimiao.template.TemplateViewModel
import com.a10miaomiao.bilimiao.widget.flow.FlowLayoutManager
import com.chad.library.adapter.base.listener.OnItemClickListener
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexboxLayoutManager
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import splitties.dimensions.dip
import splitties.toast.toast
import splitties.views.*
import splitties.views.dsl.core.*
import splitties.views.dsl.recyclerview.recyclerView

class SearchStartFragment : Fragment(), DIAware, MyPage {

    override val pageConfig = myPageConfig {
        title = "搜索"
    }

    override val di: DI by lazyUiDi(ui = { ui })

    private val ID_editText = 102

    private lateinit var mEditText: EditText

    private val viewModel by diViewModel<SearchStartViewModel>(di)
    private val supportHelper by instance<SupportHelper>()

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
        mEditText = view.findViewById(ID_editText)
        mEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val text = p0.toString()
                viewModel.loadSuggestData(text, mEditText)
            }
        })
        val text = requireArguments().getString(MainNavGraph.args.text)
        if (text != null) {
            mEditText.setText(text)
        }
    }

    override fun onResume() {
        super.onResume()
        supportHelper.showSoftInput(mEditText)
    }

    override fun onPause() {
        super.onPause()
        supportHelper.hideSoftInput(mEditText)
    }

    private val handleEditorAction = TextView.OnEditorActionListener { v, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED
            && event.action == KeyEvent.ACTION_DOWN) {
            handleSearchClick.onClick(v)
            return@OnEditorActionListener true
        }
        return@OnEditorActionListener false
    }
    private val handleSearchClick = View.OnClickListener {
        val keyword = mEditText.text.toString()
        viewModel.startSearch(keyword, it)
    }
    private val handleCloseClick = View.OnClickListener {
        val text = mEditText.text.toString()
        if (text.isEmpty()) {
            findNavController().popBackStack()
        } else {
            mEditText.setText("")
        }
    }

    private val handleTagItemClick = OnItemClickListener { adapter, view, position ->
        val item = adapter.data[position]
        if (item is String) {
            viewModel.startSearch(item, view)
        }
    }

    private val handleDeleteHistoryClick = View.OnClickListener {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("确定清空搜索历史，喵？")
            setNegativeButton("确定清空") { dialog, which ->
                viewModel.deleteAllSearchHistory()
                toast("已清空了喵")
            }
            setPositiveButton("取消", null)
        }.show()

    }

    val itemTagUi = miaoBindingItemUi<String> { item, index ->
        frameLayout {
            views {
                +frameLayout {
                    apply(ViewStyle.roundRect(dip(5)))
                    setBackgroundResource(config.selectableItemBackground)

                    views {
                        +textView {
                            backgroundColor = config.blockBackgroundColor
                            padding = dip(5)
                            _text = item
                        }
                    }
                }..lParams {
                    rightMargin =  dip(8)
                    bottomMargin = dip(5)
                }
            }
        }

    }

    fun MiaoUI.secrchBoxView (): View {
        return frameLayout {
            backgroundColor = config.blockBackgroundColor
            bottomPadding = config.dividerSize
            horizontalPadding = config.pagePadding

            views {
                val iconSize = dip(24)
                +editText(ID_editText) {
                    hint = "请输入ID或关键字"
                    horizontalPadding = iconSize + dip(15)

                    imeOptions = EditorInfo.IME_ACTION_SEARCH
                    setOnEditorActionListener(handleEditorAction)

                }..lParams(matchParent, dip(48))
                +imageView {
                    setImageResource(R.drawable.ic_search_24dp)
                    setBackgroundResource(config.selectableItemBackgroundBorderless)
                }..lParams(iconSize, iconSize) {
                    leftMargin = config.dividerSize
                    gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                    setOnClickListener(handleSearchClick)
                }
                +imageView {
                    setImageResource(R.drawable.ic_close_grey_24dp)
                    setBackgroundResource(config.selectableItemBackgroundBorderless)
                    setOnClickListener(handleCloseClick)
                }..lParams(iconSize, iconSize) {
                    rightMargin = config.dividerSize
                    gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                }
            }
        }
    }

    val ui = miaoBindingUi {
        val windowStore = miaoStore<WindowStore>(viewLifecycleOwner, di)
        val contentInsets = windowStore.getContentInsets(parentView)

        verticalLayout {
            _leftPadding = contentInsets.left
            _rightPadding = contentInsets.right
            _bottomPadding = contentInsets.bottom

            views {
                +secrchBoxView().apply {
                    _topPadding = contentInsets.top
                }..lParams(width = matchParent)

                +verticalLayout {
                    views {
                        +recyclerView {
                            isNestedScrollingEnabled = false
                            _miaoLayoutManage(
                                FlexboxLayoutManager(requireActivity()).apply {
                                    flexDirection = FlexDirection.ROW
                                    flexWrap = FlexWrap.WRAP
                                }
                            )
                            _miaoAdapter(
                                items = viewModel.suggestList,
                                itemUi = itemTagUi,
                            ) {
                                setOnItemClickListener(handleTagItemClick)
                            }
                        }..lParams(matchParent, wrapContent) {
                            horizontalMargin = config.pagePadding
                            verticalMargin = config.dividerSize
                        }

                        +horizontalLayout {
                            gravity = Gravity.CENTER_HORIZONTAL
                            _show = viewModel.historyList.isNotEmpty()

                            views {
                                +textView {
                                    textSize = 16f
                                    text = "最近搜索"
                                }..lParams(matchParent, wrapContent) {
                                    weight = 1f
                                }

                                +textView {
                                    text = "删除全部"
                                    alpha = 0.75f
                                    setBackgroundResource(config.selectableItemBackgroundBorderless)
                                    setOnClickListener(handleDeleteHistoryClick)
                                }..lParams(wrapContent, wrapContent)
                            }
                        }..lParams(matchParent, wrapContent) {
                            horizontalMargin = config.pagePadding
                            bottomMargin = config.dividerSize
                        }

                        +recyclerView {
                            isNestedScrollingEnabled = false
                            _miaoLayoutManage(
                                FlexboxLayoutManager(requireActivity()).apply {
                                    flexDirection = FlexDirection.ROW
                                    flexWrap = FlexWrap.WRAP
                                }
                            )
                            _miaoAdapter(
                                items = viewModel.historyList,
                                itemUi = itemTagUi,
                            ) {
                                setOnItemClickListener(handleTagItemClick)
                            }
                        }..lParams(matchParent, wrapContent) {
                            horizontalMargin = config.pagePadding
                            verticalMargin = config.dividerSize
                        }
                    }
                }.wrapInNestedScrollView {
                }..lParams(matchParent, matchParent) {
                    weight = 1f
                }
            }
        }
    }

}