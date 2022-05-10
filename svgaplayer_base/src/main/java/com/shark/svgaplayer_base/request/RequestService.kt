package com.shark.svgaplayer_base.request

import androidx.annotation.WorkerThread
import com.shark.svgaplayer_base.SvgaLoader
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.target.ViewTarget
import com.shark.svgaplayer_base.util.HardwareBitmapService
import com.shark.svgaplayer_base.util.Logger
import com.shark.svgaplayer_base.util.SystemCallbacks
import kotlinx.coroutines.Job

/** Handles operations that act on [SVGARequest]s. */
internal class RequestService(
    private val imageLoader: SvgaLoader,
    private val systemCallbacks: SystemCallbacks,
    logger: Logger?
) {

    private val hardwareBitmapService = HardwareBitmapService(logger)

    /**
     * Wrap [initialRequest] to automatically dispose and/or restart the [ImageRequest]
     * based on its lifecycle.
     */
    fun requestDelegate(initialRequest: SVGARequest, job: Job): RequestDelegate {
        val lifecycle = initialRequest.lifecycle
        return when (val target = initialRequest.target) {
            is ViewTarget<*> ->
                ViewTargetRequestDelegate(imageLoader, initialRequest, target, lifecycle, job)
            else -> BaseRequestDelegate(lifecycle, job)
        }
    }

    fun errorResult(request: SVGARequest, throwable: Throwable): ErrorResult {
        return ErrorResult(
            drawable = null,
            request = request,
            throwable = throwable
        )
    }


    @WorkerThread
    fun options(
        request: SVGARequest
    ): Options {
        // Disable fetching from the network if we know we're offline.

        val networkCachePolicy = if (systemCallbacks.isOnline) {
            request.networkCachePolicy
        } else {
            CachePolicy.DISABLED
        }
        // Disable allowRgb565 if there are transformations or the requested config is ALPHA_8.
        // ALPHA_8 is a mask config where each pixel is 1 byte so it wouldn't make sense to use RGB_565 as an optimization in that case.

        return Options(
            context = request.context,
            headers = request.headers,
            parameters = request.parameters,
            memoryCachePolicy = request.memoryCachePolicy,
            diskCachePolicy = request.diskCachePolicy,
            networkCachePolicy = networkCachePolicy
        )
    }

}
