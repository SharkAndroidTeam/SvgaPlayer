package com.shark.svgaplayer_base.fetch

import android.net.Uri
import android.os.NetworkOnMainThreadException
import android.webkit.MimeTypeMap
import androidx.annotation.VisibleForTesting
import com.shark.svgaplayer_base.SvgaLoader
import com.shark.svgaplayer_base.annotation.ExperimentalCoilApi
import com.shark.svgaplayer_base.decode.DataSource
import com.shark.svgaplayer_base.decode.SVGASource
import com.shark.svgaplayer_base.request.Options
import com.shark.svgaplayer_base.disk.DiskCache
import com.shark.svgaplayer_base.network.CacheResponse
import com.shark.svgaplayer_base.network.CacheStrategy
import com.shark.svgaplayer_base.network.CacheStrategy.Companion.combineHeaders
import com.shark.svgaplayer_base.network.HttpException
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.util.*
import com.shark.svgaplayer_base.util.await
import com.shark.svgaplayer_base.util.closeQuietly
import com.shark.svgaplayer_base.util.getMimeTypeFromUrl
import com.shark.svgaplayer_base.util.md5Key
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.BufferedSource
import okio.IOException
import okio.buffer
import java.net.HttpURLConnection

internal class HttpUriFetcher(
    private val url: String,
    private val options: Options,
    private val callFactory: Lazy<Call.Factory>,
    private val diskCache: Lazy<DiskCache?>,
    private val respectCacheHeaders: Boolean
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        var snapshot = readFromDiskCache()
        try {
            // Fast path: fetch the image from the disk cache without performing a network request.
            val cacheStrategy: CacheStrategy
            if (snapshot != null) {
                // Always return cached images with empty metadata as they were likely added manually.
                if (fileSystem.metadata(snapshot.metadata).size == 0L) {
                    return SourceResult(
                        source = snapshot.toSVGASource(),
                        mimeType = getMimeType(url, null),
                        dataSource = DataSource.DISK
                    )
                }

                // Return the candidate from the cache if it is eligible.
                if (respectCacheHeaders) {
                    cacheStrategy =
                        CacheStrategy.Factory(newRequest(), snapshot.toCacheResponse()).compute()
                    if (cacheStrategy.networkRequest == null && cacheStrategy.cacheResponse != null) {
                        return SourceResult(
                            source = snapshot.toSVGASource(),
                            mimeType = getMimeType(url, cacheStrategy.cacheResponse.contentType),
                            dataSource = DataSource.DISK
                        )
                    }
                } else {
                    // Skip checking the cache headers if the option is disabled.
                    return SourceResult(
                        source = snapshot.toSVGASource(),
                        mimeType = getMimeType(url, snapshot.toCacheResponse()?.contentType),
                        dataSource = DataSource.DISK
                    )
                }

            } else {
                cacheStrategy = CacheStrategy.Factory(newRequest(), null).compute()
            }


            // Slow path: fetch the image from the network.
            var response = executeNetworkRequest(cacheStrategy.networkRequest!!)
            var responseBody = response.requireBody()
            try {
                // Write the response to the disk cache then open a new snapshot.
                snapshot = writeToDiskCache(
                    snapshot = snapshot,
                    request = cacheStrategy.networkRequest,
                    response = response,
                    cacheResponse = cacheStrategy.cacheResponse
                )
                if (snapshot != null) {
                    return SourceResult(
                        source = snapshot.toSVGASource(),
                        mimeType = getMimeType(this.url, snapshot.toCacheResponse()?.contentType),
                        dataSource = DataSource.NETWORK
                    )
                }

                // If we failed to read a new snapshot then read the response body if it's not empty.
                if (responseBody.contentLength() > 0) {
                    return SourceResult(
                        source = responseBody.toSVGASource(),
                        mimeType = getMimeType(this.url, responseBody.contentType()),
                        dataSource = response.toDataSource()
                    )
                } else {
                    // If the response body is empty, execute a new network request without the
                    // cache headers.
                    response.closeQuietly()
                    response = executeNetworkRequest(newRequest())
                    responseBody = response.requireBody()

                    return SourceResult(
                        source = responseBody.toSVGASource(),
                        mimeType = getMimeType(this.url, responseBody.contentType()),
                        dataSource = response.toDataSource()
                    )
                }
            } catch (e: Exception) {
                response.closeQuietly()
                throw e
            }
        } catch (e: Exception) {
            snapshot?.closeQuietly()
            throw e
        }
    }

    private val diskCacheKey get() = options.diskCacheKey ?: url

    private val fileSystem get() = diskCache.value!!.fileSystem

    private fun readFromDiskCache(): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled) {
            diskCache.value?.get(diskCacheKey)
        } else {
            null
        }
    }

    private fun writeToDiskCache(
        snapshot: DiskCache.Snapshot?,
        request: Request,
        response: Response,
        cacheResponse: CacheResponse?
    ): DiskCache.Snapshot? {
        // Short circuit if we're not allowed to cache this response.
        if (!isCacheable(request, response)) {
            snapshot?.closeQuietly()
            return null
        }

        // Open a new editor.
        val editor = if (snapshot != null) {
            snapshot.closeAndEdit()
        } else {
            diskCache.value?.edit(diskCacheKey)
        }

        // Return `null` if we're unable to write to this entry.
        if (editor == null) return null

        try {
            // Write the response to the disk cache.
            if (response.code == HttpURLConnection.HTTP_NOT_MODIFIED && cacheResponse != null) {
                // Only update the metadata.
                val combinedResponse = response.newBuilder()
                    .headers(combineHeaders(cacheResponse.responseHeaders, response.headers))
                    .build()
                fileSystem.write(editor.metadata) {
                    CacheResponse(combinedResponse).writeTo(this)
                }
            } else {
                // Update the metadata and the image data.
                fileSystem.write(editor.metadata) {
                    CacheResponse(response).writeTo(this)
                }
                fileSystem.write(editor.data) {
                    response.body!!.source().readAll(this)
                }
            }
            return editor.commitAndGet()
        } catch (e: Exception) {
            editor.abortQuietly()
            throw e
        } finally {
            response.closeQuietly()
        }
    }

    private fun newRequest(): Request {
        val request = Request.Builder()
            .url(url)
            .headers(options.headers)

        // Attach all custom tags to this request.
//        @Suppress("UNCHECKED_CAST")
        options.tags.asMap().forEach { request.tag(it.key as Class<Any>, it.value) }

        val diskRead = options.diskCachePolicy.readEnabled
        val networkRead = options.networkCachePolicy.readEnabled
        when {
            !networkRead && diskRead -> {
                request.cacheControl(CacheControl.FORCE_CACHE)
            }
            networkRead && !diskRead -> if (options.diskCachePolicy.writeEnabled) {
                request.cacheControl(CacheControl.FORCE_NETWORK)
            } else {
                request.cacheControl(CACHE_CONTROL_FORCE_NETWORK_NO_CACHE)
            }
            !networkRead && !diskRead -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
            }
        }

        return request.build()
    }

    private suspend fun executeNetworkRequest(request: Request): Response {
        val response = if (isMainThread()) {
            if (options.networkCachePolicy.readEnabled) {
                // Prevent executing requests on the main thread that could block due to a
                // networking operation.
                throw NetworkOnMainThreadException()
            } else {
                // Work around: https://github.com/Kotlin/kotlinx.coroutines/issues/2448
                callFactory.value.newCall(request).execute()
            }
        } else {
            // Suspend and enqueue the request on one of OkHttp's dispatcher threads.
            callFactory.value.newCall(request).await()
        }
        if (!response.isSuccessful && response.code != HttpURLConnection.HTTP_NOT_MODIFIED) {
            response.body?.closeQuietly()
            throw HttpException(response)
        }
        return response
    }

    /**
     * Parse the response's `content-type` header.
     *
     * "text/plain" is often used as a default/fallback MIME type.
     * Attempt to guess a better MIME type from the file extension.
     */
    @VisibleForTesting
    internal fun getMimeType(url: String, contentType: MediaType?): String? {
        val rawContentType = contentType?.toString()
        if (rawContentType == null || rawContentType.startsWith(MIME_TYPE_TEXT_PLAIN)) {
            MimeTypeMap.getSingleton().getMimeTypeFromUrl(url)?.let { return it }
        }
        return rawContentType?.substringBefore(';')
    }

    private fun isCacheable(request: Request, response: Response): Boolean {
        return options.diskCachePolicy.writeEnabled &&
                (!respectCacheHeaders || CacheStrategy.isCacheable(request, response))
    }

    private fun DiskCache.Snapshot.toCacheResponse(): CacheResponse? {
        try {
            return fileSystem.read(metadata) {
                CacheResponse(this)
            }
        } catch (_: IOException) {
            // If we can't parse the metadata, ignore this entry.
            return null
        }
    }

    private fun DiskCache.Snapshot.toSVGASource(): SVGASource {
        return SVGASource(data, fileSystem, diskCacheKey, this)
    }

    private fun ResponseBody.toSVGASource(): SVGASource {
        return SVGASource(source(), options.context)
    }

    private fun Response.toDataSource(): DataSource {
        return if (networkResponse != null) DataSource.NETWORK else DataSource.DISK
    }

    private fun Response.requireBody(): ResponseBody {
        return checkNotNull(body) { "response body == null" }
    }

    class Factory(
        private val callFactory: Lazy<Call.Factory>,
        private val diskCache: Lazy<DiskCache?>,
        private val respectCacheHeaders: Boolean
    ) : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: SvgaLoader): Fetcher? {
            if (!isApplicable(data)) return null
            return HttpUriFetcher(
                data.toString(),
                options,
                callFactory,
                diskCache,
                respectCacheHeaders
            )
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == "http" || data.scheme == "https"
        }
    }

    companion object {
        private const val MIME_TYPE_TEXT_PLAIN = "text/plain"

        val CACHE_CONTROL_FORCE_NETWORK_NO_CACHE: CacheControl =
            CacheControl.Builder().noCache().noStore().build()
        val CACHE_CONTROL_NO_NETWORK_NO_CACHE: CacheControl =
            CacheControl.Builder().noCache().onlyIfCached().build()
    }
}
