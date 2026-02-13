package com.zaralyn.study.mask.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zaralyn.study.mask.databinding.ItemSettingBinding
import com.zaralyn.study.mask.model.SettingItem
import com.zaralyn.study.mask.model.SettingType

class SettingsAdapter(
    private val moduleContext: Context,
    private val onSettingClicked: (SettingItem) -> Unit
) : ListAdapter<SettingItem, SettingsAdapter.SettingsViewHolder>(SettingsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val binding = ItemSettingBinding.inflate(
            LayoutInflater.from(moduleContext),
            parent,
            false
        )
        return SettingsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        val settingItem = getItem(position)
        holder.bind(settingItem)
    }

    inner class SettingsViewHolder(
        private val binding: ItemSettingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(settingItem: SettingItem) {
            binding.settingTitle.text = settingItem.title
            binding.settingDescription.text = settingItem.description

            // 根据类型显示不同的控件
            when (settingItem.type) {
                SettingType.SWITCH -> {
                    binding.settingSwitch.visibility = android.view.View.VISIBLE
                    binding.settingArrow.visibility = android.view.View.GONE
                }
                SettingType.NAVIGATION -> {
                    binding.settingSwitch.visibility = android.view.View.GONE
                    binding.settingArrow.visibility = android.view.View.VISIBLE
                }
                SettingType.INFO -> {
                    binding.settingSwitch.visibility = android.view.View.GONE
                    binding.settingArrow.visibility = android.view.View.GONE
                }
            }

            binding.root.setOnClickListener {
                onSettingClicked(settingItem)
            }
        }
    }

    private class SettingsDiffCallback : DiffUtil.ItemCallback<SettingItem>() {
        override fun areItemsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean {
            return oldItem == newItem
        }
    }
}