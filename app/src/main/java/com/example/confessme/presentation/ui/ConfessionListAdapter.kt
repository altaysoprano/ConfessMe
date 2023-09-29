package com.example.confessme.presentation.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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
import com.example.confessme.databinding.ConfessItemBinding
import com.google.firebase.Timestamp

class ConfessionListAdapter(
    val confessList: MutableList<Confession> = mutableListOf(),
    private val isMyConfession: Boolean,
    private val onAnswerClick: (String, Boolean, String, Boolean) -> Unit,
    private val onFavoriteClick: (String) -> Unit
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

        @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
        fun bind(confess: Confession) {
            binding.apply {
                setItems(confess, binding, itemView, adapterPosition)
            }
        }
    }

    private fun setItems(
        confess: Confession,
        binding: ConfessItemBinding,
        itemView: View,
        adapterPosition: Int
    ) {
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

        binding.confessionsScreenUsername.text = confess.fromUserUsername
        binding.confessionsScreenConfession.text = spannable
        binding.confessionsScreenTimestamp.text = calculateTimeSinceConfession(confess.timestamp as Timestamp)

        if (confess.fromUserImageUrl.isNotEmpty()) {
            Glide.with(itemView)
                .load(confess.fromUserImageUrl)
                .into(binding.confessionsScreenProfileImage)
        }

        setAnswerAndFavoriteItems(confess, binding, itemView, adapterPosition)

        binding.confessionsScreenConfession.setOnClickListener {
            confess.isExpanded = !confess.isExpanded
            updateTextViewExpansion(binding.confessionsScreenConfession, confess.isExpanded)
        }

        updateTextViewExpansion(binding.confessionsScreenConfession, confess.isExpanded)
    }

    private fun setAnswerAndFavoriteItems(
        confess: Confession,
        binding: ConfessItemBinding,
        itemView: View,
        adapterPosition: Int
    ) {
        if (confess.answered) {
            binding.icAnswer.alpha = 1f
            binding.icAnswer.setColorFilter(Color.parseColor("#BA0000"))
        } else if (!isMyConfession) {
            binding.icAnswer.alpha = 1f
            binding.icAnswer.setColorFilter(Color.parseColor("#b8b8b8"))
        } else {
            binding.icAnswer.alpha = 0.5f
            binding.icAnswer.setColorFilter(Color.parseColor("#b8b8b8"))
            binding.icAnswer.isEnabled = false // Durum 9: Kullanıcı kendi confess'ine sahip ve yanıtlanmadı
        }

        if (confess.favorited) {
            binding.icFavorite.alpha = 1f
            binding.icFavorite.setColorFilter(Color.parseColor("#BA0000"))
        } else if (!isMyConfession) {
            binding.icFavorite.alpha = 1f
            binding.icFavorite.setColorFilter(Color.parseColor("#b8b8b8"))
        } else {
            binding.icFavorite.alpha = 0.5f
            binding.icFavorite.setColorFilter(Color.parseColor("#b8b8b8"))
        }

        if (isMyConfession) {
            binding.icFavorite.alpha = 0.5f
            binding.icFavorite.isEnabled = false
        } else {
            // Durum 9: Kullanıcı kendi confess'ine sahip değil
            binding.icFavorite.isEnabled = true
            binding.icAnswer.isEnabled = true
        }

        binding.icAnswer.setOnClickListener {
            val confessAnswer = confessList[adapterPosition]
            onAnswerClick(confessAnswer.id, confess.answered, confess.answer.text, confess.answer.favorited)
        }

        binding.icFavorite.setOnClickListener {
            val confessFavorite = confessList[adapterPosition]
            confessFavorite.favorited = !confessFavorite.favorited
            notifyItemChanged(adapterPosition)
            onFavoriteClick(confessFavorite.id)
        }

        itemView.setOnClickListener {

        }
    }

    fun updateItem(position: Int, updatedConfession: Confession) {
        confessList[position] = updatedConfession
        notifyItemChanged(position)
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
