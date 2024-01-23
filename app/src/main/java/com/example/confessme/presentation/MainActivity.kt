package com.example.confessme.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.confessme.R
import com.example.confessme.databinding.ActivityMainBinding
import com.example.confessme.presentation.utils.BottomNavBarControl
import com.example.confessme.presentation.utils.FragmentNavigation
import com.example.confessme.presentation.home.HomeFragment
import com.example.confessme.presentation.auth.LoginFragment
import com.example.confessme.presentation.profile.my_profile.ProfileFragment
import com.example.confessme.presentation.search.SearchFragment
import com.example.confessme.utils.MyPreferences
import com.example.confessme.utils.MyUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), FragmentNavigation,
    BottomNavBarControl {

    private lateinit var binding: ActivityMainBinding
    private lateinit var myPreferences: MyPreferences
    private lateinit var myPreferencesForActivity: MyPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        myPreferences = MyPreferences(this)
        myPreferencesForActivity = MyPreferences(applicationContext)

        MyUtils.applyAppTheme(myPreferences, this)
        MyUtils.applyAppTheme(myPreferencesForActivity, applicationContext)
        MyUtils.setAppLanguage(myPreferences, this)
        MyUtils.setAppLanguage(myPreferencesForActivity, applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        supportFragmentManager.beginTransaction()
            .add(R.id.coordinator, LoginFragment())
            .commit()
        binding.bottomNavigationView.visibility = View.GONE

        setBottomNavBarSelectListener()
    }

    private fun setBottomNavBarSelectListener() {
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

    override fun setSelectedItemId(itemId: Int) {
        if (binding.bottomNavigationView.selectedItemId != itemId) {
            binding.bottomNavigationView.selectedItemId = itemId
        }
    }
}