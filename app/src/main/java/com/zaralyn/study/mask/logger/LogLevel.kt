package com.zaralyn.study.mask.logger

/**
 * 日志级别
 */
enum class LogLevel(val priority: Int, val tag: String) {
    VERBOSE(0, "V"),
    DEBUG(1, "D"),
    INFO(2, "I"),
    WARN(3, "W"),
    ERROR(4, "E")
}