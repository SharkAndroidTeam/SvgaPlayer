package com.shark.svgaplayer_base.key

import com.shark.svgaplayer_base.request.Options
import java.io.File

internal class FileKeyer(private val addLastModifiedToFileCacheKey: Boolean) : Keyer<File> {

    override fun key(data: File, options: Options): String {
        return if (addLastModifiedToFileCacheKey) {
            "${data.path}:${data.lastModified()}"
        } else {
            data.path
        }
    }
}
