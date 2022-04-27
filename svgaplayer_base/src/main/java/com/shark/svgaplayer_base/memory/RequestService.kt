package com.shark.svgaplayer_base.memory

import androidx.annotation.WorkerThread
import com.shark.svgaplayer_base.decode.Options
import com.shark.svgaplayer_base.request.CachePolicy
import com.shark.svgaplayer_base.request.ErrorResult
import com.shark.svgaplayer_base.request.SVGARequest
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.util.Logger

/** Handles operations that act on [SVGARequest]s. */
internal class RequestService(private val logger: Logger?) {

    fun errorResult(request: SVGARequest, throwable: Throwable): ErrorResult {
        return ErrorResult(
            drawable = null,
            request = request,
            throwable = throwable
        )
    }

    @WorkerThread
    fun options(
        request: SVGARequest,
        size: Size,
        isOnline: Boolean
    ): Options {
        // Disable fetching from the network if we know we're offline.
        val networkCachePolicy = if (isOnline) request.networkCachePolicy else CachePolicy.DISABLED

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
