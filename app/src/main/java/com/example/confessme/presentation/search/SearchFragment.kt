package com.example.confessme.presentation.search

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.R
import com.example.confessme.data.model.FollowUser
import com.example.confessme.data.model.User
import com.example.confessme.databinding.FragmentSearchBinding
import com.example.confessme.presentation.utils.BottomNavBarControl
import com.example.confessme.presentation.utils.ConfessMeDialog
import com.example.confessme.presentation.profile.other_user_profile.OtherUserProfileFragment
import com.example.confessme.presentation.utils.FragmentNavigation
import com.example.confessme.presentation.utils.UserListAdapter
import com.example.confessme.utils.MyUtils
import com.example.confessme.presentation.utils.UiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var navRegister: FragmentNavigation
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val currentUserUid = currentUser?.uid ?: ""
    private var limit: Long = 10
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var dialogHelper: ConfessMeDialog
    private lateinit var userListAdapter: UserListAdapter
    private lateinit var historyListAdapter: UserListAdapter
    private var bottomNavBarControl: BottomNavBarControl? = null
    private var callback: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        binding.searchToolbar.title = getString(R.string.search)
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.searchToolbar)

        binding.searchToolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        setAdapters()
        setupRecyclerViews()
        viewModel.getSearchHistoryUsers(limit)
        setSearchBar()
        setOnBackPressed()
        setDeleteAllClickListener()
        observeSearchView()
        setInitialBottomNavBarSelectedItem()

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeSearchResults()
        observeHistoryResults()
        observeDeleteAllHistory()
        observeDeleteHistory()
    }

    private fun setupRecyclerViews() {
        binding.searchResultsRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userListAdapter
        }
        binding.historyResultsRecyclerviewId.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyListAdapter
        }
    }

    private fun setAdapters() {
        userListAdapter = UserListAdapter(
            mutableListOf(),
            currentUserUid = currentUserUid,
            onItemClick = { user ->
                onItemClick(user)
            },
            onFollowClick = { userUid, userName, userToken, isFollowing ->
                followOrUnfollowUser(userUid, userName, userToken, ListType.UserList, isFollowing)
            },
            onItemLongPress = {},
            context = requireContext()
        )

        historyListAdapter = UserListAdapter(
            mutableListOf(),
            currentUserUid = currentUserUid,
            onItemClick = { user ->
                onItemClick(user)
            },
            onFollowClick = { userUid, userName, userToken, isFollowing ->
                followOrUnfollowUser(
                    userUid,
                    userName,
                    userToken,
                    ListType.HistoryList,
                    isFollowing
                )
            },
            onItemLongPress = {
                dialogHelper = ConfessMeDialog(requireContext())
                dialogHelper.showDialog(
                    getString(R.string.delete_search),
                    getString(R.string.are_you_sure_you_want_to_delete_the_selected_search),
                    getString(R.string.yes),
                    getString(R.string.no),
                    { viewModel.deleteHistoryItem(it.uid) }
                )
            },
            context = requireContext()
        )
    }


    private fun setSearchBar() {
        val searchView = binding.searchView

        searchView.setOnQueryTextFocusChangeListener { view, hasFocus ->
            viewModel.setSearchViewFocused(hasFocus)
            if (viewModel.searchViewFocused.value == true) {
                (activity as AppCompatActivity?)!!.supportActionBar?.apply {
                    MyUtils.showKeyboard(requireActivity(), view.findFocus())
                    setDisplayHomeAsUpEnabled(true)
                    setDisplayShowHomeEnabled(true)
                    setHomeAsUpIndicator(R.drawable.ic_back)
                }
            } else {
                (activity as AppCompatActivity?)!!.supportActionBar?.apply {
                    setDisplayHomeAsUpEnabled(false)
                    setDisplayShowHomeEnabled(false)
                }
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchViewText(newText ?: "")
                if (!viewModel.searchViewText.value.isNullOrBlank()) {
                    binding.historyResultsRecyclerviewId.visibility = View.GONE
                    binding.historyTitle.visibility = View.GONE
                    binding.deleteAllHistoryTextView.visibility = View.GONE
                    viewModel.searchUsers(viewModel.searchViewText.value ?: "")
                } else {
                    binding.searchNoUserFoundView.root.visibility = View.GONE
                    binding.resultsTitle.visibility = View.GONE
                    binding.searchResultsRecyclerviewId.visibility = View.GONE
                    viewModel.getSearchHistoryUsers(limit)
                }
                return true
            }
        })
    }

    private fun updateRecyclerViewMargins(isVisible: Boolean) {
        val actionBarHeight = getActionBarHeight()

        updateRecyclerViewMargin(binding.searchResultsRecyclerviewId, actionBarHeight, isVisible)
        updateRecyclerViewMargin(binding.historyResultsRecyclerviewId, actionBarHeight, isVisible)
    }

    private fun updateRecyclerViewMargin(
        recyclerView: RecyclerView,
        actionBarHeight: Int,
        isVisible: Boolean
    ) {
        val layoutParams = recyclerView.layoutParams as ConstraintLayout.LayoutParams

        if (isVisible) {
            layoutParams.bottomMargin = actionBarHeight
        } else {
            layoutParams.bottomMargin = 0
        }

        recyclerView.layoutParams = layoutParams
    }

    private fun getActionBarHeight(): Int {
        val tv = TypedValue()
        return if (requireActivity().theme.resolveAttribute(
                android.R.attr.actionBarSize,
                tv,
                true
            )
        ) {
            TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        } else {
            0
        }
    }

    fun onBottomNavItemReselected() {
        val searchView = binding.searchView

        searchView.requestFocus()
        val inputMethodManager =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT)
    }

    private fun observeSearchResults() {
        viewModel.searchResults.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarSearch.visibility = View.VISIBLE
                    binding.historyResultsRecyclerviewId.visibility = View.GONE
                    binding.historyTitle.visibility = View.GONE
                    binding.deleteAllHistoryTextView.visibility = View.GONE
                }

                is UiState.Failure -> {
                    binding.progressBarSearch.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarSearch.visibility = View.GONE
                    binding.historyResultsRecyclerviewId.visibility = View.GONE
                    binding.historyTitle.visibility = View.GONE
                    binding.deleteAllHistoryTextView.visibility = View.GONE
                    binding.resultsTitle.visibility = View.VISIBLE
                    binding.searchResultsRecyclerviewId.visibility = View.VISIBLE

                    if (state.data.isEmpty()) {
                        binding.searchNoUserFoundView.root.visibility = View.VISIBLE
                    } else {
                        binding.searchNoUserFoundView.root.visibility = View.GONE
                    }
                    userListAdapter.updateList(state.data)
                }
            }
        }
    }

    private fun observeHistoryResults() {
        viewModel.historyResults.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.resultsTitle.visibility = View.GONE
                    binding.searchResultsRecyclerviewId.visibility = View.GONE
                    binding.progressBarSearch.visibility = View.VISIBLE
                    binding.searchNoUserFoundView.root.visibility = View.GONE
                }

                is UiState.Failure -> {
                    binding.progressBarSearch.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarSearch.visibility = View.GONE
                    binding.resultsTitle.visibility = View.GONE
                    binding.searchResultsRecyclerviewId.visibility = View.GONE
                    binding.searchNoUserFoundView.root.visibility = View.GONE

                    if (state.data.isEmpty()) {
                        binding.historyResultsRecyclerviewId.visibility = View.GONE
                        binding.historyTitle.visibility = View.GONE
                        binding.deleteAllHistoryTextView.visibility = View.GONE
                    } else {
                        binding.historyResultsRecyclerviewId.visibility = View.VISIBLE
                        binding.historyTitle.visibility = View.VISIBLE
                        binding.deleteAllHistoryTextView.visibility = View.VISIBLE
                    }
                    historyListAdapter.updateList(state.data)
                }
            }
        }
    }

    private fun observeDeleteAllHistory() {
        viewModel.deleteAllHistory.observe(this) { state ->
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
                    binding.historyResultsRecyclerviewId.visibility = View.GONE
                    binding.historyTitle.visibility = View.GONE
                    binding.deleteAllHistoryTextView.visibility = View.GONE

                    historyListAdapter.updateList(emptyList())
                }
            }
        }
    }

    private fun observeDeleteHistory() {
        viewModel.deleteHistoryItem.observe(this) { state ->
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

                    val deletedHistoryId = state.data

                    val position =
                        deletedHistoryId.let { findPositionById(it, historyListAdapter.userList) }
                    if (position != -1) {
                        historyListAdapter.removeHistory(position)
                        if (historyListAdapter.userList.isEmpty()) {
                            binding.historyTitle.visibility = View.GONE
                            binding.deleteAllHistoryTextView.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun observeSearchView() {
        viewModel.searchViewFocused.observe(viewLifecycleOwner) { isFocused ->
            val isTextNotEmpty = !viewModel.searchViewText.value.isNullOrBlank()
            if (isFocused || isTextNotEmpty) {
                hideBottomNavigationView()
            } else {
                showBottomNavigationView()
            }
        }

        viewModel.searchViewText.observe(viewLifecycleOwner) { newText ->
            val isFocused = viewModel.searchViewFocused.value == true
            if (isFocused || !newText.isNullOrBlank()) {
                hideBottomNavigationView()
            } else {
                showBottomNavigationView()
            }
        }
    }

    private fun onItemClick(user: User) {
        val bundle = Bundle()
        bundle.putString("userEmail", user.email)
        bundle.putString("userUid", user.uid)
        bundle.putString("userName", user.userName)
        bundle.putString("userToken", user.token)

        val profileFragment = OtherUserProfileFragment()
        profileFragment.arguments = bundle

        viewModel.addToSearchHistory(user.uid)

        navRegister.navigateFrag(profileFragment, true)
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.searchViewText.value?.isNotEmpty() == true) {
            (activity as AppCompatActivity?)!!.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
                setHomeAsUpIndicator(R.drawable.ic_back)
            }
        }

        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun setDeleteAllClickListener() {
        binding.deleteAllHistoryTextView.setOnClickListener {
            dialogHelper = ConfessMeDialog(requireContext())
            dialogHelper.showDialog(
                getString(R.string.delete_all_history),
                getString(R.string.are_you_sure_you_want_to_delete_the_entire_search_history),
                getString(R.string.yes),
                getString(R.string.no),
                { viewModel.deleteAllHistory() }
            )
        }
    }

    private fun followOrUnfollowUser(
        userUidToFollowOrUnfollow: String,
        userName: String,
        userToken: String,
        listType: ListType,
        isFollowing: Boolean
    ) {
        if (userUidToFollowOrUnfollow.isEmpty()) {
            return
        }

        var adapter: UserListAdapter? = null

        when (listType) {
            ListType.UserList -> {
                adapter = userListAdapter
            }

            ListType.HistoryList -> {
                adapter = historyListAdapter
            }
        }

        val position = findPositionById(userUidToFollowOrUnfollow, adapter.userList)

        viewModel.followOrUnfollowUser(userUidToFollowOrUnfollow, userName, userToken, isFollowing)

        val userFollowStateObserver = object : Observer<UiState<FollowUser>> {
            override fun onChanged(state: UiState<FollowUser>) {
                when (state) {
                    is UiState.Loading -> {
                        if (position != -1) {
                            adapter.userList[position].isFollowingInProgress = true
                            adapter.notifyItemChanged(position)
                        }
                    }

                    is UiState.Success -> {
                        if (position != -1) {
                            adapter.userList[position].isFollowingInProgress = false
                            adapter.userList[position].isFollowing = state.data.isFollowed
                            adapter.notifyItemChanged(position)
                        }
                        viewModel.followUserState.removeObserver(this)
                    }

                    is UiState.Failure -> {
                        if (position != -1) {
                            adapter.userList[position].isFollowingInProgress = false
                            adapter.notifyItemChanged(position)
                        }
                        Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

        viewModel.followUserState.observe(this, userFollowStateObserver)
    }

    private fun setOnBackPressed() {
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.searchViewFocused.value == true || viewModel.searchViewText.value?.isNotEmpty() == true) {
                    disableSearchView()
                    binding.searchView.setQuery("", false)
                    (activity as AppCompatActivity?)!!.supportActionBar?.apply {
                        setDisplayHomeAsUpEnabled(false)
                        setDisplayShowHomeEnabled(false)
                    }
                } else {
                    isEnabled = false
                    hideKeyboard()
                    requireActivity().onBackPressed()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun disableSearchView() {
        val searchView = binding.searchView

        if (searchView.hasFocus()) {
            searchView.clearFocus()
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus ?: View(requireContext())
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun hideBottomNavigationView() {
        val bottomNavigationView =
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.visibility = View.GONE
        updateRecyclerViewMargins(false)
    }

    private fun showBottomNavigationView() {
        val bottomNavigationView =
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.visibility = View.VISIBLE
        updateRecyclerViewMargins(true)
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

    private fun setInitialBottomNavBarSelectedItem() {
        bottomNavBarControl?.setSelectedItemId(R.id.search)
    }

    private fun findPositionById(userId: String, userList: MutableList<User>): Int {
        for (index in 0 until userList.size) {
            if (userList[index].uid == userId) {
                return index
            }
        }
        return -1
    }
}
