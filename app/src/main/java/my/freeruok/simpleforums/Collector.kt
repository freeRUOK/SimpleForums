/* Collector.kt */
// * 2651688427@qq.com
// * 把用户收藏的贴文保存到数据库
package my.freeruok.simpleforums

import androidx.room.*

@Dao
interface MessageDao {
    @Query("select * from MESSAGE_TAB")
    fun all(): List<Message>

    @Query("select * from MESSAGE_TAB where view_count != 0 limit :offset, :maxNum")
    fun fastThread(offset: Int, maxNum: Int): MutableList<Message>

    @Query("select * from MESSAGE_TAB where view_count == 0 and tid == :tid limit :offset, :maxNum")
    fun fastPost(tid: Long, offset: Int, maxNum: Int): MutableList<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(messages: List<Message>)

    @Delete
    fun remove(message: Message): Int
}

@Database(entities = [Message::class], version = 1, exportSchema = false)
abstract class CollectorDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
