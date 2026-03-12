package com.example.tradeoff.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeoff.R
import com.example.tradeoff.model.ChatInboxThread

class ChatInboxAdapter(
    private val onThreadClick: (ChatInboxThread) -> Unit
) : RecyclerView.Adapter<ChatInboxAdapter.InboxThreadViewHolder>() {

    private val threads = mutableListOf<ChatInboxThread>()

    fun submitThreads(newThreads: List<ChatInboxThread>) {
        threads.clear()
        threads.addAll(newThreads)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboxThreadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_inbox_thread, parent, false)
        return InboxThreadViewHolder(view, onThreadClick)
    }

    override fun onBindViewHolder(holder: InboxThreadViewHolder, position: Int) {
        holder.bind(threads[position])
    }

    override fun getItemCount(): Int = threads.size

    class InboxThreadViewHolder(
        itemView: View,
        private val onThreadClick: (ChatInboxThread) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val avatar = itemView.findViewById<TextView>(R.id.tvInboxAvatar)
        private val name = itemView.findViewById<TextView>(R.id.tvInboxName)
        private val preview = itemView.findViewById<TextView>(R.id.tvInboxPreview)
        private val time = itemView.findViewById<TextView>(R.id.tvInboxTime)

        fun bind(thread: ChatInboxThread) {
            avatar.text = thread.displayName
                .firstOrNull()
                ?.uppercaseChar()
                ?.toString()
                ?: "#"
            name.text = thread.displayName
            preview.text = thread.preview
            time.text = thread.timeLabel
            itemView.setOnClickListener {
                onThreadClick(thread)
            }
        }
    }
}
