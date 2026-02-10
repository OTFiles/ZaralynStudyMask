package com.zaralyn.study.mask

import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class LogViewerActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var clearButton: Button
    private lateinit var refreshButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建界面
        val rootLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        
        // 标题栏
        val titleLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1E88E5.toInt())
            setPadding(16, 16, 16, 16)
        }
        
        val title = TextView(this).apply {
            text = "日志查看器"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            )
        }
        
        titleLayout.addView(title)
        rootLayout.addView(titleLayout)
        
        // 按钮栏
        val buttonLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
        }
        
        refreshButton = Button(this).apply {
            text = "刷新日志"
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            )
            setOnClickListener { refreshLogs() }
        }
        
        clearButton = Button(this).apply {
            text = "清除日志"
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            )
            setOnClickListener { clearLogs() }
        }
        
        buttonLayout.addView(refreshButton)
        buttonLayout.addView(clearButton)
        rootLayout.addView(buttonLayout)
        
        // 日志显示区域
        scrollView = ScrollView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }
        
        logTextView = TextView(this).apply {
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(8, 8, 8, 8)
            setBackgroundColor(0xFFF5F5F5.toInt())
        }
        
        scrollView.addView(logTextView)
        rootLayout.addView(scrollView)
        
        setContentView(rootLayout)
        
        // 自动加载日志
        refreshLogs()
    }
    
    private fun refreshLogs() {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-s", "ZaralynStudyMask"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val logs = StringBuilder()
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    logs.append(line).append("\n")
                }
            }
            
            process.waitFor()
            
            if (logs.isNotEmpty()) {
                logTextView.text = logs.toString()
            } else {
                logTextView.text = "暂无日志\n\n提示：先修补一个应用并运行，然后点击刷新按钮查看日志"
            }
            
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        } catch (e: Exception) {
            logTextView.text = "读取日志失败：${e.message}\n${e.stackTraceToString()}"
        }
    }
    
    private fun clearLogs() {
        try {
            Runtime.getRuntime().exec(arrayOf("logcat", "-c"))
            logTextView.text = "日志已清除"
        } catch (e: Exception) {
            logTextView.text = "清除日志失败：${e.message}"
        }
    }
}