/* LicenseActivity.kt */
// * 2651688427@qq.com
// 实现展示许可协议
package my.freeruok.simpleforums

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStreamReader

class LicenseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "权限及隐私说明"
        setContentView(R.layout.activity_license)
        // 读取assets目录的readMe.txt文件到textView
        InputStreamReader(assets.open("readMe.txt")).use {
            val licenseText = findViewById<TextView>(R.id.license_text)
            licenseText.text = it.readText()
        }
        findViewById<Button>(R.id.license_ok_button).setOnClickListener {
            // 用户点击同意后在本地标记一个值
            getSharedPreferences(USER_DATA, MODE_PRIVATE).edit().putInt(LICENSE_CODE, 0).apply()
            finish()
        }
    }
}
