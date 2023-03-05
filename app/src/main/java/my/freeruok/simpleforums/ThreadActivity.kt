/* ThreadActivity.kt */
// * 2651688427@qq.com
// *实现用户的发帖UI功能
package my.freeruok.simpleforums

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable

import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import kotlin.concurrent.thread

class ThreadActivity : AppCompatActivity() {
    lateinit var threadSpinner: Spinner
    lateinit var spinnerAdapter: ArrayAdapter<Section>

    lateinit var sections: Array<Section>
    val section: MutableSection = MutableSection(Section(), Section())

    lateinit var titleText: EditText
    lateinit var contentText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val titleFmt = if (MainActivity.forum.isOnline) {
            "在 ${MainActivity.forum.name} 发表新主题， 当前是用户 ${MainActivity.forum.userName}"
        } else {
            "在 ${MainActivity.forum.name} 发表新主题， 当前只限暂存草稿， 请登录后在发布"
        }
        supportActionBar?.title = titleFmt
        setContentView(R.layout.activity_thread)
        setSections()
        setpostThreadAction()

    }

    fun setSections() {
        threadSpinner = findViewById(R.id.post_select_spinner)
        threadSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                val spinnerView = parent as Spinner
                val currentSection = sections.get(position)
                if (currentSection.subSections.isNotEmpty()) {
                    AlertDialog.Builder(this@ThreadActivity).apply {
                        setItems(
                            currentSection.subSections.map { it.toString() }.toTypedArray(),
                            object : DialogInterface.OnClickListener {
                                override fun onClick(dialog: DialogInterface?, which: Int) {
                                    section.first = currentSection
                                    section.second = currentSection.subSections.get(which)
                                    spinnerView.contentDescription =
                                        "${section.first.name} ${section.second.name}"
                                }
                            })
                        show()
                    }
                } else {
                    section.first = currentSection
                    spinnerView.contentDescription = section.first.name

                }
            }


            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        })

        if (!MainActivity.forum.isOnline) {
            return
        }
        thread {
            sections = MainActivity.forum.section()
            if (sections.isEmpty()) {
                return@thread
            }
            this.runOnUiThread {
                spinnerAdapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_expandable_list_item_1,
                    sections
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_expandable_list_item_1)
                threadSpinner.adapter = spinnerAdapter
                recoverManuscript()
            }
        }
    }

    private fun saveManuscript(title: String, content: String) {
        val tids = "${section.first.id}\n${section.second.id}"
        getSharedPreferences(USER_DATA, MODE_PRIVATE).edit().run {
            putString("manuscript_${MainActivity.forum.name}", "${tids}\n${title}\n${content}")
            apply()
        }
    }

    private fun recoverManuscript() {
        val (tid1, tid2, title, content) = getSharedPreferences(
            USER_DATA,
            MODE_PRIVATE
        ).getString("manuscript_${MainActivity.forum.name}", "0\n0\n\n")
            ?.split("\n", limit = 4) ?: listOf("0", "0", "", "")
        if (title.isNotEmpty() || content.isNotEmpty()) {
            titleText.setText(title)
            contentText.setText(content)
            titleText.setSelection(title.length)
            contentText.setSelection(content.length)
            val id1 = tid1.toInt()
            val id2 = tid2.toInt()
            if (id1 != 0) {
                for (i in 0 until sections.size) {
                    if (id1 == sections[i].id) {
                        section.first = sections[i]
                        threadSpinner.setSelection(i)
                        break
                    }
                }
                if (section.first.subSections.isNotEmpty()) {
                    section.second = section.first.subSections.find { it.id == id2 } ?: Section()
                    threadSpinner.contentDescription =
                        "${section.first.name} ${section.second.name}"
                } else {
//                    threadSpinner.contentDescription = section.first.name
                }
            }
        }

    }

    fun setpostThreadAction() {
        titleText = findViewById<EditText>(R.id.thread_title_text)
        contentText = findViewById<EditText>(R.id.thread_content_text)
        val sendButton = findViewById<Button>(R.id.send_thread_button)

        val editTextChangedListener: (Editable?) -> Unit = {
            if (it != null && it.length >= 5) {
                sendButton.isEnabled = true
            } else {
                sendButton.isEnabled = false
            }
        }

        titleText.addTextChangedListener { editTextChangedListener(it) }
        contentText.addTextChangedListener { editTextChangedListener(it) }

        sendButton.setOnClickListener {
            if (section.first.id == 0) {
                Util.toast("选择板块")
                return@setOnClickListener
            }
            sendButton.isEnabled = false
            thread {
                val threadId = try {
                    MainActivity.forum.thread(
                        titleText.text.toString(),
                        contentText.text.toString(),
                        section
                    )
                } catch (e: Exception) {
                    0
                }
                this.runOnUiThread {
                    sendButton.isEnabled = true
                    if (threadId == 0) {
                        Util.toast("发布失败")
                    } else {
                        titleText.setText("")
                        contentText.setText("")
                        getSharedPreferences(USER_DATA, MODE_PRIVATE).edit()
                            .remove("manuscript_${MainActivity.forum.name}").apply()
                        if (!Util.hideInputMethod(contentText)) {
                            Util.hideInputMethod(titleText)
                        }
                        val curMessage = Message(id = threadId.toLong(), postCount = 1)
                        MainActivity.forum.currentMessage = curMessage
                        finish()
                        val intent = Intent(this, PostActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    fun endEdit() {
        if (titleText.text.isEmpty() && contentText.text.isEmpty()) {
            return
        }
        val title = titleText.text.toString()
        val content = contentText.text.toString()
        saveManuscript(title, content)
    }

    override fun onPause() {
        super.onPause()
        endEdit()
    }
}
