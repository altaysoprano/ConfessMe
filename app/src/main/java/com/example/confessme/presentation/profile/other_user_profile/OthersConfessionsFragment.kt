package com.example.confessme.presentation.profile.other_user_profile

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
import com.example.confessme.databinding.FragmentOthersConfessionsBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.databinding.NoConfessionsHereBeTheFirstOneBinding
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
class OthersConfessionsFragment(
    private val userUid: String,
    private val confessionCategory: ConfessionCategory
) : OtherUserViewPagerFragment(), ScrollableToTop {

    private lateinit var binding: FragmentOthersConfessionsBinding
    private lateinit var profileBinding: FragmentProfileBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var confessListAdapter: ConfessionListAdapter
    private lateinit var currentUserUid: String
    private lateinit var noConfessFoundBinding: NoConfessionsHereBeTheFirstOneBinding

    private val viewModel: ConfessViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentOthersConfessionsBinding.inflate(inflater, container, false)
        profileBinding = FragmentProfileBinding.inflate(inflater, container, false)
        noConfessFoundBinding = binding.othersConfessionsNoConfessFoundView
        navRegister = activity as FragmentNavigation
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUserUid = currentUser?.uid ?: ""

        setConfessionListAdapter()
        setupRecyclerView(binding.othersConfessionsListRecyclerviewId, confessListAdapter
        ) { viewModel.fetchConfessions(userUid, limit, confessionCategory) }

        viewModel.fetchConfessions(userUid, limit, confessionCategory)

        binding.swipeRefreshLayoutOthersConfessions.setOnRefreshListener {
            viewModel.fetchConfessions(userUid, limit, confessionCategory)
            confessListAdapter.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeAddBookmarks()
        observeFetchConfessions()
        observeDeleteConfession()
        observeRemoveBookmark()
    }

    private fun setConfessionListAdapter() {
        confessListAdapter = ConfessionListAdapter(
            requireContext(),
            mutableListOf(),
            currentUserUid,
            false,
            onAnswerClick = { confessionId ->
                onAnswerClick(confessionId, confessListAdapter, currentUserUid)
            },
            onFavoriteClick = { isFavorited, confessionId -> },
            onConfessDeleteClick = { confessionId ->
                viewModel.deleteConfession(confessionId)
            },
            onConfessBookmarkClick = { confessionId, timestamp, userUid ->
                viewModel.addBookmark(confessionId, null, userUid)
            },
            onBookmarkRemoveClick = { confessionId -> },
            onItemPhotoClick = { photoUserUid, photoUserEmail, photoUserToken, photoUserName ->
                onItemPhotoClick(photoUserEmail, photoUserUid, photoUserName, photoUserToken)
            },
            onUserNameClick = { userNameUserUid, userNameUserEmail, userNameUserToken, userNameUserName ->
                onUserNameClick(
                    userNameUserEmail,
                    userNameUserUid,
                    userNameUserName,
                    userNameUserToken
                )
            },
            onTimestampClick = { date ->
                Toast.makeText(context, date, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun observeFetchConfessions() {
        viewModel.fetchConfessionsState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarOthersConfessions.visibility = View.VISIBLE
                    noConfessFoundBinding.root.visibility = View.GONE
                }

                is UiState.Failure -> {
                    binding.progressBarOthersConfessions.visibility = View.GONE
                    binding.swipeRefreshLayoutOthersConfessions.isRefreshing = false
                    noConfessFoundBinding.root.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarOthersConfessions.visibility = View.GONE
                    binding.swipeRefreshLayoutOthersConfessions.isRefreshing = false
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

    private fun observeDeleteConfession() {
        viewModel.deleteConfessionState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarOthersConfessionsGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarOthersConfessionsGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarOthersConfessionsGeneral.visibility = View.GONE
                    val deletedConfession = state.data
                    val position = deletedConfession?.let { findPositionById(it.id, confessListAdapter) }

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

    private fun observeAddBookmarks() {
        viewModel.addBookmarkState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarOthersConfessionsGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarOthersConfessionsGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarOthersConfessionsGeneral.visibility = View.GONE
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
                    binding.progressBarOthersConfessionsGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarOthersConfessionsGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarOthersConfessionsGeneral.visibility = View.GONE
                    val removedBookmark = state.data

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

    private fun onItemPhotoClick(
        photoUserEmail: String, photoUserUid: String,
        photoUserName: String, photoUserToken: String
    ) {
        navigateToUserProfile(
            photoUserEmail, photoUserUid, photoUserName, photoUserToken,
            navRegister, this.userUid
        )
    }

    private fun onUserNameClick(
        userNameUserEmail: String, userNameUserUid: String,
        userNameUserName: String, userNameUserToken: String
    ) {
        navigateToUserProfile(
            userNameUserEmail, userNameUserUid, userNameUserName,
            userNameUserToken, navRegister, this.userUid
        )
    }

    override fun scrollToTop() {
        binding.othersConfessionsListRecyclerviewId.smoothScrollToPosition(0)
    }
}