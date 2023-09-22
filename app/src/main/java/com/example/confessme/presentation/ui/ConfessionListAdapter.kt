package com.example.confessme.presentation.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.confessme.data.model.Confession
import com.example.confessme.data.model.User
import com.example.confessme.databinding.ConfessItemBinding
import com.example.confessme.databinding.UserItemBinding
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConfessionListAdapter(
    private val confessList: MutableList<Confession> = mutableListOf()
) : RecyclerView.Adapter<ConfessionListAdapter.ConfessionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfessionViewHolder {
        val binding = ConfessItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConfessionViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ConfessionListAdapter.ConfessionViewHolder,
        position: Int
    ) {
        val confess = confessList[position]
        holder.bind(confess)
    }

    override fun getItemCount(): Int = confessList.size

    fun updateList(newList: List<Confession>) {
        confessList.clear()
        confessList.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ConfessionViewHolder(private val binding: ConfessItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(confess: Confession) {
            binding.apply {
                confessionsScreenUsername.text = confess.fromUserUsername
                confessionsScreenToUserName.text = "@" + confess.username + " "
                confessionsScreenConfession.text = confess.text

                val timestamp = confess.timestamp as Timestamp
                val timeSinceConfession = calculateTimeSinceConfession(timestamp)
                confessionsScreenTimestamp.text = timeSinceConfession

                if (confess.fromUserImageUrl.isNotEmpty()) {
                    Glide.with(itemView)
                        .load(confess.fromUserImageUrl)
                        .into(confessionsScreenProfileImage)
                }

                itemView.setOnClickListener {

                }
            }
        }
    }

    private fun calculateTimeSinceConfession(confessionTimestamp: Timestamp): String {
        val currentTime = Timestamp.now()
        val timeDifference = currentTime.seconds - confessionTimestamp.seconds

        val minutes = timeDifference / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            timeDifference < 60 -> "$timeDifference seconds ago"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            else -> {
                if (days == 1L) {
                    "1 day ago"
                } else {
                    "$days days ago"
                }
            }
        }
    }
}
