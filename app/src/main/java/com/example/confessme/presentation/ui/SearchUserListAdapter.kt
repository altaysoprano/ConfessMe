package com.example.confessme.presentation.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.data.model.User
import com.example.confessme.databinding.UserItemBinding

class SearchUserListAdapter(
    private val userList: MutableList<User> = mutableListOf(),
    private val onItemClick: (User) -> Unit
) :
    RecyclerView.Adapter<SearchUserListAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = userList.size

    fun updateList(newList: List<User>) {
        userList.clear()
        userList.addAll(newList)
        notifyDataSetChanged()
    }

    inner class UserViewHolder(private val binding: UserItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                searchScreenUsername.text = user.userName

                if (user.imageUrl.isNotEmpty()) {
                    Glide.with(itemView)
                        .load(user.imageUrl)
                        .into(searchScreenProfileImage)
                } else {
                    binding.searchScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
                }

                itemView.setOnClickListener {
                    onItemClick(user)
                }
            }
        }
    }
}
