package com.shark.svgaplayer_base.fetch

import android.webkit.MimeTypeMap
import com.shark.svgaplayer_base.SvgaLoader
import com.shark.svgaplayer_base.decode.DataSource
import com.shark.svgaplayer_base.decode.SVGASource
import com.shark.svgaplayer_base.request.Options
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.util.md5Key
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import java.io.File

/**

 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
class FileFetcher(private val data: File) : Fetcher {

    class Factory : Fetcher.Factory<File> {

        override fun create(data: File, options: Options, imageLoader: SvgaLoader): Fetcher {
            return FileFetcher(data)
        }
    }

    override suspend fun fetch(): FetchResult? {
        return SourceResult(
            source = SVGASource(file = data.toOkioPath()),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(data.extension),
            dataSource = DataSource.DISK
        )
    }
}