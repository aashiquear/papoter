package com.papoter.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.papoter.app.R
import com.papoter.app.data.Message
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_ASSISTANT = 2
        private const val PAYLOAD_STREAMING = "streaming"

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean =
                oldItem == newItem
        }
    }

    private var streamingText: String = ""

    fun setStreamingText(text: String) {
        streamingText = text
        val pos = currentList.size - 1
        if (pos >= 0 && getItemViewType(pos) == VIEW_TYPE_ASSISTANT) {
            notifyItemChanged(pos, PAYLOAD_STREAMING)
        }
    }

    fun clearStreamingText() {
        streamingText = ""
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).role == "user") VIEW_TYPE_USER else VIEW_TYPE_ASSISTANT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_user, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_bot, parent, false)
            AssistantMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AssistantMessageViewHolder -> {
                val isLastAssistant = position == currentList.size - 1 && streamingText.isNotEmpty()
                holder.bind(message, if (isLastAssistant) streamingText else null)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_STREAMING) && holder is AssistantMessageViewHolder) {
            holder.updateStreaming(streamingText)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textContent: TextView = itemView.findViewById(R.id.text_content)
        private val textTime: TextView = itemView.findViewById(R.id.text_time)

        fun bind(message: Message) {
            textContent.text = message.content
            textTime.text = formatTime(message.timestamp)
        }
    }

    class AssistantMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textContent: TextView = itemView.findViewById(R.id.text_content)
        private val textTime: TextView = itemView.findViewById(R.id.text_time)

        fun bind(message: Message, streaming: String?) {
            textContent.text = streaming ?: message.content
            textTime.text = formatTime(message.timestamp)
        }

        fun updateStreaming(text: String) {
            textContent.text = text
        }
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
