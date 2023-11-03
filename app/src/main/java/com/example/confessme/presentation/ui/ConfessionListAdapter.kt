package com.example.confessme.presentation.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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
    private val currentUserUid: String,
    private val isBookmarks: Boolean,
    private val onAnswerClick: (String, String, String, String, String, String, Boolean, String, Boolean, String) -> Unit,
    private val onFavoriteClick: (Boolean, String) -> Unit,
    private val onConfessDeleteClick: (String) -> Unit,
    private val onConfessBookmarkClick: (String, String, String) -> Unit,
    private val onBookmarkRemoveClick: (String) -> Unit,
    private val onItemPhotoClick: (String, String, String) -> Unit,
    private val onUserNameClick: (String, String, String) -> Unit
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
        val toUserName = "@${confess.username} "
        val text = "$toUserName${confess.text}"

        val usernameColor = ContextCompat.getColor(itemView.context, R.color.confessmered)
        val usernameStart = 0
        val usernameEnd = toUserName.length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val userNameClickedUser = confessList[adapterPosition]

                if (currentUserUid != userNameClickedUser.userId) {
                    onUserNameClick(
                        userNameClickedUser.userId,
                        userNameClickedUser.email,
                        userNameClickedUser.username
                    )
                }
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = usernameColor
                ds.isUnderlineText = false
            }
        }

        val confessionTextView = binding.confessionsScreenConfession

        confessionTextView.text = text
        binding.confessionsScreenUsername.text = confess.fromUserUsername
        binding.confessionsScreenTimestamp.text = calculateTimeSinceConfession(confess.timestamp as Timestamp)

        confessionTextView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val viewTreeObserver = confessionTextView.viewTreeObserver
                viewTreeObserver.removeOnGlobalLayoutListener(this)

                confessionTextView.setOnClickListener {
                    confess.isExpanded = !confess.isExpanded
                    notifyItemChanged(adapterPosition)
                    notifyItemRangeChanged(adapterPosition, 1)
                }

                if (confessionTextView.lineCount > 2) {
                    if(adapterPosition == 1 || adapterPosition == 0) {
                        Log.d("Mesaj: ", "position $adapterPosition line count > 2'de, lineCount = ${binding.confessionsScreenConfession.lineCount}")
                    }
                    if(confess.isExpanded) {
                        if(adapterPosition == 1 || adapterPosition == 0) {
                            Log.d("Mesaj: ", "position $adapterPosition expandedta")
                        }
                        val spannable = SpannableString(text)
                        spannable.setSpan(clickableSpan, usernameStart, usernameEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannable.setSpan(StyleSpan(Typeface.BOLD), usernameStart, usernameEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        confessionTextView.text = spannable
                    } else {
                        if(adapterPosition == 1 || adapterPosition == 0) {
                            Log.d("Mesaj: ", "position $adapterPosition not expandedta")
                        }
                        val layout = confessionTextView.layout
                        val endOfLastLine =
                            layout.getLineEnd(1)
                        val newVal = confessionTextView.text.subSequence(0, endOfLastLine - 3)
                            .toString() + "..."
                        val spannableLongText = SpannableString(newVal)
                        spannableLongText.setSpan(clickableSpan, usernameStart, usernameEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannableLongText.setSpan(StyleSpan(Typeface.BOLD), usernameStart, usernameEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        confessionTextView.text = SpannableString(spannableLongText)
                    }
                    confessionTextView.movementMethod = LinkMovementMethod.getInstance()
                    confessionTextView.highlightColor = Color.TRANSPARENT
                } else {
                    if(adapterPosition == 1 || adapterPosition == 0) {
                        Log.d("Mesaj: ", "position $adapterPosition line count < 2'de, lineCount = ${binding.confessionsScreenConfession.lineCount}")
                    }
                    val spannable = SpannableString(text)
                    spannable.setSpan(clickableSpan, usernameStart, usernameEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannable.setSpan(StyleSpan(Typeface.BOLD), usernameStart, usernameEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                    confessionTextView.text = SpannableString(spannable)
                    confessionTextView.movementMethod = LinkMovementMethod.getInstance()
                    confessionTextView.highlightColor = Color.TRANSPARENT
                }
            }
        })

        if (confess.fromUserImageUrl.isNotEmpty()) {
            Glide.with(itemView)
                .load(confess.fromUserImageUrl)
                .into(binding.confessionsScreenProfileImage)
        } else {
            binding.confessionsScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
        }

        setAnswerFavoriteAndMoreActionsItems(confess, binding, itemView, adapterPosition)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("RestrictedApi")
    private fun setAnswerFavoriteAndMoreActionsItems(
        confess: Confession,
        binding: ConfessItemBinding,
        itemView: View,
        adapterPosition: Int
    ) {
        if(currentUserUid == confessList[adapterPosition].fromUserId) {
            binding.icFavorite.alpha = 0.5f
            binding.icAnswer.alpha = 0.5f
            binding.icFavorite.isEnabled = false
            binding.icAnswer.isEnabled = false
        } else if(currentUserUid == confessList[adapterPosition].userId) {
            binding.icAnswer.isEnabled = true
            binding.icFavorite.isEnabled = true
            binding.icFavorite.alpha = 1f
        } else {
            binding.icFavorite.alpha = 0.5f
            binding.icAnswer.alpha = 0.5f
            binding.icFavorite.isEnabled = false
            binding.icAnswer.isEnabled = false
        }

        if(confess.answered) {
            binding.icAnswer.setColorFilter(Color.parseColor("#BA0000"))
            binding.icAnswer.isEnabled = true
            binding.icAnswer.alpha = 1f
        } else {
            binding.icAnswer.setColorFilter(Color.parseColor("#b8b8b8"))
            if(currentUserUid == confessList[adapterPosition].fromUserId) {
                binding.icAnswer.alpha = 0.5f
                binding.icAnswer.isEnabled = false
            } else if(currentUserUid == confessList[adapterPosition].userId) {
                binding.icAnswer.alpha = 1f
                binding.icAnswer.isEnabled = true
            }
            else {
                binding.icAnswer.alpha = 0.5f
                binding.icAnswer.isEnabled = false
            }
        }

        if(confess.favorited) {
            binding.icFavorite.setColorFilter(Color.parseColor("#BA0000"))
        } else {
            binding.icFavorite.setColorFilter(Color.parseColor("#b8b8b8"))
        }

        binding.icAnswer.setOnClickListener {
            val confessAnswer = confessList[adapterPosition]
            val confessDateTimestamp = confess.answer.timestamp

            onAnswerClick(
                confessAnswer.id,
                confessAnswer.userId,
                confessAnswer.fromUserId,
                confess.answer.fromUserImageUrl,
                confess.answer.fromUserUsername,
                confess.answer.username,
                confess.answered,
                confess.answer.text,
                confess.answer.favorited,
                if (confessDateTimestamp != null) calculateTimeSinceConfession(confessDateTimestamp as Timestamp) else ""
            )
        }

        binding.icFavorite.setOnClickListener {
            val confessFavorite = confessList[adapterPosition]
            confessFavorite.favorited = !confessFavorite.favorited
            notifyDataSetChanged()
            onFavoriteClick(confessFavorite.favorited, confessFavorite.id)
        }

        binding.confessionsScreenProfileImage.setOnClickListener {
            val photoClickedUser = confessList[adapterPosition]

            if (currentUserUid != photoClickedUser.fromUserId) {
                onItemPhotoClick(
                    photoClickedUser.fromUserId,
                    photoClickedUser.fromUserEmail,
                    photoClickedUser.fromUserUsername
                )
            }
        }

        binding.moreActionButton.setOnClickListener { view ->
            val popupMenu = androidx.appcompat.widget.PopupMenu(view.context, view)
            popupMenu.menuInflater.inflate(R.menu.confess_item_more_actions_menu, popupMenu.menu)
            popupMenu.setForceShowIcon(true)

            val bookmarkItem = popupMenu.menu.getItem(0)
            bookmarkItem.title = "Add to Bookmarks"

            val unbookmarkItem = popupMenu.menu.getItem(1)
            unbookmarkItem.title = "Remove Bookmark"

            val deleteItem = popupMenu.menu.getItem(2)
            val s = SpannableString("Delete Confess")
            s.setSpan(ForegroundColorSpan(Color.RED), 0, s.length, 0)
            deleteItem.title = s

            bookmarkItem.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_bookmark)
            unbookmarkItem.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_unbookmark)

            deleteItem.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_delete)
            deleteItem.icon?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)

            if(currentUserUid != confessList[adapterPosition].fromUserId) {
                popupMenu.menu.removeItem(R.id.action_delete)
            }
            if (isBookmarks) {
                popupMenu.menu.removeItem(R.id.action_bookmark)
            } else {
                popupMenu.menu.removeItem(R.id.action_unbookmark)
            }

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_bookmark -> {
                        val confessionToBookmark = confessList[adapterPosition]

                        onConfessBookmarkClick(confessionToBookmark.id, confessionToBookmark.timestamp.toString(), confessionToBookmark.fromUserId)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_delete -> {
                        val confessIdToDelete = confessList[adapterPosition].id
                        dialogHelper.showDeleteConfessionDialog(
                            "confession",
                            { onConfessDeleteClick(confessIdToDelete) })
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_unbookmark -> {
                        val confessToUnbookmark = confessList[adapterPosition]
                        onBookmarkRemoveClick(confessToUnbookmark.id)
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
        notifyDataSetChanged()
    }

    fun removeConfession(position: Int) {
        confessList.removeAt(position)
        notifyItemRemoved(position)
        notifyDataSetChanged()
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
