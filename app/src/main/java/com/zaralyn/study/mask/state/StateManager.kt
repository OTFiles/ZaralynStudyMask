package com.zaralyn.study.mask.state

import android.content.Context
import android.content.SharedPreferences
import com.zaralyn.study.mask.core.constants.Constants
import com.zaralyn.study.mask.logger.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * 统一状态管理器
 * 提供统一的状态管理接口，支持内存缓存和持久化存储
 */
class StateManager private constructor(
    private val packageName: String,
    private val logger: Logger = Logger.create("StateManager")
) {

    companion object {
        private val instances = ConcurrentHashMap<String, StateManager>()

        /**
         * 获取StateManager实例
         */
        fun getInstance(packageName: String, logger: Logger = Logger.create("StateManager")): StateManager {
            return instances.getOrPut(packageName) {
                StateManager(packageName, logger)
            }
        }

        /**
         * 清除所有实例
         */
        fun clearAll() {
            instances.clear()
        }
    }

    // 内存缓存
    private val memoryCache = ConcurrentHashMap<String, Any>()

    // SharedPreferences（延迟初始化）
    private var prefs: SharedPreferences? = null

    /**
     * 获取SharedPreferences
     */
    fun getSharedPreferences(): SharedPreferences {
        // 注意：在Xposed环境中，无法直接获取Context
        // 这里返回null，实际使用时需要通过其他方式获取
        return prefs ?: throw IllegalStateException("SharedPreferences not initialized")
    }

    /**
     * 初始化SharedPreferences
     */
    fun initializeSharedPreferences(prefs: SharedPreferences) {
        this.prefs = prefs
        logger.debug("SharedPreferences initialized for $packageName")
    }

    /**
     * 获取布尔值（从内存缓存）
     */
    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return memoryCache[key] as? Boolean ?: default
    }

    /**
     * 设置布尔值（内存缓存）
     */
    fun setBoolean(key: String, value: Boolean) {
        memoryCache[key] = value
    }

    /**
     * 获取整数值（从内存缓存）
     */
    fun getInt(key: String, default: Int = 0): Int {
        return memoryCache[key] as? Int ?: default
    }

    /**
     * 设置整数值（内存缓存）
     */
    fun setInt(key: String, value: Int) {
        memoryCache[key] = value
    }

    /**
     * 获取字符串值（从内存缓存）
     */
    fun getString(key: String, default: String? = null): String? {
        return memoryCache[key] as? String ?: default
    }

    /**
     * 设置字符串值（内存缓存）
     */
    fun setString(key: String, value: String?) {
        if (value != null) {
            memoryCache[key] = value
        } else {
            memoryCache.remove(key)
        }
    }

    /**
     * 获取长整数值（从内存缓存）
     */
    fun getLong(key: String, default: Long = 0L): Long {
        return memoryCache[key] as? Long ?: default
    }

    /**
     * 设置长整数值（内存缓存）
     */
    fun setLong(key: String, value: Long) {
        memoryCache[key] = value
    }

    /**
     * 持久化布尔值到SharedPreferences
     */
    fun persistBoolean(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
        logger.debug("Persisted boolean: $key = $value")
    }

    /**
     * 持久化整数值到SharedPreferences
     */
    fun persistInt(key: String, value: Int) {
        prefs?.edit()?.putInt(key, value)?.apply()
        logger.debug("Persisted int: $key = $value")
    }

    /**
     * 持久化字符串值到SharedPreferences
     */
    fun persistString(key: String, value: String?) {
        if (value != null) {
            prefs?.edit()?.putString(key, value)?.apply()
        } else {
            prefs?.edit()?.remove(key)?.apply()
        }
        logger.debug("Persisted string: $key = $value")
    }

    /**
     * 持久化长整数值到SharedPreferences
     */
    fun persistLong(key: String, value: Long) {
        prefs?.edit()?.putLong(key, value)?.apply()
        logger.debug("Persisted long: $key = $value")
    }

    /**
     * 从SharedPreferences加载布尔值
     */
    fun loadBoolean(key: String, default: Boolean = false): Boolean {
        val value = prefs?.getBoolean(key, default) ?: default
        memoryCache[key] = value
        return value
    }

    /**
     * 从SharedPreferences加载整数值
     */
    fun loadInt(key: String, default: Int = 0): Int {
        val value = prefs?.getInt(key, default) ?: default
        memoryCache[key] = value
        return value
    }

    /**
     * 从SharedPreferences加载字符串值
     */
    fun loadString(key: String, default: String? = null): String? {
        val value = prefs?.getString(key, default)
        if (value != null) {
            memoryCache[key] = value
        }
        return value
    }

    /**
     * 从SharedPreferences加载长整数值
     */
    fun loadLong(key: String, default: Long = 0L): Long {
        val value = prefs?.getLong(key, default) ?: default
        memoryCache[key] = value
        return value
    }

    /**
     * 清除内存缓存
     */
    fun clearMemoryCache() {
        memoryCache.clear()
        logger.debug("Memory cache cleared")
    }

    /**
     * 清除所有状态
     */
    fun clearAll() {
        clearMemoryCache()
        prefs?.edit()?.clear()?.apply()
        logger.debug("All state cleared")
    }

    /**
     * 获取内存缓存大小
     */
    fun getCacheSize(): Int {
        return memoryCache.size
    }
}