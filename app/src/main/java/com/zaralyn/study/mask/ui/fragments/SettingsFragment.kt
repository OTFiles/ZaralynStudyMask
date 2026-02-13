package com.zaralyn.study.mask.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.zaralyn.study.mask.adapter.SettingsAdapter
import com.zaralyn.study.mask.core.ModuleClassLoaderManager
import com.zaralyn.study.mask.data.SettingData
import com.zaralyn.study.mask.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsAdapter: SettingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSettingsList()
    }

    private fun setupSettingsList() {
        val ctx = ModuleClassLoaderManager.getModuleContext()
        settingsAdapter = SettingsAdapter(
            moduleContext = ctx,
            onSettingClicked = { settingItem ->
                when (settingItem.id) {
                    "s3" -> {
                        Toast.makeText(
                            requireContext(),
                            "隐私政策页面",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    "s4" -> {
                        Toast.makeText(
                            requireContext(),
                            "用户协议页面",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    "s5" -> {
                        Toast.makeText(
                            requireContext(),
                            settingItem.description,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        binding.settingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = settingsAdapter
        }

        val settings = SettingData.getSettingItems()
        settingsAdapter.submitList(settings)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}