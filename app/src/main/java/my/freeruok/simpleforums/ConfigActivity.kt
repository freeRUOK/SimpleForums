package my.freeruok.simpleforums

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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

        val spinner = findViewById<Spinner>(R.id.media_cache_max_size_spinner)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val value: Long = position.toLong() * 256 * 1024 * 1024
                getSharedPreferences(USER_DATA, MODE_PRIVATE).edit()
                    .putLong(MEDIA_CACHE_MAX_SIZE, value).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
        val maxSizes = arrayOf("不缓存", "256mb", "512mb", "768mb", "1gb")
        spinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1, maxSizes)
    }
}
