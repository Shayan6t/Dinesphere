package com.example.dinesphere

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private var notifications: List<NotificationModel>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notificationIcon: ImageView = itemView.findViewById(R.id.notificationIcon)
        val businessName: TextView = itemView.findViewById(R.id.notificationBusinessName)
        val title: TextView = itemView.findViewById(R.id.notificationItemTitle)
        val description: TextView = itemView.findViewById(R.id.notificationItemDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        // Set business name
        holder.businessName.text = notification.businessName

        // Set title and description
        holder.title.text = notification.title
        holder.description.text = notification.message

        // Load restaurant image using Picasso or use default icon
        if (!notification.restaurantImage.isNullOrEmpty()) {
            Picasso.get()
                .load(notification.restaurantImage)
                .placeholder(R.drawable.notifications)
                .error(R.drawable.notifications)
                .fit()
                .centerCrop()
                .into(holder.notificationIcon)
        } else {
            holder.notificationIcon.setImageResource(R.drawable.notifications)
        }

        // Optional: Click listener to navigate to restaurant details
        holder.itemView.setOnClickListener {
            // TODO: Navigate to restaurant details
            // You can use the notification.restaurantId to open RestaurantActivity
        }
    }

    override fun getItemCount(): Int = notifications.size

    fun updateNotifications(newNotifications: List<NotificationModel>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    private fun formatDateTime(dateTimeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(dateTimeString)
            date?.let { outputFormat.format(it) } ?: dateTimeString
        } catch (e: Exception) {
            dateTimeString
        }
    }
}