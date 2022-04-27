package com.shark.svgaplayer_base.recycle

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.collection.SparseArrayCompat
import androidx.collection.set
import com.shark.svgaplayer_base.memory.WeakMemoryCache
import com.shark.svgaplayer_base.util.Logger
import com.shark.svgaplayer_base.util.forEachIndices
import com.shark.svgaplayer_base.util.identityHashCode
import com.shark.svgaplayer_base.util.log
import com.opensource.svgaplayer.SVGAVideoEntity
import java.lang.ref.WeakReference

/**
 * SVGAVideoEntity引用计数器
 * @Author svenj
 * @Date 2020/11/26
 * @Email svenjzm@gmail.com
 */
interface VideoEntityRefCounter {
    /**
     * Increase the reference count for [videoEntity] by one.
     */
    fun increment(videoEntity: SVGAVideoEntity)

    /**
     * Decrease the reference count for [videoEntity] by one.
     *
     * @return True if [videoEntity] was added decrement operation.
     */
    fun decrement(videoEntity: SVGAVideoEntity): Boolean

    /**
     * Mark [videoEntity] as valid/invalid.
     *
     * Once a bitmap has been marked as invalid it cannot be made valid again.
     */
    fun setValid(videoEntity: SVGAVideoEntity, isValid: Boolean)
}

internal object EmptyVideoEntityRefCounter : VideoEntityRefCounter {
    override fun increment(videoEntity: SVGAVideoEntity) {}

    override fun decrement(videoEntity: SVGAVideoEntity) = false

    override fun setValid(videoEntity: SVGAVideoEntity, isValid: Boolean) {}
}

internal class RealVideoEntityRefCounter(
    private val weakMemoryCache: WeakMemoryCache,
    private val logger: Logger?
) : VideoEntityRefCounter {
    internal val values = SparseArrayCompat<Value>()
    internal var operationsSinceCleanUp = 0

    @Synchronized
    override fun increment(videoEntity: SVGAVideoEntity) {
        val key = videoEntity.identityHashCode
        val value = getValue(key, videoEntity)
        value.count++
        logger?.log(TAG, Log.VERBOSE) { "INCREMENT: [$key, ${value.count}, ${value.isValid}]" }
        cleanUpIfNecessary()
    }

    @Synchronized
    override fun decrement(videoEntity: SVGAVideoEntity): Boolean {
        val key = videoEntity.identityHashCode
        val value = getValueOrNull(key, videoEntity) ?: run {
            logger?.log(TAG, Log.VERBOSE) { "DECREMENT: [$key, UNKNOWN, UNKNOWN]" }
            return false
        }
        value.count--
        logger?.log(TAG, Log.VERBOSE) { "DECREMENT: [$key, ${value.count}, ${value.isValid}]" }

        // If the videoEntity is valid and its count reaches 0, remove it
        // from the WeakMemoryCache and clear it.
        val removed = value.count <= 0 && value.isValid
        if (removed) {
            values.remove(key)
            weakMemoryCache.remove(videoEntity)
            videoEntity.clear()
        }

        cleanUpIfNecessary()
        return removed
    }

    @Synchronized
    override fun setValid(videoEntity: SVGAVideoEntity, isValid: Boolean) {
        val key = videoEntity.identityHashCode
        if (isValid) {
            val value = getValueOrNull(key, videoEntity)
            if (value == null) {
                values[key] = Value(WeakReference(videoEntity), 0, true)
            }
        } else {
            val value = getValue(key, videoEntity)
            value.isValid = false
        }
        cleanUpIfNecessary()
    }

    private fun cleanUpIfNecessary() {
        if (operationsSinceCleanUp++ >= CLEAN_UP_INTERVAL) {
            cleanUp()
        }
    }

    internal fun cleanUp() {
        val toRemove = arrayListOf<Int>()
        for (index in 0 until values.size()) {
            val value = values.valueAt(index)
            if (value.entity.get() == null) {
                // Don't remove the values while iterating over the loop so
                // we don't trigger SparseArray's internal GC for each removal.
                toRemove += index
            }
        }
        toRemove.forEachIndices(values::removeAt)
    }

    private fun getValue(key: Int, entity: SVGAVideoEntity): Value {
        var value = getValueOrNull(key, entity)
        if (value == null) {
            value = Value(WeakReference(entity), 0, false)
            values[key] = value
        }
        return value
    }

    private fun getValueOrNull(key: Int, entity: SVGAVideoEntity): Value? {
        return values[key]?.takeIf { it.entity.get() === entity }
    }

    internal class Value(
        val entity: WeakReference<SVGAVideoEntity>,
        var count: Int,
        var isValid: Boolean
    )

    companion object {
        private const val TAG = "RealBitmapReferenceCounter"
        private const val CLEAN_UP_INTERVAL = 50
        private val MAIN_HANDLER = Handler(Looper.getMainLooper())
    }
}