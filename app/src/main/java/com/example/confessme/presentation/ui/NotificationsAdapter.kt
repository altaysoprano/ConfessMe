package com.example.confessme.presentation.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.data.model.Notification
import com.example.confessme.data.model.User
import com.example.confessme.databinding.NotificationItemBinding
import com.example.confessme.databinding.UserItemBinding

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

        val context = binding.root.context

        val height50dp = context.resources.getDimensionPixelSize(R.dimen.height_50dp)

        fun bind(notification: Notification) {
            binding.apply {
                notificationScreenUsername.text = notification.fromUserUsername
                notificationsScreenConfession.text = notification.text
                notificationsScreenNotification.text = notification.description

                if (notificationsScreenConfession.text.isBlank()) {
                    notificationsScreenConfession.visibility = View.GONE
                    notificationsScreenGeneralLinearLayout.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                    notificationsScreenGeneralLinearLayout.layoutParams.height = height50dp
                } else {
                    notificationsScreenConfession.visibility = View.VISIBLE
                    notificationsScreenGeneralLinearLayout.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                    notificationsScreenGeneralLinearLayout.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                }

                if (notification.fromUserImageUrl.isNotEmpty()) {
                    Glide.with(itemView)
                        .load(notification.fromUserImageUrl)
                        .into(notificationScreenProfileImage)
                } else {
                    binding.notificationScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
                }

                binding.notificationScreenProfileImage.setOnClickListener {
                    val photoClickedUser = notificationsList[adapterPosition]

                    if (currentUserUid != photoClickedUser.fromUserId && photoClickedUser.fromUserId != "") {
                        onItemPhotoClick(
                            photoClickedUser.fromUserId,
                            photoClickedUser.fromUserToken,
                            photoClickedUser.fromUserUsername
                        )
                    }
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