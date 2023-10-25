package com.example.confessme.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.R
import com.example.confessme.data.model.User
import com.example.confessme.databinding.FragmentFollowsBinding
import com.example.confessme.databinding.FragmentSearchBinding
import com.example.confessme.presentation.FollowsViewModel
import com.example.confessme.presentation.OtherUserViewPagerAdapter
import com.example.confessme.presentation.SearchViewModel
import com.example.confessme.util.FollowType
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FollowsFragment : Fragment() {

    private lateinit var binding: FragmentFollowsBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var userUid: String
    private lateinit var currentUserUid: String
    private var followTypeOrdinal: Int = -1
    private var limit: Long = 20
    private val viewModel: FollowsViewModel by viewModels()

    private val userListAdapter = SearchUserListAdapter(mutableListOf()) { user ->
        onItemClick(user)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentFollowsBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.followsToolbar)
        setHasOptionsMenu(true)
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUserUid = currentUser?.uid ?: ""
        userUid = arguments?.getString("userUid") ?: "Empty Uid"
        followTypeOrdinal = arguments?.getInt("followType") ?: -1
        setTitle()

        (activity as AppCompatActivity?)!!.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
        }

        getFollowings(followTypeOrdinal)
        setupRecyclerView()
        observeSearchResults()

        return binding.root
    }

    private fun setupRecyclerView() {
        binding.followsRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userListAdapter
        }

        binding.followsRecyclerviewId.addOnScrollListener(object :
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
                    getFollowings(followTypeOrdinal)
                }
            }
        })
    }

    private fun observeSearchResults() {
        viewModel.followingUsers.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarFollows.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarFollows.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarFollows.visibility = View.GONE
                    if (state.data.isEmpty()) {
                        binding.followsNoUserFoundView.root.visibility = View.VISIBLE
                    } else {
                        binding.followsNoUserFoundView.root.visibility = View.GONE
                    }
                    userListAdapter.updateList(state.data)
                }
            }
        }
    }

    private fun onItemClick(user: User) {
/*
        if (currentUserUid != answerFromUserUid) {
            val bundle = Bundle()
            bundle.putString("userUid", answerFromUserUid)

            val profileFragment = OtherUserProfileFragment()
            profileFragment.arguments = bundle

            dismiss()
            navRegister.navigateFrag(profileFragment, true)
        }
*/

        if(currentUserUid != user.uid) {
            val bundle = Bundle()
            bundle.putString("userEmail", user.email)
            bundle.putString("userUid", user.uid)

            val profileFragment = OtherUserProfileFragment()
            profileFragment.arguments = bundle

            navRegister.navigateFrag(profileFragment, true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getFollowings(followTypeOrdinal: Int) {
        if (userUid != "Empty Uid" && followTypeOrdinal != -1) {
            val followType = FollowType.values()[followTypeOrdinal]
            viewModel.getFollowUsers(userUid, limit, followType)
        } else {
            Toast.makeText(
                requireContext(),
                "An error occured. Please try again.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setTitle() {
        val followType = FollowType.values()[followTypeOrdinal]
        if (followType == FollowType.MyFollowers || followType == FollowType.OtherUserFollowers) {
            (activity as AppCompatActivity?)!!.title = "Followers"
        } else if (followType == FollowType.MyFollowings || followType == FollowType.OtherUserFollowings) {
            (activity as AppCompatActivity?)!!.title = "Following"
        } else {
            (activity as AppCompatActivity?)!!.title = "Error"
        }
    }
}