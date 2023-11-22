package com.example.confessme.presentation.ui

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.confessme.R
import com.example.confessme.databinding.FragmentNotificationsBinding
import com.example.confessme.databinding.FragmentOtherUserProfileBinding
import com.example.confessme.presentation.BottomNavBarControl
import com.example.confessme.presentation.NotificationsViewModel
import com.example.confessme.presentation.OtherUserViewPagerAdapter
import com.example.confessme.presentation.ProfileViewModel
import com.example.confessme.util.UiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    private lateinit var binding: FragmentNotificationsBinding
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var notificationsListAdapter: NotificationsAdapter
    private var bottomNavBarControl: BottomNavBarControl? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        (activity as AppCompatActivity?)!!.title = "Notifications"
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.notificationsToolbar)
        notificationsListAdapter = NotificationsAdapter()
        setupRecyclerView()
        setHasOptionsMenu(true)

        viewModel.fetchNotifications()

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeSignOut()
        observeFetchNotifications()
    }

    private fun setupRecyclerView() {
        binding.notificationsRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationsListAdapter
        }
    }

    private fun observeFetchNotifications() {
        viewModel.fetchNotificationsState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarNotifications.visibility = View.VISIBLE
                    // noConfessFoundBinding.root.visibility = View.GONE
                }

                is UiState.Failure -> {
                    binding.progressBarNotifications.visibility = View.GONE
                    // binding.swipeRefreshLayoutMyConfessions.isRefreshing = false
                    // noConfessFoundBinding.root.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarNotifications.visibility = View.GONE
                    // binding.swipeRefreshLayoutMyConfessions.isRefreshing = false
                    /*
                    if (state.data.isEmpty()) {
                        noConfessFoundBinding.root.visibility = View.VISIBLE
                    } else {
                        noConfessFoundBinding.root.visibility = View.GONE
                        confessListAdapter.updateList(state.data)
                    }
*/
                    notificationsListAdapter.updateList(state.data)
                }
            }
        }
    }

    private fun observeSignOut() {
        viewModel.signOutState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarNotifications.visibility = View.VISIBLE
                    setHomeScreenDisabled(true)
                }

                is UiState.Failure -> {
                    binding.progressBarNotifications.visibility = View.GONE
                    setHomeScreenDisabled(false)
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarNotifications.visibility = View.GONE
                    setHomeScreenDisabled(false)
                    val fragmentManager = parentFragmentManager
                    for (i in 0 until fragmentManager.backStackEntryCount) {
                        fragmentManager.popBackStack()
                    }
                    fragmentManager.beginTransaction()
                        .replace(R.id.coordinator, LoginFragment())
                        .commit()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.notifications_menu, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sign_out -> {
                signOut()
            }

            R.id.ic_notifications -> {
                requireActivity().onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setHomeScreenDisabled(disabled: Boolean) {
        if (disabled) {
            disableBottomNavigationBarInActivity()
            binding.root.alpha = 0.5f
            enableDisableViewGroup(requireView() as ViewGroup, false)
        } else {
            enableBottomNavigationBarInActivity()
            binding.root.alpha = 1f
            enableDisableViewGroup(requireView() as ViewGroup, true)
        }
    }

    fun enableDisableViewGroup(viewGroup: ViewGroup, enabled: Boolean) {
        val childCount = viewGroup.childCount
        for (i in 0 until childCount) {
            val view = viewGroup.getChildAt(i)
            view.isEnabled = enabled
            if (view is ViewGroup) {
                enableDisableViewGroup(view, enabled)
            }
        }
    }

    private fun setOnBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.signOutState.value is UiState.Loading) {
                    return
                }
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOnBackPressed()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BottomNavBarControl) {
            bottomNavBarControl = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        bottomNavBarControl = null
    }

    private fun disableBottomNavigationBarInActivity() {
        bottomNavBarControl?.disableBottomNavigationBar()
    }

    private fun enableBottomNavigationBarInActivity() {
        bottomNavBarControl?.enableBottomNavigationBar()
    }

    fun signOut() {
        viewModel.signOut()
    }

}