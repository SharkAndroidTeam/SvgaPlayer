package com.shark.svgaplayer_base.fetch

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.shark.svgaplayer_base.SvgaLoader
import com.shark.svgaplayer_base.decode.AssetMetadata
import com.shark.svgaplayer_base.decode.DataSource
import com.shark.svgaplayer_base.decode.SVGASource
import com.shark.svgaplayer_base.request.Options
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.util.firstPathSegment
import com.shark.svgaplayer_base.util.getMimeTypeFromUrl
import com.shark.svgaplayer_base.util.isAssetUri
import com.shark.svgaplayer_base.util.md5Key
import okio.buffer
import okio.source
import java.io.File

/**

 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
class AssetUriFetcher(private val data: Uri,
                      private val options: Options) : Fetcher {
    companion object {
        const val ASSET_FILE_PATH_ROOT = "android_asset"
    }

    override suspend fun fetch(): FetchResult? {
        val path = data.pathSegments.drop(1).joinToString("/")

        return SourceResult(
            source = SVGASource(
                source = options.context.assets.open(path).source().buffer(),
                context = options.context,
                metadata = AssetMetadata(data.lastPathSegment!!)
            ),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromUrl(path),
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: SvgaLoader): Fetcher? {
            if (!isAssetUri(data)) return null
            return AssetUriFetcher(data, options)
        }
    }


}