package com.example.confessme.presentation.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.R
import com.example.confessme.databinding.FragmentConfessionsToMeBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.databinding.NoConfessFoundBinding
import com.example.confessme.presentation.ConfessViewModel
import com.example.confessme.presentation.ProfileSearchSharedViewModel
import com.example.confessme.util.UiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfessionsToMeFragment(private val isMyConfessions: Boolean) : Fragment() {

    private lateinit var binding: FragmentConfessionsToMeBinding
    private lateinit var profileBinding: FragmentProfileBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var noConfessFoundBinding: NoConfessFoundBinding
    private var limit: Long = 20

    private lateinit var confessListAdapter: ConfessionListAdapter

    private val viewModel: ConfessViewModel by viewModels()
    private val sharedViewModel: ProfileSearchSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentConfessionsToMeBinding.inflate(inflater, container, false)
        profileBinding = FragmentProfileBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        noConfessFoundBinding = binding.confessionsToMeNoConfessFoundView
        confessListAdapter = ConfessionListAdapter(
            requireContext(),
            mutableListOf(),
            isMyConfessions,
            onAnswerClick = { confessionId, isAnswered, answerText, isFavorited ->
                if (!confessionId.isNullOrEmpty()) {
                    val bundle = Bundle()
                    bundle.putString("confessionId", confessionId)
                    bundle.putBoolean("isAnswered", isAnswered)
                    bundle.putString("answerText", answerText)
                    bundle.putBoolean("favorited", isFavorited)
                    val confessAnswerFragment = ConfessAnswerFragment()
                    confessAnswerFragment.arguments = bundle
                    confessAnswerFragment.show(requireActivity().supportFragmentManager, "ConfessAnswerFragment")
/*
                    navRegister.navigateFrag(confessAnswerFragment, true)
*/
                } else {
                    Toast.makeText(requireContext(), "Confession not found", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            onFavoriteClick = { confessionId ->
                viewModel.addFavorite(confessionId)
            },
            onConfessDeleteClick = {},
            onItemPhotoClick = { userEmail, userName ->
                sharedViewModel.setSelectedUserEmail(userEmail)
                sharedViewModel.setSelectedUserName(userName)

                val profileFragment = ProfileFragment()
                navRegister.navigateFrag(profileFragment, true)
            }
        )

        viewModel.fetchConfessions(limit, isMyConfessions)

        binding.confessionToMeListRecyclerviewId.addOnScrollListener(object :
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
                    viewModel.fetchConfessions(limit, isMyConfessions)
                }
            }
        })

        setupRecyclerView()
        observeFetchConfessions()
        observeAddFavorite()

        return binding.root
    }

    private fun setupRecyclerView() {
        binding.confessionToMeListRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = confessListAdapter
        }
    }

    private fun observeFetchConfessions() {
        viewModel.fetchConfessionsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessionsToMe.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessionsToMe.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessionsToMe.visibility = View.GONE
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

    private fun observeAddFavorite() {
        viewModel.addFavoriteState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessionsToMe.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessionsToMe.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessionsToMe.visibility = View.GONE
                    val updatedConfession = state.data

                    val position = updatedConfession?.let { findPositionById(it.id) }
                    if (position != -1) {
                        if (updatedConfession != null) {
                            if (position != null) {
                                confessListAdapter.updateItem(position, updatedConfession)
                            }
                        }
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