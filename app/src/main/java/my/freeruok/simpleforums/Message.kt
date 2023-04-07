/* Message.kt */
// * 2651688427@qq.com
// 一条论坛贴文
package my.freeruok.simpleforums

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

import org.jsoup.Jsoup

// room和网站的数据类
@Entity(tableName = "MESSAGE_TAB")
data class Message(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    @ColumnInfo(name = "tid")
    var tid: Long = 0L,
    @ColumnInfo(name = "url")
    var url: String = "",
    @ColumnInfo(name = "content")
    var content: String = "",
    @ColumnInfo(name = "author")
    var author: String = "",
    @ColumnInfo(name = "floor")
    var floor: Int = 0,
    @ColumnInfo(name = "date_line")
    var dateLine: Long = 0L,
    @ColumnInfo(name = "date_fmt")
    var dateFmt: String = "",
    @ColumnInfo(name = "post_count")
    var postCount: Int = 0,
    @ColumnInfo(name = "view_count")
    var viewCount: Int = 0,
    @ColumnInfo(name = "is_database")
    var isDatabase: Boolean = false,
    @ColumnInfo(name = "last_date")
    var lastDate: Long = 0L,
    @ColumnInfo(name = "last_date_fmt")
    var lastDateFmt: String = "",
    @ColumnInfo(name = "last_post")
    var lastPost: String = "",
    @Ignore
    var resources: Map<String, TextProcesser.TextType> = mapOf()
) {
    fun formatThread(): String {
        return StringBuilder().apply {
            append(Jsoup.parse(content).text())
            if (id != 0L) {
                append(" 来自本地数据源 ")
            }
            if (author.isNotEmpty()) {
                append(" $viewCount 次点击 ${postCount - 1} 次跟帖， ")
                append("$author 发布于 ${dateFmt}, ")
            }
            if (lastPost.isNotEmpty()) {
                append("最后 $lastPost 回复于 $lastDateFmt")
            }
        }.toString()
    }

    fun formatPost(): String {
        val auth = when (floor) {
            0 -> ""
            1 -> "楼主 ${author}说： "
            else -> "$floor 楼 ${author}说： "
        }
        return "$auth ${Jsoup.parse(content).text()} 发布于 $dateFmt"
    }

    @Ignore
    @Volatile
    var pageNumber: Int = 1
}
