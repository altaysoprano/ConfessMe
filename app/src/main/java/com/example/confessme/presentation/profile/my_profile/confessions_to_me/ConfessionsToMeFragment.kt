package com.example.confessme.presentation.profile.my_profile.confessions_to_me

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.R
import com.example.confessme.databinding.FragmentConfessionsToMeBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.databinding.NoConfessionsToYouBinding
import com.example.confessme.presentation.confess.ConfessViewModel
import com.example.confessme.presentation.profile.ScrollableToTop
import com.example.confessme.presentation.confess.ConfessAnswerFragment
import com.example.confessme.presentation.profile.ConfessionListAdapter
import com.example.confessme.presentation.utils.FragmentNavigation
import com.example.confessme.presentation.profile.other_user_profile.OtherUserProfileFragment
import com.example.confessme.presentation.profile.ConfessionCategory
import com.example.confessme.presentation.profile.my_profile.MyProfileViewPagerFragment
import com.example.confessme.utils.MyUtils
import com.example.confessme.presentation.utils.ShareHelper
import com.example.confessme.presentation.utils.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfessionsToMeFragment: MyProfileViewPagerFragment(), ScrollableToTop {

    private lateinit var binding: FragmentConfessionsToMeBinding
    private lateinit var profileBinding: FragmentProfileBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var noConfessFoundBinding: NoConfessionsToYouBinding
    private lateinit var currentUserUid: String
    private var myUserName = ""
    private lateinit var shareHelper: ShareHelper

    private lateinit var confessListAdapter: ConfessionListAdapter

    private val viewModel: ConfessViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentConfessionsToMeBinding.inflate(inflater, container, false)
        profileBinding = FragmentProfileBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        noConfessFoundBinding = binding.confessionsToMeNoConfessFoundView
        val currentUser = FirebaseAuth.getInstance().currentUser
        shareHelper = ShareHelper(requireContext())
        currentUserUid = currentUser?.uid ?: ""

        setConfessListAdapter()
        setupRecyclerView(binding.confessionToMeListRecyclerviewId, confessListAdapter,
            {viewModel.fetchConfessions("", limit, ConfessionCategory.CONFESSIONS_TO_ME)})
        setOnShareYourProfileTextClickListener()

        viewModel.fetchConfessions("", limit, ConfessionCategory.CONFESSIONS_TO_ME)

        binding.swipeRefreshLayoutConfessionsToMe.setOnRefreshListener {
            viewModel.fetchConfessions("", limit, ConfessionCategory.CONFESSIONS_TO_ME)
            confessListAdapter.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeAddBookmarks()
        observeFetchConfessions()
        observeAddFavorite()
        observeRemoveBookmark()
    }

    private fun setConfessListAdapter() {
        confessListAdapter = ConfessionListAdapter(
            requireContext(),
            mutableListOf(),
            currentUserUid,
            false,
            onAnswerClick = { confessionId ->
                onAnswerClick(confessionId, currentUserUid, confessListAdapter)
            },
            onFavoriteClick = { isFavorited, confessionId ->
                viewModel.addFavorite(isFavorited, confessionId)
            },
            onConfessDeleteClick = {},
            onConfessBookmarkClick = { confessionId, timestamp, userUid ->
                viewModel.addBookmark(confessionId, null, userUid)
            },
            onBookmarkRemoveClick = {confessionId -> },
            onItemPhotoClick = { photoUserUid, photoUserEmail, photoUserToken, photoUserName ->
                onItemPhotoClick(photoUserEmail, photoUserUid, photoUserName, photoUserToken, navRegister)
            },
            onUserNameClick = { userNameUserUid, userNameUserEmail, userNameUserToken, userNameUserName ->
                onUserNameClick(userNameUserEmail, userNameUserUid, userNameUserName, userNameUserToken, navRegister)
            }
        )
    }

    private fun observeFetchConfessions() {
        viewModel.fetchConfessionsState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessionsToMe.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessionsToMe.visibility = View.GONE
                    binding.swipeRefreshLayoutConfessionsToMe.isRefreshing = false
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessionsToMe.visibility = View.GONE
                    binding.swipeRefreshLayoutConfessionsToMe.isRefreshing = false
                    if (state.data.isEmpty()) {
                        noConfessFoundBinding.root.visibility = View.VISIBLE
                    } else {
                        noConfessFoundBinding.root.visibility = View.GONE
                        confessListAdapter.updateList(state.data)
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observeAddFavorite() {
        viewModel.addFavoriteState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessionsToMe.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessionsToMe.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessionsToMe.visibility = View.GONE
                    val updatedConfession = state.data

                    val position = updatedConfession?.let { findPositionById(it.id, confessListAdapter) }
                    if (position != -1) {
                        if (updatedConfession != null) {
                            if (position != null) {
                                confessListAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeAddBookmarks() {
        viewModel.addBookmarkState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessionsToMeGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessionsToMeGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessionsToMeGeneral.visibility = View.GONE
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

    private fun observeRemoveBookmark() {
        viewModel.removeBookmarkState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessionsToMeGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessionsToMeGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessionsToMeGeneral.visibility = View.GONE
                    val removedBookmark = state.data

                    MyUtils.showSnackbar(
                        rootView = requireActivity().window.decorView.rootView,
                        descriptionText = getString(R.string.removed_from_bookmarks),
                        buttonText = getString(R.string.undo),
                        activity = requireActivity(),
                        context = requireContext(),
                        onButtonClicked = {
                            if(removedBookmark != null) {
                                viewModel.addBookmark(
                                    confessionId = removedBookmark.confessionId,
                                    timestamp = removedBookmark.timestamp,
                                    userUid = removedBookmark.userId
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    private fun setOnShareYourProfileTextClickListener() {
        noConfessFoundBinding.shareYourProfileText.setOnClickListener {
            if (!myUserName.isNullOrEmpty()) {
                shareHelper.shareImage(myUserName)
            } else {
                Toast.makeText(context, getString(R.string.share_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun scrollToTop() {
        binding.confessionToMeListRecyclerviewId.smoothScrollToPosition(0)
    }

    fun receiveUserName(username: String) {
        myUserName = username
    }
}