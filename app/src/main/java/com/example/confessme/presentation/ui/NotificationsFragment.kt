package com.example.confessme.presentation.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import com.example.confessme.R
import com.example.confessme.databinding.FragmentNotificationsBinding
import com.example.confessme.databinding.FragmentOtherUserProfileBinding
import com.example.confessme.presentation.NotificationsViewModel
import com.example.confessme.presentation.OtherUserViewPagerAdapter
import com.example.confessme.presentation.ProfileViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    private lateinit var binding: FragmentNotificationsBinding
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: NotificationsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        (activity as AppCompatActivity?)!!.title = "Notifications"
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.notificationsToolbar)
        setHasOptionsMenu(true)

        return binding.root
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

    fun signOut() {
        navRegister = activity as FragmentNavigation
        viewModel.signOut()
        val fragmentManager = parentFragmentManager
        for (i in 0 until fragmentManager.backStackEntryCount) {
            fragmentManager.popBackStack()
        }
        fragmentManager.beginTransaction()
            .replace(R.id.coordinator, LoginFragment())
            .commit()
    }

}