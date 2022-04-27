package com.shark.svgaplayer_base.util

import java.io.File

/**
 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
internal const val movieBinary = "movie.binary"

internal const val movieSpec = "movie.spec"

internal fun File.isSVGAUnzipFile(): Boolean {
    fun hasChild(vararg fileNames: String): Boolean {
        if (this.isDirectory) {
            val childFileNames = this.list().toSet()
            return fileNames.any { childFileNames.contains(it) }
        }
        return false
    }

    return hasChild(movieBinary, movieSpec)
}

internal fun File.makeSureExist() {
    val dir = this
    if (dir.exists()) {
        if (!dir.isDirectory) {
            dir.deleteRecursively()
            dir.mkdirs()
        }
    } else {
        dir.mkdirs()
    }
}