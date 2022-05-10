@file:Suppress("NOTHING_TO_INLINE")

package com.shark.svgaplayer_base.request

import androidx.annotation.MainThread
import com.shark.svgaplayer_base.target.PoolableViewTarget
import com.shark.svgaplayer_base.EventListener
import com.shark.svgaplayer_base.recycle.EmptyVideoEntityRefCounter
import com.shark.svgaplayer_base.recycle.VideoEntityRefCounter
import com.shark.svgaplayer_base.util.Logger
import com.shark.svgaplayer_base.util.requestManager
import com.shark.svgaplayer_base.target.Target
import com.opensource.svgaplayer.SVGAVideoEntity
import com.shark.svgaplayer_base.util.setValid


/**
 * Wrap a [Target] to support [SVGAVideoEntity] pooling.
 *
 * @see DelegateService
 */
internal sealed class TargetDelegate {

    open val target: Target? get() = null

    @MainThread
    open fun start(cached: SVGAVideoEntity?) {
    }

    @MainThread
    open suspend fun success(result: SuccessResult) {
    }

    @MainThread
    open suspend fun error(result: ErrorResult) {
    }

    @MainThread
    open fun clear() {
    }
}

/**
 * An empty target delegate. Used if the request has no target and does not need to invalidate bitmaps.
 */
internal object EmptyTargetDelegate : TargetDelegate()

/**
 * Only invalidate the success bitmap.
 *
 * Used if [SVGARequest.target] is null and the success [Drawable] is exposed.
 *
 * @see SvgaLoader.execute
 */
internal class InvalidatableEmptyTargetDelegate(
    private val referenceCounter: VideoEntityRefCounter
) : TargetDelegate() {

    override suspend fun success(result: SuccessResult) {
        referenceCounter.setValid(result.entity, false)
    }
}

/**
 * Invalidate the cached videoEntity and the success videoEntity.
 */
internal class InvalidateTargetDelegate(
    override val target: Target,
    private val referenceCounter: VideoEntityRefCounter,
    private val eventListener: EventListener,
    private val logger: Logger?
) : TargetDelegate() {

    override fun start(cached: SVGAVideoEntity?) {
        referenceCounter.setValid(cached, false)
        target.onStart()
    }

    override suspend fun success(result: SuccessResult) {
        referenceCounter.setValid(result.entity, false)
        target.onSuccess(result, eventListener, logger)
    }

    override suspend fun error(result: ErrorResult) {
        target.onError(result, eventListener, logger)
    }
}

/**
 * Handle the reference counts for the cached bitmap and the success bitmap.
 */
internal class PoolableTargetDelegate(
    override val target: PoolableViewTarget<*>,
    private val referenceCounter: VideoEntityRefCounter,
    private val eventListener: EventListener,
    private val logger: Logger?
) : TargetDelegate() {

    override fun start(cached: SVGAVideoEntity?) {
        replace(cached) { onStart() }
    }

    override suspend fun success(result: SuccessResult) {
        replace(result.entity) { onSuccess(result, eventListener, logger) }
    }

    override suspend fun error(result: ErrorResult) {
        replace(null) { onError(result, eventListener, logger) }
    }

    override fun clear() {
        replace(null) { onClear() }
    }

    /** Replace the current videoEntity reference with [SVGAVideoEntity]. */
    private inline fun replace(entity: SVGAVideoEntity?, update: PoolableViewTarget<*>.() -> Unit) {
        // Skip reference counting if bitmap pooling is disabled.
        if (referenceCounter is EmptyVideoEntityRefCounter) {
            target.update()
        } else {
            increment(entity)
            target.update()
            decrement(entity)
        }
    }

    /** Increment the reference counter for the current entity. */
    private fun increment(entity: SVGAVideoEntity?) {
        entity?.let(referenceCounter::increment)
    }

    /** Replace the reference to the previous entity and decrement its reference count. */
    private fun decrement(entity: SVGAVideoEntity?) {
        val previous = target.view.requestManager.put(this, entity)
        previous?.let(referenceCounter::decrement)
    }
}

private inline val SVGAResult.entity: SVGAVideoEntity?
    get() = drawable?.videoItem

private inline fun Target.onSuccess(
    result: SuccessResult,
    eventListener: EventListener,
    logger: Logger?
) {
    onSuccess(result.drawable)
}

private inline fun Target.onError(
    result: ErrorResult,
    eventListener: EventListener,
    logger: Logger?
) {
    onError()
}

private const val TAG = "TargetDelegate"
