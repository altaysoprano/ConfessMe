package com.example.confessme.presentation.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.confessme.data.model.Confession
import com.example.confessme.data.model.User
import com.example.confessme.databinding.ConfessItemBinding
import com.example.confessme.databinding.UserItemBinding

class ConfessionListAdapter(
    private val confessList: MutableList<Confession> = mutableListOf()
) : RecyclerView.Adapter<ConfessionListAdapter.ConfessionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfessionViewHolder {
        val binding = ConfessItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConfessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConfessionListAdapter.ConfessionViewHolder, position: Int) {
        val confess = confessList[position]
        holder.bind(confess)
    }

    override fun getItemCount(): Int = confessList.size

    fun updateList(newList: List<Confession>) {
        confessList.clear()
        confessList.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ConfessionViewHolder(private val binding: ConfessItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(confess: Confession) {
            binding.apply {
                confessionsScreenUsername.text = "@" + confess.username
                confessionsScreenConfession.text = confess.text

                if (confess.imageUrl.isNotEmpty()) {
                    Glide.with(itemView)
                        .load(confess.imageUrl)
                        .into(confessionsScreenProfileImage)
                }

                itemView.setOnClickListener {

                }
            }
        }
    }
}
