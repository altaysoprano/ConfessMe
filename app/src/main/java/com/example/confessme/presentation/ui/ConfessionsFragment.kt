package com.example.confessme.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.confessme.R
import com.example.confessme.databinding.FragmentConfessionsBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.presentation.ConfessViewModel
import com.example.confessme.presentation.SearchViewModel
import com.example.confessme.util.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfessionsFragment(private val isMyConfessions: Boolean) : Fragment() {

    private lateinit var binding: FragmentConfessionsBinding
    private lateinit var profileBinding: FragmentProfileBinding

    private val confessListAdapter = ConfessionListAdapter(mutableListOf(), isMyConfessions)
    private val viewModel: ConfessViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentConfessionsBinding.inflate(inflater, container, false)
        profileBinding = FragmentProfileBinding.inflate(inflater, container, false)

        viewModel.fetchConfessions(isMyConfessions)

        setupRecyclerView()
        observeFetchConfessions()

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
                    profileBinding.progressBarProfile.visibility = View.VISIBLE
                }
                is UiState.Failure -> {
                    profileBinding.progressBarProfile.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
                is UiState.Success -> {
                    profileBinding.progressBarProfile.visibility = View.GONE
                    confessListAdapter.updateList(state.data)
                }
            }
        }
    }



}