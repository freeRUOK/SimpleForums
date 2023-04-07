/* ResourceActivity.kt */
// * 2651688427@qq.com
//*实现帖子资源展示和默认操作
package my.freeruok.simpleforums

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class ResourceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_resource)
        request1 = registerForActivityResult(RequestPermission()) {
            if (it) {
                Util.toast("打电话权限被允许")
            } else {
                Util.toast("打电话权限被拒绝")
            }
        }

        val resList = findViewById<ListView>(R.id.resource_list_view)
        resList.emptyView = findViewById(R.id.resource_list_text)
        val res = PostActivity.currentPost.resources.keys.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1, res)
        resList.adapter = adapter
        // 点击默认动作
        resList.setOnItemClickListener { _, _, position, _ ->
            val action =
                if (PostActivity.currentPost.resources[res[position]] == TextProcesser.TextType.PHONE_NUMBER) {
                    Intent.ACTION_CALL to Uri.parse("tel:${res[position]}")
                } else {
                    Intent.ACTION_VIEW to Uri.parse(res[position])
                }
            val intent = Intent(action.first, action.second)
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Util.toast("操作无法完成， 系统没有合适的处理程序。")
            } catch (e: SecurityException) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CALL_PHONE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    request1.launch(Manifest.permission.CALL_PHONE)
                }
            }
        }

        // 长按拷贝到系统剪贴板
        resList.setOnItemLongClickListener { _, _, position, _ ->
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("forum", res[position]))
            Util.toast("内容已拷贝到剪贴板")
            true
        }
    }

    private lateinit var request1: ActivityResultLauncher<String>
}
