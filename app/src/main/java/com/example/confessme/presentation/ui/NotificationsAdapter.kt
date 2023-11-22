package com.example.confessme.presentation.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.data.model.Notification
import com.example.confessme.data.model.User
import com.example.confessme.databinding.NotificationItemBinding
import com.example.confessme.databinding.UserItemBinding

class NotificationsAdapter(
    val notificationsList: MutableList<Notification> = mutableListOf(),
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificationsAdapter.NotificationViewHolder {
        val binding =
            NotificationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: NotificationsAdapter.NotificationViewHolder,
        position: Int
    ) {
        val notification = notificationsList[position]
        holder.bind(notification)
    }

    override fun getItemCount(): Int = notificationsList.size

    inner class NotificationViewHolder(private val binding: NotificationItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification) {
            binding.apply {
                notificationScreenUsername.text = notification.fromUserUsername
                notificationsScreenConfession.text = notification.text
                notificationsScreenNotification.text = notification.description

                if (notification.fromUserImageUrl.isNotEmpty()) {
                    Glide.with(itemView)
                        .load(notification.fromUserImageUrl)
                        .into(notificationScreenProfileImage)
                } else {
                    binding.notificationScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
                }
            }
        }
    }

    fun updateList(newList: List<Notification>) {
        notificationsList.clear()
        notificationsList.addAll(newList)
        notifyDataSetChanged()
    }

}