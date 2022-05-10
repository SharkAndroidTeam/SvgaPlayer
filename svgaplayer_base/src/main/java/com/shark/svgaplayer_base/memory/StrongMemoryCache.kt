package com.shark.svgaplayer_base.memory

import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.*
import android.graphics.Bitmap
import androidx.collection.LruCache
import com.opensource.svgaplayer.SVGAVideoEntity
import com.shark.svgaplayer_base.memory.MemoryCache.Key
import com.shark.svgaplayer_base.util.allocationByteCountCompat

/** An in-memory cache that holds strong references [Bitmap]s. */
internal interface StrongMemoryCache {
    /** The current size of the memory cache in bytes. */
    val size: Int

    /** The maximum size of the memory cache in bytes. */
    val maxSize: Int

    val keys: Set<Key>

    /** Get the value associated with [key]. */
    fun get(key: Key): MemoryCache.Value?

    /** Set the value associated with [key]. */
    fun set(key: Key, videoEntity: SVGAVideoEntity, extras: Map<String, Any>)

    /** Remove the value referenced by [key] from this cache. */
    fun remove(key: Key): Boolean

    /** Remove all values from this cache. */
    fun clearMemory()

    /** @see ComponentCallbacks2.onTrimMemory */
    fun trimMemory(level: Int)
}

/** A [StrongMemoryCache] implementation that caches nothing. */
internal class EmptyStrongMemoryCache(
    private val weakMemoryCache: WeakMemoryCache
) : StrongMemoryCache {

    override val size get() = 0

    override val maxSize get() = 0

    override fun get(key: Key): MemoryCache.Value? = null

    override fun set(key: Key, videoEntity: SVGAVideoEntity, extras: Map<String, Any>) {
        weakMemoryCache.set(key, videoEntity, extras, videoEntity.allocationByteCountCompat)
    }

    override fun remove(key: Key) = false

    override val keys get() = emptySet<Key>()

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** A [StrongMemoryCache] implementation backed by an [LruCache]. */
internal class RealStrongMemoryCache(
    maxSize: Int,
    private val weakMemoryCache: WeakMemoryCache
) : StrongMemoryCache {

    private val cache = object : LruCache<Key, InternalValue>(maxSize) {
        override fun sizeOf(key: Key, value: InternalValue) = value.size
        override fun entryRemoved(
            evicted: Boolean,
            key: Key,
            oldValue: InternalValue,
            newValue: InternalValue?
        ) = weakMemoryCache.set(key, oldValue.entity, oldValue.extras, oldValue.size)
    }



    override val size get() = cache.size()
    override val maxSize get() = cache.maxSize()
    override val keys get() = cache.snapshot().keys

    override fun get(key: Key): MemoryCache.Value? {
        return cache.get(key)?.let { MemoryCache.Value(it.entity, it.extras) }
    }


    override fun set(key: Key, videoEntity: SVGAVideoEntity, extras: Map<String, Any>) {
        val size = videoEntity.allocationByteCountCompat
        if (size <= maxSize) {
            cache.put(key, InternalValue(videoEntity, extras, size))
        } else {
            // If the bitmap is too big for the cache, don't attempt to store it as doing
            // so will cause the cache to be cleared. Instead, evict an existing element
            // with the same key if it exists and add the bitmap to the weak memory cache.
            cache.remove(key)
            weakMemoryCache.set(key, videoEntity, extras, size)
        }
    }

    @Synchronized
    override fun remove(key: Key): Boolean {
        return cache.remove(key) != null
    }

    @Synchronized
    override fun clearMemory() {
        cache.evictAll()
    }

    @Synchronized
    override fun trimMemory(level: Int) {
        if (level >= TRIM_MEMORY_BACKGROUND) {
            clearMemory()
        } else if (level in TRIM_MEMORY_RUNNING_LOW until TRIM_MEMORY_UI_HIDDEN) {
            cache.trimToSize(size / 2)
        }
    }

    private class InternalValue(
        val entity: SVGAVideoEntity,
        val extras: Map<String, Any>,
        val size: Int
    )
    companion object {
        private const val TAG = "RealStrongMemoryCache"
    }
}
