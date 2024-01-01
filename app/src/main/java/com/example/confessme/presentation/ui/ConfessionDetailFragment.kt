package com.example.confessme.presentation.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.data.model.Confession
import com.example.confessme.databinding.FragmentConfessionDetailBinding
import com.example.confessme.presentation.ConfessMeDialog
import com.example.confessme.presentation.ConfessionDetailViewModel
import com.example.confessme.util.MyUtils
import com.example.confessme.util.UiState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfessionDetailFragment : Fragment(), AnswerDataListener {

    private lateinit var binding: FragmentConfessionDetailBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var confessionId: String
    private lateinit var currentUserUid: String
    private var isConfessFavorited: Boolean = false
    private val viewModel: ConfessionDetailViewModel by viewModels()
    private lateinit var dialogHelper: ConfessMeDialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConfessionDetailBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity?)!!.title = "Confess Detail"
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.confessionDetailToolbar)
        navRegister = activity as FragmentNavigation
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUserUid = currentUser?.uid ?: ""
        confessionId = arguments?.getString("confessionId") ?: ""
        dialogHelper = ConfessMeDialog(requireContext())

        setHasOptionsMenu(true)

        (activity as AppCompatActivity?)!!.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
        }

        viewModel.getConfession(confessionId)

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeGetConfession()
        observeFavorite()
        observeDeleteConfession()
        observeAddBookmarks()
    }

    private fun observeGetConfession() {
        viewModel.getConfessionState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessionDetail.visibility = View.VISIBLE
                    binding.confessionDetailRelativeLayout.visibility = View.GONE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessionDetail.visibility = View.GONE
                    binding.confessionDetailRelativeLayout.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessionDetail.visibility = View.GONE
                    binding.confessionDetailRelativeLayout.visibility = View.VISIBLE
                    if (state.data != null) {
                        setItem(state.data, binding, requireView())
                    }
                }
            }
        }
    }

    private fun setItem(
        confess: Confession,
        binding: FragmentConfessionDetailBinding,
        itemView: View,
    ) {
        val toUserName = "@${confess.username} "
        val spannable = SpannableString("$toUserName${confess.text}")

        val usernameColor = ContextCompat.getColor(itemView.context, R.color.confessmered)
        val usernameStart = 0
        val usernameEnd = toUserName.length

        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {

                    if (currentUserUid != confess.userId) {
                        onUserNameClick(
                            confess.userId,
                            confess.email,
                            confess.userToken,
                            confess.username
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

        val usernameTv = binding.confessionDetailScreenUsername
        usernameTv.text = confess.fromUserUsername
        if (confess.fromUserUsername.equals("Anonymous")) {
            usernameTv.setBackgroundResource(R.drawable.anonymous_username_background)
        } else {
            usernameTv.setBackgroundColor(Color.TRANSPARENT)
        }
        binding.confessionDetailScreenConfession.text = spannable
        binding.confessionDetailScreenConfession.movementMethod = LinkMovementMethod.getInstance()
        binding.confessionDetailScreenConfession.highlightColor = Color.TRANSPARENT
        binding.confessionDetailScreenTimestamp.text =
            MyUtils.calculateTimeSinceConfession(confess.timestamp as Timestamp, requireContext())

        if (confess.fromUserImageUrl.isNotEmpty()) {
            Glide.with(itemView)
                .load(confess.fromUserImageUrl)
                .into(binding.confessionDetailScreenProfileImage)
        } else {
            binding.confessionDetailScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
        }

        binding.confessionDetailScreenTimestamp.setOnClickListener {
            val date = MyUtils.convertFirestoreTimestampToReadableDate(confess.timestamp, requireContext())
            Toast.makeText(context, date, Toast.LENGTH_SHORT).show()
        }

        setAnswerFavoriteAndMoreActionsItems(confess, binding, itemView)
    }

    private fun onUserNameClick(
        userUid: String,
        userEmail: String,
        userToken: String,
        userName: String
    ) {
        val bundle = Bundle()
        bundle.putString("userEmail", userEmail)
        bundle.putString("userUid", userUid)
        bundle.putString("userName", userName)
        bundle.putString("userToken", userToken)

        val profileFragment = OtherUserProfileFragment()
        profileFragment.arguments = bundle

        navRegister.navigateFrag(profileFragment, true)
    }

    private fun onAnswerClick(confessionId: String) {
        if (!confessionId.isNullOrEmpty()) {
            val bundle = Bundle()
            bundle.putString("confessionId", confessionId)
            bundle.putString("currentUserUid", currentUserUid)

            val confessAnswerFragment = ConfessAnswerFragment(
                { position, updatedConfession -> },
                { confessionId ->
                    -1
                }
            )
            confessAnswerFragment.setDataListener(this)
            confessAnswerFragment.arguments = bundle
            confessAnswerFragment.show(
                requireActivity().supportFragmentManager,
                "ConfessAnswerFragment"
            )
        } else {
            Toast.makeText(requireContext(), getString(R.string.confession_not_found), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun onFavoriteClick(favorited: Boolean) {
        viewModel.addFavorite(favorited, confessionId)
    }

    override fun onAnswerDataReceived(isAnswered: Boolean) {
        if (isAnswered) {
            binding.confessionDetailScreenIcAnswer.setColorFilter(Color.parseColor("#BA0000"))
        } else {
            binding.confessionDetailScreenIcAnswer.setColorFilter(Color.parseColor("#b8b8b8"))
        }
    }

    @SuppressLint("RestrictedApi")
    private fun setAnswerFavoriteAndMoreActionsItems(
        confess: Confession,
        binding: FragmentConfessionDetailBinding,
        itemView: View
    ) {
        if (currentUserUid == confess.fromUserId) {
            binding.confessionDetailScreenIcFavorite.alpha = 0.5f
            binding.confessionDetailScreenIcAnswer.alpha = 0.5f
            binding.confessionDetailScreenIcFavorite.isEnabled = false
            binding.confessionDetailScreenIcAnswer.isEnabled = false
        } else if (currentUserUid == confess.userId) {
            binding.confessionDetailScreenIcAnswer.isEnabled = true
            binding.confessionDetailScreenIcFavorite.isEnabled = true
            binding.confessionDetailScreenIcFavorite.alpha = 1f
        } else {
            binding.confessionDetailScreenIcFavorite.alpha = 0.5f
            binding.confessionDetailScreenIcAnswer.alpha = 0.5f
            binding.confessionDetailScreenIcFavorite.isEnabled = false
            binding.confessionDetailScreenIcAnswer.isEnabled = false
        }

        if (confess.answered) {
            binding.confessionDetailScreenIcAnswer.setColorFilter(Color.parseColor("#BA0000"))
            binding.confessionDetailScreenIcAnswer.isEnabled = true
            binding.confessionDetailScreenIcAnswer.alpha = 1f
        } else {
            binding.confessionDetailScreenIcAnswer.setColorFilter(Color.parseColor("#b8b8b8"))
            if (currentUserUid == confess.fromUserId) {
                binding.confessionDetailScreenIcAnswer.alpha = 0.5f
                binding.confessionDetailScreenIcAnswer.isEnabled = false
            } else if (currentUserUid == confess.userId) {
                binding.confessionDetailScreenIcAnswer.alpha = 1f
                binding.confessionDetailScreenIcAnswer.isEnabled = true
            } else {
                binding.confessionDetailScreenIcAnswer.alpha = 0.5f
                binding.confessionDetailScreenIcAnswer.isEnabled = false
            }
        }

        isConfessFavorited = confess.favorited

        if (isConfessFavorited) {
            binding.confessionDetailScreenIcFavorite.setColorFilter(Color.parseColor("#BA0000"))
        } else {
            binding.confessionDetailScreenIcFavorite.setColorFilter(Color.parseColor("#b8b8b8"))
        }

        binding.confessionDetailScreenIcAnswer.setOnClickListener {
            val confessAnswer = confess

            onAnswerClick(
                confessAnswer.id
            )
        }

        binding.confessionDetailScreenIcFavorite.setOnClickListener {
            isConfessFavorited = !isConfessFavorited
            onFavoriteClick(isConfessFavorited)
        }

        binding.confessionDetailScreenProfileImage.setOnClickListener {
            val photoClickedUser = confess

            if (currentUserUid != photoClickedUser.fromUserId && photoClickedUser.fromUserId != "") {
                val bundle = Bundle()
                bundle.putString("userEmail", photoClickedUser.fromUserEmail)
                bundle.putString("userUid", photoClickedUser.fromUserId)
                bundle.putString("userName", photoClickedUser.fromUserUsername)
                bundle.putString("userToken", photoClickedUser.fromUserToken)

                val profileFragment = OtherUserProfileFragment()
                profileFragment.arguments = bundle

                navRegister.navigateFrag(profileFragment, true)
            }
        }

        binding.confessionDetailScreenMoreActionButton.setOnClickListener { view ->
            val popupMenu = androidx.appcompat.widget.PopupMenu(view.context, view)
            popupMenu.menuInflater.inflate(R.menu.confess_item_more_actions_menu, popupMenu.menu)
            popupMenu.setForceShowIcon(true)

            val bookmarkItem = popupMenu.menu.getItem(0)
            bookmarkItem.title = getString(R.string.add_to_bookmarks)

            val unbookmarkItem = popupMenu.menu.getItem(1)
            unbookmarkItem.title = getString(R.string.remove_bookmark)

            val deleteItem = popupMenu.menu.getItem(2)
            val s = SpannableString(getString(R.string.delete_confess))
            s.setSpan(ForegroundColorSpan(Color.RED), 0, s.length, 0)
            deleteItem.title = s

            bookmarkItem.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_bookmark)
            unbookmarkItem.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_unbookmark)

            deleteItem.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_delete)
            deleteItem.icon?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)

            val confession = confess

            if (currentUserUid != confession.fromUserId && currentUserUid != confession.anonymousId) {
                popupMenu.menu.removeItem(R.id.action_delete)
            }

            popupMenu.menu.removeItem(R.id.action_unbookmark)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_bookmark -> {
                        val confessionToBookmark = confess

                        viewModel.addBookmark(
                            confessionToBookmark.id,
                            null,
                            confessionToBookmark.fromUserId
                        )
                        return@setOnMenuItemClickListener true
                    }

                    R.id.action_delete -> {
                        val confessIdToDelete = confess.id
                        dialogHelper.showDialog(
                            getString(R.string.delete_confess_on),
                            getString(R.string.are_you_sure_you_really_want_to_delete_this_confession),
                            { viewModel.deleteConfession(confessIdToDelete) })
                        return@setOnMenuItemClickListener true
                    }

                    R.id.action_unbookmark -> {
/*
                        val confessToUnbookmark = confess
                        onBookmarkRemoveClick(confessToUnbookmark.id)
*/
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

    private fun observeFavorite() {
        viewModel.addFavoriteState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.confessionDetailScreenIcFavorite.visibility = View.INVISIBLE
                    binding.confessionDetailScreenProgressBarFavorite.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.confessionDetailScreenIcFavorite.visibility = View.VISIBLE
                    binding.confessionDetailScreenProgressBarFavorite.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.confessionDetailScreenIcFavorite.visibility = View.VISIBLE
                    binding.confessionDetailScreenProgressBarFavorite.visibility = View.GONE
                    val updatedConfession = state.data

                    setFavorite(updatedConfession?.favorited)
                }
            }
        }
    }

    private fun setFavorite(favorited: Boolean?) {
        if (favorited == true) {
            binding.confessionDetailScreenIcFavorite.setColorFilter(resources.getColor(R.color.confessmered))
        } else {
            binding.confessionDetailScreenIcFavorite.setColorFilter(Color.parseColor("#B8B8B8"))
        }
    }

    private fun observeDeleteConfession() {
        viewModel.deleteConfessionState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessionDetail.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessionDetail.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessionDetail.visibility = View.GONE
                    requireActivity().onBackPressed()

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.confession_deleted_successfully),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
    }

    private fun observeAddBookmarks() {
        viewModel.addBookmarkState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessionDetail.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessionDetail.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessionDetail.visibility = View.GONE
                    val confession = state.data
                    MyUtils.showSnackbar(
                        rootView = requireActivity().window.decorView.rootView,
                        descriptionText = getString(R.string.successfully_added_to_bookmarks),
                        buttonText = getString(R.string.undo),
                        activity = requireActivity(),
                        context = requireContext(),
                        onButtonClicked = {
                            confession?.id?.let { viewModel.deleteBookmark(it) }
                        }
                    )
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
                return true
            }
        }
        return false
    }
}