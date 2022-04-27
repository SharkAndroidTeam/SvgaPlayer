package com.shark.svgaplayer_base.intercept

import com.shark.svgaplayer_base.request.SVGARequest
import com.shark.svgaplayer_base.request.SVGAResult
import com.shark.svgaplayer_base.size.Size


/**
 * Observe, transform, short circuit, or retry requests to an [SvgaLoader]'s image engine.
 *
 * NOTE: The interceptor chain is launched from the main thread by default.
 * See [SvgaLoader.Builder.launchInterceptorChainOnMainThread] for more information.
 */
interface Interceptor {

    suspend fun intercept(chain: Chain): SVGAResult

    interface Chain {

        val request: SVGARequest

        val size: Size

        /**
         * Set the requested [Size] to load the image at.
         *
         * @param size The requested size for the image.
         */
        fun withSize(size: Size): Chain

        /**
         * Continue executing the chain.
         *
         * @param request The request to proceed with.
         */
        suspend fun proceed(request: SVGARequest): SVGAResult
    }
}
