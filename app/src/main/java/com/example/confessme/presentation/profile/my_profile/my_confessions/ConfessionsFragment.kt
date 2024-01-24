package com.example.confessme.presentation.profile.my_profile.my_confessions

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
import com.example.confessme.databinding.FragmentConfessionsBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.databinding.YouHaveNoConfessionsBinding
import com.example.confessme.presentation.confess.ConfessViewModel
import com.example.confessme.presentation.profile.ScrollableToTop
import com.example.confessme.presentation.confess.ConfessAnswerFragment
import com.example.confessme.presentation.profile.ConfessionListAdapter
import com.example.confessme.presentation.utils.FragmentNavigation
import com.example.confessme.presentation.profile.other_user_profile.OtherUserProfileFragment
import com.example.confessme.presentation.search.SearchFragment
import com.example.confessme.presentation.profile.ConfessionCategory
import com.example.confessme.presentation.profile.my_profile.MyProfileViewPagerFragment
import com.example.confessme.utils.MyUtils
import com.example.confessme.presentation.utils.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfessionsFragment: MyProfileViewPagerFragment(), ScrollableToTop {

    private lateinit var binding: FragmentConfessionsBinding
    private lateinit var profileBinding: FragmentProfileBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var currentUserUid: String
    private lateinit var noConfessFoundBinding: YouHaveNoConfessionsBinding

    private val viewModel: ConfessViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentConfessionsBinding.inflate(inflater, container, false)
        profileBinding = FragmentProfileBinding.inflate(inflater, container, false)
        noConfessFoundBinding = binding.confessionsNoConfessFoundView
        navRegister = activity as FragmentNavigation
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUserUid = currentUser?.uid ?: ""

        setAdapter()
        setRecyclerView()
        setConfessSomeoneTextOnClickListener()
        fetchConfessions()
        setSwiping(binding.swipeRefreshLayoutMyConfessions, {
            viewModel.fetchConfessions("", limit, ConfessionCategory.MY_CONFESSIONS)
            confessListAdapter.notifyDataSetChanged()
        })

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeAddBookmarks()
        observeFetchConfessions()
        observeDeleteConfession()
        observeRemoveBookmark()
    }

    private fun setAdapter() {
        setAdapter(
            isBookmarks = false,
            currentUserUid = currentUserUid,
            navRegister = navRegister,
            onFavoriteClick = { isFavorited, confessionId ->
            },
            onConfessDeleteClick = { confessionId ->
                viewModel.deleteConfession(confessionId)
            },
            onConfessBookmarkClick = { confessionId, timestamp, userUid ->
                viewModel.addBookmark(confessionId, null, userUid)
            },
            onBookmarkRemoveClick = {confessionId -> },
        )
    }

    private fun setRecyclerView() {
        setupRecyclerView(binding.confessionListRecyclerviewId, confessListAdapter,
            {viewModel.fetchConfessions("", limit, ConfessionCategory.MY_CONFESSIONS)})
    }

    private fun fetchConfessions() {
        viewModel.fetchConfessions("", limit, ConfessionCategory.MY_CONFESSIONS)
    }

    private fun observeFetchConfessions() {
        viewModel.fetchConfessionsState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessions.visibility = View.VISIBLE
                    noConfessFoundBinding.root.visibility = View.GONE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessions.visibility = View.GONE
                    binding.swipeRefreshLayoutMyConfessions.isRefreshing = false
                    noConfessFoundBinding.root.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessions.visibility = View.GONE
                    binding.swipeRefreshLayoutMyConfessions.isRefreshing = false
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
                    binding.progressBarConfessionsGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessionsGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessionsGeneral.visibility = View.GONE
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
                    binding.progressBarConfessionsGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessionsGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessionsGeneral.visibility = View.GONE
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
                    binding.progressBarConfessionsGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessionsGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessionsGeneral.visibility = View.GONE
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

    fun setConfessSomeoneTextOnClickListener() {
        binding.confessionsNoConfessFoundView.confessSomeoneText.setOnClickListener {
            navRegister.navigateFrag(SearchFragment(), false)
        }
    }

    override fun scrollToTop() {
        binding.confessionListRecyclerviewId.smoothScrollToPosition(0)
    }
}