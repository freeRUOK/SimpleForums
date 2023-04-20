package my.freeruok.simpleforums

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

object TTSEngine {
    private lateinit var engine: TextToSpeech
    private val bundle: Bundle = Bundle()
    private var ok: Boolean = false
    private val strings = mutableListOf<String>()
    private var pos: Int = 0

    fun init() {
        engine = TextToSpeech(App.context) {
            if (it == TextToSpeech.SUCCESS) {
                engine.setOnUtteranceProgressListener(OnUtteranceProgressListener())
                ok = true
                engine.language = Locale.SIMPLIFIED_CHINESE
            }
        }
    }

    private const val speakLength = 30
    private var currentSeek = 0
    fun seek(): Boolean {
        if (strings.isEmpty()) {
            return false
        }
        if (currentSeek + speakLength < strings[pos].length) {
            currentSeek += speakLength
            return false
        }
        currentSeek = 0
        return true
    }

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
        if (result == 0) {
            if (tar == 0 && engine.isSpeaking) {
                engine.stop()
            } else {
                val endIndex = if (currentSeek + speakLength <= strings[pos].length) {
                    currentSeek + speakLength
                } else {
                    strings[pos].length
                }
                val text = strings[pos].substring(currentSeek, endIndex)
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, "$pos")
            }
        }
        return result
    }

    fun addAll(strs: List<String>) {
        val isEmpty = strings.isEmpty()
        strings.addAll(strs.filter { it.isNotEmpty() })
        if (isEmpty && strings.isNotEmpty()) {
            speak()
        }
    }

    fun add(s: String) {
        if (s.isNotEmpty()) {
            addAll(listOf(s))
        }
    }

    fun clear() {
        strings.clear()
        pos = 0
        engine.stop()
    }

    val textCount: Int
        get() = strings.size

    fun shutdown() {
        if (engine.isSpeaking) {
            engine.stop()
        }
        engine.shutdown()
        ok = false
    }

    val isSuccess: Boolean
        get() = ok
}

class OnUtteranceProgressListener : UtteranceProgressListener() {
    override fun onDone(utteranceId: String?) {
        if (TTSEngine.seek()) {
            TTSEngine.speak(1)
        } else {
            TTSEngine.speak(0)
        }
    }

    override fun onStart(utteranceId: String?) {

    }

    override fun onError(utteranceId: String?) {

    }
}
