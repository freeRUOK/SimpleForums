/*PostActivity.kt */
// * 2651688427@qq.com
// * 发布和编辑帖子

package my.freeruok.simpleforums

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import kotlin.concurrent.thread

class PostActivity : AppCompatActivity() {
    lateinit var mPlayer: ExoPlayer
    val posts = mutableListOf<Message>()
    lateinit var postList: ListView
    lateinit var postAdapter: MessageAdapter
    lateinit var postListText: TextView
    lateinit var swipeLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_post)

        postListText = findViewById(R.id.post_list_text)
        postAdapter = MessageAdapter(posts)
        postList = findViewById(R.id.post_list)
        postList.emptyView = postListText
        postList.adapter = postAdapter

        swipeLayout = findViewById(R.id.post_swipe_layout)
        swipeLayout.setOnRefreshListener {
            Util.showView(this, findViewById(R.id.post_layout_title), 10)
            MainActivity.forum.loadPosts(this, isReload = true)
        }
        postList.setOnScrollListener(OnScrollListener())
        setSendEventListener()
        setTitleView()
    }

    fun setSendEventListener() {
        if (!MainActivity.forum.isOnline) {
            return
        }
        val sendButton = findViewById<Button>(R.id.send_post_button)
        val postText = findViewById<EditText>(R.id.post_text)

        postText.isEnabled = true

        postList.setOnItemClickListener { parent, view, position, id ->
            if (Util.showInputMethod(postText)) {
                val cm = posts.get(position)
                val floor = if (cm.floor == 1) {
                    "楼主"
                } else {
                    "${cm.floor} 楼"
                }
                val fmt = "回${floor}${cm.author}: \n${postText.text.toString()}"
                postText.setText(fmt)
                postText.setSelection(fmt.length)
            }
        }

        postText.addTextChangedListener {
            if (it != null && it.length >= 5) {
                sendButton.isEnabled = true
            } else {
                sendButton.isEnabled = false
            }
        }

        sendButton.setOnClickListener {
            val content = postText.text.toString().trim()
            sendButton.isEnabled = false
            thread {
                val result = try {
                    MainActivity.forum.post(content)
                } catch (e: Exception) {
                    Message()
                }
                this.runOnUiThread {
                    sendButton.isEnabled = true
                    if (result.content.isNotEmpty()) {
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

    fun setTitleView() {
        val titleText = findViewById<TextView>(R.id.title)
        titleText.text = if (MainActivity.forum.currentMessage.content.isNotEmpty()) {
            "${MainActivity.forum.currentMessage.content} - 来自： ${MainActivity.forum.name}"
        } else {
            "新发主题 - 来自： ${MainActivity.forum.name}"
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
