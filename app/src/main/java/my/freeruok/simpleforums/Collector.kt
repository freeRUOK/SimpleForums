/* Collector.kt */
// * 2651688427@qq.com
// * 把用户收藏的贴文保存到数据库
package my.freeruok.simpleforums

import androidx.room.*

// 给MESSAGE_TAB数据表定义一组数据库操作接口
@Dao
interface MessageDao {
    // 查询所有数据记录
    @Query("select * from $MESSAGE_TAB")
    fun all(): List<Message>

    // 查询主题， 分页模式
    @Query("select * from $MESSAGE_TAB where view_count != 0 ORDER BY id DESC limit :offset, :maxNum")
    fun fastThread(offset: Int, maxNum: Int): MutableList<Message>

    // 查询主题详细内容， 分页模式
    @Query("select * from $MESSAGE_TAB where view_count == 0 and tid == :tid limit :offset, :maxNum")
    fun fastPost(tid: Long, offset: Int, maxNum: Int): MutableList<Message>

    // 插入数据记录， 使用默认实现
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(messages: List<Message>)

    // 删除一条记录， 使用默认实现
    @Delete
    fun remove(message: Message): Int

    // 清空当前数据表
    @Query("delete from $MESSAGE_TAB")
    fun clear()
}

// MESSAGE_TAB数据表和room之间搭建一个桥梁
@Database(entities = [Message::class], version = 2, exportSchema = false)
abstract class CollectorDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
