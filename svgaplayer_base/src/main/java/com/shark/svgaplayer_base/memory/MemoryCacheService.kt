package com.shark.svgaplayer_base.memory

import android.graphics.drawable.BitmapDrawable
import androidx.annotation.VisibleForTesting
import com.opensource.svgaplayer.SVGADrawable
import com.opensource.svgaplayer.SVGADynamicEntity
import com.opensource.svgaplayer.SVGAVideoEntity
import com.shark.svgaplayer_base.EventListener
import com.shark.svgaplayer_base.SvgaLoader
import com.shark.svgaplayer_base.decode.DataSource
import com.shark.svgaplayer_base.intercept.EngineInterceptor
import com.shark.svgaplayer_base.intercept.Interceptor
import com.shark.svgaplayer_base.request.*
import com.shark.svgaplayer_base.request.RequestService
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.util.Logger
import com.shark.svgaplayer_base.util.toDrawable
import okhttp3.Call

internal class MemoryCacheService(
    private val imageLoader: SvgaLoader,
    private val requestService: RequestService,
    private val logger: Logger?,
) {

    /** Create a [MemoryCache.Key] for this request. */
    fun newCacheKey(
        request: SVGARequest,
        mappedData: Any,
        options: Options,
        eventListener: EventListener
    ): MemoryCache.Key? {
        // Fast path: an explicit memory cache key has been set.
        request.memoryCacheKey?.let { return it }

        // Slow path: create a new memory cache key.
        eventListener.keyStart(request, mappedData)
        val base = imageLoader.components.key(mappedData, options)
        eventListener.keyEnd(request, base)
        if (base == null) return null

        return MemoryCache.Key(base)
    }

    /** Get the [MemoryCache.Value] for this request. */
    fun getCacheValue(
        request: SVGARequest,
        cacheKey: MemoryCache.Key,
        size: Size,
    ): MemoryCache.Value? {
        if (!request.memoryCachePolicy.readEnabled) return null
        return imageLoader.memoryCache?.get(cacheKey)
    }

    fun setCacheValue(
        cacheKey: MemoryCache.Key?,
        request: SVGARequest,
        result: EngineInterceptor.ExecuteResult
    ): Boolean {
        if (!request.memoryCachePolicy.writeEnabled) return false
        val memoryCache = imageLoader.memoryCache
        if (memoryCache == null || cacheKey == null) return false
        val entity = (result.videoEntity as? SVGAVideoEntity) ?: return false

        // Create and set the memory cache value.
        val extras = mutableMapOf<String, Any>()
        extras[EXTRA_IS_SAMPLED] = result.isSampled
        result.diskCacheKey?.let { extras[EXTRA_DISK_CACHE_KEY] = it }
        memoryCache[cacheKey] = MemoryCache.Value(entity, extras)
        return true
    }

    /** Create a [SuccessResult] from the given [cacheKey] and [cacheValue]. */
    suspend fun newResult(
        request: SVGARequest,
        cacheKey: MemoryCache.Key,
        cacheValue: MemoryCache.Value,
        callFactory: Call.Factory,
        options: Options
    ):SuccessResult {
        val dynamicEntity = request.dynamicEntityBuilder?.create(callFactory, options)
            ?: SVGADynamicEntity()
        return SuccessResult(
            drawable = SVGADrawable(cacheValue.entity, dynamicEntity),
            request = request,
            metadata = SVGAResult.Metadata(
                memoryCacheKey = cacheKey,
                isSampled = cacheValue.isSampled,
                dataSource = DataSource.MEMORY_CACHE,
            )
        )
    }


    private val MemoryCache.Value.isSampled: Boolean
        get() = (extras[EXTRA_IS_SAMPLED] as? Boolean) ?: false

    private val MemoryCache.Value.diskCacheKey: String?
        get() = extras[EXTRA_DISK_CACHE_KEY] as? String

    companion object {
        private const val TAG = "MemoryCacheService"
        @VisibleForTesting
        internal const val EXTRA_IS_SAMPLED = "svga#is_sampled"
        @VisibleForTesting
        internal const val EXTRA_DISK_CACHE_KEY = "svga#disk_cache_key"
    }


}
