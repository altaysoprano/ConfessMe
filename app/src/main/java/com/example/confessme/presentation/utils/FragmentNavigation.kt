package com.example.confessme.presentation.utils

import androidx.fragment.app.Fragment

interface FragmentNavigation {
    fun navigateFrag(fragment: Fragment, addToStack: Boolean)
}