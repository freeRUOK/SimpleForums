/* MessageAdapter.kt */
// * 2651688427@qq.com
// 实现后台数据和前台数据容器的绑定
// 所谓的适配器就是一座桥， 桥梁两边自由发挥互不打扰

package my.freeruok.simpleforums

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class MessageAdapter(private val messages: MutableList<Message>) : BaseAdapter() {
    override fun getCount(): Int {
        return messages.size
    }

    override fun getItem(position: Int): Any {
        return messages[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // listView滚动的时候动态加载布局
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val viewHolder: ViewHolder
        if (convertView == null) {
// 加载布局
            view = LayoutInflater.from(App.context).inflate(R.layout.thread_list, parent, false)
            viewHolder = ViewHolder(view.findViewById(R.id.item_text))
            view.tag = viewHolder
        } else {
// 使用已经缓存的布局， 改善应用程序的性能
// 整体用到了享元模式
            view = convertView
            viewHolder = view.tag as ViewHolder
        }
        val message = messages[position]
        val fmt =
            if (message.postCount != 0) {
                message.formatThread()
            } else {
                message.formatPost()
            }
// 给新的listView布局设置内容
        viewHolder.textView.text = fmt
        return view
    }

    // 用于内部缓存布局
    inner class ViewHolder(val textView: TextView)
}
