/* Util.kt */
// * 2651688427@qq.com
// 常用工具函数， 网络请求； toast； 震动播放音效等
package my.freeruok.simpleforums

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import kotlin.concurrent.thread

object Util {
    // 提交http请求
    fun fastHttp(
        url: String,
        headers: Map<String, String> = mapOf(),
        querys: Map<String, Any> = mapOf(),
        isMoveable: Boolean = false,
        cookie: MyCookie? = null,
        contentType: String = ""
    ): ByteArray {
        val request = Request.Builder().run {
            url(url)
            addHeader(
                "User-Agent", if (isMoveable) {
                    "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Mobile Safari/537.36 Edg/109.0.1518.78"
                } else {
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.134 Safari/537.36 Edg/103.0.1264.71"
                }
            )
            if (headers.isNotEmpty()) {
                headers.forEach { addHeader(it.key, it.value) }
            }
            if (querys.isNotEmpty()) {
                post(if (contentType == CONTENT_TYPE_JSON) {
                    RequestBody.create(contentType.toMediaType(), JSONObject(querys).toString())
                } else {
                    FormBody.Builder().run {
                        querys.forEach { add(it.key, it.value.toString()) }
                        build()
                    }
                }
                )
            }
            build()
        }
        val client = OkHttpClient.Builder().run {
            if (cookie != null) {
                cookieJar(cookie)
            }
            build()
        }
        val response = client.newCall(request).execute()

        return if (response.isSuccessful) {
            response.body?.bytes() ?: byteArrayOf()
        } else {
            byteArrayOf()
        }
    }

    // 封装toast
    fun toast(text: String) {
        Toast.makeText(App.context, text, Toast.LENGTH_SHORT).show()
    }

    // 震动初始化
    private lateinit var vibrator: Vibrator
    var vibrateSwitch: Boolean = true
    fun init() {
        if (!this::vibrator.isInitialized) {
            vibrator = App.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrateSwitch = App.context.getSharedPreferences(USER_DATA, App.MOD_PRIVATE).getBoolean(
            VIBRATE_SWITCH, true
        )
    }

    // 震动
    fun vibrant(times: LongArray, strength: IntArray, isRepeat: Boolean = false) {
        if (vibrateSwitch && vibrator.hasVibrator()) {
            val r = if (isRepeat) 0 else -1
            val aa = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM).build()
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(times, strength, r), aa)
            } else {
                vibrator.vibrate(times, r, aa)
            }
        }
    }

    // 停止震动
    fun stopVibrant() {
        if (vibrator.hasVibrator()) {
            vibrator.cancel()
        }
    }

    // 收起键盘
    fun hideInputMethod(view: View): Boolean {
        val manager =
            App.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return manager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // 展开键盘
    fun showInputMethod(view: View): Boolean {
        if (view.requestFocus() && view.requestFocusFromTouch()) {
            val manager =
                App.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            return manager.showSoftInput(view, 0)
        }
        return false
    }

    @Volatile
    private var isShow: Boolean = false
    fun showView(activity: AppCompatActivity, view: View, second: Int) {
        if (isShow) {
            return
        }
        isShow = true
        view.visibility = View.VISIBLE
        thread {
            Thread.sleep((second * 1000).toLong())
            activity.runOnUiThread {
                view.visibility = View.GONE
                isShow = false
            }
        }
    }

    fun addCollects(messages: List<Message>) {
        val dao =
            Room.databaseBuilder(
                App.context,
                CollectorDatabase::class.java,
                FORUMS_APP_DATABASE_NAME
            ).build()
        dao.messageDao().save(messages)
    }

    fun fastCollector(curMessage: Message? = null): MutableList<Message> {
        val dao =
            Room.databaseBuilder(
                App.context,
                CollectorDatabase::class.java,
                FORUMS_APP_DATABASE_NAME
            ).build()

        val result = if (curMessage == null) {
            dao.messageDao().fastThread((MainActivity.forum.pageNumber - 1) * 20, 20)
        } else {
            val tid = curMessage.tid
            val page = curMessage.pageNumber
            dao.messageDao().fastPost(tid, (page - 1) * 20, 20)
        }
        return result
    }

    // 清空数据，  *注意完全从本地删除所有记录
    fun clearCollector() {
        Room.databaseBuilder(App.context, CollectorDatabase::class.java, FORUMS_APP_DATABASE_NAME)
            .build().messageDao().clear()
    }
}
