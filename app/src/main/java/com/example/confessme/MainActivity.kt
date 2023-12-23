package com.example.confessme

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract.Profile
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.confessme.data.model.Confession
import com.example.confessme.databinding.ActivityMainBinding
import com.example.confessme.presentation.BottomNavBarControl
import com.example.confessme.presentation.ui.ConfessFragment
import com.example.confessme.presentation.ui.ConfessionUpdateListener
import com.example.confessme.presentation.ui.ConfessionsToMeFragment
import com.example.confessme.presentation.ui.FragmentNavigation
import com.example.confessme.presentation.ui.HomeFragment
import com.example.confessme.presentation.ui.LoginFragment
import com.example.confessme.presentation.ui.ProfileFragment
import com.example.confessme.presentation.ui.SearchFragment
import com.example.confessme.util.MyPreferences
import com.example.confessme.util.MyUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), FragmentNavigation, ConfessionUpdateListener,
    BottomNavBarControl {

    private lateinit var binding: ActivityMainBinding
    private lateinit var myPreferences: MyPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        myPreferences = MyPreferences(this)

        setContentView(binding.root)

        MyUtils.applyAppTheme(myPreferences, this)
        MyUtils.setAppLanguage(myPreferences, this)

        supportFragmentManager.beginTransaction()
            .add(R.id.coordinator, LoginFragment())
            .commit()
        binding.bottomNavigationView.visibility = View.GONE

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    if (getCurrentFragment() is HomeFragment) {
                        ((getCurrentFragment()) as HomeFragment).onBottomNavItemReselected()
                    } else {
                        navigateFrag(HomeFragment(), false)
                    }
                }
                R.id.profile -> {
                    if (getCurrentFragment() is ProfileFragment) {
                        ((getCurrentFragment()) as ProfileFragment).onBottomNavItemReselected()
                    } else {
                        navigateFrag(ProfileFragment(), false)
                    }
                }
                R.id.search -> {
                    if (getCurrentFragment() is SearchFragment) {
                        ((getCurrentFragment()) as SearchFragment).onBottomNavItemReselected()
                    } else {
                        navigateFrag(SearchFragment(), false)
                    }
                }
                else -> {

                }
            }
            true
        }
    }

    fun restartActivity() {
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.coordinator)
    }

    override fun navigateFrag(fragment: Fragment, addToStack: Boolean) {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        transaction.setCustomAnimations(
            R.anim.fade_in,
            R.anim.fade_out,
            R.anim.fade_in,
            R.anim.fade_out
        )

        transaction.replace(R.id.coordinator, fragment)

        when (fragment) {
            is HomeFragment, is ProfileFragment, is SearchFragment -> {
                binding.bottomNavigationView.visibility = View.VISIBLE

                if (!addToStack) {
                    fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                }
            }
            else -> binding.bottomNavigationView.visibility = View.GONE
        }

        if (addToStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }

    override fun updateConfessionItem(position: Int, updatedConfession: Confession) {
        val fragmentManager = supportFragmentManager
        val confessionsToMeFragment = fragmentManager.findFragmentByTag("confessionsToMeFragment") as? ConfessionsToMeFragment
        confessionsToMeFragment?.updateConfessionItem(position, updatedConfession)
    }

    override fun findPositionById(confessionId: String): Int {
        val fragmentManager = supportFragmentManager
        val confessionsToMeFragment = fragmentManager.findFragmentByTag("confessionsToMeFragment") as? ConfessionsToMeFragment
        return confessionsToMeFragment?.findPositionById(confessionId) ?: -1
    }

    override fun disableBottomNavigationBar() {
        binding.bottomNavigationView.alpha = 0.5f
        for (i in 0 until binding.bottomNavigationView.menu.size()) {
            binding.bottomNavigationView.menu.getItem(i).isEnabled = false
        }
    }

    override fun enableBottomNavigationBar() {
        binding.bottomNavigationView.alpha = 1.0f
        for (i in 0 until binding.bottomNavigationView.menu.size()) {
            binding.bottomNavigationView.menu.getItem(i).isEnabled = true
        }
    }
}