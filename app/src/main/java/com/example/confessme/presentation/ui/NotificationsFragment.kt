package com.example.confessme.presentation.ui

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.R
import com.example.confessme.databinding.FragmentNotificationsBinding
import com.example.confessme.databinding.FragmentOtherUserProfileBinding
import com.example.confessme.databinding.NoConfessionsHereBinding
import com.example.confessme.presentation.BottomNavBarControl
import com.example.confessme.presentation.NotificationsViewModel
import com.example.confessme.presentation.OtherUserViewPagerAdapter
import com.example.confessme.presentation.ProfileViewModel
import com.example.confessme.util.MyPreferences
import com.example.confessme.util.UiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    private lateinit var binding: FragmentNotificationsBinding
    private lateinit var noNotificationsBinding: NoConfessionsHereBinding
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var notificationsListAdapter: NotificationsAdapter
    private lateinit var currentUserUid: String
    private var bottomNavBarControl: BottomNavBarControl? = null
    private var limit: Long = 20
    private lateinit var myPreferences: MyPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        noNotificationsBinding = binding.noConfessionsHereText
        noNotificationsBinding.noConfessionsHereText.text = "No notifications found here"
        myPreferences = MyPreferences(requireContext())
        navRegister = activity as FragmentNavigation
        (activity as AppCompatActivity?)!!.title = getString(R.string.notifications)
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.notificationsToolbar)
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUserUid = currentUser?.uid ?: ""
        notificationsListAdapter = NotificationsAdapter(currentUserUid = currentUserUid,
            onItemPhotoClick = { photoUserUid, photoUserToken, userNameUserName ->
                onItemPhotoClick(photoUserUid, userNameUserName, photoUserToken)
            },
            onItemClick = {confessionId ->
                onItemClick(confessionId = confessionId)
            }
        )
        setupRecyclerView()
        applyAppTheme()
        setHasOptionsMenu(true)

        viewModel.fetchNotifications(limit)

        binding.swipeRefreshLayoutNotifications.setOnRefreshListener {
            viewModel.onSwiping(limit)
            notificationsListAdapter.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeSignOut()
        observeFetchNotifications()
        observePaging()
        observeSwiping()
    }

    private fun setupRecyclerView() {
        binding.notificationsRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationsListAdapter
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            this.addOnScrollListener(object :
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
                        viewModel.onPaging(limit)
                    }
                }
            })
        }
    }

    private fun observeFetchNotifications() {
        viewModel.fetchNotificationsState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarNotificationsGeneral.visibility = View.VISIBLE
                    noNotificationsBinding.root.visibility = View.GONE
                }

                is UiState.Failure -> {
                    binding.progressBarNotificationsGeneral.visibility = View.GONE
                    binding.swipeRefreshLayoutNotifications.isRefreshing = false
                    noNotificationsBinding.root.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarNotificationsGeneral.visibility = View.GONE
                    binding.swipeRefreshLayoutNotifications.isRefreshing = false
                    if (state.data.isEmpty()) {
                        noNotificationsBinding.root.visibility = View.VISIBLE
                    } else {
                        noNotificationsBinding.root.visibility = View.GONE
                        notificationsListAdapter.updateList(state.data)
                    }
                }
            }
        }
    }

    private fun observePaging() {
        viewModel.onPagingState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarNotifications.visibility = View.VISIBLE
                    noNotificationsBinding.root.visibility = View.GONE
                }

                is UiState.Failure -> {
                    binding.progressBarNotifications.visibility = View.GONE
                    noNotificationsBinding.root.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarNotifications.visibility = View.GONE
                    if (state.data.isEmpty()) {
                        noNotificationsBinding.root.visibility = View.VISIBLE
                    } else {
                        noNotificationsBinding.root.visibility = View.GONE
                        notificationsListAdapter.updateList(state.data)
                    }
                }
            }
        }
    }

    private fun observeSwiping() {
        viewModel.onSwipeState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    noNotificationsBinding.root.visibility = View.GONE
                }

                is UiState.Failure -> {
                    noNotificationsBinding.root.visibility = View.GONE
                    binding.swipeRefreshLayoutNotifications.isRefreshing = false
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.swipeRefreshLayoutNotifications.isRefreshing = false
                    if (state.data.isEmpty()) {
                        noNotificationsBinding.root.visibility = View.VISIBLE
                    } else {
                        noNotificationsBinding.root.visibility = View.GONE
                        notificationsListAdapter.updateList(state.data)
                    }
                }
            }
        }
    }

    private fun observeSignOut() {
        viewModel.signOutState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarNotificationsGeneral.visibility = View.VISIBLE
                    setHomeScreenDisabled(true)
                }

                is UiState.Failure -> {
                    binding.progressBarNotificationsGeneral.visibility = View.GONE
                    setHomeScreenDisabled(false)
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarNotificationsGeneral.visibility = View.GONE
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

    private fun onItemPhotoClick(photoUserUid: String, photoUserName: String, photoUserToken: String) {
        val bundle = Bundle()
        bundle.putString("userUid", photoUserUid)
        bundle.putString("userName", photoUserName)
        bundle.putString("userToken", photoUserToken)

        val profileFragment = OtherUserProfileFragment()
        profileFragment.arguments = bundle

        navRegister.navigateFrag(profileFragment, true)
    }

    private fun onItemClick(confessionId: String) {
        val bundle = Bundle()
        bundle.putString("confessionId", confessionId)

        val confessionDetailFragment = ConfessionDetailFragment()
        confessionDetailFragment.arguments = bundle

        navRegister.navigateFrag(confessionDetailFragment, true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.notifications_menu, menu)

        val openCloseLightsItem = menu.findItem(R.id.open_close_lights)
        val icon = ContextCompat.getDrawable(requireContext(), getOpenCloseItemIcon())
        openCloseLightsItem.title = getOpenCloseItemText()
        openCloseLightsItem.icon = icon

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sign_out -> {
                signOut()
            }

            R.id.ic_notifications -> {
                requireActivity().onBackPressed()
            }
            R.id.open_close_lights -> {
                val nightModeEnabled = myPreferences.isNightModeEnabled()
                val icon = ContextCompat.getDrawable(requireContext(), getOpenCloseItemIcon())
                saveNightMode(!nightModeEnabled)
                AppCompatDelegate.setDefaultNightMode(if (!nightModeEnabled) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                })
                item.title = getOpenCloseItemText()
                item.icon = icon
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveNightMode(isNightModeEnabled: Boolean) {
        myPreferences.setNightMode(isNightModeEnabled)
    }

    private fun isDarkModeEnabled(): Boolean {
        return myPreferences.isNightModeEnabled()
    }

    private fun getOpenCloseItemText(): String {
        return if (isDarkModeEnabled()) {
            getString(R.string.open_lights)
        } else {
            getString(R.string.close_lights)
        }
    }

    private fun getOpenCloseItemIcon(): Int {
        val isDarkMode = isDarkModeEnabled()

        return if (isDarkMode) {
            R.drawable.ic_light
        } else {
            R.drawable.ic_dark_mode
        }
    }

    private fun applyAppTheme() {
        val isDarkModeEnabled = myPreferences.isNightModeEnabled()

        if (isDarkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
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