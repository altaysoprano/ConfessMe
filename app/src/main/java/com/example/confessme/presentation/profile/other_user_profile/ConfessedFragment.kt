package com.example.confessme.presentation.profile.other_user_profile

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.R
import com.example.confessme.databinding.FragmentConfessedBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.databinding.NoConfessionsHereBinding
import com.example.confessme.presentation.confess.ConfessViewModel
import com.example.confessme.presentation.profile.ScrollableToTop
import com.example.confessme.presentation.profile.ConfessionListAdapter
import com.example.confessme.presentation.utils.FragmentNavigation
import com.example.confessme.presentation.profile.ConfessionCategory
import com.example.confessme.utils.MyUtils
import com.example.confessme.presentation.utils.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfessedFragment(
    private val userUid: String,
    private val confessionCategory: ConfessionCategory
) : OtherUserViewPagerFragment(), ScrollableToTop {

    private lateinit var binding: FragmentConfessedBinding
    private lateinit var profileBinding: FragmentProfileBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var noConfessFoundBinding: NoConfessionsHereBinding
    private lateinit var currentUserUid: String

    private lateinit var confessListAdapter: ConfessionListAdapter

    private val viewModel: ConfessViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentConfessedBinding.inflate(inflater, container, false)
        profileBinding = FragmentProfileBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        noConfessFoundBinding = binding.confessedNoConfessFoundView
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUserUid = currentUser?.uid ?: ""

        setConfessListAdapter()
        setupRecyclerView(binding.confessedListRecyclerviewId, confessListAdapter
        ) { viewModel.fetchConfessions(userUid, limit, confessionCategory) }

        viewModel.fetchConfessions(userUid, limit, confessionCategory)

        binding.swipeRefreshLayoutConfessed.setOnRefreshListener {
            viewModel.fetchConfessions(userUid, limit, confessionCategory)
            confessListAdapter.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeFetchConfessions()
        observeAddFavorite()
        observeAddBookmarks()
        observeRemoveBookmark()
    }

    private fun setConfessListAdapter() {
        confessListAdapter = ConfessionListAdapter(
            requireContext(),
            mutableListOf(),
            currentUserUid,
            false,
            onAnswerClick = { confessionId ->
                onAnswerClick(confessionId, confessListAdapter, currentUserUid)
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
                onItemPhotoClick(photoUserEmail, photoUserUid, photoUserName, photoUserToken)
            },
            onUserNameClick = { userNameUserUid, userNameUserEmail, userNameUserToken, userNameUserName ->
                onUserNameClick(userNameUserEmail, userNameUserUid, userNameUserName, userNameUserToken)
            },
            onTimestampClick = {date ->
                Toast.makeText(context, date, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun observeFetchConfessions() {
        viewModel.fetchConfessionsState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessed.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessed.visibility = View.GONE
                    binding.swipeRefreshLayoutConfessed.isRefreshing = false
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessed.visibility = View.GONE
                    binding.swipeRefreshLayoutConfessed.isRefreshing = false
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
                    binding.progressBarConfessed.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessed.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessed.visibility = View.GONE
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
                    binding.progressBarConfessedGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessedGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessedGeneral.visibility = View.GONE
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
                    binding.progressBarConfessedGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessedGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessedGeneral.visibility = View.GONE
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

    private fun onItemPhotoClick(photoUserEmail: String, photoUserUid: String, photoUserName: String, photoUserToken: String) {
        navigateToUserProfile(photoUserEmail, photoUserUid, photoUserName, photoUserToken, navRegister, this.userUid)
    }

    private fun onUserNameClick(userNameUserEmail: String, userNameUserUid: String, userNameUserName: String, userNameUserToken: String) {
        navigateToUserProfile(userNameUserEmail, userNameUserUid, userNameUserName, userNameUserToken, navRegister, this.userUid)
    }

    override fun scrollToTop() {
        binding.confessedListRecyclerviewId.smoothScrollToPosition(0)
    }
}