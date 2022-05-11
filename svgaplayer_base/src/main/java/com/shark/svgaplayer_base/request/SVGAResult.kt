package com.shark.svgaplayer_base.request

import android.graphics.drawable.Drawable
import com.shark.svgaplayer_base.decode.DataSource
import com.shark.svgaplayer_base.memory.MemoryCache
import com.opensource.svgaplayer.SVGADrawable
import com.shark.svgaplayer_base.annotation.ExperimentalCoilApi

/**
 * Represents the result of an image request.
 *
 * @see IkCosler.execute
 */
sealed class SVGAResult {
    abstract val drawable: Drawable?
    abstract val request: SVGARequest
}


/**
 * Indicates that the request completed successfully.
 */
class SuccessResult(
    /**
     * The success drawable.
     */
    override val drawable: SVGADrawable,

    /**
     * The request that was executed to create this result.
     */
    override val request: SVGARequest,

    /**
     * The data source that the image was loaded from.
     */
    val dataSource: DataSource,

    /**
     * The cache key for the image in the memory cache.
     * It is 'null' if the image was not written to the memory cache.
     */
    val memoryCacheKey: MemoryCache.Key? = null,

    /**
     * The cache key for the image in the disk cache.
     * It is 'null' if the image was not written to the disk cache.
     */
    val diskCacheKey: String? = null,

    /**
     * 'true' if the image is sampled (i.e. loaded into memory at less than its original size).
     */
    val isSampled: Boolean = false,

    ) : SVGAResult() {

    fun copy(
        drawable: SVGADrawable = this.drawable,
        request: SVGARequest = this.request,
        dataSource: DataSource = this.dataSource,
        memoryCacheKey: MemoryCache.Key? = this.memoryCacheKey,
        diskCacheKey: String? = this.diskCacheKey,
        isSampled: Boolean = this.isSampled,
    ) = SuccessResult(
        drawable = drawable,
        request = request,
        dataSource = dataSource,
        memoryCacheKey = memoryCacheKey,
        diskCacheKey = diskCacheKey,
        isSampled = isSampled,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is SuccessResult &&
                drawable == other.drawable &&
                request == other.request &&
                dataSource == other.dataSource &&
                memoryCacheKey == other.memoryCacheKey &&
                diskCacheKey == other.diskCacheKey &&
                isSampled == other.isSampled
    }

    override fun hashCode(): Int {
        var result = drawable.hashCode()
        result = 31 * result + request.hashCode()
        result = 31 * result + dataSource.hashCode()
        result = 31 * result + memoryCacheKey.hashCode()
        result = 31 * result + diskCacheKey.hashCode()
        result = 31 * result + isSampled.hashCode()
        return result
    }
}


/**
 * Indicates that an error occurred while executing the request.
 */
class ErrorResult(
    /**
     * The error drawable.
     */
    override val drawable: SVGADrawable?,

    /**
     * The request that was executed to create this result.
     */
    override val request: SVGARequest,

    /**
     * The error that failed the request.
     */
    val throwable: Throwable,
) : SVGAResult() {

    fun copy(
        drawable: SVGADrawable? = this.drawable,
        request: SVGARequest = this.request,
        throwable: Throwable = this.throwable,
    ) = ErrorResult(
        drawable = drawable,
        request = request,
        throwable = throwable,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ErrorResult &&
                drawable == other.drawable &&
                request == other.request &&
                throwable == other.throwable
    }

    override fun hashCode(): Int {
        var result = drawable.hashCode()
        result = 31 * result + request.hashCode()
        result = 31 * result + throwable.hashCode()
        return result
    }
}
