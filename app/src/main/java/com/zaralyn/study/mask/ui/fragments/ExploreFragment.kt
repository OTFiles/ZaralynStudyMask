package com.zaralyn.study.mask.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.zaralyn.study.mask.adapter.ExploreAdapter
import com.zaralyn.study.mask.data.ExploreData
import com.zaralyn.study.mask.databinding.FragmentExploreBinding

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private lateinit var exploreAdapter: ExploreAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupExploreList()
    }

    private fun setupExploreList() {
        exploreAdapter = ExploreAdapter(
            onExploreClicked = { exploreItem ->
                Toast.makeText(
                    requireContext(),
                    "${exploreItem.title} ${requireContext().getString(com.zaralyn.study.mask.R.string.explore_coming_soon)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        binding.exploreRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = exploreAdapter
        }

        val exploreItems = ExploreData.getExploreItems()
        exploreAdapter.submitList(exploreItems)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}