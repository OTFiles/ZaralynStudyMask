package com.zaralyn.study.mask

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.zaralyn.study.mask.databinding.ActivityReaderBinding
import java.io.File

class ReaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOK_FILE_NAME = "extra_book_file_name"
        const val EXTRA_BOOK_TITLE = "extra_book_title"
    }

    private lateinit var binding: ActivityReaderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val fileName = intent.getStringExtra(EXTRA_BOOK_FILE_NAME)
        val title = intent.getStringExtra(EXTRA_BOOK_TITLE)

        title?.let {
            binding.toolbar.title = it
        }

        loadContent(fileName)
    }

    private fun loadContent(fileName: String?) {
        if (fileName == null) {
            showError("文件名不能为空")
            return
        }

        // 尝试从外部存储读取
        val externalFile = File("/sdcard/EnglishBook/$fileName")
        if (externalFile.exists()) {
            try {
                val content = externalFile.readText()
                binding.contentTextView.text = content
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 从 assets 读取测试文件
        try {
            val content = assets.open("test.md").bufferedReader().use { it.readText() }
            binding.contentTextView.text = content
        } catch (e: Exception) {
            e.printStackTrace()
            showError("无法读取文件: $fileName")
        }
    }

    private fun showError(message: String) {
        binding.contentTextView.text = "错误: $message"
    }
}