package com.shark.svgaplayer_base.intercept

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import com.opensource.svgaplayer.SVGADrawable
import com.opensource.svgaplayer.SVGADynamicEntity
import com.opensource.svgaplayer.SVGAVideoEntity
import com.shark.svgaplayer_base.ComponentRegistry
import com.shark.svgaplayer_base.EventListener
import com.shark.svgaplayer_base.SvgaLoader
import com.shark.svgaplayer_base.decode.DataSource
import com.shark.svgaplayer_base.decode.DecodeResult
import com.shark.svgaplayer_base.fetch.FetchResult
import com.shark.svgaplayer_base.fetch.Fetcher
import com.shark.svgaplayer_base.fetch.SVGAEntityResult
import com.shark.svgaplayer_base.fetch.SourceResult
import com.shark.svgaplayer_base.memory.*
import com.shark.svgaplayer_base.recycle.VideoEntityRefCounter
import com.shark.svgaplayer_base.request.*
import com.shark.svgaplayer_base.request.RequestService
import com.shark.svgaplayer_base.request.SVGAResult.Metadata
import com.shark.svgaplayer_base.size.OriginalSize
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.util.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

/** The last interceptor in the chain which executes the [SVGARequest]. */
internal class EngineInterceptor(
    private val imageLoader: SvgaLoader,
    private val requestService: RequestService,
    private val logger: Logger?,
    private val callFactory: Call.Factory,
) : Interceptor {

    private val memoryCacheService = MemoryCacheService(imageLoader, requestService, logger)


    override suspend fun intercept(chain: Interceptor.Chain): SVGAResult {
        try {
            val request = chain.request
            val data = request.data
            val size = chain.size
            val eventListener = chain.eventListener

            val options = requestService.options(request)

            // Perform any data mapping.
            eventListener.mapStart(request, data)
            val mappedData = imageLoader.components.map(data, options)
            eventListener.mapEnd(request, mappedData)


            // Check the memory cache.
            val cacheKey =
                memoryCacheService.newCacheKey(request, mappedData, options, eventListener)
            val cacheValue = cacheKey?.let { memoryCacheService.getCacheValue(request, it, size) }

            // Fast path: return the value from the memory cache.
            if (cacheValue != null) {
                return memoryCacheService.newResult(
                    request,
                    cacheKey,
                    cacheValue,
                    callFactory,
                    options
                )
            }

            // Slow path: fetch, decode, transform, and cache the image.
            return withContext(request.fetcherDispatcher) {
                // Fetch and decode the image.
                val result =
                    execute(request, mappedData, options, cacheKey?.key ?: "", eventListener)

                // Write the result to the memory cache.
                memoryCacheService.setCacheValue(cacheKey, request, result)

                // Return the result.
                val dynamicEntity = request.dynamicEntityBuilder?.create(callFactory, options)
                    ?: SVGADynamicEntity()
                SuccessResult(
                    drawable = SVGADrawable(result.videoEntity, dynamicEntity),
                    request = request,
                    metadata = Metadata(
                        memoryCacheKey = cacheKey,
                        isSampled = result.isSampled,
                        dataSource = DataSource.MEMORY_CACHE,
                    )
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            } else {
                return requestService.errorResult(chain.request, throwable)
            }
        }
    }

    /** Execute the [Fetcher], decode any data into a [Drawable], and apply any [Transformation]s. */
    private suspend fun execute(
        request: SVGARequest,
        mappedData: Any,
        _options: Options,
        cacheKey: String,
        eventListener: EventListener
    ): ExecuteResult {
        var options = _options
        var components = imageLoader.components
        var fetchResult: FetchResult? = null
        val executeResult = try {
            if (request.fetcherFactory != null || request.decoderFactory != null) {
                components = components.newBuilder()
                    .addFirst(request.fetcherFactory)
                    .addFirst(request.decoderFactory)
                    .build()
            }

            // Fetch the data.
            fetchResult = fetch(components, request, mappedData, options, eventListener)
            val result = when (fetchResult) {
                is SVGAEntityResult -> fetchResult
                is SourceResult -> {
                    val decodeResult: DecodeResult
                    var searchIndex = 0
                    while (true) {
                        val pair =
                            components.newDecoder(fetchResult, options, imageLoader, searchIndex)
                        checkNotNull(pair) { "Unable to create a decoder that supports: $mappedData" }
                        val decoder = pair.first
                        searchIndex = pair.second + 1

                        eventListener.decodeStart(request, decoder, options)
                        val result = decoder.decode()
                        eventListener.decodeEnd(request, decoder, options, result)

                        if (result != null) {
                            decodeResult = result
                            break
                        }
                    }
                    SVGAEntityResult(
                        videoEntity = decodeResult.entity,
                        isSampled = decodeResult.isSampled,
                        dataSource = fetchResult.dataSource
                    )
                }
            }
            ExecuteResult(
                videoEntity = result.videoEntity,
                isSampled = result.isSampled,
                dataSource = result.dataSource,
                diskCacheKey = cacheKey
            )
        } finally {
            // Ensure the fetch result's source is always closed.
            (fetchResult as? SourceResult)?.source?.closeQuietly()
        }
        return executeResult
    }

    private suspend fun fetch(
        components: ComponentRegistry,
        request: SVGARequest,
        mappedData: Any,
        options: Options,
        eventListener: EventListener
    ): FetchResult {
        val fetchResult: FetchResult
        var searchIndex = 0
        while (true) {
            val pair = components.newFetcher(mappedData, options, imageLoader, searchIndex)
            checkNotNull(pair) { "Unable to create a fetcher that supports: $mappedData" }
            val fetcher = pair.first
            searchIndex = pair.second + 1

            eventListener.fetchStart(request, fetcher, options)
            val result = fetcher.fetch()
            try {
                eventListener.fetchEnd(request, fetcher, options, result)
            } catch (throwable: Throwable) {
                // Ensure the source is closed if an exception occurs before returning the result.
                (result as? SourceResult)?.source?.closeQuietly()
                throw throwable
            }

            if (result != null) {
                fetchResult = result
                break
            }
        }
        return fetchResult
    }


    class ExecuteResult(
        val videoEntity: SVGAVideoEntity,
        val isSampled: Boolean,
        val dataSource: DataSource,
        val diskCacheKey: String?
    ) {
        fun copy(
            videoEntity: SVGAVideoEntity = this.videoEntity,
            isSampled: Boolean = this.isSampled,
            dataSource: DataSource = this.dataSource,
            diskCacheKey: String? = this.diskCacheKey
        ) = ExecuteResult(videoEntity, isSampled, dataSource, diskCacheKey)
    }


    companion object {
        private const val TAG = "EngineInterceptor"
    }
}
