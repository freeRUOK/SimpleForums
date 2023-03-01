/*ThreadActivity.kt */
// * 2651688427@qq.com
// * 发布和编辑帖子

package my.freeruok.simpleforums

import android.os.Bundle
import android.widget.AbsListView
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.concurrent.thread

class PostActivity : AppCompatActivity() {
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
            MainActivity.forum.loadPosts(this, isReload = true)
        }
        postList.setOnScrollListener(OnScrollListener())
        setSendEventListener()






    }

    fun setSendEventListener() {
        if (!MainActivity.forum.isOnline) {
            return
        }
        val sendButton = findViewById<Button>(R.id.send_post_button)
        val postText = findViewById<TextView>(R.id.post_text)

        postText.isEnabled = true

        postText.addTextChangedListener {
            if (it != null && it.length <= 5) {
                sendButton.isEnabled = false
            } else {
                sendButton.isEnabled = true
            }
        }

        sendButton.setOnClickListener {
            val content = postText.text.toString().trim()
            sendButton.isEnabled = false
            thread {
                val success = try {
                    MainActivity.forum.post(content)
                } catch (e: Exception) {
                    false
                }
                this.runOnUiThread {
                    sendButton.isEnabled = true
                    if (success) {
                        postText.text = ""
                        Util.hideInputMethod(postText)
                        MainActivity.forum.loadPosts(this)
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
                Util.vibrant(longArrayOf(40, 20), intArrayOf(0, 120))
            }
        }
    }
}
