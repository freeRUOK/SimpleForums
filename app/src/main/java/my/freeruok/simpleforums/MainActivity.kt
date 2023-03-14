/* MainActivity.kt */
// * 2651688427@qq.com
// 应用程序的主活动展示所有论坛和所选择的主题帖列表
package my.freeruok.simpleforums

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    val threadList = mutableListOf<Message>()
    lateinit var messageAdapter: MessageAdapter
    private lateinit var contentList: ListView
    lateinit var contentListText: TextView
    private lateinit var forumRadioGroup: RadioGroup
    private lateinit var statusText: TextView
    lateinit var swipeRefresh: SwipeRefreshLayout
    private var isDatabase: Boolean = false

    companion object {
        lateinit var forum: Forum
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        searchActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK && it.data != null) {
                    val kw = it.data?.getStringExtra(KEYWORD_STR) ?: ""
                    if (kw.isNotEmpty()) {
                        keyword = kw
                        forum.load(this, keyword = keyword, isDatabase = isDatabase)
                    }
                }
            }

        userLoginActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    statusText.text = forum.statusText
                    forum.load(this, isDatabase = isDatabase)
                } else {
                    contentListText.text = "${forum.name}要求用户登录"
                }
            }

        exportCollectorActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument()) {
                if (it != null) {
                    Util.exportCollector(it)
                } else {
                    Util.toast("取消导出")
                }
            }
        checkLicense()
        Util.init()
        setHideBar()
        loadForum()

        contentList.emptyView = contentListText

        forumRadioGroup.setOnCheckedChangeListener { _, _ ->
            isDatabase = false
            loadForum()
            forum.load(this, isDatabase = isDatabase)
        }

        val moreButton = findViewById<Button>(R.id.more_button)
        moreButton.setOnClickListener {
            popupMoreMenu(moreButton)
        }

        setContentList()
        findViewById<Button>(R.id.start_search_button).setOnClickListener {
            searchActivityResultLauncher.launch(Intent(this, SearchActivity::class.java))
        }
    }

    private fun setHideBar() {
        val vibrateSwitch = findViewById<Switch>(R.id.vibrate_switch)
        vibrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            Util.vibrateSwitch = if (isChecked) {
                true
            } else {
                false
            }
        }
        vibrateSwitch.isChecked = Util.vibrateSwitch
    }

    private fun setContentList() {
        swipeRefresh.setOnRefreshListener {
            Util.showView(this, findViewById(R.id.main_hide_bar), 15)
            if (keyword.isNotEmpty()) {
                keyword = ""
            }
            isDatabase = false
            forum.load(this, isDatabase = isDatabase)
        }
        contentList.setOnScrollListener(OnScrollListener())
        contentList.setOnItemClickListener { _, _, position, _ ->
            forum.currentMessage = threadList[position]
            val intent = Intent(this, PostActivity::class.java)
            startActivity(intent)
        }
    }

    inner class OnScrollListener : AbsListView.OnScrollListener {
        override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {

        }

        override fun onScroll(
            view: AbsListView?,
            firstVisibleItem: Int,
            visibleItemCount: Int,
            totalItemCount: Int
        ) {
            if ((!App.isLoading) && firstVisibleItem + visibleItemCount >= totalItemCount) {

                forum.load(
                    this@MainActivity,
                    forum.pageNumber == 1,
                    keyword = keyword,
                    isDatabase = this@MainActivity.isDatabase
                )
            } else {
                Util.vibrant(longArrayOf(40, 20), intArrayOf(0, 120))
            }
        }
    }


    override fun onPause() {
        super.onPause()
        val id = forumRadioGroup.checkedRadioButtonId
        App.context.getSharedPreferences(USER_DATA, MODE_PRIVATE).edit().putInt("forum_id", id)
            .putBoolean(VIBRATE_SWITCH, Util.vibrateSwitch)
            .apply()
    }

    var keyword: String = ""

    private lateinit var searchActivityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var userLoginActivityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var exportCollectorActivityResultLauncher: ActivityResultLauncher<String>

    private fun loadForum() {
        if (!this::contentList.isInitialized) {
            val forumId = App.context.getSharedPreferences(USER_DATA, MODE_PRIVATE)
                .getInt("forum_id", R.id.aimang_radio)
            forumRadioGroup = findViewById(R.id.forum_radio_group)
            contentList = findViewById(R.id.content_list)
            contentListText = findViewById(R.id.content_list_text)
            messageAdapter = MessageAdapter(threadList)
            contentList.adapter = messageAdapter
            statusText = findViewById(R.id.title)
            forumRadioGroup.check(forumId)
            swipeRefresh = findViewById(R.id.swipe_refresh)
        }
        forum = when (forumRadioGroup.checkedRadioButtonId) {
            R.id.aimang_radio -> AMForum()
            R.id.abm365_radio -> BMForum()
            R.id.qt_radio -> QTForum()
            else -> ZDForum()
        }
        forum.loadUser()
        statusText.text = forum.statusText
    }

    private fun popupMoreMenu(view: View) {
        val popupMoreMenu = PopupMenu(this, view)
        popupMoreMenu.menuInflater.inflate(R.menu.more_menu, popupMoreMenu.menu)
        popupMoreMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.more_login -> {
                    if (forum.isOnline) {
                        AlertDialog.Builder(this).apply {
                            setMessage("${forum.name}用户${forum.userName}已经登录。")
                            setPositiveButton("注销") { _, _ ->
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
                R.id.more_open -> {
                    val intent = Intent(Intent.ACTION_DEFAULT)
                    intent.data = Uri.parse(forum.baseURL)
                    startActivity(intent)
                }
                R.id.more_thread -> {
                    startActivity(Intent(this, ThreadActivity::class.java))
                }
                R.id.more_more_open_collector -> {
                    isDatabase = true
                    forum.load(this, isDatabase = isDatabase)
                }
                R.id.more_more_export_collector -> {
                    val dtf = DateTimeFormatter.ofPattern("u-MM-dd").format(LocalDateTime.now())
                    val fileName =
                        "${resources.getString(R.string.app_name)}（收藏夹导出包）_${dtf}.zip"
                    exportCollectorActivityResultLauncher.launch(fileName)
                }
                R.id.more_more_clear_collector -> {
                    AlertDialog.Builder(this).apply {
                        setMessage("清空数据不可恢复， 谨慎操作！")
                        setPositiveButton("确认清空") { _, _ ->
                            thread { Util.clearCollector() }
                        }
                        show()
                    }
                }
            }
            false
        }
        popupMoreMenu.show()
    }

    private fun checkLicense() {
        if (!getSharedPreferences(USER_DATA, MODE_PRIVATE).contains(LICENSE_CODE)) {
            startActivity(Intent(this, LicenseActivity::class.java))
        }
    }
}
