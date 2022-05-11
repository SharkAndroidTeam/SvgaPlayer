package com.shark.svgaplayer_base.fetch

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.TypedValue
import android.webkit.MimeTypeMap
import com.shark.svgaplayer_base.SvgaLoader
import com.shark.svgaplayer_base.decode.DataSource
import com.shark.svgaplayer_base.decode.ResourceMetadata
import com.shark.svgaplayer_base.decode.SVGASource
import com.shark.svgaplayer_base.request.Options
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.util.getMimeTypeFromUrl
import com.shark.svgaplayer_base.util.md5Key
import com.shark.svgaplayer_base.util.nightMode
import okio.buffer
import okio.source

internal class ResourceUriFetcher(
    private val data: Uri,
    private val options: Options
) : Fetcher {


    override suspend fun fetch(): FetchResult? {
        // Expected format: android.resource://example.package.name/12345678
        val packageName =
            data.authority?.takeIf { it.isNotBlank() } ?: throwInvalidUriException(data)
        val resId = data.pathSegments.lastOrNull()?.toIntOrNull() ?: throwInvalidUriException(data)

        val context = options.context
        val resources = if (packageName == context.packageName) {
            context.resources
        } else {
            context.packageManager.getResourcesForApplication(packageName)
        }
        val path = TypedValue().apply { resources.getValue(resId, this, true) }.string
        val entryName = path.substring(path.lastIndexOf('/'))
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromUrl(entryName)

        val typedValue = TypedValue()
        val inputStream = resources.openRawResource(resId, typedValue)
        return SourceResult(
            SVGASource(
                source = inputStream.source().buffer(),
                context = context,
                metadata = ResourceMetadata(packageName, resId, typedValue.density)
            ),
            mimeType = mimeType,
            dataSource = DataSource.MEMORY
        )
    }

    private fun throwInvalidUriException(data: Uri): Nothing {
        throw IllegalStateException("Invalid ${ContentResolver.SCHEME_ANDROID_RESOURCE} URI: $data")
    }

    class Factory : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: SvgaLoader): Fetcher? {
            if (!isApplicable(data)) return null
            return ResourceUriFetcher(data, options)
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE
        }
    }

    companion object {
        private const val MIME_TYPE_XML = "text/xml"
    }


}
