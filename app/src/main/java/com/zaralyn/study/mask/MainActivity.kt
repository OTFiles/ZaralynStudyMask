package com.zaralyn.study.mask

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建简单的界面
        val rootLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            gravity = android.view.Gravity.CENTER
        }
        
        // 标题
        val title = TextView(this).apply {
            text = "ZaralynStudyMask"
            textSize = 28f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF1E88E5.toInt())
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }
        
        // 描述
        val description = TextView(this).apply {
            text = "高中英语学习伪装层\n\n这是一个 NPatch 模块，用于为 Android 应用添加\"高中英语学习\"的伪装界面。\n\n使用方法：\n1. 使用 NPatch 修补目标应用\n2. 启动修补后的应用\n3. 连击5次主页键或按F10键进入原应用"
            textSize = 16f
            setTextColor(0xFF333333.toInt())
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }
        
        // 查看日志按钮
        val logButton = Button(this).apply {
            text = "查看日志"
            setOnClickListener {
                val intent = Intent(this@MainActivity, LogViewerActivity::class.java)
                startActivity(intent)
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }
        
        rootLayout.addView(title)
        rootLayout.addView(description)
        rootLayout.addView(logButton)
        
        setContentView(rootLayout)
    }
}