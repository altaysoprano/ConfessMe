package com.example.confessme

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.confessme.data.model.Confession
import com.example.confessme.databinding.ActivityMainBinding
import com.example.confessme.presentation.ui.ConfessFragment
import com.example.confessme.presentation.ui.ConfessionUpdateListener
import com.example.confessme.presentation.ui.ConfessionsToMeFragment
import com.example.confessme.presentation.ui.FragmentNavigation
import com.example.confessme.presentation.ui.HomeFragment
import com.example.confessme.presentation.ui.LoginFragment
import com.example.confessme.presentation.ui.ProfileFragment
import com.example.confessme.presentation.ui.SearchFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), FragmentNavigation, ConfessionUpdateListener {

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
}