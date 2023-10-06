package com.example.confessme

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.confessme.databinding.ActivityMainBinding
import com.example.confessme.presentation.ui.FragmentNavigation
import com.example.confessme.presentation.ui.HomeFragment
import com.example.confessme.presentation.ui.LoginFragment
import com.example.confessme.presentation.ui.ProfileFragment
import com.example.confessme.presentation.ui.SearchFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), FragmentNavigation {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction()
            .add(R.id.coordinator, LoginFragment())
            .commit()
        binding.bottomNavigationView.visibility = View.GONE

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    navigateFrag(HomeFragment(), false)
                }

                R.id.profile -> {
                    navigateFrag(ProfileFragment(), false)
                }

                R.id.search -> {
                    navigateFrag(SearchFragment(), false)
                }

                else -> {
                }
            }
            true
        }
    }

    override fun navigateFrag(fragment: Fragment, addToStack: Boolean) {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
            .replace(R.id.coordinator, fragment)

        when (fragment) {
            is HomeFragment, is ProfileFragment, is SearchFragment -> {
                binding.bottomNavigationView.visibility = View.VISIBLE

                // Tüm fragmentları geri alırken ilişkilendirilen işlemi de kaldır
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

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.profileFragment)
        if (currentFragment is ProfileFragment) {
            currentFragment.onBackPressedInProfileFragment()
        } else {
            super.onBackPressed()
        }
    }
}