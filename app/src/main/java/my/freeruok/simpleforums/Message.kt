/* Message.kt */
// * 2651688427@qq.com
// 一条论坛贴文
package my.freeruok.simpleforums

import org.jsoup.Jsoup

data class Message(
    val id: Long = 0L,
    val url: String = "",
    val content: String = "",
    val author: String = "",
    val floor: Int = 0,
    val dateLine: Long = 0L,
    val dateFmt: String = "",
    var post: Int = 0,
    val view: Int = 0,
    val lastDate: Long = 0L,
    val lastDateFmt: String = "",
    val lastPost: String = ""
) {
    fun formatThread(): String {
        return StringBuilder().apply {
            append(Jsoup.parse(content).text())
            if (author.isNotEmpty()) {
                append(" ${view} 次点击 ${post - 1} 次跟帖， ")
                append("${author} 发布于 ${dateFmt}, ")
            }
            if (lastPost.isNotEmpty()) {
                append("最后 ${lastPost} 回复于 ${lastDateFmt}")
            }
        }.toString()
    }

    fun formatPost(): String {
        val auth = when (floor) {
            0 -> ""
            1 -> "楼主 ${author}说： "
            else -> "${floor} 楼 ${author}说： "
        }
        return "${auth}${Jsoup.parse(content).text()} 发布于 ${dateFmt}"
    }

    @Volatile
    var pageNumber: Int = 1
}
