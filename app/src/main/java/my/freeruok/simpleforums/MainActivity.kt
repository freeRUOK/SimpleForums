/* MainActivity.kt */
// * 2651688427@qq.com
// 应用程序的主活动展示所有论坛和所选择的主题帖列表
package my.freeruok.simpleforums

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

// 应用程序的启动活动, 显示默认主题列表
class MainActivity : AppCompatActivity() {
    val threadList = mutableListOf<Message>()
    lateinit var messageAdapter: MessageAdapter
    private lateinit var contentList: ListView
    lateinit var contentListText: TextView
    private lateinit var forumRadioGroup: RadioGroup
    private lateinit var statusText: TextView
    lateinit var swipeRefresh: SwipeRefreshLayout
    lateinit var orderSpinner: Spinner
    private var isDatabase: Boolean = false

    // 当前论坛实力
    companion object {
        lateinit var forum: Forum
        val orderStrings =
            arrayOf(ORDER_MODE_NEW_THREAD, ORDER_MODE_LAST_THREAD, ORDER_MODE_ESSENCE_THREAD)
        lateinit var messageIds: Map<Long, Long>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

// 注册搜索活动
        searchActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
// 处理获取的搜索关键字
                if (it.resultCode == RESULT_OK && it.data != null) {
                    val kw = it.data?.getStringExtra(KEYWORD_STR) ?: ""
                    if (kw.isNotEmpty()) {
                        keyword = kw
                        forum.load(this, keyword = keyword, isDatabase = isDatabase)
                    }
                }
            }

// 注册用户登录活动
        userLoginActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
// 处理用户登录活动结束后的状态
                if (it.resultCode == RESULT_OK) {
                    statusText.text = forum.statusText
                    forum.load(this, isDatabase = isDatabase)
                } else {
                    contentListText.text = "${forum.name}要求用户登录"
                }
            }

// 注册导出收藏夹外部活动
        exportCollectorActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument()) {
// 处理用户选择文件后的状态
                if (it != null) {
                    Util.exportCollector(it, this)
                } else {
                    Util.toast("取消导出")
                }
            }

        shareActivityLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            }

// 处理用户协议
        checkLicense()
// 初始化相关工具包， 比如震动等
        Util.init()

// 载入网站设置
        loadForum()
// 设置listView无数据的情况下展示的view
        contentList.emptyView = contentListText

// 底部网站被切换
        forumRadioGroup.setOnCheckedChangeListener { _, _ ->
            isDatabase = false
            loadForum()
            val pos = orderStrings.indexOf(forum.currentOrder)
            if (pos != -1) {
                orderSpinner.setSelection(pos)
            }
            forum.load(this, isDatabase = isDatabase)
        }

// 弹出更多菜单
        val moreButton = findViewById<Button>(R.id.more_button)
        moreButton.setOnClickListener {
            popupMoreMenu(moreButton)
        }

// 设置内容列表方面的UI
        setContentList()
// 启动搜索活动
        findViewById<Button>(R.id.start_search_button).setOnClickListener {
            searchActivityResultLauncher.launch(Intent(this, SearchActivity::class.java))
        }
        // 设置排序切换
        setOrderMode()
    }

    // 设置内容列表的UI
    private fun setContentList() {
// 下拉刷新
        swipeRefresh.setOnRefreshListener {
// 清空搜索关键字
            if (keyword.isNotEmpty()) {
                keyword = ""
            }
            isDatabase = false
            forum.load(this, isDatabase = isDatabase)
        }
// 设置列表滚动事件
        contentList.setOnScrollListener(OnScrollListener())
// 列表的某元素被点击， 打开主题的详细内容
        contentList.setOnItemClickListener { _, _, position, _ ->
            forum.currentMessage = threadList[position]
            val intent = Intent(this, PostActivity::class.java)
            startActivity(intent)
        }

        contentList.setOnItemLongClickListener { parent, view, position, id ->
            forum.currentMessage = threadList[position]
            val ttsControlView = findViewById<ViewGroup>(R.id.tts_control_view)
            popupMoreMenu(ttsControlView)
            true
        }
    }

    // 列表的滚动事件实现
    inner class OnScrollListener : AbsListView.OnScrollListener {
        override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {

        }

        // 列表滚动回调方法
        override fun onScroll(
            view: AbsListView?,
            firstVisibleItem: Int,
            visibleItemCount: Int,
            totalItemCount: Int
        ) {
            if ((!App.isLoading) && firstVisibleItem + visibleItemCount >= totalItemCount) {
// 滚动到最后元素的时候继续加载更多数据
                forum.load(
                    this@MainActivity,
                    forum.pageNumber == 1,
                    keyword = keyword,
                    isDatabase = this@MainActivity.isDatabase
                )
            } else {
// 滚动的过程中让设备震动
                Util.vibrant(longArrayOf(40, 20), intArrayOf(0, 120))
            }
        }
    }

    // 前台活动不可见的情况下
    override fun onStop() {
        super.onStop()
// 保存犯罪现场， 比如当前的网站， 震动开关等
        val id = forumRadioGroup.checkedRadioButtonId
        App.context.getSharedPreferences(USER_DATA, MODE_PRIVATE).edit().putInt("forum_id", id)
            .apply()
    }

    var keyword: String = ""

    private lateinit var searchActivityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var userLoginActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportCollectorActivityResultLauncher: ActivityResultLauncher<String>
    lateinit var shareActivityLauncher: ActivityResultLauncher<IntentSenderRequest>

    // 载入网站配置
    private fun loadForum() {
// 初始化部分组件
        if (!this::contentList.isInitialized) {
            Util.loadCollectorIds()
            val pref = App.context.getSharedPreferences(USER_DATA, MODE_PRIVATE)
            val forumId = pref.getInt("forum_id", R.id.aimang_radio)
            forumRadioGroup = findViewById(R.id.forum_radio_group)
            contentList = findViewById(R.id.content_list)
            contentListText = findViewById(R.id.content_list_text)
            messageAdapter = MessageAdapter(threadList)
            contentList.adapter = messageAdapter
            statusText = findViewById(R.id.title)
            forumRadioGroup.check(forumId)
            swipeRefresh = findViewById(R.id.swipe_refresh)
        }
// 设置当前网站
        forum = when (forumRadioGroup.checkedRadioButtonId) {
            R.id.aimang_radio -> AMForum()
            R.id.abm365_radio -> BMForum()
            R.id.qt_radio -> QTForum()
            else -> ZDForum()
        }
// 载入用户信息设置状态
        forum.loadUser()
        statusText.text = forum.statusText
    }

    // 处理更多菜单
    private fun popupMoreMenu(view: View) {
        val menuId = if (view.id == R.id.more_button) {
            R.menu.more_menu
        } else {
            R.menu.thread_menu
        }
        val popupMoreMenu = PopupMenu(this, view)
        popupMoreMenu.menuInflater.inflate(menuId, popupMoreMenu.menu)
// 某个菜单被选择
        popupMoreMenu.setOnMenuItemClickListener {
            when (it.itemId) {
// 用户登录
                R.id.more_login -> {
                    if (forum.isOnline) {
                        AlertDialog.Builder(this).apply {
                            setMessage("${forum.name}用户${forum.userName}已经登录。")
                            setPositiveButton("注销") { _, _ ->
// 清空用户登录信息
                                forum.saveUser("", "")
                                forum.userName = ""
                                forum.userAuth = ""
                                statusText.text = forum.statusText
                            }
                            setNegativeButton("放弃") { _, _ -> }
                            show()
                        }
                    } else {
                        userLoginActivityResultLauncher.launch(
                            Intent(
                                this,
                                LoginActivity::class.java
                            )
                        )
                    }
                }
                R.id.more_config -> {
                    startActivity(Intent(this, ConfigActivity::class.java))
                }
// 用浏览器打开网站
                R.id.more_open -> {
                    val intent = Intent(Intent.ACTION_DEFAULT)
                    intent.data = Uri.parse(forum.baseURL)
                    startActivity(intent)
                }
// 打开发布新主题的活动
                R.id.more_thread -> {
                    startActivity(Intent(this, ThreadActivity::class.java))
                }
// 打开收藏夹
                R.id.more_more_open_collector -> {
                    isDatabase = true
                    forum.load(this, isDatabase = isDatabase)
                }
// 导出收藏夹
                R.id.more_more_export_collector -> {
                    val dtf = DateTimeFormatter.ofPattern("u-MM-dd").format(LocalDateTime.now())
                    val fileName =
                        "${resources.getString(R.string.app_name)}（收藏夹导出包）_${dtf}.zip"
                    exportCollectorActivityResultLauncher.launch(fileName)
                }
// 清空收藏夹
                R.id.more_more_clear_collector -> {
                    AlertDialog.Builder(this).apply {
                        setMessage("清空数据不可恢复， 谨慎操作！")
                        setPositiveButton("确认清空") { _, _ ->
                            thread { Util.clearCollector() }
                        }
                        show()
                    }
                }
                R.id.thread_menu_speak -> {
                    forum.bindTTS(view as ViewGroup)
                    setTTSControlView()
                }
                R.id.threadmenu_copy_url -> {
                    Util.toast("已拷贝")
                }
            }
            false
        }
        popupMoreMenu.show()
    }

    // 应用程序第一次启动的时候弹出用户协议和说明
    private fun checkLicense() {
        if (!getSharedPreferences(USER_DATA, MODE_PRIVATE).contains(LICENSE_CODE)) {
            startActivity(Intent(this, LicenseActivity::class.java))
        }
    }

    private fun setOrderMode() {
        if (!this::orderSpinner.isInitialized) {
            orderSpinner = findViewById<Spinner>(R.id.order_spaner)
            orderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    forum.currentOrder = orderStrings[position]
                    orderSpinner.contentDescription = orderStrings[position]
                    forum.load(this@MainActivity, isDatabase = isDatabase)
                    forum.saveOrderMode()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }

            orderSpinner.adapter =
                ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_expandable_list_item_1,
                    orderStrings
                )
        }
    }

    fun setTTSControlView() {
        arrayOf(
            R.id.tts_control_back,
            R.id.tts_control_play,
            R.id.tts_control_next,
            R.id.tts_control_stop
        ).forEach {
            findViewById<Button>(it).setOnClickListener(onTTSControlClick)
        }
    }

    val onTTSControlClick: (View?) -> Unit = { v ->
        if (v != null) {
            when (v.id) {
                R.id.tts_control_play -> TTSEngine.speak()
                R.id.tts_control_back -> TTSEngine.speak(-1)
                R.id.tts_control_next -> TTSEngine.speak(1)
                R.id.tts_control_stop -> {
                    TTSEngine.shutdown()
                    findViewById<ViewGroup>(R.id.tts_control_view).visibility = View.GONE
                }
            }
        }
    }


}
