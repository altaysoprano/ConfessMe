package com.example.confessme.presentation.profile

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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.data.model.Confession
import com.example.confessme.databinding.ConfessItemBinding
import com.example.confessme.presentation.utils.ConfessMeDialog
import com.example.confessme.utils.MyUtils
import com.google.firebase.Timestamp

class ConfessionListAdapter(
    private val context: Context,
    val confessList: MutableList<Confession> = mutableListOf(),
    private val currentUserUid: String,
    private val isBookmarks: Boolean,
    private val onAnswerClick: (String) -> Unit,
    private val onFavoriteClick: (Boolean, String) -> Unit,
    private val onConfessDeleteClick: (String) -> Unit,
    private val onConfessBookmarkClick: (String, Timestamp, String) -> Unit,
    private val onBookmarkRemoveClick: (String) -> Unit,
    private val onItemPhotoClick: (String, String, String, String) -> Unit,
    private val onUserNameClick: (String, String, String, String) -> Unit,
) : RecyclerView.Adapter<ConfessionListAdapter.ConfessionViewHolder>() {

    private val dialogHelper = ConfessMeDialog(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfessionViewHolder {
        val binding = ConfessItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConfessionViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onBindViewHolder(
        holder: ConfessionViewHolder,
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
        val toUserName = "@${MyUtils.truncateText(confess.username, 18)} "
        val spannable = SpannableString("$toUserName${confess.text}")

        val usernameColor = ContextCompat.getColor(itemView.context, R.color.confessmered)
        val usernameStart = 0
        val usernameEnd = toUserName.length

        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val userNameClickedUser = confessList[adapterPosition]

                    if (currentUserUid != userNameClickedUser.userId) {
                        onUserNameClick(
                            userNameClickedUser.userId,
                            userNameClickedUser.email,
                            userNameClickedUser.userToken,
                            userNameClickedUser.username
                        )
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.color = usernameColor
                    ds.isUnderlineText = false
                }
            },
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

        val usernameTv = binding.confessionsScreenUsername
        usernameTv.text = confess.fromUserUsername
        if (confess.fromUserUsername.equals("Anonymous")) {
            usernameTv.setBackgroundResource(R.drawable.anonymous_username_background)
        } else {
            usernameTv.setBackgroundColor(Color.TRANSPARENT)
        }
        binding.confessionsScreenConfession.text = spannable
        binding.confessionsScreenConfession.movementMethod = LinkMovementMethod.getInstance()
        binding.confessionsScreenConfession.highlightColor = Color.TRANSPARENT
        binding.confessionsScreenTimestamp.text =
            MyUtils.calculateTimeSinceConfession(confess.timestamp as Timestamp, context)

        if (confess.fromUserImageUrl.isNotEmpty()) {
            Glide.with(itemView)
                .load(confess.fromUserImageUrl)
                .placeholder(R.drawable.empty_profile_photo)
                .into(binding.confessionsScreenProfileImage)
        } else {
            binding.confessionsScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
        }

        setAnswerFavoriteAndMoreActionsItems(confess, binding, adapterPosition)

        binding.confessionsScreenConfession.setOnClickListener {
            confess.isExpanded = !confess.isExpanded
            notifyItemChanged(adapterPosition)
        }

        binding.confessionsScreenConfession.setOnLongClickListener {
            MyUtils.copyTextToClipboard(confess.text, context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("RestrictedApi")
    private fun setAnswerFavoriteAndMoreActionsItems(
        confess: Confession,
        binding: ConfessItemBinding,
        adapterPosition: Int
    ) {
        if (currentUserUid == confessList[adapterPosition].fromUserId) {
            binding.icFavorite.alpha = 0.5f
            binding.icAnswer.alpha = 0.5f
            binding.icFavorite.isEnabled = false
            binding.icAnswer.isEnabled = false
        } else if (currentUserUid == confessList[adapterPosition].userId) {
            binding.icAnswer.isEnabled = true
            binding.icFavorite.isEnabled = true
            binding.icFavorite.alpha = 1f
        } else {
            binding.icFavorite.alpha = 0.5f
            binding.icAnswer.alpha = 0.5f
            binding.icFavorite.isEnabled = false
            binding.icAnswer.isEnabled = false
        }

        if (confess.answered) {
            binding.icAnswer.setColorFilter(Color.parseColor("#BA0000"))
            binding.icAnswer.isEnabled = true
            binding.icAnswer.alpha = 1f
        } else {
            binding.icAnswer.setColorFilter(Color.parseColor("#b8b8b8"))
            if (currentUserUid == confessList[adapterPosition].fromUserId) {
                binding.icAnswer.alpha = 0.5f
                binding.icAnswer.isEnabled = false
            } else if (currentUserUid == confessList[adapterPosition].userId) {
                binding.icAnswer.alpha = 1f
                binding.icAnswer.isEnabled = true
            } else {
                binding.icAnswer.alpha = 0.5f
                binding.icAnswer.isEnabled = false
            }
        }

        if (confess.favorited) {
            binding.icFavorite.setColorFilter(Color.parseColor("#BA0000"))
        } else {
            binding.icFavorite.setColorFilter(Color.parseColor("#b8b8b8"))
        }

        binding.icAnswer.setOnClickListener {
            val confessAnswer = confessList[adapterPosition]

            onAnswerClick(
                confessAnswer.id
            )
        }

        binding.icFavorite.setOnClickListener {
            val confessFavorite = confessList[adapterPosition]
            confessFavorite.favorited = !confessFavorite.favorited
            notifyItemChanged(adapterPosition)
            onFavoriteClick(confessFavorite.favorited, confessFavorite.id)
        }

        binding.confessionsScreenProfileImage.setOnClickListener {
            val photoClickedUser = confessList[adapterPosition]

            if (currentUserUid != photoClickedUser.fromUserId && photoClickedUser.fromUserId != "") {
                onItemPhotoClick(
                    photoClickedUser.fromUserId,
                    photoClickedUser.fromUserEmail,
                    photoClickedUser.fromUserToken,
                    photoClickedUser.fromUserUsername
                )
            }
        }

        binding.moreActionButton.setOnClickListener { view ->
            val popupMenu = androidx.appcompat.widget.PopupMenu(view.context, view)
            popupMenu.menuInflater.inflate(R.menu.confess_item_more_actions_menu, popupMenu.menu)
            popupMenu.setForceShowIcon(true)

            val bookmarkItem = popupMenu.menu.getItem(0)
            bookmarkItem.title = context.getString(R.string.add_to_bookmarks)

            val unbookmarkItem = popupMenu.menu.getItem(1)
            unbookmarkItem.title = context.getString(R.string.remove_bookmark)

            val deleteItem = popupMenu.menu.getItem(2)
            val s = SpannableString(context.getString(R.string.delete_confess))
            s.setSpan(ForegroundColorSpan(Color.RED), 0, s.length, 0)
            deleteItem.title = s

            bookmarkItem.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_bookmark)
            unbookmarkItem.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_unbookmark)

            deleteItem.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_delete)
            deleteItem.icon?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)

            val confession = confessList[adapterPosition]

            if (currentUserUid != confession.fromUserId && currentUserUid != confession.anonymousId) {
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

                        onConfessBookmarkClick(
                            confessionToBookmark.id,
                            confessionToBookmark.timestamp as Timestamp,
                            confessionToBookmark.fromUserId
                        )
                        return@setOnMenuItemClickListener true
                    }

                    R.id.action_delete -> {
                        val confessIdToDelete = confessList[adapterPosition].id
                        dialogHelper.showDialog(
                            context.getString(R.string.delete_confess_on),
                            context.getString(R.string.are_you_sure_you_really_want_to_delete_this_confession),
                            context.getString(R.string.yes),
                            context.getString(R.string.no),
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

        val date = MyUtils.convertFirestoreTimestampToReadableDate(confess.timestamp, context)
        binding.confessionsScreenTimestamp.tooltipText = date
    }

    fun updateItem(position: Int, updatedConfession: Confession) {
        confessList[position] = updatedConfession
        notifyItemChanged(position)
    }

    fun removeConfession(position: Int) {
        confessList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, confessList.size)
    }

    fun addConfession(newConfession: Confession, position: Int) {
        confessList.add(position, newConfession)
        notifyItemInserted(position)
        notifyItemRangeChanged(position, confessList.size)
    }
}
