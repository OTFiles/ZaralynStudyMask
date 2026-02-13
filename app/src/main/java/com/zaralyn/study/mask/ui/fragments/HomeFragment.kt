package com.zaralyn.study.mask.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zaralyn.study.mask.R
import com.zaralyn.study.mask.ReaderActivity
import com.zaralyn.study.mask.adapter.BookAdapter
import com.zaralyn.study.mask.adapter.GradeAdapter
import com.zaralyn.study.mask.core.ModuleClassLoaderManager
import com.zaralyn.study.mask.data.BookData
import com.zaralyn.study.mask.databinding.FragmentHomeBinding
import com.zaralyn.study.mask.logger.Logger
import com.zaralyn.study.mask.logger.LogLevel
import com.zaralyn.study.mask.model.Grade

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var currentGrade = Grade.GRADE_10
    private lateinit var gradeAdapter: GradeAdapter
    private lateinit var bookAdapter: BookAdapter

    private val logger = Logger.create("HomeFragment", LogLevel.DEBUG)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGradeSelection()
        setupBookList()
    }

    private fun setupGradeSelection() {
        val ctx = ModuleClassLoaderManager.getModuleContext()
        gradeAdapter = GradeAdapter(
            moduleContext = ctx,
            grades = listOf(Grade.GRADE_10, Grade.GRADE_11, Grade.GRADE_12),
            onGradeSelected = { grade ->
                currentGrade = grade
                gradeAdapter.setSelectedGrade(grade)
                updateBookList()
            }
        )

        binding.gradeContainer.removeAllViews()

        for (grade in listOf(Grade.GRADE_10, Grade.GRADE_11, Grade.GRADE_12)) {
            val gradeView = createGradeView(grade)
            binding.gradeContainer.addView(gradeView)
        }

        // 默认选中高一
        selectGrade(Grade.GRADE_10)
    }

    private fun createGradeView(grade: Grade): View {
        val ctx = ModuleClassLoaderManager.getModuleContext()
        val inflater = LayoutInflater.from(ctx)
        val gradeView = inflater.inflate(R.layout.item_grade_card, binding.gradeContainer, false)

        gradeView.setOnClickListener {
            selectGrade(grade)
        }

        return gradeView
    }

    private fun selectGrade(grade: Grade) {
        currentGrade = grade

        // 更新所有年级卡片的选中状态
        val ctx = ModuleClassLoaderManager.getModuleContext()
        for (i in 0 until binding.gradeContainer.childCount) {
            val child = binding.gradeContainer.getChildAt(i)
            val cardView = child.findViewById<androidx.cardview.widget.CardView>(R.id.cardView)
            val gradeName = child.findViewById<android.widget.TextView>(R.id.gradeName)
            val gradeIcon = child.findViewById<android.widget.ImageView>(R.id.gradeIcon)

            if (child.tag == grade) {
                // 选中状态
                cardView.setCardBackgroundColor(ctx.getColor(R.color.md_theme_primary))
                gradeName.setTextColor(ctx.getColor(R.color.white))
                gradeIcon.setColorFilter(ctx.getColor(R.color.white))
            } else {
                // 未选中状态
                cardView.setCardBackgroundColor(ctx.getColor(R.color.md_theme_surfaceContainer))
                gradeName.setTextColor(ctx.getColor(R.color.text_primary))
                gradeIcon.setColorFilter(ctx.getColor(R.color.md_theme_primary))
            }
        }

        updateBookList()
    }

    private fun setupBookList() {
        val ctx = ModuleClassLoaderManager.getModuleContext()
        bookAdapter = BookAdapter(
            moduleContext = ctx,
            onBookClicked = { book ->
                val intent = Intent(requireContext(), ReaderActivity::class.java).apply {
                    putExtra("title", book.title)
                    putExtra("filename", book.fileName)
                }
                startActivity(intent)
            }
        )

        binding.bookRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = bookAdapter
        }

        // 初始加载书本列表
        bookAdapter.submitList(BookData.getBooksForGrade(currentGrade))
    }

    private fun updateBookList() {
        bookAdapter.submitList(BookData.getBooksForGrade(currentGrade))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}