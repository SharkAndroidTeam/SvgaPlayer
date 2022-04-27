package com.shark.svgaplayer_base.memory

import com.shark.svgaplayer_base.recycle.VideoEntityRefCounter
import com.opensource.svgaplayer.SVGAVideoEntity

class RealMemoryCache(
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache,
    private val referenceCounter: VideoEntityRefCounter
) : MemoryCache {

    override val size get() = strongMemoryCache.size

    override val maxSize get() = strongMemoryCache.maxSize

    override fun get(key: MemoryCache.Key): SVGAVideoEntity? {
        val value = strongMemoryCache.get(key) ?: weakMemoryCache.get(key)
        return value?.videoEntity?.also { referenceCounter.setValid(it, false) }
    }

    override fun set(key: MemoryCache.Key, entity: SVGAVideoEntity) {
        referenceCounter.setValid(entity, false)
        strongMemoryCache.set(key, entity, false)
        weakMemoryCache.remove(key) // Clear any existing weak values.
    }

    override fun remove(key: MemoryCache.Key): Boolean {
        // Do not short circuit.
        val removedStrong = strongMemoryCache.remove(key)
        val removedWeak = weakMemoryCache.remove(key)
        return removedStrong || removedWeak
    }

    override fun clear() {
        strongMemoryCache.clearMemory()
        weakMemoryCache.clearMemory()
    }

    interface Value {
        val videoEntity: SVGAVideoEntity
        val isSampled: Boolean
    }
}
