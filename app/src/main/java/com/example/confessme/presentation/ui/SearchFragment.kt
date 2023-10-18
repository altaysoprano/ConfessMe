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
import com.example.confessme.presentation.SharedViewModel
import com.example.confessme.presentation.SearchViewModel
import com.example.confessme.util.UiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: SearchViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val userListAdapter = SearchUserListAdapter(mutableListOf()) { user ->
        onItemClick(user)
    }

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
                        binding.searchNoConfessFoundView.nothingFoundText.text = "No User Found"
                        binding.searchNoConfessFoundView.root.visibility = View.VISIBLE
                    } else {
                        binding.searchNoConfessFoundView.root.visibility = View.GONE
                    }
                    userListAdapter.updateList(state.data)
                }
            }
        }
    }

    private fun onItemClick(user: User) {
        sharedViewModel.setSelectedUserUid(user.uid)
        sharedViewModel.setSelectedUserName(user.userName)
        sharedViewModel.setSelectedUserEmail(user.email)

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
}
