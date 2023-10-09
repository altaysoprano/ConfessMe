package com.example.confessme.presentation.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.data.model.Confession
import com.example.confessme.databinding.ConfessItemBinding
import com.example.confessme.presentation.DialogHelper
import com.google.firebase.Timestamp

class ConfessionListAdapter(
    private val context: Context,
    val confessList: MutableList<Confession> = mutableListOf(),
    private val isMyConfession: Boolean,
    private val onAnswerClick: (String, Boolean, String, Boolean, String) -> Unit,
    private val onFavoriteClick: (String) -> Unit,
    private val onConfessDeleteClick: (String) -> Unit,
    private val onItemPhotoClick: (String, String) -> Unit
) : RecyclerView.Adapter<ConfessionListAdapter.ConfessionViewHolder>() {

    private val dialogHelper = DialogHelper(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfessionViewHolder {
        val binding = ConfessItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConfessionViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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

        @RequiresApi(Build.VERSION_CODES.Q)
        @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
        fun bind(confess: Confession) {
            binding.apply {
                setItems(confess, binding, itemView, adapterPosition)
                Log.d("Mesaj: ", "Foto: " + confessList[0].fromUserImageUrl.toString())
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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
        binding.confessionsScreenTimestamp.text =
            calculateTimeSinceConfession(confess.timestamp as Timestamp)

        if (confess.fromUserImageUrl.isNotEmpty()) {
            Log.d("Mesaj: ", "Confesstext: ${confess.text} ve image: ${confess.fromUserImageUrl}")
            Glide.with(itemView)
                .load(confess.fromUserImageUrl)
                .into(binding.confessionsScreenProfileImage)
        } else {
            binding.confessionsScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
            Log.d(
                "Mesaj: ",
                "Confesstext: ${confess.text} ve image boş. URL: ${confess.fromUserImageUrl}"
            )
        }

        setAnswerAndFavoriteItems(confess, binding, itemView, adapterPosition)

        binding.confessionsScreenConfession.setOnClickListener {
            confess.isExpanded = !confess.isExpanded
            updateTextViewExpansion(binding.confessionsScreenConfession, confess.isExpanded)
        }

        updateTextViewExpansion(binding.confessionsScreenConfession, confess.isExpanded)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("RestrictedApi")
    private fun setAnswerAndFavoriteItems(
        confess: Confession,
        binding: ConfessItemBinding,
        itemView: View,
        adapterPosition: Int
    ) {
        if (isMyConfession) {
            binding.icFavorite.alpha = 0.5f
            binding.icFavorite.isEnabled = false
            binding.icAnswer.isEnabled = true
            binding.moreActionButton.visibility = View.VISIBLE
        } else {
            binding.moreActionButton.visibility = View.GONE
            binding.icFavorite.isEnabled = true
            binding.icAnswer.isEnabled = true
        }

        if (confess.answered) {
            binding.icAnswer.alpha = 1f
            binding.icAnswer.setColorFilter(Color.parseColor("#BA0000"))
        } else if (!isMyConfession) {
            binding.icAnswer.alpha = 1f
            binding.icAnswer.setColorFilter(Color.parseColor("#b8b8b8"))
        } else {
            binding.icAnswer.alpha = 0.5f
            binding.icAnswer.setColorFilter(Color.parseColor("#b8b8b8"))
            binding.icAnswer.isEnabled =
                false
        }

        if (confess.favorited) {
            binding.icFavorite.alpha = if (isMyConfession) 0.5f else 1f
            binding.icFavorite.setColorFilter(Color.parseColor("#BA0000"))
        } else if (!isMyConfession) {
            binding.icFavorite.alpha = 1f
            binding.icFavorite.setColorFilter(Color.parseColor("#b8b8b8"))
        } else {
            binding.icFavorite.alpha = 0.5f
            binding.icFavorite.setColorFilter(Color.parseColor("#b8b8b8"))
        }

        binding.icAnswer.setOnClickListener {
            val confessAnswer = confessList[adapterPosition]
            val confessDateTimestamp = confess.answer.timestamp

            onAnswerClick(
                confessAnswer.id,
                confess.answered,
                confess.answer.text,
                confess.answer.favorited,
                if(confessDateTimestamp != null) calculateTimeSinceConfession(confessDateTimestamp as Timestamp) else ""
            )
        }

        binding.icFavorite.setOnClickListener {
            val confessFavorite = confessList[adapterPosition]
            confessFavorite.favorited = !confessFavorite.favorited
            notifyDataSetChanged()
            onFavoriteClick(confessFavorite.id)
        }

        binding.confessionsScreenProfileImage.setOnClickListener {
            val photoClickedUser = confessList[adapterPosition]

            onItemPhotoClick(photoClickedUser.fromUserEmail, photoClickedUser.fromUserUsername)
        }

        binding.moreActionButton.setOnClickListener { view ->
            val popupMenu = androidx.appcompat.widget.PopupMenu(view.context, view)
            popupMenu.menuInflater.inflate(R.menu.confess_item_more_actions_menu, popupMenu.menu)
            popupMenu.setForceShowIcon(true)

            val positionOfMenuItem = 0
            val item = popupMenu.menu.getItem(positionOfMenuItem)
            val s = SpannableString("Delete Confess")
            s.setSpan(ForegroundColorSpan(Color.RED), 0, s.length, 0)
            item.title = s

            item.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_delete)

            item.icon?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_delete -> {
                        val confessIdToDelete = confessList[adapterPosition].id
                        dialogHelper.showDeleteConfessionDialog(
                            "confession",
                            { onConfessDeleteClick(confessIdToDelete) })
                        return@setOnMenuItemClickListener true
                    }

                    else -> false
                }
            }
            popupMenu.show()
        }

        itemView.setOnClickListener {

        }
    }

    fun updateItem(position: Int, updatedConfession: Confession) {
        confessList[position] = updatedConfession
        notifyItemChanged(position)
        Log.d("Mesaj: ", "$position'daki ${updatedConfession} update edildi")
    }

    fun removeConfession(position: Int) {
        confessList.removeAt(position)
        notifyItemRemoved(position)
        notifyDataSetChanged()
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
