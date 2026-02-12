package com.zaralyn.study.mask.logger

import android.util.Log
import de.robv.android.xposed.XposedBridge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Xposed日志实现
 * 同时输出到logcat和XposedBridge
 */
class XposedLogger(
    override val tag: String,
    override val level: LogLevel = LogLevel.DEBUG
) : Logger {
    
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    override fun verbose(message: String) {
        if (shouldLog(LogLevel.VERBOSE)) {
            val formattedMessage = formatMessage(message)
            Log.v(tag, formattedMessage)
            XposedBridge.log("[$tag] $formattedMessage")
        }
    }
    
    override fun verbose(message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.VERBOSE)) {
            val formattedMessage = formatMessage(message)
            Log.v(tag, formattedMessage, throwable)
            XposedBridge.log("[$tag] $formattedMessage")
            XposedBridge.log(Log.getStackTraceString(throwable))
        }
    }
    
    override fun debug(message: String) {
        if (shouldLog(LogLevel.DEBUG)) {
            val formattedMessage = formatMessage(message)
            Log.d(tag, formattedMessage)
            XposedBridge.log("[$tag] $formattedMessage")
        }
    }
    
    override fun debug(message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.DEBUG)) {
            val formattedMessage = formatMessage(message)
            Log.d(tag, formattedMessage, throwable)
            XposedBridge.log("[$tag] $formattedMessage")
            XposedBridge.log(Log.getStackTraceString(throwable))
        }
    }
    
    override fun info(message: String) {
        if (shouldLog(LogLevel.INFO)) {
            val formattedMessage = formatMessage(message)
            Log.i(tag, formattedMessage)
            XposedBridge.log("[$tag] $formattedMessage")
        }
    }
    
    override fun info(message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.INFO)) {
            val formattedMessage = formatMessage(message)
            Log.i(tag, formattedMessage, throwable)
            XposedBridge.log("[$tag] $formattedMessage")
            XposedBridge.log(Log.getStackTraceString(throwable))
        }
    }
    
    override fun warn(message: String) {
        if (shouldLog(LogLevel.WARN)) {
            val formattedMessage = formatMessage(message)
            Log.w(tag, formattedMessage)
            XposedBridge.log("[$tag] $formattedMessage")
        }
    }
    
    override fun warn(message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.WARN)) {
            val formattedMessage = formatMessage(message)
            Log.w(tag, formattedMessage, throwable)
            XposedBridge.log("[$tag] $formattedMessage")
            XposedBridge.log(Log.getStackTraceString(throwable))
        }
    }
    
    override fun error(message: String) {
        if (shouldLog(LogLevel.ERROR)) {
            val formattedMessage = formatMessage(message)
            Log.e(tag, formattedMessage)
            XposedBridge.log("[$tag] $formattedMessage")
        }
    }
    
    override fun error(message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.ERROR)) {
            val formattedMessage = formatMessage(message)
            Log.e(tag, formattedMessage, throwable)
            XposedBridge.log("[$tag] $formattedMessage")
            XposedBridge.log(Log.getStackTraceString(throwable))
        }
    }
    
    /**
     * 格式化消息（添加时间戳）
     */
    private fun formatMessage(message: String): String {
        val timestamp = timestampFormat.format(Date())
        return "[$timestamp] $message"
    }
}