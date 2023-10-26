package com.example.confessme.presentation.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.R
import com.example.confessme.databinding.FragmentConfessionsBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.databinding.YouHaveNoConfessionsBinding
import com.example.confessme.presentation.ConfessViewModel
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Math.abs

@AndroidEntryPoint
class ConfessionsFragment(
    private val userUid: String,
    private val confessionCategory: ConfessionCategory
    ) : Fragment() {

    private lateinit var binding: FragmentConfessionsBinding
    private lateinit var profileBinding: FragmentProfileBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var confessListAdapter: ConfessionListAdapter
    private lateinit var noConfessFoundBinding: YouHaveNoConfessionsBinding
    private var limit: Long = 20

    private val viewModel: ConfessViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentConfessionsBinding.inflate(inflater, container, false)
        profileBinding = FragmentProfileBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserUid = currentUser?.uid ?: ""
        confessListAdapter = ConfessionListAdapter(
            requireContext(),
            mutableListOf(),
            currentUserUid,
            false,
            onAnswerClick = { confessionId, userId, fromUserUid, fromUserImageUrl, answeredUserName, confessedUserName, isAnswered, answerText, isFavorited, answerDate ->
                if (!confessionId.isNullOrEmpty()) {
                    val bundle = Bundle()
                    bundle.putString("confessionId", confessionId)
                    bundle.putBoolean("isAnswered", isAnswered)
                    bundle.putString("answerText", answerText)
                    bundle.putString("currentUserUid", currentUserUid)
                    bundle.putString("answerUserUid", userId)
                    bundle.putString("fromUserImageUrl", fromUserImageUrl)
                    bundle.putString("answeredUserName", answeredUserName)
                    bundle.putString("confessedUserName", confessedUserName)
                    bundle.putString("answerFromUserUid", fromUserUid)
                    bundle.putBoolean("favorited", isFavorited)
                    bundle.putString("answerDate", answerDate)
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
            },
            onFavoriteClick = {isFavorited, confessionId ->

            },
            onConfessDeleteClick = { confessionId ->
                viewModel.deleteConfession(confessionId)
            },
            onConfessBookmarkClick = { confessionId, timestamp, userUid ->
                viewModel.addBookmark(confessionId, timestamp, userUid)
            },
            onBookmarkRemoveClick = {confessionId -> },
            onItemPhotoClick = { userUid, userEmail, userName ->

                val bundle = Bundle()
                bundle.putString("userEmail", userEmail)
                bundle.putString("userUid", userUid)

                val profileFragment = OtherUserProfileFragment()
                profileFragment.arguments = bundle

                navRegister.navigateFrag(profileFragment, true)
            },
            onUserNameClick =  { userUid, userEmail, userName ->

                val bundle = Bundle()
                bundle.putString("userEmail", userEmail)
                bundle.putString("userUid", userUid)

                val profileFragment = OtherUserProfileFragment()
                profileFragment.arguments = bundle

                navRegister.navigateFrag(profileFragment, true)
            }
        )
        noConfessFoundBinding = binding.confessionsNoConfessFoundView

        viewModel.fetchConfessions(userUid, limit, confessionCategory)

        binding.confessionListRecyclerviewId.addOnScrollListener(object :
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
                    viewModel.fetchConfessions(userUid, limit, confessionCategory)
                }
            }
        })

        binding.swipeRefreshLayoutMyConfessions.setOnRefreshListener {
            viewModel.fetchConfessions(userUid, limit, confessionCategory)
            confessListAdapter.notifyDataSetChanged()
        }

        setupRecyclerView()
        observeFetchConfessions()
        observeDeleteConfession()
        observeAddBookmarks()

        return binding.root
    }

    private fun setupRecyclerView() {
        binding.confessionListRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = confessListAdapter
        }
    }

    private fun observeFetchConfessions() {
        viewModel.fetchConfessionsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessions.visibility = View.VISIBLE
                    noConfessFoundBinding.root.visibility = View.GONE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessions.visibility = View.GONE
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
        viewModel.deleteConfessionState.observe(viewLifecycleOwner) { state ->
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

    private fun observeAddBookmarks() {
        viewModel.addBookmarkState.observe(viewLifecycleOwner) { state ->
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
                    Toast.makeText(requireContext(), "Successfully added to bookmarks", Toast.LENGTH_SHORT).show()
                }
            }
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

}