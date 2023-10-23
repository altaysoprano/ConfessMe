package com.example.confessme.presentation.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.data.model.Confession
import com.example.confessme.databinding.FragmentConfessedBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.databinding.NoConfessionsHereBinding
import com.example.confessme.presentation.ConfessViewModel
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfessedFragment(
    private val userUid: String,
    private val confessionCategory: ConfessionCategory
) : Fragment(), ConfessionUpdateListener {

    private lateinit var binding: FragmentConfessedBinding
    private lateinit var profileBinding: FragmentProfileBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var noConfessFoundBinding: NoConfessionsHereBinding
    private var limit: Long = 20

    private lateinit var confessListAdapter: ConfessionListAdapter

    private val viewModel: ConfessViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentConfessedBinding.inflate(inflater, container, false)
        profileBinding = FragmentProfileBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        noConfessFoundBinding = binding.confessedNoConfessFoundView
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserUid = currentUser?.uid ?: ""
        confessListAdapter = ConfessionListAdapter(
            requireContext(),
            mutableListOf(),
            currentUserUid,
            onAnswerClick = { confessionId, userId, fromUserUid, fromUserImageUrl, answeredUserName, confessedUserName, isAnswered, answerText, isFavorited, answerDate ->
                if (!confessionId.isNullOrEmpty()) {
                    val bundle = Bundle()
                    bundle.putString("confessionId", confessionId)
                    bundle.putBoolean("isAnswered", isAnswered)
                    bundle.putString("answerText", answerText)
                    bundle.putString("currentUserUid", currentUserUid)
                    bundle.putString("answerUserUid", userId)
                    bundle.putString("answerFromUserUid", fromUserUid)
                    bundle.putString("fromUserImageUrl", fromUserImageUrl)
                    bundle.putString("answeredUserName", answeredUserName)
                    bundle.putString("confessedUserName", confessedUserName)
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
            onFavoriteClick = { isFavorited, confessionId ->
                viewModel.addFavorite(isFavorited, confessionId)
            },
            onConfessDeleteClick = {},
            onItemPhotoClick = { userUid, userEmail, userName ->
                val bundle = Bundle()
                bundle.putString("userEmail", userEmail)
                bundle.putString("userUid", userUid)

                val profileFragment = OtherUserProfileFragment()
                profileFragment.arguments = bundle

                navRegister.navigateFrag(profileFragment, true)
            },
            onUserNameClick = { userUid, userEmail, userName ->
                val bundle = Bundle()
                bundle.putString("userEmail", userEmail)
                bundle.putString("userUid", userUid)

                val profileFragment = OtherUserProfileFragment()
                profileFragment.arguments = bundle

                navRegister.navigateFrag(profileFragment, true)
            }
        )

        viewModel.fetchConfessions(userUid, limit, confessionCategory)

        binding.confessedListRecyclerviewId.addOnScrollListener(object :
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

        binding.swipeRefreshLayoutConfessed.setOnRefreshListener {
            viewModel.fetchConfessions(userUid, limit, confessionCategory)
            confessListAdapter.notifyDataSetChanged()
        }

        setupRecyclerView()
        observeFetchConfessions()
        observeAddFavorite()

        return binding.root
    }

    private fun setupRecyclerView() {
        binding.confessedListRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = confessListAdapter
        }
    }


    private fun observeFetchConfessions() {
        viewModel.fetchConfessionsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessed.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessed.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessed.visibility = View.GONE
                    binding.swipeRefreshLayoutConfessed.isRefreshing = false
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

    @SuppressLint("NotifyDataSetChanged")
    private fun observeAddFavorite() {
        viewModel.addFavoriteState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessed.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessed.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessed.visibility = View.GONE
                    val updatedConfession = state.data

                    val position = updatedConfession?.let { findPositionById(it.id) }
                    if (position != -1) {
                        if (updatedConfession != null) {
                            if (position != null) {
                                confessListAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun findPositionById(confessionId: String): Int {
        for (index in 0 until confessListAdapter.confessList.size) {
            if (confessListAdapter.confessList[index].id == confessionId) {
                return index
            }
        }
        return -1
    }

    override fun updateConfessionItem(position: Int, updatedConfession: Confession) {
        confessListAdapter.updateItem(position, updatedConfession)
    }
}