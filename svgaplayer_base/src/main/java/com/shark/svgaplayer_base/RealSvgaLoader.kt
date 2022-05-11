package com.shark.svgaplayer_base

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import com.shark.svgaplayer_base.decode.SVGAVideoEntityDecoder
import com.shark.svgaplayer_base.disk.DiskCache
import com.shark.svgaplayer_base.fetch.*
import com.shark.svgaplayer_base.intercept.EngineInterceptor
import com.shark.svgaplayer_base.intercept.RealInterceptorChain
import com.shark.svgaplayer_base.key.FileKeyer
import com.shark.svgaplayer_base.key.UriKeyer
import com.shark.svgaplayer_base.map.FileUriMapper
import com.shark.svgaplayer_base.map.ResourceUriMapper
import com.shark.svgaplayer_base.map.StringMapper
import com.shark.svgaplayer_base.memory.*
import com.shark.svgaplayer_base.request.*
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.target.Target
import com.shark.svgaplayer_base.target.ViewTarget
import com.shark.svgaplayer_base.util.*
import com.shark.svgaplayer_base.util.job
import kotlinx.coroutines.*
import okhttp3.Call
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

/**

 * @Author svenj
 * @Date 2020/11/26
 * @Email svenjzm@gmail.com
 */
internal class RealSvgaLoader(
    val context: Context,
    override val defaults: DefaultRequestOptions,
    val memoryCacheLazy: Lazy<MemoryCache?>,
    val diskCacheLazy: Lazy<DiskCache?>,
    val callFactoryLazy: Lazy<Call.Factory>,
    val eventListenerFactory: EventListener.Factory,
    val componentRegistry: ComponentRegistry,
    val options: SVGALoaderOptions,
    val logger: Logger?,
) : SvgaLoader {


    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate +
            CoroutineExceptionHandler { _, throwable -> logger?.log(TAG, throwable) })

    private val systemCallbacks = SystemCallbacks(this, context, options.networkObserverEnabled)

    private val requestService = RequestService(this, systemCallbacks, logger)
    override val memoryCache by memoryCacheLazy
    override val diskCache by diskCacheLazy


    override val components = componentRegistry.newBuilder()
        // Mappers
        .add(StringMapper())
        .add(FileUriMapper())
        .add(ResourceUriMapper(context))
        // Keyers
        .add(UriKeyer())
        .add(FileKeyer(options.addLastModifiedToFileCacheKey))
        // Fetchers
        .add(HttpUriFetcher.Factory(callFactoryLazy, diskCacheLazy, options.respectCacheHeaders))
        .add(FileFetcher.Factory())
        .add(AssetUriFetcher.Factory())
        .add(ResourceUriFetcher.Factory())
        // Decoders
        .add(SVGAVideoEntityDecoder.Factory(options.bitmapFactoryMaxParallelism))
        .build()

    private val interceptors =
        components.interceptors + EngineInterceptor(this, requestService, logger,callFactoryLazy.value,)

    private val isShutdown = AtomicBoolean(false)

    override fun enqueue(request: SVGARequest): Disposable {
        // Start executing the request on the main thread.
        val job = scope.async {
            executeMain(request, REQUEST_TYPE_ENQUEUE).also { result ->
                if (result is ErrorResult) logger?.log(TAG, result.throwable)
            }
        }

        // Update the current request attached to the view and return a new disposable.
        return if (request.target is ViewTarget<*>) {
            request.target.view.requestManager.getDisposable(job)
        } else {
            OneShotDisposable(job)
        }
    }

    override suspend fun execute(request: SVGARequest) = coroutineScope {
        // Start executing the request on the main thread.
        val job = async(Dispatchers.Main.immediate) {
            executeMain(request, REQUEST_TYPE_EXECUTE)
        }

        // Update the current request attached to the view and await the result.
        if (request.target is ViewTarget<*>) {
            request.target.view.requestManager.getDisposable(job)
        }
        return@coroutineScope job.await()
    }


    @MainThread
    private suspend fun executeMain(initialRequest: SVGARequest, type: Int): SVGAResult {
        // Wrap the request to manage its lifecycle.
        val requestDelegate = requestService.requestDelegate(initialRequest, coroutineContext.job)
            .apply { assertActive() }

        // Apply this image loader's defaults to this request.
        val request = initialRequest.newBuilder().defaults(defaults).build()

        // Create a new event listener.
        val eventListener = eventListenerFactory.create(request)

        try {
            // Fail before starting if data is null.
            if (request.data == NullRequestData) throw NullRequestDataException()

            // Set up the request's lifecycle observers.
            requestDelegate.start()

            // Enqueued requests suspend until the lifecycle is started.
            if (type == REQUEST_TYPE_ENQUEUE) request.lifecycle.awaitStarted()

            // Set the placeholder on the target.
            val cached = null
            try {
                request.target?.onStart()
                eventListener.onStart(request)
                request.listener?.onStart(request)
            } finally {
                // referenceCounter.decrement(cached)
            }

            // Resolve the size.
            eventListener.resolveSizeStart(request)
            val size = request.sizeResolver.size()
            eventListener.resolveSizeEnd(request, size)

            // Execute the interceptor chain.
            val result = withContext(request.interceptorDispatcher) {
                RealInterceptorChain(
                    initialRequest = request,
                    interceptors = interceptors,
                    index = 0,
                    request = request,
                    size = size,
                    eventListener = eventListener,
                ).proceed(request)
            }

            // Set the result on the target.
            when (result) {
                is SuccessResult -> onSuccess(result, request.target, eventListener)
                is ErrorResult -> onError(result, request.target, eventListener)
            }
            return result
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                onCancel(request, eventListener)
                throw throwable
            } else {
                // Create the default error result if there's an uncaught exception.
                val result = requestService.errorResult(request, throwable)
                onError(result, request.target, eventListener)
                return result
            }
        } finally {
            requestDelegate.complete()
        }
    }


    /** Called by [SystemCallbacks.onTrimMemory]. */
    @Suppress("SAFE_CALL_WILL_CHANGE_NULLABILITY", "UNNECESSARY_SAFE_CALL")
    internal fun onTrimMemory(level: Int) {
        // https://github.com/coil-kt/coil/issues/1211
        memoryCacheLazy?.value?.trimMemory(level)
    }

    override fun shutdown() {
        if (isShutdown.getAndSet(true)) return
        scope.cancel()
        systemCallbacks.shutdown()
        memoryCache?.clear()
    }

    override fun newBuilder() = SvgaLoader.Builder(this)


    private fun onSuccess(
        result: SuccessResult,
        target: Target?,
        eventListener: EventListener
    ) {
        val request = result.request
        val dataSource = result.dataSource
        logger?.log(TAG, Log.INFO) { "Successful (${dataSource.name}) - ${request.data}" }
//        transition(result, target, eventListener) { target?.onSuccess(result.drawable) }
        target?.onSuccess(result.drawable)
        eventListener.onSuccess(request, result)
        request.listener?.onSuccess(request, result)
    }

    private fun onError(
        result: ErrorResult,
        target: Target?,
        eventListener: EventListener
    ) {
        val request = result.request
        logger?.log(TAG, Log.INFO) {
            " Failed - ${request.data} - ${result.throwable}"
        }
//        transition(result, target, eventListener) { target?.onError() }
        target?.onError()
        eventListener.onError(request, result.throwable)
        request.listener?.onError(request, result.throwable)
    }

    private fun onCancel(request: SVGARequest, eventListener: EventListener) {
        logger?.log(TAG, Log.INFO) {
            "Cancelled - ${request.data}"
        }
        eventListener.onCancel(request)
        request.listener?.onCancel(request)
    }


//    private inline fun transition(
//        result: SVGAResult,
//        target: Target?,
//        eventListener: EventListener,
//        setDrawable: () -> Unit
//    ) {
//        if (target !is TransitionTarget) {
//            setDrawable()
//            return
//        }
//
//        val transition = result.request.transitionFactory.create(target, result)
//        if (transition is NoneTransition) {
//            setDrawable()
//            return
//        }
//
//        eventListener.transitionStart(result.request, transition)
//        transition.transition()
//        eventListener.transitionEnd(result.request, transition)
//    }

    companion object {
        private const val TAG = "RealImageLoader"
        private const val REQUEST_TYPE_ENQUEUE = 0
        private const val REQUEST_TYPE_EXECUTE = 1
    }

}