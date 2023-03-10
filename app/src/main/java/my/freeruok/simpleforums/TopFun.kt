/* TopFun.kt */
// * 2651688427@qq.com
// 扩展函数定义, 而且这里关联了通用的forums模块和Android平台
package my.freeruok.simpleforums

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import java.security.MessageDigest
import kotlin.concurrent.thread

// 计算字符串的md5值
fun CharSequence.md5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toString().toByteArray())
    return bytes.hex()
}

// 获取bytes的十六进制格式字符串
fun ByteArray.hex(): String {
    return joinToString("") { "%02X".format(it) }.lowercase()
}

//* 启动用户登录activity
fun startLogin(activity: AppCompatActivity) {
    val intent = Intent(activity, LoginActivity::class.java)
    activity.startActivityForResult(intent, REQUEST_CODE_LOGIN)
}

//* 加载论坛主题帖数据
fun Forum.load(
    activity: MainActivity,
    isReload: Boolean = true,
    keyword: String = "",
    isDatabase: Boolean
) {
    if (this.isForce && !this.isOnline && (!isDatabase)) {
        startLogin(activity)
        return
    }
    App.isLoading = true

    if (isReload) {
        activity.threadList.clear()
        activity.messageAdapter.notifyDataSetInvalidated()
        activity.contentListText.text = "正在加载或刷新……"
        this.pageNumber = 1
    }

    thread {
        val messages = try {
            if (isDatabase) {
                Util.fastCollector()
            } else if (keyword.isEmpty()) {
                this.parse()
            } else {
                this.parseSearch(keyword)
            }
        } catch (e: Exception) {
            listOf()
        }
        activity.runOnUiThread {
            if (messages.isEmpty()) {

                activity.contentListText.text = if (isDatabase) {
                    "暂无本地收藏记录"
                } else if (keyword.isEmpty()) {
                    "加载失败， 请检查你的网络， 或者与开发者联系。"
                } else {
                    "无搜索结果， 更换一个关键字试试"
                }
                if (this.pageNumber > 1) {
                    this.pageNumber--
                }
                App.isLoading = false
                return@runOnUiThread
            }
            this.pageNumber++
            activity.threadList += messages
            if (isReload) {
                activity.messageAdapter.notifyDataSetInvalidated()
                activity.swipeRefresh.isRefreshing = false
            } else {
                activity.messageAdapter.notifyDataSetChanged()
            }
            App.isLoading = false
        }
    }
}

//* 设置用户状态
val Forum.statusText: String
    get() {
        return if (this.isOnline) {
            "${this.userName} - ${this.name}"
        } else {
            "${this.name}  - 未登录"
        }
    }

fun Forum.loadPosts(activity: PostActivity, isReload: Boolean = true) {
    if (activity.posts.size >= this.currentMessage.postCount) {
        return
    }
    App.isLoading = true
    if (isReload) {
        currentMessage.pageNumber = 1
        activity.posts.clear()
        activity.postAdapter.notifyDataSetInvalidated()
        activity.postListText.text = "正在加载或刷新……"
    }

    thread {
        val messages = try {
            if (this.currentMessage.isDatabase) {
                Util.fastCollector(this.currentMessage)
            } else {
                this.parsePosts(this.currentMessage.pageNumber)
            }
        } catch (e: Exception) {
            listOf()
        }

        activity.runOnUiThread {
            if (messages.isEmpty()) {
                activity.postListText.text =if (this.currentMessage.isDatabase) {
                    "收藏字段暂无详细内容"
                } else {
                    "加载失败检查网络状态或与开发者联系"
                }
                App.isLoading = false
                activity.swipeLayout.isRefreshing = false
                return@runOnUiThread
            }

            val mediaSources = messages.filter { it.mediaItems.isNotEmpty() }
            if (mediaSources.isNotEmpty()) {
                activity.findViewById<View>(com.google.android.exoplayer2.ui.R.id.exo_repeat_toggle).visibility =
                    View.GONE
                activity.findViewById<View>(com.google.android.exoplayer2.ui.R.id.exo_shuffle).visibility =
                    View.GONE
                activity.findViewById<View>(com.google.android.exoplayer2.ui.R.id.exo_pause).visibility =
                    View.GONE
                val playerView = activity.findViewById<PlayerControlView>(R.id.player_view)
                if (playerView.visibility == View.GONE) {
                    activity.mPlayer = ExoPlayer.Builder(activity).build()
                    playerView.player = activity.mPlayer

                    playerView.visibility = View.VISIBLE
                }
                mediaSources.forEach {
                    it.mediaItems.forEach { item ->
                        activity.mPlayer.addMediaItem(item)
                    }
                }
            }
            activity.posts += messages
            this.currentMessage.pageNumber++
            if (isReload) {
                activity.postAdapter.notifyDataSetInvalidated()
                activity.swipeLayout.isRefreshing = false
            } else {
                activity.postAdapter.notifyDataSetChanged()
            }
            App.isLoading = false
//activity.setTitle()
        }
    }
}

// 错误检查,
fun checkOrHintInput(input: String, hint: String = "Input Error"): Boolean {
    if (input.isNotEmpty()) {
        return false
    }
    Util.toast(hint)
    return true
}
