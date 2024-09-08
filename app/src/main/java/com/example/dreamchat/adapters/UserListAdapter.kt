package com.example.dreamchat.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.dreamchat.R
import com.example.dreamchat.databinding.ItemUserPreviewBinding
import com.example.dreamchat.model.UserData

class UserListAdapter : RecyclerView.Adapter<UserListAdapter.UserListViewHolder>() {
    inner class UserListViewHolder(val binding: ItemUserPreviewBinding) : ViewHolder(binding.root)

    private val differCallback = object : DiffUtil.ItemCallback<UserData>() {
        override fun areItemsTheSame(oldItem: UserData, newItem: UserData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserData, newItem: UserData): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserListViewHolder {
        val itemView =
            ItemUserPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UserListViewHolder, position: Int) {
        val oneUser = differ.currentList[position]
        holder.binding.apply {
            if(oneUser.image != "") {
                Log.e("Loading Image", oneUser.image)
                Glide.with(this.root).load(oneUser.image).error(R.drawable.profile_image).into(userImage)
            }
            userName.text = oneUser.name
            ivCheckbox.visibility = if (oneUser.isSelected) View.VISIBLE else View.INVISIBLE
            root.setOnClickListener { onItemClickListener?.let { it(oneUser) } }
        }
    }

    private var onItemClickListener: ((UserData) -> Unit)? = null

    fun setOnItemClickListener(transfer: (UserData) -> Unit) {
        onItemClickListener = transfer
    }

    override fun getItemCount(): Int = differ.currentList.size

}