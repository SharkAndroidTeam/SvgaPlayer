package com.shark.svgaplayer_base.request

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.shark.svgaplayer_base.SvgaLoader
import com.shark.svgaplayer_base.util.metadata
import kotlinx.coroutines.Job

internal sealed class RequestDelegate : DefaultLifecycleObserver {

    /** Called when the image request completes for any reason. */
    @MainThread
    open fun complete() {
    }

    /** Cancel any in progress work and free all resources. */
    @MainThread
    open fun dispose() {
    }

    /** Automatically dispose this delegate when the containing lifecycle is destroyed. */
    @MainThread
    override fun onDestroy(owner: LifecycleOwner) = dispose()
}

/** A request delegate for a one-shot requests with a non-poolable target. */
internal class BaseRequestDelegate(
    private val lifecycle: Lifecycle,
    private val job: Job
) : RequestDelegate() {

    override fun complete() {
        lifecycle.removeObserver(this)
    }

    override fun dispose() {
        job.cancel()
    }
}

/** A request delegate for requests with a [ViewTarget]. */
internal class ViewTargetRequestDelegate(
    private val imageLoader: SvgaLoader,
    private val request: SVGARequest,
    private val targetDelegate: TargetDelegate,
    private val job: Job
) : RequestDelegate() {

    /** Repeat this request with the same params. */
    @MainThread
    fun restart() {
        imageLoader.enqueue(request)
    }

    override fun dispose() {
        job.cancel()
        targetDelegate.clear()
        targetDelegate.metadata = null
        if (request.target is LifecycleObserver) {
            request.lifecycle.removeObserver(request.target)
        }
        request.lifecycle.removeObserver(this)
    }
}
