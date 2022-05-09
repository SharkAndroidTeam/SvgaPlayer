package com.shark.svgaplayer_base.memory

import android.content.Context
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.annotation.FloatRange
import com.shark.svgaplayer_base.size.Size
import com.opensource.svgaplayer.SVGAVideoEntity
import com.shark.svgaplayer_base.util.calculateMemoryCacheSize
import com.shark.svgaplayer_base.util.defaultMemoryCacheSizePercent
import kotlinx.parcelize.Parcelize

/**
 * An in-memory cache of recently loaded images.
 */
interface MemoryCache {

    /** The current size of the cache in bytes. */
    val size: Int

    /** The maximum size of the cache in bytes. */
    val maxSize: Int

    /** Get the [Bitmap] associated with [key]. */
    operator fun get(key: Key): SVGAVideoEntity?

    /** Set the [Bitmap] associated with [key]. */
    operator fun set(key: Key, entity: SVGAVideoEntity)

    /**
     * Remove the [Bitmap] referenced by [key].
     *
     * @return True if [key] was present in the cache. Else, return false.
     */
    fun remove(key: Key): Boolean

    /** Remove all values from the memory cache. */
    fun clear()

    /** The cache key for an image in the memory cache. */
    sealed class Key : Parcelable {

        companion object {
            /** Create a simple memory cache key. */
            @JvmStatic
            @JvmName("create")
            operator fun invoke(value: String): Key = Simple(value)
        }

        /** A simple memory cache key that wraps a [String]. Create new instances using [invoke]. */
        @Parcelize
        internal data class Simple(val value: String) : Key()

        /**
         * A complex memory cache key. Instances cannot be created directly as they often cannot be created
         * synchronously. Instead they are created by an [ImageLoader]'s image pipeline and are returned as part
         * of a successful image request's [ImageResult.Metadata].
         *
         * A request's metadata is accessible through [SVGARequest.Listener.onSuccess] and [SuccessResult].
         *
         * This class is an implementation detail and its fields may change in future releases.
         */
        @Parcelize
        internal data class Complex(
            val base: String,
            val size: Size?,
            val parameters: Map<String, String>
        ) : Key()
    }

//    class Builder(private val context: Context) {
//
//        private var maxSizePercent = defaultMemoryCacheSizePercent(context)
//        private var maxSizeBytes = 0
//        private var strongReferencesEnabled = true
//        private var weakReferencesEnabled = true
//
//        /**
//         * Set the maximum size of the memory cache as a percentage of this application's
//         * available memory.
//         */
//        fun maxSizePercent(@FloatRange(from = 0.0, to = 1.0) percent: Double) = apply {
//            require(percent in 0.0..1.0) { "size must be in the range [0.0, 1.0]." }
//            this.maxSizeBytes = 0
//            this.maxSizePercent = percent
//        }
//
//        /**
//         * Set the maximum size of the memory cache in bytes.
//         */
//        fun maxSizeBytes(size: Int) = apply {
//            require(size >= 0) { "size must be >= 0." }
//            this.maxSizePercent = 0.0
//            this.maxSizeBytes = size
//        }
//
//        /**
//         * Enables/disables strong reference tracking of values added to this memory cache.
//         */
//        fun strongReferencesEnabled(enable: Boolean) = apply {
//            this.strongReferencesEnabled = enable
//        }
//
//        /**
//         * Enables/disables weak reference tracking of values added to this memory cache.
//         * Weak references do not contribute to the current size of the memory cache.
//         * This ensures that if a [Bitmap] hasn't been garbage collected yet it will be
//         * returned from the memory cache.
//         */
//        fun weakReferencesEnabled(enable: Boolean) = apply {
//            this.weakReferencesEnabled = enable
//        }
//
//        /**
//         * Create a new [MemoryCache] instance.
//         */
//        fun build(): MemoryCache {
//            val weakMemoryCache = if (weakReferencesEnabled) {
//                RealWeakMemoryCache()
//            } else {
//                EmptyWeakMemoryCache()
//            }
//            val strongMemoryCache = if (strongReferencesEnabled) {
//                val maxSize = if (maxSizePercent > 0) {
//                    calculateMemoryCacheSize(context, maxSizePercent)
//                } else {
//                    maxSizeBytes
//                }
//                if (maxSize > 0) {
//                    RealStrongMemoryCache(maxSize, weakMemoryCache)
//                } else {
//                    EmptyStrongMemoryCache(weakMemoryCache)
//                }
//            } else {
//                EmptyStrongMemoryCache(weakMemoryCache)
//            }
//            return RealMemoryCache(strongMemoryCache, weakMemoryCache)
//        }
//    }

}
