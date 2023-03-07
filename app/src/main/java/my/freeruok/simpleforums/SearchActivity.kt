/* SearchActivity.kt */
// * 2651688427@qq.com
// 实现搜索UI

package my.freeruok.simpleforums

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener

class SearchActivity : AppCompatActivity() {
    val keywords: Set<String> = mutableSetOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_search)
        setUI()

    }

    fun setKeyword(keyword: String) {

        setResult(RESULT_OK, Intent().putExtra(KEYWORD_STR, keyword))
        finish()
    }

    fun setUI() {
        val keywordText = findViewById<EditText>(R.id.keyword_text)
        val clearButton = findViewById<Button>(R.id.clear_keyword_button)
        val searchButton = findViewById<Button>(R.id.search_button)
        val closeButton = findViewById<Button>(R.id.close_search_activity_button)
        val keywordList = findViewById<ListView>(R.id.keyword_list)
        keywordList.emptyView = findViewById(R.id.keyword_list_text)
        keywordText.addTextChangedListener {
            if (it != null && it.isNotEmpty() && clearButton.visibility == View.GONE) {
                clearButton.visibility = View.VISIBLE
            } else {
                clearButton.visibility = View.GONE
            }
        }

        clearButton.setOnClickListener {
            keywordText.setText("")
        }

        searchButton.setOnClickListener {
            val kw = keywordText.text.toString().trim()
            if (kw.isEmpty()) {
                Util.toast("关键字无效")
                keywordText.setText("")
                return@setOnClickListener
            }
            setKeyword(kw)
        }

        closeButton.setOnClickListener {
            finish()
        }

    }
}
