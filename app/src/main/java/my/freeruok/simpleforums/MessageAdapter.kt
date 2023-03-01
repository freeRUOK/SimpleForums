package my.freeruok.simpleforums

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class MessageAdapter(val messages: MutableList<Message>) : BaseAdapter() {

    override fun getCount(): Int {
        return messages.size
    }

    override fun getItem(position: Int): Any {
        return messages.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val viewHolder: ViewHolder
        if (convertView == null) {
            view = LayoutInflater.from(App.context).inflate(R.layout.thread_list, parent, false)
            viewHolder = ViewHolder(view.findViewById<TextView>(R.id.item_text))
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }
        val message = messages.get(position)
        val fmt =
            if (message.post != 0) {
                message.formatThread()
            } else {
                message.formatPost()
            }
        viewHolder.textView.text = fmt
        return view
    }

    inner class ViewHolder(val textView: TextView)
}
