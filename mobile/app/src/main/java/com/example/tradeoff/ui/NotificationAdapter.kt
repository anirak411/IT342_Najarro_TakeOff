package com.example.tradeoff.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tradeoff.R
import com.example.tradeoff.model.NotificationItem

class NotificationAdapter(
    private val onItemClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val items = mutableListOf<NotificationItem>()

    fun submitItems(newItems: List<NotificationItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class NotificationViewHolder(
        itemView: View,
        private val onItemClick: (NotificationItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        private val body: TextView = itemView.findViewById(R.id.tvNotificationBody)
        private val time: TextView = itemView.findViewById(R.id.tvNotificationTime)

        fun bind(item: NotificationItem) {
            title.text = item.title
            body.text = item.body
            time.text = item.timeLabel
            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
