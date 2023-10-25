package com.example.confessme.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.R
import com.example.confessme.data.model.Confession
import com.example.confessme.databinding.FragmentBookmarksBinding
import com.example.confessme.databinding.FragmentConfessionsBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.databinding.NoConfessionsHereBinding
import com.example.confessme.databinding.YouHaveNoConfessionsBinding
import com.example.confessme.presentation.BookmarksViewModel
import com.example.confessme.presentation.ConfessViewModel
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookmarksFragment() : Fragment() {

    private lateinit var binding: FragmentBookmarksBinding
    private lateinit var profileBinding: FragmentProfileBinding
    private lateinit var confessListAdapter: ConfessionListAdapter
    private lateinit var navRegister: FragmentNavigation
    private lateinit var noConfessFoundBinding: NoConfessionsHereBinding
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
        val currentUserUid = currentUser?.uid ?: ""
        confessListAdapter = ConfessionListAdapter(
            requireContext(),
            mutableListOf(),
            currentUserUid,
            true,
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
            },
            onConfessBookmarkClick = { confessionId, userUid ->
            },
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
        noConfessFoundBinding = binding.noConfessionsHereText

        viewModel.fetchBookmarks(limit)

        binding.bookmarkListRecyclerviewId.addOnScrollListener(object :
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

        binding.swipeRefreshLayoutMyConfessions.setOnRefreshListener {
            viewModel.fetchBookmarks(limit)
            confessListAdapter.notifyDataSetChanged()
        }

        setupRecyclerView()
        observeFetchBookmarks()

        return binding.root
    }

    private fun setupRecyclerView() {
        binding.bookmarkListRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = confessListAdapter
        }
    }

    private fun observeFetchBookmarks() {
        viewModel.fetchBookmarksState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarBookmarks.visibility = View.VISIBLE
                    noConfessFoundBinding.root.visibility = View.GONE
                }

                is UiState.Failure -> {
                    binding.progressBarBookmarks.visibility = View.GONE
                    noConfessFoundBinding.root.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarBookmarks.visibility = View.GONE
                    binding.swipeRefreshLayoutMyConfessions.isRefreshing = false
                    if (state.data.isEmpty()) {
                        noConfessFoundBinding.root.visibility = View.VISIBLE
                    } else {
                        noConfessFoundBinding.root.visibility = View.GONE
                        confessListAdapter.updateList(state.data as List<Confession>)
                    }
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