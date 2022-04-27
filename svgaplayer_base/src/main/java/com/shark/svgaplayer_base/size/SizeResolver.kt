package com.shark.svgaplayer_base.size

import androidx.annotation.MainThread

/**
 * An interface for measuring the target size for an image request.
 *
 * @see SVGARequest.Builder.size
 */
interface SizeResolver {

    companion object {
        /** Create a [SizeResolver] with a fixed [size]. */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(size: Size): SizeResolver = RealSizeResolver(size)
    }

    /** Return the [Size] that the image should be loaded at. */
    @MainThread
    suspend fun size(): Size
}
