package com.example.confessme.presentation.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.data.model.Notification
import com.example.confessme.databinding.NotificationItemBinding

class NotificationsAdapter(
    private val currentUserUid: String,
    private val onItemPhotoClick: (String, String, String) -> Unit,
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

                if (notificationsScreenConfession.text.isBlank()) {
                    setFollowedNotificationLayout(binding, itemView, adapterPosition)
                } else {
                    setFavReplyAndConfessionNotificationLayout(binding, adapterPosition)
                }

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

    fun setFollowedNotificationLayout(
        binding: NotificationItemBinding,
        itemView: View,
        position: Int
    ) {
        val context = binding.root.context
        val height50dp = context.resources.getDimensionPixelSize(R.dimen.height_50dp)

        binding.notificationsScreenConfession.visibility = View.GONE
        binding.notificationsScreenGeneralLinearLayout.layoutParams.width =
            LinearLayout.LayoutParams.MATCH_PARENT
        binding.notificationsScreenGeneralLinearLayout.layoutParams.height = height50dp
        itemView.setOnClickListener {
            val itemClickedUser = notificationsList[position]

            if (currentUserUid != itemClickedUser.fromUserId && itemClickedUser.fromUserId != "") {
                onItemPhotoClick(
                    itemClickedUser.fromUserId,
                    itemClickedUser.fromUserToken,
                    itemClickedUser.fromUserUsername
                )
            }
        }
        binding.notificationsScreenNotification.setOnClickListener {

            if (position != RecyclerView.NO_POSITION) {
                val itemClickedUser = notificationsList[position]

                if (currentUserUid != itemClickedUser.fromUserId && itemClickedUser.fromUserId != "") {
                    onItemPhotoClick(
                        itemClickedUser.fromUserId,
                        itemClickedUser.fromUserToken,
                        itemClickedUser.fromUserUsername
                    )
                }
            }
        }
    }

    fun setFavReplyAndConfessionNotificationLayout(
        binding: NotificationItemBinding,
        position: Int
    ) {
        binding.notificationsScreenConfession.visibility = View.VISIBLE
        binding.notificationsScreenGeneralLinearLayout.layoutParams.width =
            LinearLayout.LayoutParams.MATCH_PARENT
        binding.notificationsScreenGeneralLinearLayout.layoutParams.height =
            LinearLayout.LayoutParams.WRAP_CONTENT

        binding.notificationScreenProfileImage.setOnClickListener {
            val photoClickedUser = notificationsList[position]

            if (currentUserUid != photoClickedUser.fromUserId && photoClickedUser.fromUserId != "") {
                onItemPhotoClick(
                    photoClickedUser.fromUserId,
                    photoClickedUser.fromUserToken,
                    photoClickedUser.fromUserUsername
                )
            }
        }
    }

    fun updateList(newList: List<Notification>) {
        notificationsList.clear()
        notificationsList.addAll(newList)
        notifyDataSetChanged()
    }

}