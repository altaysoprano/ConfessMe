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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.R
import com.example.confessme.databinding.FragmentConfessionsBinding
import com.example.confessme.databinding.FragmentOthersConfessionsBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.databinding.NoConfessionsHereBeTheFirstOneBinding
import com.example.confessme.databinding.NoConfessionsHereBinding
import com.example.confessme.databinding.YouHaveNoConfessionsBinding
import com.example.confessme.presentation.ConfessViewModel
import com.example.confessme.presentation.ScrollableToTop
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Math.abs

@AndroidEntryPoint
class OthersConfessionsFragment(
    private val userUid: String,
    private val confessionCategory: ConfessionCategory
) : Fragment(), ScrollableToTop {

    private lateinit var binding: FragmentOthersConfessionsBinding
    private lateinit var profileBinding: FragmentProfileBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var confessListAdapter: ConfessionListAdapter
    private lateinit var currentUserUid: String
    private lateinit var noConfessFoundBinding: NoConfessionsHereBeTheFirstOneBinding
    private var limit: Long = 20

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
        setupRecyclerView()

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
    }

    private fun setupRecyclerView() {
        binding.othersConfessionsListRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = confessListAdapter
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
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
                        viewModel.fetchConfessions(userUid, limit, confessionCategory)
                    }
                }
            })
        }
    }

    private fun setConfessionListAdapter() {
        confessListAdapter = ConfessionListAdapter(
            requireContext(),
            mutableListOf(),
            currentUserUid,
            false,
            onAnswerClick = { confessionId ->
                onAnswerClick(
                    confessionId,
                )
            },
            onFavoriteClick = { isFavorited, confessionId -> },
            onConfessDeleteClick = { confessionId ->
                viewModel.deleteConfession(confessionId)
            },
            onConfessBookmarkClick = { confessionId, timestamp, userUid ->
                viewModel.addBookmark(confessionId, timestamp, userUid)
            },
            onBookmarkRemoveClick = { confessionId -> },
            onItemPhotoClick = { photoUserUid, photoUserEmail, photoUserToken, photoUserName ->
                onItemPhotoClick(photoUserEmail, photoUserUid, photoUserName, photoUserToken)
            },
            onUserNameClick =  { userNameUserUid, userNameUserEmail, userNameUserToken, userNameUserName ->
                onUserNameClick(userNameUserEmail, userNameUserUid, userNameUserName, userNameUserToken)
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
                    Toast.makeText(
                        requireContext(),
                        "Successfully added to bookmarks",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun onAnswerClick(
        confessionId: String
    ) {
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

    private fun onItemPhotoClick(photoUserEmail: String, photoUserUid: String,
                                 photoUserName: String, photoUserToken: String) {
        val bundle = Bundle()
        bundle.putString("userEmail", photoUserEmail)
        bundle.putString("userUid", photoUserUid)
        bundle.putString("userName", photoUserName)
        bundle.putString("userToken", photoUserToken)

        val profileFragment = OtherUserProfileFragment()
        profileFragment.arguments = bundle

        navRegister.navigateFrag(profileFragment, true)
    }

    private fun onUserNameClick(userNameUserEmail: String, userNameUserUid: String,
                                userNameUserName: String, userNameUserToken: String) {
        val bundle = Bundle()
        bundle.putString("userEmail", userNameUserEmail)
        bundle.putString("userUid", userNameUserUid)
        bundle.putString("userName", userNameUserName)
        bundle.putString("userToken", userNameUserToken)

        val profileFragment = OtherUserProfileFragment()
        profileFragment.arguments = bundle

        navRegister.navigateFrag(profileFragment, true)
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
        binding.othersConfessionsListRecyclerviewId.smoothScrollToPosition(0)
    }
}