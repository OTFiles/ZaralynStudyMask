package com.zaralyn.study.mask.data

import com.zaralyn.study.mask.model.ExploreItem
import com.zaralyn.study.mask.R

object ExploreData {
    fun getExploreItems(): List<ExploreItem> {
        return listOf(
            ExploreItem("e1", "听力训练", "提升英语听力能力", R.drawable.ic_explore),
            ExploreItem("e2", "口语练习", "增强英语口语表达", R.drawable.ic_explore),
            ExploreItem("e3", "阅读理解", "提高阅读理解能力", R.drawable.ic_explore),
            ExploreItem("e4", "写作提升", "培养英语写作技巧", R.drawable.ic_explore)
        )
    }
}