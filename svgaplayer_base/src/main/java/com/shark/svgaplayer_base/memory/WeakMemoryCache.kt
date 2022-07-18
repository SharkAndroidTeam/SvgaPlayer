package com.shark.svgaplayer_base.memory

import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import androidx.annotation.VisibleForTesting
import com.opensource.svgaplayer.SVGAVideoEntity
import com.shark.svgaplayer_base.memory.MemoryCache.Key
import com.shark.svgaplayer_base.util.firstNotNullOfOrNullIndices
import com.shark.svgaplayer_base.util.identityHashCode
import com.shark.svgaplayer_base.util.removeIfIndices
import java.lang.ref.WeakReference

/**
 * An in-memory cache that holds weak references to [SVGAVideoEntity]s.
 *
 * This is used as a secondary caching layer for [StrongMemoryCache]. [StrongMemoryCache] holds strong references
 * to its bitmaps. Bitmaps are added to this cache when they're removed from [StrongMemoryCache].
 */
interface WeakMemoryCache {

    /** Remove [videoEntity] from this cache. */
    fun remove(videoEntity: SVGAVideoEntity): Boolean

    val keys: Set<Key>

    fun get(key: Key): MemoryCache.Value?

    fun set(key: Key, videoEntity: SVGAVideoEntity, extras: Map<String, Any>, size: Int)

    fun remove(key: Key): Boolean

    fun clearMemory()

    fun trimMemory(level: Int)
}

/** A [WeakMemoryCache] implementation that holds no references. */
internal class EmptyWeakMemoryCache : WeakMemoryCache {

    override fun get(key: Key): MemoryCache.Value? = null

    override fun set(key: Key, videoEntity: SVGAVideoEntity, extras: Map<String, Any>, size: Int) {}

    override fun remove(key: Key) = false

    override val keys get() = emptySet<Key>()

    override fun remove(videoEntity: SVGAVideoEntity) = false

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** A [WeakMemoryCache] implementation backed by a [HashMap]. */
internal class RealWeakMemoryCache: WeakMemoryCache {

    @VisibleForTesting
    internal val cache = LinkedHashMap<Key, ArrayList<InternalValue>>()
    private var operationsSinceCleanUp = 0

    override val keys @Synchronized get() = cache.keys.toSet()


    @Synchronized
    override fun get(key: Key): MemoryCache.Value? {
        val values = cache[key] ?: return null

        // Find the first bitmap that hasn't been collected.
        val value = values.firstNotNullOfOrNullIndices { value ->
            value.entity.get()?.let { MemoryCache.Value(it, value.extras) }
        }

        cleanUpIfNecessary()
        return value
    }

    @Synchronized
    override fun set(key: Key, videoEntity: SVGAVideoEntity, extras: Map<String, Any>, size: Int) {
        val values = cache.getOrPut(key) { arrayListOf() }

        // Insert the value into the list sorted descending by size.
        run {
            val identityHashCode = videoEntity.identityHashCode
            val newValue = InternalValue(identityHashCode, WeakReference(videoEntity), extras, size)
            for (index in values.indices) {
                val value = values[index]
                if (size >= value.size) {
                    if (value.identityHashCode == identityHashCode && value.entity.get() === videoEntity) {
                        values[index] = newValue
                    } else {
                        values.add(index, newValue)
                    }
                    return@run
                }
            }
            values += newValue
        }

        cleanUpIfNecessary()
    }

    @Synchronized
    override fun remove(key: Key): Boolean {
        return cache.remove(key) != null
    }

    @Synchronized
    override fun remove(videoEntity: SVGAVideoEntity): Boolean {
        val identityHashCode = videoEntity.identityHashCode

        // Find the bitmap in the cache and remove it.
        val removed = run {
            cache.values.forEach { values ->
                for (index in values.indices) {
                    if (values[index].identityHashCode == identityHashCode) {
                        values.removeAt(index)
                        return@run true
                    }
                }
            }
            return@run false
        }

        cleanUpIfNecessary()
        return removed
    }

    @Synchronized
    override fun clearMemory() {
        operationsSinceCleanUp = 0
        cache.clear()
    }
    @Synchronized
    override fun trimMemory(level: Int) {
        if (level >= TRIM_MEMORY_RUNNING_LOW && level != TRIM_MEMORY_UI_HIDDEN) {
            cleanUp()
        }
    }


    private fun cleanUpIfNecessary() {
        if (operationsSinceCleanUp++ >= CLEAN_UP_INTERVAL) {
            cleanUp()
        }
    }
    internal fun cleanUp() {
        operationsSinceCleanUp = 0

        // Remove all the values whose references have been collected.
        val iterator = cache.values.iterator()
        while (iterator.hasNext()) {
            val list = iterator.next()

            if (list.count() <= 1) {
                // Typically, the list will only contain 1 item. Handle this case in an optimal way here.
                if (list.firstOrNull()?.entity?.get() == null) {
                    iterator.remove()
                }
            } else {
                // Iterate over the list of values and delete individual entries that have been collected.
                list.removeIfIndices { it.entity.get() == null }

                if (list.isEmpty()) {
                    iterator.remove()
                }
            }
        }
    }

    @VisibleForTesting
    internal class InternalValue(
        val identityHashCode: Int,
        val entity: WeakReference<SVGAVideoEntity>,
        val extras: Map<String, Any>,
        val size: Int
    )

    companion object {
        private const val CLEAN_UP_INTERVAL = 10
    }

}
