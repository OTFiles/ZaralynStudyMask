package com.zaralyn.study.mask.data

import com.zaralyn.study.mask.model.Book
import com.zaralyn.study.mask.model.Grade

object BookData {
    fun getBooksForGrade(grade: Grade): List<Book> {
        return when (grade) {
            Grade.GRADE_10 -> listOf(
                Book("b1", "必修一", "English Book 1", "compulsory_1.md", grade),
                Book("b2", "必修二", "English Book 2", "compulsory_2.md", grade),
                Book("b3", "英语语法", "Grammar Guide", "grammar.md", grade),
                Book("b4", "词汇手册", "Vocabulary Handbook", "vocabulary.md", grade)
            )
            Grade.GRADE_11 -> listOf(
                Book("b5", "必修三", "English Book 3", "compulsory_3.md", grade),
                Book("b6", "必修四", "English Book 4", "compulsory_4.md", grade),
                Book("b7", "阅读训练", "Reading Practice", "reading.md", grade),
                Book("b8", "写作指南", "Writing Guide", "writing.md", grade)
            )
            Grade.GRADE_12 -> listOf(
                Book("b9", "必修五", "English Book 5", "compulsory_5.md", grade),
                Book("b10", "选修六", "English Book 6", "elective_6.md", grade),
                Book("b11", "高考真题", "Exam Papers", "exam_papers.md", grade),
                Book("b12", "冲刺复习", "Sprint Review", "sprint_review.md", grade)
            )
        }
    }
}