package com.zaralyn.study.mask.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zaralyn.study.mask.R
import com.zaralyn.study.mask.ReaderActivity
import com.zaralyn.study.mask.adapter.BookAdapter
import com.zaralyn.study.mask.adapter.GradeAdapter
import com.zaralyn.study.mask.data.BookData
import com.zaralyn.study.mask.databinding.FragmentHomeBinding
import com.zaralyn.study.mask.model.Grade

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var currentGrade = Grade.GRADE_10
    private lateinit var gradeAdapter: GradeAdapter
    private lateinit var bookAdapter: BookAdapter

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
        gradeAdapter = GradeAdapter(
            grades = listOf(Grade.GRADE_10, Grade.GRADE_11, Grade.GRADE_12),
            onGradeSelected = { grade ->
                currentGrade = grade
                gradeAdapter.setSelectedGrade(grade)
                updateBookList()
            }
        )

        binding.gradeContainer.removeAllViews()
        Grade.GRADE_10.value = 10
        Grade.GRADE_11.value = 11
        Grade.GRADE_12.value = 12

        for (grade in listOf(Grade.GRADE_10, Grade.GRADE_11, Grade.GRADE_12)) {
            val gradeView = createGradeView(grade)
            binding.gradeContainer.addView(gradeView)
        }

        // 默认选中高一
        selectGrade(Grade.GRADE_10)
    }

    private fun createGradeView(grade: Grade): View {
        val inflater = LayoutInflater.from(requireContext())
        val gradeView = inflater.inflate(R.layout.item_grade_card, binding.gradeContainer, false)

        gradeView.setOnClickListener {
            selectGrade(grade)
        }

        return gradeView
    }

    private fun selectGrade(grade: Grade) {
        currentGrade = grade

        // 更新所有年级卡片的选中状态
        for (i in 0 until binding.gradeContainer.childCount) {
            val child = binding.gradeContainer.getChildAt(i)
            val isSelected = when (i) {
                0 -> grade == Grade.GRADE_10
                1 -> grade == Grade.GRADE_11
                2 -> grade == Grade.GRADE_12
                else -> false
            }

            val contentLayout = child.findViewById<LinearLayout>(R.id.gradeCardContent)
            if (isSelected) {
                contentLayout.setBackgroundColor(requireContext().getColor(R.color.md_theme_primary))
                child.findViewById<android.widget.TextView>(R.id.gradeName).setTextColor(
                    requireContext().getColor(R.color.white)
                )
                child.findViewById<android.widget.ImageView>(R.id.gradeIcon).setColorFilter(
                    requireContext().getColor(R.color.white)
                )
            } else {
                contentLayout.setBackgroundColor(requireContext().getColor(R.color.gray_light))
                child.findViewById<android.widget.TextView>(R.id.gradeName).setTextColor(
                    requireContext().getColor(R.color.text_primary)
                )
                child.findViewById<android.widget.ImageView>(R.id.gradeIcon).setColorFilter(
                    requireContext().getColor(R.color.md_theme_primary)
                )
            }
        }

        updateBookList()
    }

    private fun setupBookList() {
        bookAdapter = BookAdapter(
            onBookClicked = { book ->
                val intent = Intent(requireContext(), ReaderActivity::class.java)
                intent.putExtra(ReaderActivity.EXTRA_BOOK_FILE_NAME, book.fileName)
                intent.putExtra(ReaderActivity.EXTRA_BOOK_TITLE, book.title)
                startActivity(intent)
            }
        )

        binding.bookRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = bookAdapter
        }

        updateBookList()
    }

    private fun updateBookList() {
        val books = BookData.getBooksForGrade(currentGrade)
        bookAdapter.submitList(books)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}