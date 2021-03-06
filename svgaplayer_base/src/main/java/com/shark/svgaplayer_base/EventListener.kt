package com.shark.svgaplayer_base

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.shark.svgaplayer_base.decode.DecodeResult
import com.shark.svgaplayer_base.decode.Decoder
import com.shark.svgaplayer_base.request.Options
import com.shark.svgaplayer_base.fetch.FetchResult
import com.shark.svgaplayer_base.fetch.Fetcher
import com.shark.svgaplayer_base.request.SVGARequest
import com.shark.svgaplayer_base.request.SVGAResult
import com.shark.svgaplayer_base.request.SuccessResult
import com.shark.svgaplayer_base.size.Size

interface EventListener : SVGARequest.Listener {

    @MainThread
    override fun onStart(request: SVGARequest) {}

    @MainThread
    fun resolveSizeStart(request: SVGARequest) {}

    @MainThread
    fun resolveSizeEnd(request: SVGARequest, size: Size) {}

    @AnyThread
    fun mapStart(request: SVGARequest, input: Any) {}

    @AnyThread
    fun mapEnd(request: SVGARequest, output: Any) {}

    /**
     * Called before [Keyer.key].
     *
     * @param input The data that will be converted.
     */
    @MainThread
    fun keyStart(request: SVGARequest, input: Any) {}

    /**
     * Called after [Keyer.key].
     *
     * @param output The data after it has been converted into a string key.
     *  If [output] is 'null' it will not be cached in the memory cache.
     */
    @MainThread
    fun keyEnd(request: SVGARequest, output: String?) {}

    @WorkerThread
    fun fetchStart(request: SVGARequest, fetcher: Fetcher, options: Options) {}

    @WorkerThread
    fun fetchEnd(request: SVGARequest, fetcher: Fetcher, options: Options, result: FetchResult?) {}

    @WorkerThread
    fun decodeStart(request: SVGARequest, decoder: Decoder, options: Options) {}

    @WorkerThread
    fun decodeEnd(request: SVGARequest, decoder: Decoder, options: Options, result: DecodeResult?) {}

    @MainThread
    override fun onCancel(request: SVGARequest) {}

    @MainThread
    override fun onError(request: SVGARequest, throwable: Throwable) {}

    @MainThread
    override fun onSuccess(request: SVGARequest, result: SuccessResult) {}


    /** A factory that creates new [EventListener] instances. */
    fun interface Factory {

        companion object {
            @JvmField val NONE = Factory(EventListener.NONE)

            /** Create an [EventListener.Factory] that always returns [listener]. */
            @JvmStatic
            @JvmName("create")
            operator fun invoke(listener: EventListener) = Factory { listener }
        }

        /** Return a new [EventListener]. */
        fun create(request: SVGARequest): EventListener
    }

    companion object {
        @JvmField val NONE = object : EventListener {}
    }
}
