package com.example.tradeoff.ui

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeoff.R
import com.example.tradeoff.model.ChatMessage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ChatMessageAdapter(
    private val currentUserEmail: String
) : RecyclerView.Adapter<ChatMessageAdapter.ChatViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    fun submitMessages(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position], currentUserEmail)
    }

    override fun getItemCount(): Int = messages.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val row: LinearLayout = itemView.findViewById(R.id.layoutChatRow)
        private val message: TextView = itemView.findViewById(R.id.tvChatMessage)
        private val time: TextView = itemView.findViewById(R.id.tvChatTime)

        fun bind(chat: ChatMessage, userEmail: String) {
            val mine = chat.senderEmail.equals(userEmail, ignoreCase = true)
            row.gravity = if (mine) Gravity.END else Gravity.START
            message.background = itemView.context.getDrawable(
                if (mine) R.drawable.bg_chat_bubble_mine else R.drawable.bg_chat_bubble_theirs
            )
            message.setTextColor(
                itemView.context.getColor(
                    if (mine) android.R.color.white else R.color.text_main
                )
            )
            message.text = chat.content

            val formattedTime = formatChatTimestamp(chat.createdAt)
            time.text = formattedTime
            time.visibility = if (formattedTime.isBlank()) View.GONE else View.VISIBLE
            time.setTextColor(
                itemView.context.getColor(
                    if (mine) android.R.color.white else R.color.text_sub
                )
            )
        }

        private fun formatChatTimestamp(rawTimestamp: String?): String {
            if (rawTimestamp.isNullOrBlank()) return ""
            val parsed = runCatching { LocalDateTime.parse(rawTimestamp) }.getOrNull() ?: return ""
            val today = LocalDate.now()
            val locale = Locale.getDefault()
            return if (parsed.toLocalDate().isEqual(today)) {
                parsed.format(DateTimeFormatter.ofPattern("h:mm a", locale))
            } else {
                parsed.format(DateTimeFormatter.ofPattern("MMM d, h:mm a", locale))
            }
        }
    }
}
