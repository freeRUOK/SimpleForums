/* MessageAdapter.kt */
// * 2651688427@qq.com

package my.freeruok.simpleforums

import android.media.AudioManager
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import org.jsoup.nodes.Element

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
        val message = messages.get(position)
        val audio = message.sources.find { it.tagName() == "audio" }

        if (convertView == null || audio != null) {
            view = LayoutInflater.from(App.context).inflate(R.layout.thread_list, parent, false)
            viewHolder = ViewHolder(
                view.findViewById<TextView>(R.id.item_text),
                view.findViewById(R.id.player)
            )
            view.tag = viewHolder
            if (audio is Element) {
                viewHolder.appendPlayer(audio.attr("src"))
            }
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val fmt =
            if (message.postCount != 0) {
                message.formatThread()
            } else {
                message.formatPost()
            }
        viewHolder.textView.text = fmt
        return view
    }

    inner class ViewHolder(val textView: TextView, val player: ViewGroup) {
        fun appendPlayer(audioSrc: String) {
            player.visibility = View.VISIBLE
            val fallBackBtn = player.findViewById<Button>(R.id.player_fall_back)
            val playBtn = player.findViewById<Button>(R.id.player_play)
            val speedBtn = player.findViewById<Button>(R.id.player_speed)

            val mediaPlayer = MediaPlayer().apply {
                setOnPreparedListener {
                    it?.start()
                    playBtn.text = "暂停"
                    playBtn.isEnabled = true
                    setVisibility(View.VISIBLE, fallBackBtn, speedBtn)
                }
                setOnErrorListener { mp, _, _ ->
                    mp?.release()
                    playBtn.isEnabled = false
                    playBtn.text = "无法播放音频"
                    setVisibility(View.GONE, fallBackBtn, speedBtn)
                    false
                }
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setDataSource(audioSrc)
            }
            PostActivity.mediaPlayers.add(mediaPlayer)

            playBtn.setOnClickListener {
                if (playBtn.text == "播放") {
                    if (mediaPlayer.currentPosition == 0) {
                        mediaPlayer.prepareAsync()
                        playBtn.isEnabled = false
                    } else {
                        mediaPlayer.start()
                        playBtn.text = "暂停"
                    }
                } else if (playBtn.text == "暂停") {
                    mediaPlayer.pause()
                    playBtn.text = "播放"
                }
            }

            val skip: (View) -> Unit = {
                if (mediaPlayer.isPlaying) {
                    val target = when (it.id) {
                        R.id.player_fall_back -> mediaPlayer.currentPosition + -30 * 1000
                        R.id.player_speed -> mediaPlayer.currentPosition + 30 * 1000
                        else -> mediaPlayer.currentPosition
                    }
                    when (target) {
                        in 0 until mediaPlayer.duration -> mediaPlayer.seekTo(target)
                        else -> mediaPlayer.seekTo(0)
                    }
                }
            }
            fallBackBtn.setOnClickListener(skip)
            speedBtn.setOnClickListener(skip)
        }

        private fun setVisibility(mod: Int, vararg views: View) {
            views.forEach { it.visibility = mod }
        }
    }
}
