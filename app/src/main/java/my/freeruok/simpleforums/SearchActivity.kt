/* SearchActivity.kt */
// * 2651688427@qq.com
// 实现搜索UI

package my.freeruok.simpleforums

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener

class SearchActivity : AppCompatActivity() {
    private lateinit var keywords: MutableList<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_search)
        setUI()
    }

    // 处理搜索关键字
    private fun setKeyword(kw: String) {
        // 初始化关键字列表
        if (!this::keywords.isInitialized) {
            keywords = mutableListOf()
        }
        // 更新关键字
        keywords.remove(kw)
        keywords.add(0, kw)
        getSharedPreferences(USER_DATA, MODE_PRIVATE).edit()
            .putString(KEYWORDS, keywords.joinToString("\n")).apply()
        // 给调用方返回搜索关键字
        setResult(RESULT_OK, Intent().putExtra(KEYWORD_STR, kw))
        finish()
    }

    private fun setUI() {
        val keywordText = findViewById<EditText>(R.id.keyword_text)
        val clearButton = findViewById<Button>(R.id.clear_keyword_button)
        val searchButton = findViewById<Button>(R.id.search_button)
        val closeButton = findViewById<Button>(R.id.close_search_activity_button)
        val keywordList = findViewById<ListView>(R.id.keyword_list)
        val clearKeywordsButton = findViewById<Button>(R.id.clear_keywords_button)
        keywordList.emptyView = findViewById(R.id.keyword_list_text)

        keywordText.hint = "搜索${MainActivity.forum.name}"
        // 延迟自动弹出键盘
        keywordText.postOnAnimationDelayed({
            Util.showInputMethod(keywordText)
        }, 200)

        // 显示或隐藏清空搜索框按钮
        keywordText.addTextChangedListener {
            if (it != null && it.isNotEmpty() && clearButton.visibility == View.GONE) {
                clearButton.visibility = View.VISIBLE
            } else {
                clearButton.visibility = View.GONE
            }
        }

        // 回车搜索
        keywordText.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                searchButton.performClick()
            }
            false
        }

        // 清空搜索框
        clearButton.setOnClickListener {
            keywordText.setText("")
        }

        // 搜索
        searchButton.setOnClickListener {
            val kw = keywordText.text.toString().trim()
            if (checkOrHintInput(kw, "关键字不能为空")) {
                return@setOnClickListener
            }
            Util.hideInputMethod(keywordText)
            setKeyword(kw)
        }

        closeButton.setOnClickListener {
            finish()
        }

        // 关键字列表项目被单机
        keywordList.setOnItemClickListener { _, _, position, _ ->
            setKeyword(keywords[position])
        }

        // 清空关键字列表
        clearKeywordsButton.setOnClickListener {
            if (keywords.isEmpty()) {
                return@setOnClickListener
            }
            getSharedPreferences(USER_DATA, MODE_PRIVATE).edit().remove(KEYWORDS).apply()
            keywords.clear()
            (keywordList?.adapter as ArrayAdapter<*>).notifyDataSetInvalidated()
            it.visibility = View.GONE
        }

        // 给关键字列表填充数据
        val kws: List<String> =
            (getSharedPreferences(USER_DATA, MODE_PRIVATE).getString(KEYWORDS, "") ?: "")
                .split('\n')

        if (kws[0].isNotEmpty()) {
            keywords = kws.toMutableList()
            keywordList.adapter =
                ArrayAdapter(
                    this,
                    android.R.layout.simple_expandable_list_item_1,
                    keywords
                )
            clearKeywordsButton.visibility = View.VISIBLE
        }
    }
}
