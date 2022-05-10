package com.shark.svgaplayer_base.request

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleObserver
import com.shark.svgaplayer_base.EventListener
import com.shark.svgaplayer_base.SvgaLoader
import com.shark.svgaplayer_base.recycle.VideoEntityRefCounter
import com.shark.svgaplayer_base.target.PoolableViewTarget
import com.shark.svgaplayer_base.target.ViewTarget
import com.shark.svgaplayer_base.target.Target
import com.shark.svgaplayer_base.util.Logger
import com.shark.svgaplayer_base.util.Utils.REQUEST_TYPE_ENQUEUE
import com.shark.svgaplayer_base.util.Utils.REQUEST_TYPE_EXECUTE
import com.shark.svgaplayer_base.util.isAttachedToWindowCompat
import com.shark.svgaplayer_base.util.requestManager
import kotlinx.coroutines.Job


/** [DelegateService] wraps [Target]s and [SVGARequest]s to manage their lifecycle. */
internal class DelegateService(
    private val imageLoader: SvgaLoader,
    private val referenceCounter: VideoEntityRefCounter,
    private val logger: Logger?) {

    @MainThread
    fun createTargetDelegate(
            target: Target?,
            type: Int,
            eventListener: EventListener
    ): TargetDelegate {
        return when (type) {
            REQUEST_TYPE_EXECUTE -> when (target) {
                null -> InvalidatableEmptyTargetDelegate(referenceCounter)
                else -> InvalidateTargetDelegate(target, referenceCounter, eventListener, logger)
            }
            REQUEST_TYPE_ENQUEUE -> when (target) {
                null -> EmptyTargetDelegate
                is PoolableViewTarget<*> -> PoolableTargetDelegate(
                        target,
                        referenceCounter,
                        eventListener,
                        logger
                )
                else -> InvalidateTargetDelegate(target, referenceCounter, eventListener, logger)
            }
            else -> error("Invalid type.")
        }
    }

    /** Wrap [request] to automatically dispose (and for [ViewTarget]s restart) the [SVGARequest] based on its lifecycle. */
    @MainThread
    fun createRequestDelegate(
        request: SVGARequest,
        targetDelegate: TargetDelegate,
        job: Job
    ): RequestDelegate {
        val lifecycle = request.lifecycle
        val delegate: RequestDelegate
        when (val target = request.target) {
            is ViewTarget<*> -> {
                delegate = ViewTargetRequestDelegate(imageLoader, request, targetDelegate, job)
                lifecycle.addObserver(delegate)

                if (target is LifecycleObserver) {
                    lifecycle.removeObserver(target)
                    lifecycle.addObserver(target)
                }

                target.view.requestManager.setCurrentRequest(delegate)

                // Call onViewDetachedFromWindow immediately if the view is already detached.
                if (!target.view.isAttachedToWindowCompat) {
                    target.view.requestManager.onViewDetachedFromWindow(target.view)
                }
            }
            else -> {
                delegate = BaseRequestDelegate(lifecycle, job)
                lifecycle.addObserver(delegate)
            }
        }
        return delegate
    }
}
