package com.shark.svgaplayer_base.size

import android.content.Context
import kotlin.math.max

/**
 * A [SizeResolver] that measures the size of the display.
 *
 * This is used as the fallback [SizeResolver] for [SVGARequest]s.
 */
class DisplaySizeResolver(private val context: Context) : SizeResolver {

    override suspend fun size(): Size {
        val metrics = context.resources.displayMetrics
        val maxDimension = Dimension(max(metrics.widthPixels, metrics.heightPixels))
        return Size(maxDimension, maxDimension)
    }


    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is DisplaySizeResolver && context == other.context)
    }

    override fun hashCode() = context.hashCode()

    override fun toString() = "DisplaySizeResolver(context=$context)"
}
