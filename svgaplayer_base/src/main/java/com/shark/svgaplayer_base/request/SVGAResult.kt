package com.shark.svgaplayer_base.request

import com.shark.svgaplayer_base.decode.DataSource
import com.shark.svgaplayer_base.memory.MemoryCache
import com.opensource.svgaplayer.SVGADrawable

/**
 * Represents the result of an image request.
 *
 * @see IkCosler.execute
 */
sealed class SVGAResult {

    abstract val drawable: SVGADrawable?
    abstract val request: SVGARequest

    /**
     * Supplemental information about a successful image request.
     *
     * @param memoryCacheKey The cache key for the image in the memory cache.
     *  It is null if the image was not written to the memory cache.
     * @param isSampled True if the image is sampled (i.e. loaded into memory at less than its original size).
     * @param dataSource The data source that the image was loaded from.
     */
    data class Metadata(
        val memoryCacheKey: MemoryCache.Key?,
        val isSampled: Boolean,
        val dataSource: DataSource
    )
}

/**
 * Indicates that the request completed successfully.
 *
 * @param drawable The success drawable.
 * @param request The request that was executed to create this result.
 * @param metadata Metadata about the request that created this response.
 */
data class SuccessResult(
    override val drawable: SVGADrawable,
    override val request: SVGARequest,
    val metadata: Metadata
) : SVGAResult()

/**
 * Indicates that an error occurred while executing the request.
 *
 * @param drawable The error drawable.
 * @param request The request that was executed to create this result.
 * @param throwable The error that failed the request.
 */
data class ErrorResult(
    override val drawable: SVGADrawable?,
    override val request: SVGARequest,
    val throwable: Throwable
) : SVGAResult()
