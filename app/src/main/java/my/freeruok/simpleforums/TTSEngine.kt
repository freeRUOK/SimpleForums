/* TTSEngine.kt */
// * 2651688427
// 实现TTS朗读功能
package my.freeruok.simpleforums

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.EngineInfo
import android.speech.tts.UtteranceProgressListener
import androidx.appcompat.app.AppCompatActivity
import java.util.*

object TTSEngine {
    private lateinit var engine: TextToSpeech

    // 如果精细化tts控制需要给speak方法传入
    private val bundle: Bundle = Bundle()
    private var ok: Boolean = false

    //要朗读的帖子内容
    private val strings = mutableListOf<String>()

    // 当前朗读的帖子索引
    private var pos: Int = 0

    fun init(engineName: String = "", callback: (() -> Unit)? = null) {
        val pref = App.context.getSharedPreferences(USER_DATA, AppCompatActivity.MODE_PRIVATE)
        val ttsEngineName = if (engineName.isNotEmpty()) {
            engineName
        } else {
            pref.getString(TTS_ENGINE_NAME, null) ?: ""
        }
        // tts初始化是异步运行的， 初始化完成后调用这里的SAM方法
        engine = TextToSpeech(App.context, {
            if (it == TextToSpeech.SUCCESS) {
                engine.setOnUtteranceProgressListener(OnUtteranceProgressListener())  //  精细化控制tts引擎的关键
                ok = true
                engine.language = Locale.SIMPLIFIED_CHINESE
                if (callback != null) {
                    callback()
                }
            }
        }, ttsEngineName)
    }

    val ttsEngineNames: List<EngineInfo>
        get() =
            if (isSuccess) {
                engine.engines
            } else {
                listOf()
            }

    fun speak(s: String) {
        engine.speak(s, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // tts引擎每次朗读的参考字符个数
    private const val refeSpeakLength = 40

    // tts当前朗读的内容的开始索引和结束索引
    private var currentHeadIndex = 0
    private var currentTailIndex = 0

    // 向前移动朗读内容的索引，索引如果没有到达结尾返回false
    fun seek(): Boolean {
        if (strings.isEmpty()) {
            return false
        }
        if (currentTailIndex < strings[pos].length) {
            // 设置朗读内容的开始索引和结束索引
            currentHeadIndex = currentTailIndex
            setCurrentTailIndex()
            return false
        }
        currentHeadIndex = 0
        currentTailIndex = 0
        return true
    }

    // 设置当前朗读内容结束索引， 设置条件是小于refeSpeak， 而且用ss定义的标点符号之一结束
    private fun setCurrentTailIndex() {
        val ss = charArrayOf('，', '。', '？')
        var i = strings[pos].indexOfAny(ss, currentHeadIndex)
        while (i < currentHeadIndex + refeSpeakLength && i != -1) {
            i = strings[pos].indexOfAny(ss, i + 1)
        }
        currentTailIndex = if (i == -1 || i == strings[pos].length - 1) {
            strings[pos].length
        } else {
            i
        }
    }

    // 朗读内容， 如果传入0朗读当前帖子，传入其他值根据情况切换要朗读的帖子
// 1 下一个帖子  -1 上一个帖子 可以用这个参数向前或向后跳跃帖子
    fun speak(tar: Int = 0): Int {
        val p = pos + tar
        val result = when (p) {
            in 0 until strings.size -> {
                pos = p
                0
            }
            else -> {
                p
            }
        }
        if (tar != 0) {
            currentHeadIndex = 0
            currentTailIndex = 0
        }
        if (result == 0) {
            if (tar == 0 && engine.isSpeaking) {
                engine.stop()
                isPause = true
            } else {
                if (currentTailIndex == 0) {
                    setCurrentTailIndex()
                    startWatch()
                }
                // 朗读内容
                val text = strings[pos].substring(currentHeadIndex, currentTailIndex)
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, "$pos")
                isPause = false
            }
        } else {
            speak(
                if (tar > 0) {
                    "没有更多"
                } else {
                    "已到达开头。"
                }
            )
        }
        return result
    }

    // 添加帖子文本
    fun addAll(strs: List<String>) {
        val isEmpty = strings.isEmpty()
        strings.addAll(strs.filter { it.isNotEmpty() })
        // 首次添加自动朗读
        if (isEmpty && strings.isNotEmpty()) {
            speak()
        }
    }

    fun add(s: String) {
        if (s.isNotEmpty()) {
            addAll(listOf(s))
        }
    }

    // 清空所有帖子
    fun clear() {
        strings.clear()
        setZero()
        engine.stop()
    }

    val textCount: Int
        get() = strings.size

    // 释放tts资源
    fun shutdown() {
        if (engine.isSpeaking) {
            engine.stop()
        }
        engine.shutdown()
        ok = false
        setZero()
    }

    private fun setZero() {
        pos = 0
        currentTailIndex = 0
        currentHeadIndex = 0
    }

    val isSuccess: Boolean
        get() = ok
}

// 精细化控制tts引擎
class OnUtteranceProgressListener : UtteranceProgressListener() {
    // 朗读完成
    override fun onDone(utteranceId: String?) {
        // 自动朗读下一段文本
        if (TTSEngine.seek()) {
            TTSEngine.speak(1)
        } else {
            TTSEngine.speak(0)
        }
    }

    // 开始朗读
    override fun onStart(utteranceId: String?) {

    }

    // 朗读错误,如果被其他程序打断这里抓不到， onStop也抓不到
    override fun onError(utteranceId: String?) {

    }
}
