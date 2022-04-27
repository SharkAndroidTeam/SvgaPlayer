package com.shark.svgaplayer_base.fetch

import android.webkit.MimeTypeMap
import com.shark.svgaplayer_base.decode.DataSource
import com.shark.svgaplayer_base.decode.Options
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.util.md5Key
import okio.buffer
import okio.source
import java.io.File

/**

 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
class FileFetcher(private val addLastModifiedToFileCacheKey: Boolean) : Fetcher<File> {

    override fun key(data: File): String {
        val keyString = if (addLastModifiedToFileCacheKey) "${data.path}:${data.lastModified()}" else data.path

        return keyString.md5Key()
    }

    override suspend fun fetch(data: File, size: Size, options: Options): FetchResult {
        return SourceResult(
            source = data.source().buffer(),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(data.extension),
            dataSource = DataSource.DISK
        )
    }
}