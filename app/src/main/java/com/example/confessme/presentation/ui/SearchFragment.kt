package com.example.confessme.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.confessme.R
import com.example.confessme.data.model.User
import com.example.confessme.databinding.FragmentSearchBinding
import com.example.confessme.presentation.SearchViewModel
import com.example.confessme.util.UiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var navRegister: FragmentNavigation
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val currentUserUid = currentUser?.uid ?: ""
    private val viewModel: SearchViewModel by viewModels()

    private val userListAdapter = SearchUserListAdapter(mutableListOf(),
        currentUserUid = currentUserUid,
        onItemClick = { user ->
            onItemClick(user)
        },
        onFollowClick = { userUid ->
            followOrUnfollowUser(userUid)
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        (activity as AppCompatActivity?)!!.title = "Search"
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.searchToolbar)

        setupRecyclerView()
        observeSearchResults()

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchUsers(newText.orEmpty())
                return true
            }
        })
        return binding.root
    }

    private fun setupRecyclerView() {
        binding.searchResultsRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userListAdapter
        }
    }

    private fun observeSearchResults() {
        viewModel.searchResults.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarSearch.visibility = View.VISIBLE
                }
                is UiState.Failure -> {
                    binding.progressBarSearch.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
                is UiState.Success -> {
                    binding.progressBarSearch.visibility = View.GONE
                    if(state.data.isEmpty()) {
                        binding.searchNoUserFoundView.root.visibility = View.VISIBLE
                    } else {
                        binding.searchNoUserFoundView.root.visibility = View.GONE
                    }
                    userListAdapter.updateList(state.data)
                }
            }
        }
    }

    private fun onItemClick(user: User) {
        val bundle = Bundle()
        bundle.putString("userEmail", user.email)
        bundle.putString("userUid", user.uid)

        val profileFragment = OtherUserProfileFragment()
        profileFragment.arguments = bundle

        navRegister.navigateFrag(profileFragment, true)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility =
            View.VISIBLE
        (activity as AppCompatActivity?)!!.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
        }
    }

    private fun followOrUnfollowUser(userUidToFollow: String) {
        if (!userUidToFollow.isNullOrEmpty()) {
            viewModel.followOrUnfollowUser(userUidToFollow)
            viewModel.followUserState.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressBarSearch.visibility =
                            View.VISIBLE
                    }

                    is UiState.Failure -> {
                        binding.progressBarSearch.visibility =
                            View.GONE
                        Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                            .show()
                    }

                    is UiState.Success -> {
                        binding.progressBarSearch.visibility =
                            View.GONE

                        val followUser = state.data
                        val isFollowing = state.data.isFollowed
                        val position = followUser.let { findPositionById(it.userUid) }

                        if (position != -1) {
                            if (followUser != null) {
                                if (position != null) {
                                    userListAdapter.userList[position].isFollowing = isFollowing
                                    userListAdapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun findPositionById(userId: String): Int {
        for (index in 0 until userListAdapter.userList.size) {
            if (userListAdapter.userList[index].uid == userId) {
                return index
            }
        }
        return -1
    }
}
