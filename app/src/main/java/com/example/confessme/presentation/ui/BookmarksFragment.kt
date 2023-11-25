package com.example.confessme.presentation.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.data.model.Confession
import com.example.confessme.databinding.FragmentBookmarksBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.databinding.NoConfessionsHereBinding
import com.example.confessme.presentation.BookmarksViewModel
import com.example.confessme.presentation.ScrollableToTop
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookmarksFragment() : Fragment(), ScrollableToTop {

    private lateinit var binding: FragmentBookmarksBinding
    private lateinit var profileBinding: FragmentProfileBinding
    private lateinit var confessListAdapter: ConfessionListAdapter
    private lateinit var navRegister: FragmentNavigation
    private lateinit var noConfessFoundBinding: NoConfessionsHereBinding
    private lateinit var currentUserUid: String
    private val viewModel: BookmarksViewModel by viewModels()

    private var limit: Long = 20

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
        setupRecyclerView()

        viewModel.fetchBookmarks(limit)

        setSwiping()

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeFetchBookmarks()
        observeRemoveBookmark()
        observeDeleteConfession()
    }

    private fun setAdapter() {
        confessListAdapter = ConfessionListAdapter(
            requireContext(),
            mutableListOf(),
            currentUserUid,
            true,
            onAnswerClick = { confessionId ->
                onAnswerClick(confessionId)
            },
            onFavoriteClick = {isFavorited, confessionId ->
                viewModel.addFavorite(isFavorited, confessionId)
            },
            onConfessDeleteClick = { confessionId ->
                viewModel.deleteConfession(confessionId)
            },
            onConfessBookmarkClick = { confessionId, timestamp, userUid ->
            },
            onBookmarkRemoveClick = {confessionId ->
                viewModel.deleteBookmark(confessionId)
            },
            onItemPhotoClick = { userUid, userEmail, userToken, userName ->

                val bundle = Bundle()
                bundle.putString("userEmail", userEmail)
                bundle.putString("userUid", userUid)
                bundle.putString("userName", userName)
                bundle.putString("userToken", userToken)

                val profileFragment = OtherUserProfileFragment()
                profileFragment.arguments = bundle

                navRegister.navigateFrag(profileFragment, true)
            },
            onUserNameClick =  { userUid, userEmail, userToken, userName ->

                val bundle = Bundle()
                bundle.putString("userEmail", userEmail)
                bundle.putString("userUid", userUid)
                bundle.putString("userName", userName)
                bundle.putString("userToken", userToken)

                val profileFragment = OtherUserProfileFragment()
                profileFragment.arguments = bundle

                navRegister.navigateFrag(profileFragment, true)
            }
        )
    }

    private fun setupRecyclerView() {
        binding.bookmarkListRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = confessListAdapter
            this.addOnScrollListener(object :
                RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= limit
                    ) {
                        limit += 10
                        viewModel.fetchBookmarks(limit)
                    }
                }
            })
        }
    }

    private fun setSwiping() {
        binding.swipeRefreshLayoutMyConfessions.setOnRefreshListener {
            viewModel.fetchBookmarks(limit)
        }
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
                    limit = if(state.data.size < 20) 20 else state.data.size.toLong()

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
                    val position = deletedConfession?.let { findPositionById(it.id) }

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
                    val position = findPositionById(removedBookmark.id)

                    if (position != -1) {
                        confessListAdapter.removeConfession(position)
                        limit -= 1
                    }
                }
            }
        }
    }

    private fun onAnswerClick(confessionId: String) {
        if (!confessionId.isNullOrEmpty()) {
            val bundle = Bundle()
            bundle.putString("confessionId", confessionId)
            bundle.putString("currentUserUid", currentUserUid)

            val confessAnswerFragment = ConfessAnswerFragment(
                { position, updatedConfession ->
                    confessListAdapter.updateItem(position, updatedConfession)
                },
                { confessionId ->
                    findPositionById(confessionId)
                }
            )
            confessAnswerFragment.arguments = bundle
            confessAnswerFragment.show(
                requireActivity().supportFragmentManager,
                "ConfessAnswerFragment"
            )
        } else {
            Toast.makeText(requireContext(), "Confession not found", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun findPositionById(confessionId: String): Int {
        for (index in 0 until confessListAdapter.confessList.size) {
            if (confessListAdapter.confessList[index].id == confessionId) {
                return index
            }
        }
        return -1
    }

    override fun scrollToTop() {
        binding.bookmarkListRecyclerviewId.smoothScrollToPosition(0)
    }
}