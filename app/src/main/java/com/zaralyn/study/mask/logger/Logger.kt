package com.zaralyn.study.mask.logger

import android.util.Log

/**
 * 日志器接口
 */
interface Logger {
    
    /**
     * 日志级别
     */
    val level: LogLevel
    
    /**
     * 日志标签
     */
    val tag: String
    
    /**
     * VERBOSE级别日志
     */
    fun verbose(message: String)
    
    /**
     * VERBOSE级别日志（带异常）
     */
    fun verbose(message: String, throwable: Throwable)
    
    /**
     * DEBUG级别日志
     */
    fun debug(message: String)
    
    /**
     * DEBUG级别日志（带异常）
     */
    fun debug(message: String, throwable: Throwable)
    
    /**
     * INFO级别日志
     */
    fun info(message: String)
    
    /**
     * INFO级别日志（带异常）
     */
    fun info(message: String, throwable: Throwable)
    
    /**
     * WARN级别日志
     */
    fun warn(message: String)
    
    /**
     * WARN级别日志（带异常）
     */
    fun warn(message: String, throwable: Throwable)
    
    /**
     * ERROR级别日志
     */
    fun error(message: String)
    
    /**
     * ERROR级别日志（带异常）
     */
    fun error(message: String, throwable: Throwable)
    
    /**
     * 判断是否应该输出指定级别的日志
     */
    fun shouldLog(level: LogLevel): Boolean {
        return level.priority >= this.level.priority
    }
    
    companion object {
        /**
         * 创建日志器实例
         */
        fun create(tag: String, level: LogLevel = LogLevel.DEBUG): Logger {
            return XposedLogger(tag, level)
        }
    }
}