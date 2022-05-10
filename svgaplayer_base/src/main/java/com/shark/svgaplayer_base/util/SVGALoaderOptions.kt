package com.shark.svgaplayer_base.util

import com.shark.svgaplayer_base.decode.SVGAVideoEntityDecoder.Companion.DEFAULT_MAX_PARALLELISM

/**
 * Private configuration options used by [RealImageLoader].
 *
 * @see ImageLoader.Builder
 */
internal class SVGALoaderOptions(
    val addLastModifiedToFileCacheKey: Boolean = true,
    val networkObserverEnabled: Boolean = true,
    val respectCacheHeaders: Boolean = true,
    val bitmapFactoryMaxParallelism: Int = DEFAULT_MAX_PARALLELISM,
) {

    fun copy(
        addLastModifiedToFileCacheKey: Boolean = this.addLastModifiedToFileCacheKey,
        networkObserverEnabled: Boolean = this.networkObserverEnabled,
        respectCacheHeaders: Boolean = this.respectCacheHeaders,
        bitmapFactoryMaxParallelism: Int = this.bitmapFactoryMaxParallelism,
    ) = SVGALoaderOptions(
        addLastModifiedToFileCacheKey,
        networkObserverEnabled,
        respectCacheHeaders,
        bitmapFactoryMaxParallelism,
    )
}
