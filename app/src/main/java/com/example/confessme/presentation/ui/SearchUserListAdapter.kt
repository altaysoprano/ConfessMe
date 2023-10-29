package com.example.confessme.presentation.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.data.model.User
import com.example.confessme.databinding.UserItemBinding

class SearchUserListAdapter(
    val userList: MutableList<User> = mutableListOf(),
    private val currentUserUid: String,
    private val onItemClick: (User) -> Unit,
    private val onFollowClick: (String) -> Unit
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

                binding.followsProgressButtonLayout.followButtonCardview.setOnClickListener {
                    val userToFollow = userList[adapterPosition]

                    onFollowClick(userToFollow.uid)
                }

                itemView.setOnClickListener {
                    onItemClick(user)
                }

                if(currentUserUid == user.uid) {
                    binding.followsProgressButtonLayout.followButtonCardview.visibility = View.GONE
                } else {
                    binding.followsProgressButtonLayout.followButtonCardview.visibility = View.VISIBLE
                }

                if (user.isFollowing) {
                    binding.followsProgressButtonLayout.followButtonTv.text = "FOLLOWING"
                    binding.followsProgressButtonLayout.followButtonLayout.setBackgroundColor(
                        Color.WHITE
                    )
                    binding.followsProgressButtonLayout.followButtonTv.setTextColor(Color.BLACK)
                    binding.followsProgressButtonLayout.progressBarFollowButton.indeterminateTintList =
                        ColorStateList.valueOf(
                            Color.BLACK
                        )
                } else {
                    binding.followsProgressButtonLayout.followButtonTv.text = "FOLLOW"
                    binding.followsProgressButtonLayout.followButtonLayout.setBackgroundColor(
                        Color.parseColor("#cf363c")
                    )
                    binding.followsProgressButtonLayout.followButtonTv.setTextColor(
                        Color.parseColor(
                            "#ffffff"
                        )
                    )
                    binding.followsProgressButtonLayout.progressBarFollowButton.indeterminateTintList =
                        ColorStateList.valueOf(
                            Color.WHITE
                        )
                }
            }
        }
    }
}
