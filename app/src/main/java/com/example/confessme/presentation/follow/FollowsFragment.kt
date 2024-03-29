package com.example.confessme.presentation.follow

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.R
import com.example.confessme.data.model.FollowUser
import com.example.confessme.data.model.User
import com.example.confessme.databinding.FragmentFollowsBinding
import com.example.confessme.presentation.utils.FragmentNavigation
import com.example.confessme.presentation.profile.other_user_profile.OtherUserProfileFragment
import com.example.confessme.presentation.utils.UserListAdapter
import com.example.confessme.presentation.utils.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FollowsFragment : Fragment() {

    private lateinit var binding: FragmentFollowsBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var userUid: String
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val currentUserUid = currentUser?.uid ?: ""
    private var followTypeOrdinal: Int = -1
    private var limit: Long = 20
    private val viewModel: FollowsViewModel by viewModels()

    private lateinit var userListAdapter: UserListAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentFollowsBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.followsToolbar)
        setHasOptionsMenu(true)
        userUid = arguments?.getString("userUid") ?: "Empty Uid"
        followTypeOrdinal = arguments?.getInt("followType") ?: -1
        setTitle()

        (activity as AppCompatActivity?)!!.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
        }

        getFollowings(followTypeOrdinal)
        setAdapter()
        setupRecyclerView()

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeFollowsResults()
    }

    private fun setupRecyclerView() {
        binding.followsRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userListAdapter
            setHasFixedSize(true)
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

    private fun setAdapter() {
        userListAdapter = UserListAdapter(mutableListOf(),
            currentUserUid = currentUserUid,
            onItemClick = { user ->
                onItemClick(user)
            },
            onFollowClick = { userUid, userName, userToken, isFollowing ->
                followOrUnfollowUser(userUid, userName, userToken, isFollowing)
            },
            onItemLongPress = {},
            context = requireContext()
        )
    }

    private fun observeFollowsResults() {
        viewModel.followingUsers.observe(this) { state ->
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
        if (currentUserUid != user.uid) {
            val bundle = Bundle()
            bundle.putString("userEmail", user.email)
            bundle.putString("userUid", user.uid)
            bundle.putString("userName", user.userName)
            bundle.putString("userToken", user.token)

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
                getString(R.string.an_error_occured_please_try_again),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun followOrUnfollowUser(userUidToFollowOrUnfollow: String, userName: String, userToken: String, isFollowing: Boolean) {
        if (userUidToFollowOrUnfollow.isEmpty()) {
            return
        }

        val position = findPositionById(userUidToFollowOrUnfollow)

        viewModel.followOrUnfollowUser(userUidToFollowOrUnfollow, userName, userToken, isFollowing)

        val userFollowStateObserver = object : Observer<UiState<FollowUser>> {
            override fun onChanged(state: UiState<FollowUser>) {
                when (state) {
                    is UiState.Loading -> {
                        if (position != -1) {
                            userListAdapter.userList[position].isFollowingInProgress = true
                            userListAdapter.notifyItemChanged(position)
                        }
                    }
                    is UiState.Success -> {
                        if (position != -1) {
                            userListAdapter.userList[position].isFollowingInProgress = false
                            userListAdapter.userList[position].isFollowing = state.data.isFollowed
                            userListAdapter.notifyItemChanged(position)
                        }
                        viewModel.followUserState.removeObserver(this)
                    }
                    is UiState.Failure -> {
                        if (position != -1) {
                            userListAdapter.userList[position].isFollowingInProgress = false
                            userListAdapter.notifyItemChanged(position)
                        }
                        Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewModel.followUserState.observe(this, userFollowStateObserver)
    }

    private fun setTitle() {
        val followType = FollowType.values()[followTypeOrdinal]
        if (followType == FollowType.MyFollowers || followType == FollowType.OtherUserFollowers) {
            (activity as AppCompatActivity?)!!.title = getString(R.string.followers)
        } else if (followType == FollowType.MyFollowings || followType == FollowType.OtherUserFollowings) {
            (activity as AppCompatActivity?)!!.title = getString(R.string.following)
        } else {
            (activity as AppCompatActivity?)!!.title = getString(R.string.error)
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