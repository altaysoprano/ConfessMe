package com.example.confessme.presentation.profile.my_profile.bookmarks

import android.os.Bundle
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
import com.example.confessme.data.model.Confession
import com.example.confessme.databinding.FragmentBookmarksBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.databinding.NoConfessionsHereBinding
import com.example.confessme.presentation.profile.ScrollableToTop
import com.example.confessme.presentation.confess.ConfessAnswerFragment
import com.example.confessme.presentation.profile.ConfessionCategory
import com.example.confessme.presentation.profile.ConfessionListAdapter
import com.example.confessme.presentation.profile.my_profile.MyProfileViewPagerFragment
import com.example.confessme.presentation.utils.FragmentNavigation
import com.example.confessme.presentation.profile.other_user_profile.OtherUserProfileFragment
import com.example.confessme.utils.MyUtils
import com.example.confessme.presentation.utils.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookmarksFragment() : MyProfileViewPagerFragment(), ScrollableToTop {

    private lateinit var binding: FragmentBookmarksBinding
    private lateinit var profileBinding: FragmentProfileBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var noConfessFoundBinding: NoConfessionsHereBinding
    private lateinit var currentUserUid: String
    private var removedBookmarkPosition = -1
    private val viewModel: BookmarksViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        profileBinding = FragmentProfileBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUserUid = currentUser?.uid ?: ""
        noConfessFoundBinding = binding.noConfessionsHereText

        setAdapter()
        setRecyclerView()
        fetchConfessions()
        setSwiping(binding.swipeRefreshLayoutMyConfessions, {viewModel.fetchBookmarks(limit)})

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeFetchBookmarks()
        observeRemoveBookmark()
        observeDeleteConfession()
        observeAddBookmarks()
    }

    private fun setAdapter() {
        setAdapter(
            isBookmarks = true,
            currentUserUid = currentUserUid,
            navRegister = navRegister,
            onFavoriteClick = { isFavorited, confessionId ->
                viewModel.addFavorite(isFavorited, confessionId)
            },
            onConfessDeleteClick = {confessionId ->
                viewModel.deleteConfession(confessionId)
            },
            onConfessBookmarkClick = {confessionId, timestamp, userUid ->
            },
            onBookmarkRemoveClick = {confessionId ->
                viewModel.deleteBookmark(confessionId)
            }
        )
    }

    private fun setRecyclerView() {
        setupRecyclerView(binding.bookmarkListRecyclerviewId, confessListAdapter,
            { viewModel.fetchBookmarks(limit) })
    }

    private fun fetchConfessions() {
        viewModel.fetchBookmarks(limit)
    }

    private fun observeFetchBookmarks() {
        viewModel.fetchBookmarksState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarBookmarks.visibility = View.VISIBLE
                    noConfessFoundBinding.root.visibility = View.GONE
                }

                is UiState.Failure -> {
                    binding.progressBarBookmarks.visibility = View.GONE
                    binding.swipeRefreshLayoutMyConfessions.isRefreshing = false
                    noConfessFoundBinding.root.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarBookmarks.visibility = View.GONE
                    binding.swipeRefreshLayoutMyConfessions.isRefreshing = false
                    limit = if (state.data.size < 20) 20 else state.data.size.toLong()

                    if (state.data.isEmpty()) {
                        noConfessFoundBinding.root.visibility = View.VISIBLE
                        confessListAdapter.updateList(state.data as List<Confession>)
                    } else {
                        noConfessFoundBinding.root.visibility = View.GONE
                        confessListAdapter.updateList(state.data as List<Confession>)
                    }
                }
            }
        }
    }

    private fun observeDeleteConfession() {
        viewModel.deleteConfessionState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarBookmarksGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarBookmarksGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarBookmarksGeneral.visibility = View.GONE
                    val deletedConfession = state.data
                    val position =
                        deletedConfession?.let { findPositionById(it.id, confessListAdapter) }

                    if (position != -1) {
                        if (deletedConfession != null) {
                            if (position != null) {
                                confessListAdapter.removeConfession(position)
                                limit -= 1
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeRemoveBookmark() {
        viewModel.removeBookmarkState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarBookmarksGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarBookmarksGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarBookmarksGeneral.visibility = View.GONE
                    val removedBookmark = state.data
                    val position = removedBookmark?.confessionId?.let {
                        findPositionById(
                            it,
                            confessListAdapter
                        )
                    }

                    if (position != -1) {
                        if (position != null) {
                            confessListAdapter.removeConfession(position)
                            removedBookmarkPosition = position
                            limit -= 1
                        }
                    }

                    MyUtils.showSnackbar(
                        rootView = requireActivity().window.decorView.rootView,
                        descriptionText = getString(R.string.removed_from_bookmarks),
                        buttonText = getString(R.string.undo),
                        activity = requireActivity(),
                        context = requireContext(),
                        onButtonClicked = {
                            if (removedBookmark != null) {
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

    private fun observeAddBookmarks() {
        viewModel.addBookmarkState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarBookmarksGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarBookmarksGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarBookmarksGeneral.visibility = View.GONE
                    val addedConfession = state.data
                    val position = removedBookmarkPosition

                    if (position != -1) {
                        if (addedConfession != null) {
                            confessListAdapter.addConfession(addedConfession, position)
                            limit += 1
                        }
                    }

                    MyUtils.showSnackbar(
                        rootView = requireActivity().window.decorView.rootView,
                        descriptionText = getString(R.string.successfully_added_to_bookmarks),
                        buttonText = getString(R.string.undo),
                        activity = requireActivity(),
                        context = requireContext(),
                        onButtonClicked = {
                            addedConfession?.id?.let { viewModel.deleteBookmark(it) }
                        }
                    )
                }
            }
        }
    }

    override fun scrollToTop() {
        binding.bookmarkListRecyclerviewId.smoothScrollToPosition(0)
    }
}