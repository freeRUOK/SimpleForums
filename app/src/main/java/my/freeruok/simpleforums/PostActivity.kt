/*PostActivity.kt */
// * 2651688427@qq.com
// * 发布和编辑帖子

package my.freeruok.simpleforums

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import java.time.Instant
import kotlin.concurrent.thread

class PostActivity : AppCompatActivity() {
    lateinit var mPlayer: ExoPlayer
    val posts = mutableListOf<Message>()
    private lateinit var postList: ListView
    lateinit var postAdapter: MessageAdapter
    lateinit var postListText: TextView
    lateinit var swipeLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_post)

// 设置listView
        postListText = findViewById(R.id.post_list_text)
        postAdapter = MessageAdapter(posts)
        postList = findViewById(R.id.post_list)
        postList.emptyView = postListText
        postList.adapter = postAdapter

// 设置上拉刷新
        swipeLayout = findViewById(R.id.post_swipe_layout)
        swipeLayout.setOnRefreshListener {
// 显示顶部状态栏
            Util.showView(this, findViewById(R.id.post_layout_title), 10)
            MainActivity.forum.loadPosts(this, isReload = true)
        }
        postList.setOnScrollListener(OnScrollListener())
// 设置回帖等UI事件
        setSendEventListener()
        setTitleView()
    }

    private fun setSendEventListener() {
        if (!MainActivity.forum.isOnline) {
            return
        }
        val sendButton = findViewById<Button>(R.id.send_post_button)
        val postText = findViewById<EditText>(R.id.post_text)

        postText.isEnabled = true

// listView的某元素被单机
        postList.setOnItemClickListener { _, _, position, _ ->
// 在回复输入框里填充当前被点击的用户信息
            if (Util.showInputMethod(postText)) {
                val cm = posts[position]
                val floor = if (cm.floor == 1) {
                    "楼主"
                } else {
                    "${cm.floor} 楼"
                }
                val fmt = "回${floor}${cm.author}: \n${postText.text}"
// 填充数据并且把光标移动到文本最末
                postText.setText(fmt)
                postText.setSelection(fmt.length)
            }
        }

// 输入框的文本改变的时候根据情况切换发布回复按钮的可用状态
        postText.addTextChangedListener {
            if (it != null && it.length >= 5) {
                sendButton.isEnabled = true
            } else {
                sendButton.isEnabled = false
            }
        }

// 回复帖子
        sendButton.setOnClickListener {
            val content = postText.text.toString().trim()
// 防止用户多次点击发送按钮
            sendButton.isEnabled = false
            thread {
                val result = try {
                    MainActivity.forum.post(content)
                } catch (e: Exception) {
                    Message()
                }
// 更改UI
                this.runOnUiThread {
                    sendButton.isEnabled = true
                    if (result.content.isNotEmpty()) {
// 回复成功清空输入框隐藏键盘， 更新并刷新数据
                        postText.setText("")
                        Util.hideInputMethod(postText)
                        posts.add(result)
                        postAdapter.notifyDataSetChanged()
                        MainActivity.forum.currentMessage.postCount++
                    } else {
                        Util.toast("回复失败！")
                    }
                }
            }
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

                MainActivity.forum.loadPosts(
                    this@PostActivity,
                    isReload = MainActivity.forum.currentMessage.pageNumber == 1
                )

            } else {
                Util.vibrant(longArrayOf(1, 20), intArrayOf(0, 1))
            }
        }
    }

    // 设置顶部标题状态
    private fun setTitleView() {
        val titleText = findViewById<TextView>(R.id.title)
        titleText.text = if (MainActivity.forum.currentMessage.content.isNotEmpty()) {
            "${MainActivity.forum.currentMessage.content} - 来自： ${MainActivity.forum.name}"
        } else {
            "新发主题 - 来自： ${MainActivity.forum.name}"
        }
        val collectorButton = findViewById<Button>(R.id.more_button)
        collectorButton.text = if (MainActivity.forum.currentMessage.isDatabase) {
            "更新收藏"
        } else {
            "收藏"
        }
// 收藏主题
        collectorButton.setOnClickListener {
            it.isEnabled = false
            val postCount = MainActivity.forum.currentMessage.postCount
            Util.toast("开始收集数据")
            thread {
// 从网上收集全部数据
                val messages = MainActivity.forum.parsePosts(1).toMutableList()
                var pageNum = 2
                while (messages.size < postCount) {
                    messages += MainActivity.forum.parsePosts(pageNum)
                    pageNum++
                }
                MainActivity.forum.currentMessage.pageNumber = 1
// 生成所有用户共选择
                val items = mutableMapOf<String, Int>().apply {
                    messages.forEach { msg ->
                        if (!containsKey(msg.author)) {
                            this[msg.author] = size
                        }
                    }
                }

                this.runOnUiThread {
                    it.isEnabled = true
                    AlertDialog.Builder(this).apply {
                        setTitle("你需要哪些用户的内容？")
                        val users = items.keys.toTypedArray()
                        setMultiChoiceItems(
                            users,
                            items.values.map { true }.toBooleanArray()
                        ) { _, curIndex, isCheck ->
// 标记选择的内容
                            val k = users[curIndex]
                            if (isCheck) {
                                items[k] = curIndex
                            } else {
                                items[k] = -1
                            }
                        }
                        setPositiveButton("选好了") { _, _ ->
                            thread {
// 过滤出app用户选择的网站用户的内容
                                val collectors = messages.filter { msg ->
                                    items.containsKey(msg.author) && items[msg.author] != -1
                                } + MainActivity.forum.currentMessage

// 保存到数据库之前的处理
                                collectors.forEach { collector ->
                                    collector.isDatabase = true
                                    if (collector.dateLine == 0L) {
                                        collector.dateLine = Instant.now().epochSecond
                                    }
                                }

// 保存到数据库
                                Util.addCollects(collectors)
                            }
                        }
                        show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MainActivity.forum.currentMessage.pageNumber = 1
        if (this::mPlayer.isInitialized) {
            if (mPlayer.isPlaying) {
                mPlayer.stop()
            }
            mPlayer.release()
            findViewById<PlayerControlView>(R.id.player_view).visibility = View.GONE
        }
    }
}
