package com.zaralyn.study.mask.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zaralyn.study.mask.R
import com.zaralyn.study.mask.model.Grade

class GradeAdapter(
    private val grades: List<Grade>,
    private val onGradeSelected: (Grade) -> Unit
) : RecyclerView.Adapter<GradeAdapter.GradeViewHolder>() {

    private var selectedGrade: Grade = grades[0]

    fun setSelectedGrade(grade: Grade) {
        selectedGrade = grade
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grade_card, parent, false)
        return GradeViewHolder(view)
    }

    override fun onBindViewHolder(holder: GradeViewHolder, position: Int) {
        val grade = grades[position]
        holder.bind(grade, grade == selectedGrade)
    }

    override fun getItemCount(): Int = grades.size

    inner class GradeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val gradeName: TextView = itemView.findViewById(R.id.gradeName)
        private val gradeContent: LinearLayout = itemView.findViewById(R.id.gradeCardContent)

        fun bind(grade: Grade, isSelected: Boolean) {
            gradeName.text = grade.displayName

            if (isSelected) {
                gradeContent.setBackgroundColor(itemView.context.getColor(R.color.md_theme_primary))
                gradeName.setTextColor(itemView.context.getColor(R.color.white))
            } else {
                gradeContent.setBackgroundColor(itemView.context.getColor(R.color.gray_light))
                gradeName.setTextColor(itemView.context.getColor(R.color.text_primary))
            }

            itemView.setOnClickListener {
                onGradeSelected(grade)
            }
        }
    }
}