package com.example.confessme.presentation.profile.my_profile

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.confessme.R
import com.example.confessme.presentation.confess.ConfessAnswerFragment
import com.example.confessme.presentation.profile.ConfessionCategory
import com.example.confessme.presentation.profile.ConfessionListAdapter
import com.example.confessme.presentation.profile.other_user_profile.OtherUserProfileFragment
import com.example.confessme.presentation.utils.FragmentNavigation

open class MyProfileViewPagerFragment: Fragment() {

    protected var limit: Long = 20

    protected fun findPositionById(confessionId: String, adapter: ConfessionListAdapter): Int {
        for (index in 0 until adapter.confessList.size) {
            if (adapter.confessList[index].id == confessionId) {
                return index
            }
        }
        return -1
    }

    private fun navigateToUserProfile(userEmail: String, userUid: String, userName: String,
                                        userToken: String, navRegister: FragmentNavigation) {
        val bundle = Bundle()
        bundle.putString("userEmail", userEmail)
        bundle.putString("userUid", userUid)
        bundle.putString("userName", userName)
        bundle.putString("userToken", userToken)

        val profileFragment = OtherUserProfileFragment()
        profileFragment.arguments = bundle

        navRegister.navigateFrag(profileFragment, true)
    }

    protected fun onItemPhotoClick(photoUserEmail: String, photoUserUid: String, photoUserName: String,
                                 photoUserToken: String, navRegister: FragmentNavigation) {
        navigateToUserProfile(photoUserEmail, photoUserUid, photoUserName, photoUserToken, navRegister)
    }

    protected fun onUserNameClick(userNameUserEmail: String, userNameUserUid: String, userNameUserName: String,
                                userNameUserToken: String, navRegister: FragmentNavigation) {
        navigateToUserProfile(userNameUserEmail, userNameUserUid, userNameUserName, userNameUserToken, navRegister)
    }

    protected fun onAnswerClick(confessionId: String, currentUserUid: String, adapter: ConfessionListAdapter) {
        if (!confessionId.isNullOrEmpty()) {
            val bundle = Bundle()
            bundle.putString("confessionId", confessionId)
            bundle.putString("currentUserUid", currentUserUid)

            val confessAnswerFragment = ConfessAnswerFragment(
                { position, updatedConfession ->
                    adapter.updateItem(position, updatedConfession)
                },
                { confessionId ->
                    findPositionById(confessionId, adapter)
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

    protected fun setupRecyclerView(recyclerView: RecyclerView, confessListAdapter: ConfessionListAdapter,
                                  onPaging: () -> Unit) {
        recyclerView.apply {
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
                        onPaging()
                    }
                }
            })
        }
    }
}