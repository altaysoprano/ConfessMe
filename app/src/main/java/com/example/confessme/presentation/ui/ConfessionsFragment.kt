package com.example.confessme.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.confessme.R
import com.example.confessme.databinding.FragmentConfessionsBinding
import com.example.confessme.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfessionsFragment : Fragment() {

    private lateinit var binding: FragmentConfessionsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentConfessionsBinding.inflate(inflater, container, false)

        return binding.root
    }

}