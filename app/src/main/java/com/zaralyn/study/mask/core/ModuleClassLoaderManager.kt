package com.zaralyn.study.mask.core

import android.content.Context
import com.zaralyn.study.mask.core.constants.Constants
import com.zaralyn.study.mask.logger.Logger
import de.robv.android.xposed.XposedBridge
import java.lang.ref.WeakReference

/**
 * 模块ClassLoader管理器
 * 管理Xposed模块的ClassLoader和Context，用于访问模块资源和加载模块类
 */
object ModuleClassLoaderManager {

    private const val TAG = "ModuleClassLoaderManager"

    // 使用强引用保持Context，因为Xposed模块需要在整个应用生命周期内访问模块资源
    // Context.createPackageContext()返回的是Application Context，不会造成内存泄漏
    private var moduleContext: Context? = null
    private var moduleClassLoader: ClassLoader? = null
    private var isInitialized = false

    private val logger = Logger.create(TAG)

    /**
     * 初始化模块ClassLoader和Context
     * 必须在XposedHook.handleLoadPackage()中调用
     */
    fun initialize(targetContext: Context) {
        if (isInitialized) {
            logger.debug("ModuleClassLoaderManager already initialized")
            return
        }

        try {
            // 创建模块的Context
            // 使用ApplicationContext，避免与Activity生命周期绑定
            val context = targetContext.createPackageContext(
                Constants.PACKAGE_NAME,
                Context.CONTEXT_IGNORE_SECURITY
            )

            // 保存Context引用（强引用，因为模块需要持续访问）
            moduleContext = context

            // 获取模块的ClassLoader
            // 使用当前类的ClassLoader作为模块的ClassLoader
            moduleClassLoader = this.javaClass.classLoader

            isInitialized = true
            logger.info("ModuleClassLoaderManager initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize ModuleClassLoaderManager: ${e.message}", e)
            throw RuntimeException("Failed to initialize ModuleClassLoaderManager: ${e.message}", e)
        }
    }

    /**
     * 获取模块的Context
     */
    fun getModuleContext(): Context {
        if (!isInitialized || moduleContext == null) {
            throw IllegalStateException("ModuleClassLoaderManager not initialized. Call initialize() first.")
        }
        return moduleContext!!
    }

    /**
     * 获取模块的ClassLoader
     */
    fun getModuleClassLoader(): ClassLoader {
        if (!isInitialized || moduleClassLoader == null) {
            throw IllegalStateException("ModuleClassLoaderManager not initialized. Call initialize() first.")
        }
        return moduleClassLoader!!
    }

    /**
     * 检查是否已初始化
     */
    fun isReady(): Boolean = isInitialized

    /**
     * 重置（用于测试）
     */
    fun reset() {
        moduleContext = null
        moduleClassLoader = null
        isInitialized = false
    }
}