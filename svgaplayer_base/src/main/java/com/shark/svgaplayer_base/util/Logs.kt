@file:JvmName("-Logs")
@file:Suppress("NOTHING_TO_INLINE")

package com.shark.svgaplayer_base.util

import android.util.Log

internal inline fun Logger.log(tag: String, priority: Int, lazyMessage: () -> String) {
    if (level <= priority) {
        log(tag, priority, lazyMessage(), null)
    }
}

internal fun Logger.log(tag: String, throwable: Throwable) {
    if (level <= Log.ERROR) {
        log(tag, Log.ERROR, null, throwable)
    }
}
