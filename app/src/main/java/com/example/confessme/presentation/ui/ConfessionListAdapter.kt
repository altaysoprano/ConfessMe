package com.example.confessme.presentation.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.data.model.Confession
import com.example.confessme.data.model.User
import com.example.confessme.databinding.ConfessItemBinding
import com.example.confessme.databinding.UserItemBinding
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConfessionListAdapter(
    private val confessList: MutableList<Confession> = mutableListOf(),
    private val isMyConfession: Boolean,
    private val onAnswerClick: (String) -> Unit,
    private val onFavoriteClick: () -> Unit
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
                val toUserName = "@${confess.username} " // Tousername'i ayarla

                val spannable = SpannableString("$toUserName${confess.text}")

                val usernameColor = ContextCompat.getColor(itemView.context, R.color.confessmered)
                val usernameStart = 0
                val usernameEnd = toUserName.length

                spannable.setSpan(
                    ForegroundColorSpan(usernameColor),
                    usernameStart,
                    usernameEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    usernameStart,
                    usernameEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                confessionsScreenConfession.text = spannable

                val timestamp = confess.timestamp as Timestamp
                val timeSinceConfession = calculateTimeSinceConfession(timestamp)
                confessionsScreenTimestamp.text = timeSinceConfession

                if (confess.fromUserImageUrl.isNotEmpty()) {
                    Glide.with(itemView)
                        .load(confess.fromUserImageUrl)
                        .into(confessionsScreenProfileImage)
                }

                if (isMyConfession) {
                    icAnswer.alpha = 0.5f
                    icFavorite.alpha = 0.5f
                    icAnswer.isClickable = false
                    icFavorite.isClickable = false
                } else {
                    icAnswer.alpha = 1.0f
                    icFavorite.alpha = 1.0f
                    icAnswer.isClickable = true
                    icFavorite.isClickable = true
                }

                confessionsScreenConfession.setOnClickListener {
                    confess.isExpanded = !confess.isExpanded
                    updateTextViewExpansion(confessionsScreenConfession, confess.isExpanded)
                }

                updateTextViewExpansion(confessionsScreenConfession, confess.isExpanded)

                confessionsScreenConfession.viewTreeObserver.addOnGlobalLayoutListener(
                    object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            confessionsScreenConfession.viewTreeObserver.removeOnGlobalLayoutListener(this)

                        }
                    }
                )

                icAnswer.setOnClickListener {
                    val confess = confessList[adapterPosition]
                    Log.d("Mesaj: ", "Adapter'da id: ${confess.id}")
                    onAnswerClick(confess.id)
                }

                icFavorite.setOnClickListener {
                    onFavoriteClick()
                }

                itemView.setOnClickListener {

                }
            }
        }
    }

    private fun updateTextViewExpansion(textview: TextView, isExpanded: Boolean) {
        val maxLines = if (isExpanded) Int.MAX_VALUE else 2
        textview.maxLines = maxLines
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
