/* ConfigActivity.kt */
// * 2651688427
// 设置activity

package my.freeruok.simpleforums

import android.os.Bundle
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class ConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "设置"
        setContentView(R.layout.activity_config)
        initUI()
    }

    private fun initUI() {
        val vibrateSwitch = findViewById<Switch>(R.id.vibrate_switch)
        vibrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            Util.vibrateSwitch = isChecked
            getSharedPreferences(USER_DATA, MODE_PRIVATE).edit()
                .putBoolean(VIBRATE_SWITCH, isChecked).apply()
        }
        vibrateSwitch.isChecked = Util.vibrateSwitch
        if (TTSEngine.isSuccess) {
            setTTSConfig()
        } else {
            TTSEngine.init {
                setTTSConfig()
            }
        }

        findViewById<Button>(R.id.tts_speak_test).setOnClickListener {
            TTSEngine.speak("Hello World\n 会当凌绝顶 一览众山小")
        }
    }

    private fun setTTSConfig() {
        val engineSpinner = findViewById<Spinner>(R.id.tts_engine_spinner)
        val pref = getSharedPreferences(USER_DATA, MODE_PRIVATE)
        val engineName = pref.getString(TTS_ENGINE_NAME, "默认引擎") ?: "默认引擎"

        val engines = TTSEngine.ttsEngineNames

        setSpinner(this, engineSpinner, engines.map { it.label }, engineName) {
            val engine = engines[it]
            TTSEngine.shutdown()
            TTSEngine.init(engine.name)
            engineSpinner.contentDescription = engine.label
            pref.edit().putString(TTS_ENGINE_NAME, engine.name).apply()
        }
    }
}
