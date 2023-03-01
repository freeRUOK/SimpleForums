/* LoginActivity.kt */
// * 2651688427@qq.com
// 实现用户登录UI
package my.freeruok.simpleforums

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.title = "登录 ${MainActivity.forum.name}"
        val userNameText = findViewById<EditText>(R.id.user_name_text)
        val userPasswordText = findViewById<EditText>(R.id.user_password_text)
        // 登录按钮单机事件处理函数
        findViewById<Button>(R.id.login_button).setOnClickListener {
            val name = userNameText.text.toString().trim()
            val passwd = userPasswordText.text.toString().trim()
            if (checkOrHintInput(name, "用户名为空") || checkOrHintInput(passwd, "密码为空")) {
                return@setOnClickListener
            }

            thread {
                val success = try {
                    MainActivity.forum.login(name, passwd)
                } catch (e: Exception) {
                    false
                }
                this.runOnUiThread {
                    if (success) {
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Util.toast("登录失败")
                    }
                }
            }
        }

        // 处理密码框字符是否显示
        findViewById<Switch>(R.id.password_switch).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                userPasswordText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                userPasswordText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            userPasswordText.setSelection(userPasswordText.text.length)
        }
    }
}
