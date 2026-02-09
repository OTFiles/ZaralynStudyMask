package com.zaralyn.study.mask.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zaralyn.study.mask.databinding.ItemExploreCardBinding
import com.zaralyn.study.mask.model.ExploreItem

class ExploreAdapter(
    private val onExploreClicked: (ExploreItem) -> Unit
) : ListAdapter<ExploreItem, ExploreAdapter.ExploreViewHolder>(ExploreDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val binding = ItemExploreCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExploreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        val exploreItem = getItem(position)
        holder.bind(exploreItem)
    }

    inner class ExploreViewHolder(
        private val binding: ItemExploreCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exploreItem: ExploreItem) {
            binding.exploreTitle.text = exploreItem.title
            binding.exploreDescription.text = exploreItem.description
            binding.exploreIcon.setImageResource(exploreItem.iconRes)

            binding.root.setOnClickListener {
                onExploreClicked(exploreItem)
            }
        }
    }

    private class ExploreDiffCallback : DiffUtil.ItemCallback<ExploreItem>() {
        override fun areItemsTheSame(oldItem: ExploreItem, newItem: ExploreItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExploreItem, newItem: ExploreItem): Boolean {
            return oldItem == newItem
        }
    }
}