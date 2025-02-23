package com.a10miaomiao.bilimiao.comm.delegate.player

import android.content.*
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import cn.a10miaomiao.download.BiliVideoEntry
import cn.a10miaomiao.download.DownloadFlieHelper
import cn.a10miaomiao.player.*
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.delegate.helper.PicInPicHelper
import com.a10miaomiao.bilimiao.comm.delegate.helper.StatusBarHelper
import com.a10miaomiao.bilimiao.comm.delegate.theme.ThemeDelegate
import com.a10miaomiao.bilimiao.comm.network.ApiHelper
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.page.setting.DanmakuSettingFragment
import com.a10miaomiao.bilimiao.store.PlayerStore
import com.a10miaomiao.bilimiao.store.UserStore
import com.a10miaomiao.bilimiao.widget.comm.getScaffoldView
import com.a10miaomiao.bilimiao.widget.player.MiniMediaController
import com.a10miaomiao.bilimiao.widget.player.SizeWatcherView
import kotlinx.coroutines.*
import master.flame.danmaku.controller.DrawHandler
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.ui.widget.DanmakuView
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import splitties.dimensions.dip
import tv.danmaku.ijk.media.player.IMediaPlayer
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask


class PlayerDelegate(
    private var activity: AppCompatActivity,
    override val di: DI,
) : DIAware {
    private val TAG = PlayerDelegate::class.simpleName

    // 播放器player_background
    lateinit var playerService: PlayerService
    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            playerService = (service as PlayerBinder).getmPlayerService()
            initPlayer()
            initDanmaku()
            initController()
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }

    // 播放源头信息
    private val plalerSource = PlayerSourceInfo()

    private var quality = 64 // 默认[高清 720P]懒得做记忆功能，先不弄

    // 播放器信息
    private var sources = ArrayList<VideoSource>()
    private var acceptDescription = listOf<String>()
    private var acceptQuality = listOf<Int>()
    private var lastPosition = 0L //记录播放位置

    private var danmakuContext: DanmakuContext? = null
    private var danmakuTime = object : DanmakuTimer() {
        private var lastTime = 0L
        override fun currMillisecond(): Long {
            val currentPosition = mPlayer.currentPosition
            if (
                currentPosition < lastTime
                && lastTime - currentPosition < 1000
            ) {
                return lastTime
            }
            lastTime = currentPosition
            return currentPosition
        }

        override fun update(curr: Long): Long {
            lastInterval = curr - lastTime
            return lastInterval
        }
    }

//    private var disposable: Disposable? = null

    private val width by lazy {
        (activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.width
    }
    private val height by lazy {
        (activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.height
    }
    private val mAudioManager by lazy {
        activity.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager
    }

    val scaffoldApp = activity.getScaffoldView()


    private val playerStore by instance<PlayerStore>()
    private val userStore by instance<UserStore>()

    private val statusBarHelper by instance<StatusBarHelper>()
    private val themeDelegate by instance<ThemeDelegate>()

    // 组件
    private val mRoot = activity.findViewById<View>(R.id.mRoot)
    private val mController = activity.findViewById<MyMediaController>(R.id.mController)
    private val mPlayer = activity.findViewById<VideoPlayerView>(R.id.mPlayer)
    private val mDanmaku = activity.findViewById<DanmakuView>(R.id.mDanmaku)
    private val mCenterLayout = activity.findViewById<View>(R.id.mCenterLayout)
    private val mCenterTv = activity.findViewById<TextView>(R.id.mCenterTv)
    private val mText = activity.findViewById<TextView>(R.id.mText)
    private val mProgressLayout = activity.findViewById<View>(R.id.mProgressLayout)
    private val mMiniController = activity.findViewById<MiniMediaController>(R.id.mMiniController)
    private val mProgressBar = activity.findViewById<ProgressBar>(R.id.mProgressBar)

    //    private val mVideoTitleText = activity.findViewById<TextView>(R.id.videoTitleText)
    private val mSizeWatcher = activity.findViewById<SizeWatcherView>(R.id.mSizeWatcher)
    private val mErrorLayout = activity.findViewById<View>(R.id.mErrorLayout)
    private val mErrorTv = activity.findViewById<TextView>(R.id.mErrorTv)
    private val mErrorTryPlayTv = activity.findViewById<TextView>(R.id.mErrorTryPlayTv)
    private val mErrorClosePlayTv = activity.findViewById<TextView>(R.id.mErrorClosePlayTv)

    private var mPicInPicHelper: PicInPicHelper? = null

    val isMiniPlayer = MutableLiveData<Boolean>()

    // 加载
    private var timer = Timer()
    private val playerCoroutineScope = PlayerCoroutineScope()
//private val playerCoroutineScope = activity.lifecycleScope
    private var intervalJob: Job? = null
    private var loadDanmakuJob: Job? = null
    private var loadPlayurlJob: Job? = null
    private var historyJob: Job? = null

    fun onCreate(savedInstanceState: Bundle?) {
        val serviceIntent = Intent(activity, PlayerService::class.java)
        activity.startService(serviceIntent)
        activity.bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mPicInPicHelper = PicInPicHelper(activity, mPlayer)
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        quality = prefs.getInt("player_quality", 64)
    }

    /**
     * 初始化控制器
     */
    private fun initController() {
        mErrorTryPlayTv.setOnClickListener {
            restartPlay(mPlayer.currentPosition)
        }
        mErrorClosePlayTv.setOnClickListener {
            scaffoldApp.showPlayer = false
            stopPlay()
        }
        mRoot.setOnClickListener {
            val mediaController = mPlayer.mediaController
            if (mediaController.isShowing) {
                mediaController.hide()
            } else {
                mediaController.show()
            }
        }
        mController.setTitle(plalerSource.title)
        mController.setMediaPlayer(mPlayer)
        mController.setVisibilityChangedEvent(::setSystemUIVisible)
        mController.setDanmakuSwitchEvent {
            if (it) {
                mDanmaku.show()
            } else {
                mDanmaku.hide()
            }
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            prefs.edit().putBoolean("danmaku_show", it).apply()
            val intent = Intent(DanmakuSettingFragment.UPDATE_ACTION)
            activity.sendBroadcast(intent)
        }
        mController.setVideoBackEvent {
            isMiniPlayer.value = true
        }
        mController.setQualityEvent {
            val popup = QualityPopupMenu(
                activity = activity,
                anchor = it,
                list = acceptDescription,
                value = acceptDescription[acceptQuality.indexOf(quality)]
            )
            popup.setOnMenuItemClickListener(this.handleChangedQuality)
            popup.show()
        }

        mController.setRestartPlayEvent(this::restartPlay)
        mMiniController.restartPlayEvent = this::restartPlay

        mMiniController.setMediaPlayer(mPlayer)
        mMiniController.setBackOnClick(View.OnClickListener {
            scaffoldApp.showPlayer = false
            stopPlay()
        })
        mMiniController.setZoomOnClick(View.OnClickListener {
            isMiniPlayer.value = false
        })
//        val statusBarHeight = activity.getStatusBarHeight()
//        mMiniController.setPadding(0, statusBarHeight, 0, 0)
//        mVideoTitleText.layoutParams.height = statusBarHeight

        // 展开菜单
        mController.inflateMore(R.menu.mini_player_toolbar)
        mController.setOnMoreMenuItemClickListener(this::onMenuItemClick)
        mMiniController.setOnMenuItemClickListener(this::onMenuItemClick)

        mSizeWatcher.onSizeChangedListener = {
            mPlayer.post { mPlayer.requestLayout() }
            mPlayer.setVideoLayout()
        }

        themeDelegate.observeTheme(activity, Observer {
            val themeColor = activity.config.themeColor
            mController.updateColor(themeColor)
            mMiniController.updateColor(themeColor)
            mProgressBar.indeterminateTintList = ColorStateList.valueOf(themeColor)
        })

        isMiniPlayer.observe(activity, Observer {
            setPlayerMediaController(it!!)
            activity.apply {
                if (it == null || it) {
                    scaffoldApp.fullScreenPlayer = false
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    setSystemUIVisible(true)
                } else {
                    scaffoldApp.fullScreenPlayer = true
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }
            initDanmakuContext()
            mPlayer?.setVideoLayout()
        })
        isMiniPlayer.value = true
    }

    /**
     * 初始化弹幕引擎
     */
    private fun initDanmaku() {
        showText("初始化弹幕引擎")
        mDanmaku.enableDanmakuDrawingCache(true)
        danmakuContext = DanmakuContext.create()
        initDanmakuContext()
    }

    private fun initDanmakuContext() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        var scaleTextSize = prefs.getString("danmaku_fontsize", "1")?.toFloatOrNull() ?: 1f
        val danmakuSpeed = prefs.getString("danmaku_speed", "1")?.toFloatOrNull() ?: 1f
        val danmakuShow = prefs.getBoolean("danmaku_show", true)
        val danmakuR2LShow = prefs.getBoolean("danmaku_r2l_show", true)
        val danmakuFTShow = prefs.getBoolean("danmaku_ft_show", true)
        val danmakuFBShow = prefs.getBoolean("danmaku_fb_show", true)
        val danmakuSpecialShow = prefs.getBoolean("danmaku_special_show", true)
        //设置最大显示行数
        var maxLinesPair = mapOf<Int, Int>()
        //设置是否禁止重叠
        val overlappingEnablePair = mapOf(
            BaseDanmaku.TYPE_SCROLL_RL to true,
            BaseDanmaku.TYPE_FIX_TOP to true
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity.isInPictureInPictureMode) {
            scaleTextSize *= 0.6f
            maxLinesPair = mapOf(
                BaseDanmaku.TYPE_SCROLL_RL to 4,
                BaseDanmaku.TYPE_FIX_TOP to 2,
                BaseDanmaku.TYPE_FIX_BOTTOM to 2
            )
        } else if (isMiniPlayer.value === true) {
            maxLinesPair = mapOf(
                BaseDanmaku.TYPE_SCROLL_RL to 5
            )
        }
        //设置弹幕样式
        danmakuContext?.apply {
            ftDanmakuVisibility = danmakuFTShow
            fbDanmakuVisibility = danmakuFBShow
            r2LDanmakuVisibility = danmakuR2LShow
            specialDanmakuVisibility = danmakuSpecialShow
//            setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3f)
//            isDuplicateMergingEnabled = false
            setScrollSpeedFactor(danmakuSpeed)
            setScaleTextSize(scaleTextSize)
            setMaximumLines(maxLinesPair)
//            preventOverlapping(overlappingEnablePair)
        }
        mController.setDanmakuShow(danmakuShow)
        if (danmakuShow) {
            mDanmaku.show()
        } else {
            mDanmaku.hide()
        }
    }

    /**
     * 初始化播放器
     */
    private fun initPlayer() {
        showText("初始化播放器")
        //mPlayerView.setMediaBufferingIndicator(mBufferingIndicator)
        mPlayer.requestFocus()
        mPlayer.setOnInfoListener(onInfoListener)
        mPlayer.setOnSeekCompleteListener(onSeekCompleteListener)
        mPlayer.setOnCompletionListener(onCompletionListener)
        mPlayer.setOnControllerEventsListener(onControllerEventsListener)
        mPlayer.setOnGestureEventsListener(onGestureEventsListener)
        mPlayer.setOnErrorListener(onErrorListener)
        playerService.setUserAgent("Bilibili Freedoooooom/MarkII")
        playerService.setVideoPlayerView(mPlayer)
    }

    private fun onMenuItemClick(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.mini_window -> {
                mPicInPicHelper?.enterPictureInPictureMode()
            }
            R.id.video_setting -> {
                val nav = activity.findNavController(R.id.nav_bottom_sheet_fragment)
                nav.navigate(Uri.parse("bilimiao://setting/video"))
            }
            R.id.danmuku_setting -> {
                val nav = activity.findNavController(R.id.nav_bottom_sheet_fragment)
                nav.navigate(Uri.parse("bilimiao://setting/danmaku"))
            }
        }
        return true
    }

    fun updateDanmukuSetting() {
        initDanmakuContext()
    }

    fun playBangumi(sid: String, epid: String, cid: String, title: String) {
        stopPlay()
        scaffoldApp.showPlayer = true
        plalerSource.setBangumi(
            sid, epid, cid, title
        )
//        mVideoTitleText.text = "正在播放：${title}"
        playerCoroutineScope.onStart()
        loadDanmaku()
        playerStore.setPlayerInfo(plalerSource)
    }

    fun playVideo(aid: String, cid: String, title: String) {
        stopPlay()
        scaffoldApp.showPlayer = true
        plalerSource.setVideo(
            aid, cid, title
        )
//        mVideoTitleText.text = "正在播放：${title}"
        playerCoroutineScope.onStart()
        loadDanmaku()
        playerStore.setPlayerInfo(plalerSource)
        historyReport()
    }

    fun playLocalVideo(biliVideo: BiliVideoEntry) {
        stopPlay()
        scaffoldApp.showPlayer = true
        plalerSource.setLocalVideo(biliVideo)
//        mVideoTitleText.text = "正在播放：${title}"
        playerCoroutineScope.onStart()
        loadDanmaku()
        playerStore.setPlayerInfo(plalerSource)
        historyReport()
    }

    /**
     * 记录历史进度
     */
    fun historyReport() = playerCoroutineScope.launch {
        if (!userStore.isLogin()) {
            return@launch
        }
        if (plalerSource.type != PlayerSourceInfo.VIDEO) {
            return@launch
        }
        if (plalerSource.aid.isBlank() || plalerSource.cid.isBlank()) {
            return@launch
        }
        try {
            val realtimeProgress = (mPlayer.currentPosition / 1000).toString()  // 秒数
            MiaoHttp.request {
                url = "https://api.bilibili.com/x/v2/history/report"
                formBody = ApiHelper.createParams(
                    "aid" to plalerSource.aid,
                    "cid" to plalerSource.cid,
                    "progress" to realtimeProgress,
                    "realtime" to realtimeProgress,
                    "type" to "3"
                )
                method = MiaoHttp.POST
            }.awaitCall()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 加载弹幕
     */
    private fun loadDanmaku() = playerCoroutineScope.launch {
        hideError()
        mController.setTitle(plalerSource.title)
        showText("装载弹幕资源")
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val isDanmakuTimeSync = prefs.getBoolean("danmaku_time_sync", true)
        try {
            val danmukuParser = withContext(Dispatchers.IO) {
                getBiliDanmukuStream().let {
                    val loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI)
                    loader.load(it)
                    val parser = BiliDanmukuParser()
                    if (isDanmakuTimeSync) {
                        parser.timer = danmakuTime
                    }
                    val dataSource = loader.dataSource
                    parser.load(dataSource)
                    parser
                }
            }
            mDanmaku.prepare(danmukuParser, danmakuContext)
            mDanmaku.showFPS(false)
            mDanmaku.enableDanmakuDrawingCache(false)
            mDanmaku.setCallback(onDrawHandlerCallback)
        } catch (e: Exception) {
            e.printStackTrace()
            showText("装载弹幕失败")
        }
    }

    private suspend fun getBiliDanmukuStream(): InputStream {
        return BiliApiService.playerAPI.getDanmakuList(plalerSource.cid)
            .awaitCall().let {
                ByteArrayInputStream(CompressionTools.decompressXML(it.body()!!.bytes()))
            }
    }

    /**
     * 加载视频播放地址
     */
    private fun loadPlayurl() = playerCoroutineScope.launch {
        showText("读取播放地址")
        try {
            Exception("unknown type")
            sources = withContext(Dispatchers.IO) {
                when(plalerSource.type) {
                    PlayerSourceInfo.BANGUMI, PlayerSourceInfo.VIDEO -> {
                        getNetwordSources()
                    }
                    PlayerSourceInfo.LOCAL_VIDEO -> {
                        getLocalSources()
                    }
                    else -> throw Exception("unknown type")
                }
            }
            withContext(Dispatchers.Main) {
                startPlay()
            }
        } catch (e: Exception) {
            showError(e.message ?: "无法连接到御坂网络")
        }
    }

    private suspend fun getNetwordSources(): ArrayList<VideoSource> {
        val playurlData = if (plalerSource.type == PlayerSourceInfo.VIDEO) {
            BiliApiService.playerAPI.getVideoPalyUrl(
                plalerSource.aid,
                plalerSource.cid,
                quality
            )
        } else {
            BiliApiService.playerAPI.getBangumiUrl(
                plalerSource.epid,
                plalerSource.cid,
                quality
            )
        }
        acceptDescription = playurlData.accept_description
        acceptQuality = playurlData.accept_quality
        quality = playurlData.quality
        acceptDescription = playurlData.accept_description
        acceptQuality = playurlData.accept_quality
        quality = playurlData.quality
        return ArrayList<VideoSource>().apply {
            for (durl in playurlData.durl) {
                this += VideoSource().apply {
                    uri = Uri.parse(durl.url)
                    length = durl.length
                    size = durl.size
                }
            }
        }
    }

    private fun getLocalSources(): ArrayList<VideoSource> {
        val localEntry = plalerSource.localEntry ?: throw Exception("ERROR: NOT FILE")
        val videoDir = DownloadFlieHelper.getVideoPageFileDir(activity, localEntry)
        val pageData = DownloadFlieHelper.getVideoPage(activity, localEntry)
        val videoFlie = File(
            videoDir, "0" + "." + pageData.format
        )
        val sources = arrayListOf(
            VideoSource().apply {
                uri = Uri.fromFile(videoFlie)
                length = pageData.segment_list[0].duration
                size = pageData.segment_list[0].bytes
            }
        )
        acceptDescription = listOf("本地")
        acceptQuality = listOf(pageData.quality)
        quality = pageData.quality
        return sources
    }

    fun restartPlay(position: Long) {
        lastPosition = position
        if (mPlayer != null && mPlayer.isDrawingCacheEnabled) {
            mPlayer.destroyDrawingCache()
        }
        mDanmaku?.release()
        playerService.release(false)
        playerCoroutineScope.onStop()
        playerCoroutineScope.onStart()
        loadDanmaku()
        hideError()
    }

    fun startPlay() {
        hideProgressText()
        playerService.setVideoURI(
            sources, if (plalerSource.type == PlayerSourceInfo.VIDEO) mapOf(
                "Referer" to "https://www.bilibili.com/video/av$plalerSource.aid",
                "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Safari/537.36"
            ) else mapOf()
        )

        timer.cancel()
        timer = Timer()
        timer.schedule(timerTask {
            activity.runOnUiThread {
                mController.setProgress()
                mController.updatePausePlay()
                mMiniController.setProgress()
                mMiniController.updatePausePlay()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (activity.isInPictureInPictureMode) {
                        mPicInPicHelper?.updatePictureInPictureActions()
                    }
                }
            }
        }, 0, 200)
        historyReport()
    }

    fun stopPlay() {
        playerCoroutineScope.onStop()
        historyReport()
//        mVideoTitleText.text = ""
        if (mPlayer != null && mPlayer.isDrawingCacheEnabled) {
            mPlayer.destroyDrawingCache()
        }
        if (mDanmaku != null) {
            mDanmaku.release()
        }
        playerService.release(false)
        playerStore.clearPlayerInfo()
        plalerSource.reset()

        timer.cancel()
    }

    private val handleChangedQuality = PopupMenu.OnMenuItemClickListener {
        val position = it.itemId
        quality = acceptQuality[position]
        loadPlayurl()
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        prefs.edit().putInt("player_quality", quality).apply()
        false
    }

    private val onInfoListener = IMediaPlayer.OnInfoListener { mp, what, extra ->
        if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
            if (mDanmaku != null && mDanmaku.isPrepared) {
                mDanmaku.pause()
            }
            showText("缓冲中")
        } else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
            if (mDanmaku != null && mDanmaku.isPaused) {
//                mDanmaku.resume()
                mDanmaku.start(mPlayer.currentPosition)
            }
            hideProgressText()
            if (lastPosition != 0L && lastPosition < mPlayer.duration) {
                mPlayer.seekTo(lastPosition)
            }
        }
        true
    }

    private val onErrorListener =
        IMediaPlayer.OnErrorListener { iMediaPlayer, framework_err, impl_err ->
            val message =
                if (framework_err == IMediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                    cn.a10miaomiao.player.R.string.video_error_text_invalid_progressive_playback
                } else {
                    cn.a10miaomiao.player.R.string.video_error_text_unknown
                }
            showError(activity.resources.getString(message))
            true
        }

    /**
     * 弹幕加载回调
     */
    private val onDrawHandlerCallback = object : DrawHandler.Callback {
        override fun drawingFinished() {

        }

        override fun danmakuShown(danmaku: BaseDanmaku?) {

        }

        override fun prepared() {
            activity.runOnUiThread {
                loadPlayurl()
            }
        }

        override fun updateTimer(timer: DanmakuTimer) {
        }
    }

    /**
     * 视频跳转事件回调
     */
    private val onSeekCompleteListener = IMediaPlayer.OnSeekCompleteListener {
        if (mDanmaku != null && mDanmaku.isPrepared) {
            mDanmaku.seekTo(it.currentPosition)
            if (!mPlayer.isPlaying) {
                mDanmaku.pause()
            }
        }
    }

    /**
     * 视频播放完成事件回调
     */
    private val onCompletionListener = IMediaPlayer.OnCompletionListener {
        if (mDanmaku != null && mDanmaku.isPrepared()) {
//            mDanmakuView.seekTo(0L)
            mDanmaku.pause()
        }
        mPlayer.pause()
    }

    /**
     * 控制条控制状态事件回调
     */
    private val onControllerEventsListener = object : VideoPlayerView.OnControllerEventsListener {
        override fun onVideoPause() {
            if (mDanmaku != null && mDanmaku.isPrepared()) {
                mDanmaku.pause()
                historyReport()
            }
        }

        override fun OnVideoResume() {
            if (mDanmaku != null && mDanmaku.isPaused()) {
                mDanmaku.resume()
//                mHandler.sendEmptyMessageDelayed(SYNC,200);
            }
        }
    }

    /**
     * 手势控制器
     */
    val onGestureEventsListener = object : VideoPlayerView.OnGestureEventsListener {
        var isLeft = true
        var maxVolume = 100
        var volume = 0
        var screenBrightness = 0f
        var num = 0
        var current = 0L
        var denominator = activity.dip(100f)

        override fun isLocked() = mController.isLocked

        override fun onDown(e: MotionEvent) {
            isLeft = e.x < width / 2
            maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            screenBrightness = activity.window.attributes.screenBrightness
            num = if (isLeft) {
                (screenBrightness * denominator).toInt()
            } else {
                ((volume.toFloat() / maxVolume.toFloat()) * denominator).toInt()
            }
            current = mPlayer.currentPosition
        }

        override fun onXScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float): Boolean {
            mController.setDragging(true)
            current -= (distanceX * 100).toLong()
            if (current > mPlayer.duration) {
                current = mPlayer.duration
            } else if (current < 0) {
                current = 0
            }
            showCenterText(MyMediaController.generateTime(current))
            mController.setProgress(current)
            return false
        }

        override fun onYScroll(e1: MotionEvent, e2: MotionEvent, distanceY: Float): Boolean {
            num += distanceY.toInt() / 2
            num = if (num > denominator) {
                denominator.toInt()
            } else if (0 > num) {
                0
            } else {
                num
            }
            if (isLeft) {
                val lp = activity.window.attributes
                lp.screenBrightness = num / denominator
                activity.window.attributes = lp
                showCenterText("亮度：$num%")
            } else {
                volume = ((num / denominator) * maxVolume).toInt()
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                showCenterText("音量：$num%")
            }
            return false
        }

        override fun onUp(e: MotionEvent?, isXScroll: Boolean) {
            if (isXScroll) {
                mPlayer.seekTo(current)
                mController.setDragging(false)
            }
            hideCenterText()
        }
    }

    private fun showCenterText(text: String) {
        mCenterLayout.visibility = View.VISIBLE
        mCenterTv.text = text
    }

    private fun hideCenterText() {
        mCenterLayout.visibility = View.GONE
    }

    private fun showText(text: String) {
        mText.text = text
        mProgressLayout.visibility = View.VISIBLE
    }

    private fun hideProgressText() {
        mProgressLayout.visibility = View.GONE
    }

    private fun showError(message: String) {
        mErrorLayout.visibility = View.VISIBLE
        mErrorTv.text = message
    }

    private fun hideError() {
        mErrorLayout.visibility = View.GONE
    }

    fun onBackPressed(): Boolean {
        if (mController.isLocked) {
            return true
        }
        if (isMiniPlayer.value == false) {
            isMiniPlayer.value = true
            return true
        }
        return false
    }


    fun onResume() {
        historyReport()
    }

    fun onPause() {
        historyReport()
    }

    fun onStart() {
        if (mPlayer != null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            if (!prefs.getBoolean("player_background", true)) {
                mPlayer.start()
            }
            if (mDanmaku != null && mPlayer.isPlaying) {
                mDanmaku.start(mPlayer.currentPosition)
            }
        }
    }

    fun onStop() {
        if (mPlayer != null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            if (!prefs.getBoolean("player_background", true)) {
                lastPosition = mPlayer.currentPosition
                mPlayer.pause()
            }
        }
        if (mDanmaku != null && mDanmaku.isPrepared) {
            mDanmaku.pause()
        }
    }

    fun onDestroy() {
        activity.unbindService(mConnection)
        stopPlay()
    }

    fun setVideoLayout() {
        mPlayer?.setVideoLayout()
    }

    private fun setPlayerMediaController(isMini: Boolean) {
        if (isMini) {
            mPlayer.setMediaController(mMiniController)
            mController.visibility = View.GONE
        } else {
            mPlayer.setMediaController(mController)
            mMiniController.visibility = View.GONE
        }
    }

    private fun setSystemUIVisible(show: Boolean) {
        statusBarHelper.isShowStatus = show
        statusBarHelper.isShowNavigation = isMiniPlayer.value == true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            scaffoldApp.rootWindowInsets?.let {
                mController.post {
                    mController.setDisplayCutout(it.displayCutout)
                }
            }
        }
    }


    fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        mPicInPicHelper?.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) { // 进入画中画模式，则隐藏其它控件
            // 隐藏视频控制器
            mMiniController.visibility = View.GONE
            mController.visibility = View.GONE
            // 视频组件全屏
            scaffoldApp.fullScreenPlayer = true
            // 调整弹幕样式，调小字体，限制行数
            initDanmakuContext()
        } else {
            scaffoldApp.fullScreenPlayer = false
            initDanmakuContext()
        }
    }

}