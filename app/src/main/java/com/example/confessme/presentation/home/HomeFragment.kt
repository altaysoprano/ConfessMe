package com.example.confessme.presentation.home

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.R
import com.example.confessme.databinding.FragmentHomeBinding
import com.example.confessme.databinding.HomeNoConfessFoundViewBinding
import com.example.confessme.presentation.utils.BottomNavBarControl
import com.example.confessme.presentation.confess.ConfessAnswerFragment
import com.example.confessme.presentation.profile.ConfessionListAdapter
import com.example.confessme.presentation.auth.LoginFragment
import com.example.confessme.presentation.utils.FragmentNavigation
import com.example.confessme.presentation.home.notifications.NotificationsFragment
import com.example.confessme.presentation.profile.other_user_profile.OtherUserProfileFragment
import com.example.confessme.presentation.search.SearchFragment
import com.example.confessme.utils.MyPreferences
import com.example.confessme.utils.MyUtils
import com.example.confessme.presentation.utils.UiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var noConfessFoundBinding: HomeNoConfessFoundViewBinding
    private lateinit var confessListAdapter: ConfessionListAdapter
    private lateinit var currentUserUid: String
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var navRegister: FragmentNavigation
    private var bottomNavBarControl: BottomNavBarControl? = null
    private var hasUnreadNotifications: Boolean = false
    private var limit: Long = 20
    private lateinit var myPreferences: MyPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        noConfessFoundBinding = binding.homeNoConfessFoundView
        (activity as AppCompatActivity?)!!.title = getString(R.string.home)
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.homeToolbar)
        myPreferences = MyPreferences(requireContext())
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUserUid = currentUser?.uid ?: ""
        navRegister = activity as FragmentNavigation
        setHasOptionsMenu(true)

        setConfessListAdapter()
        setupRecyclerView()

        viewModel.fetchConfessions(limit)
        viewModel.fetchNotifications(limit)
        updateUserLanguage()
        setFollowSomeoneTextOnClickListener()

        binding.swipeRefreshLayoutHome.setOnRefreshListener {
            viewModel.onSwiping(limit)
            confessListAdapter.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeAddBookmarks()
        observeFetchConfessions()
        observeDeleteConfession()
        observeAddFavorite()
        observePaging()
        observeSwiping()
        observeFetchNotifications()
        observeSignOut()
        observeRemoveBookmark()
    }

    private fun setupRecyclerView() {
        binding.homeRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = confessListAdapter
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

    private fun setConfessListAdapter() {
        confessListAdapter = ConfessionListAdapter(
            requireContext(),
            mutableListOf(),
            currentUserUid,
            false,
            onAnswerClick = { confessionId ->
                onAnswerClick(confessionId)
            },
            onFavoriteClick = { isFavorited, confessionId ->
                viewModel.addFavorite(isFavorited, confessionId)
            },
            onConfessDeleteClick = { confessionId ->
                viewModel.deleteConfession(confessionId)
            },
            onConfessBookmarkClick = { confessionId, timestamp, userUid ->
                viewModel.addBookmark(confessionId, null, userUid)
            },
            onBookmarkRemoveClick = { confessionId -> },
            onItemPhotoClick = { photoUserUid, photoUserEmail, photoUserToken, photoUserName ->
                onItemPhotoClick(photoUserEmail, photoUserUid, photoUserName, photoUserToken)
            },
            onUserNameClick = { userNameUserUid, userNameUserEmail, userNameUserToken, userNameUserName ->
                onUserNameClick(userNameUserEmail, userNameUserUid, userNameUserName, userNameUserToken)
            }
        )
    }

    private fun observeFetchConfessions() {
        viewModel.fetchConfessionsState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarHomeGeneral.visibility = View.VISIBLE
                    noConfessFoundBinding.root.visibility = View.GONE
                }

                is UiState.Failure -> {
                    binding.progressBarHomeGeneral.visibility = View.GONE
                    noConfessFoundBinding.root.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarHomeGeneral.visibility = View.GONE
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

    private fun observeDeleteConfession() {
        viewModel.deleteConfessionState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarHomeGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarHomeGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarHomeGeneral.visibility = View.GONE
                    val deletedConfession = state.data
                    val position = deletedConfession?.let { findPositionById(it.id) }

                    if (position != -1) {
                        if (deletedConfession != null) {
                            if (position != null) {
                                confessListAdapter.removeConfession(position)
                                limit -= 1
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeAddBookmarks() {
        viewModel.addBookmarkState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarHomeGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarHomeGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarHomeGeneral.visibility = View.GONE
                    val confession = state.data

                    MyUtils.showSnackbar(
                        rootView = requireActivity().window.decorView.rootView,
                        descriptionText = getString(R.string.successfully_added_to_bookmarks),
                        buttonText = getString(R.string.undo),
                        activity = requireActivity(),
                        context = requireContext(),
                        onButtonClicked = {
                            confession?.id?.let { viewModel.deleteBookmark(it) }
                        }
                    )
                }
            }
        }
    }

    private fun observeRemoveBookmark() {
        viewModel.removeBookmarkState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarHomeGeneral.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarHomeGeneral.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarHomeGeneral.visibility = View.GONE
                    val removedBookmark = state.data

                    MyUtils.showSnackbar(
                        rootView = requireActivity().window.decorView.rootView,
                        descriptionText = getString(R.string.removed_from_bookmarks),
                        buttonText = getString(R.string.undo),
                        activity = requireActivity(),
                        context = requireContext(),
                        onButtonClicked = {
                            if(removedBookmark != null) {
                                viewModel.addBookmark(
                                    confessionId = removedBookmark.confessionId,
                                    timestamp = removedBookmark.timestamp,
                                    userUid = removedBookmark.userId
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observeAddFavorite() {
        viewModel.addFavoriteState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarHome.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarHome.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarHome.visibility = View.GONE
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

    private fun observePaging() {
        viewModel.onPagingState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarHome.visibility = View.VISIBLE
                    noConfessFoundBinding.root.visibility = View.GONE
                }

                is UiState.Failure -> {
                    binding.progressBarHome.visibility = View.GONE
                    noConfessFoundBinding.root.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarHome.visibility = View.GONE
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

    private fun observeSwiping() {
        viewModel.onSwipeState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    noConfessFoundBinding.root.visibility = View.GONE
                }

                is UiState.Failure -> {
                    noConfessFoundBinding.root.visibility = View.GONE
                    binding.swipeRefreshLayoutHome.isRefreshing = false
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.swipeRefreshLayoutHome.isRefreshing = false
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

    private fun observeFetchNotifications() {
        viewModel.fetchNotificationsState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {}

                is UiState.Failure -> {}

                is UiState.Success -> {
                    val notifications = state.data

                    val unseenNotifications = notifications.filter { !it.seen }
                    hasUnreadNotifications = if (unseenNotifications.isNotEmpty()) true else false
                    requireActivity().invalidateOptionsMenu()
                }
            }
        }
    }

    private fun observeSignOut() {
        viewModel.signOutState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarHomeGeneral.visibility = View.VISIBLE
                    setHomeScreenDisabled(true)
                }

                is UiState.Failure -> {
                    binding.progressBarHomeGeneral.visibility = View.GONE
                    setHomeScreenDisabled(false)
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarHomeGeneral.visibility = View.GONE
                    setHomeScreenDisabled(false)
                    navRegister.navigateFrag(LoginFragment(), false)
                }
            }
        }
    }

    private fun onAnswerClick(confessionId: String) {
        if (!confessionId.isNullOrEmpty()) {
            val bundle = Bundle()
            bundle.putString("confessionId", confessionId)
            bundle.putString("currentUserUid", currentUserUid)

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
            Toast.makeText(requireContext(), getString(R.string.confession_not_found), Toast.LENGTH_SHORT)
                .show()
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

    private fun onItemPhotoClick(photoUserEmail: String, photoUserUid: String,
                                 photoUserName: String, photoUserToken: String) {
        val bundle = Bundle()
        bundle.putString("userEmail", photoUserEmail)
        bundle.putString("userUid", photoUserUid)
        bundle.putString("userName", photoUserName)
        bundle.putString("userToken", photoUserToken)

        val profileFragment = OtherUserProfileFragment()
        profileFragment.arguments = bundle

        navRegister.navigateFrag(profileFragment, true)
    }

    private fun onUserNameClick(userNameUserEmail: String, userNameUserUid: String,
                                userNameUserName: String, userNameUserToken: String) {
        val bundle = Bundle()
        bundle.putString("userEmail", userNameUserEmail)
        bundle.putString("userUid", userNameUserUid)
        bundle.putString("userName", userNameUserName)
        bundle.putString("userToken", userNameUserToken)


        val profileFragment = OtherUserProfileFragment()
        profileFragment.arguments = bundle

        navRegister.navigateFrag(profileFragment, true)
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

    private fun findPositionById(confessionId: String): Int {
        for (index in 0 until confessListAdapter.confessList.size) {
            if (confessListAdapter.confessList[index].id == confessionId) {
                return index
            }
        }
        return -1
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility =
            View.VISIBLE

        val openCloseLightsItem = menu.findItem(R.id.open_close_lights)
        val icon = ContextCompat.getDrawable(requireContext(), getOpenCloseItemIcon())
        openCloseLightsItem.title = getOpenCloseItemText()
        openCloseLightsItem.icon = icon

        if (hasUnreadNotifications) {
            val iconDrawable = requireContext().getDrawable(R.drawable.ic_notifications_blur)
            iconDrawable?.setTint(resources.getColor(R.color.confessmered))
            val menuItem = menu.findItem(R.id.ic_notifications_home)
            menuItem.icon = iconDrawable
        } else {
            val iconDrawable = requireContext().getDrawable(R.drawable.ic_notifications_blur)
            iconDrawable?.setTint(Color.parseColor("#6c6c6c"))
            val menuItem = menu.findItem(R.id.ic_notifications_home)
            menuItem.icon = iconDrawable
        }
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sign_out -> {
                signOut()
            }

            R.id.ic_notifications_home -> {
                val notificationsFragment = NotificationsFragment()

                navRegister.navigateFrag(notificationsFragment, true)
            }
            R.id.open_close_lights -> {
                val nightModeEnabled = myPreferences.isNightModeEnabled(requireContext())
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
        return myPreferences.isNightModeEnabled(requireContext())
    }

    private fun getOpenCloseItemText(): String {
        return if (isDarkModeEnabled()) {
            getString(R.string.open_lights)
        } else {
            getString(R.string.close_lights)
        }
    }

    private fun updateUserLanguage() {
        viewModel.updateLanguage(MyUtils.getAppLanguage(myPreferences))
    }

    private fun getOpenCloseItemIcon(): Int {
        val isDarkMode = isDarkModeEnabled()

        return if (isDarkMode) {
            R.drawable.ic_light
        } else {
            R.drawable.ic_dark_mode
        }
    }

    fun onBottomNavItemReselected() {
        binding.homeRecyclerviewId.smoothScrollToPosition(0)
    }

    fun setFollowSomeoneTextOnClickListener() {
        binding.homeNoConfessFoundView.shareYourProfileText.setOnClickListener {
            navRegister.navigateFrag(SearchFragment(), false)
        }
    }
}