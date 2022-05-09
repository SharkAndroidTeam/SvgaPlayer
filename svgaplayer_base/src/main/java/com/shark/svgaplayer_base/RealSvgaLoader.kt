package com.shark.svgaplayer_base

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import com.opensource.svgaplayer.SVGAVideoEntity
import com.shark.svgaplayer_base.decode.SVGAVideoEntityDecoder
import com.shark.svgaplayer_base.disk.DiskCache
import com.shark.svgaplayer_base.fetch.*
import com.shark.svgaplayer_base.intercept.EngineInterceptor
import com.shark.svgaplayer_base.intercept.RealInterceptorChain
import com.shark.svgaplayer_base.map.FileUriMapper
import com.shark.svgaplayer_base.map.ResourceUriMapper
import com.shark.svgaplayer_base.map.StringMapper
import com.shark.svgaplayer_base.memory.*
import com.shark.svgaplayer_base.recycle.VideoEntityRefCounter
import com.shark.svgaplayer_base.request.*
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.target.ViewTarget
import com.shark.svgaplayer_base.util.*
import com.shark.svgaplayer_base.util.Utils.REQUEST_TYPE_ENQUEUE
import com.shark.svgaplayer_base.util.Utils.REQUEST_TYPE_EXECUTE
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
class RealSvgaLoader(
    val context: Context,
    override val defaults: DefaultRequestOptions,
    private val referenceCounter: VideoEntityRefCounter,
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache,
//    private val memoryCacheLazy: Lazy<MemoryCache?>,
    private val diskCacheLazy: Lazy<DiskCache?>,
    private val callFactoryLazy: Lazy<Call.Factory>,
    private val eventListenerFactory: EventListener.Factory,
    componentRegistry: ComponentRegistry,
    addLastModifiedToFileCacheKey: Boolean,
    private val launchInterceptorChainOnMainThread: Boolean,
    val logger: Logger?
) : SvgaLoader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate +
            CoroutineExceptionHandler { _, throwable -> logger?.log(TAG, throwable) })
    private val delegateService = DelegateService(this, referenceCounter, logger)
    private val memoryCacheService =
        MemoryCacheService(referenceCounter, strongMemoryCache, weakMemoryCache)
    private val requestService = RequestService(logger)
//    override val memoryCache by memoryCacheLazy
    override val diskCache by diskCacheLazy
    private val systemCallbacks = SystemCallbacks(this, context)
    private val registry = componentRegistry.newBuilder()
        // Mappers
        .add(StringMapper())
        .add(FileUriMapper())
        .add(ResourceUriMapper(context))
        // Fetchers
        .add(HttpUriFetcher(callFactoryLazy,diskCacheLazy))
        .add(HttpUrlFetcher(callFactoryLazy,diskCacheLazy))
        .add(FileFetcher(addLastModifiedToFileCacheKey))
        .add(AssetUriFetcher(context))
        .add(ResourceUriFetcher(context))
        // Decoders
        .add(SVGAVideoEntityDecoder(context))
        .build()

    private val interceptors = registry.interceptors + EngineInterceptor(
        registry, referenceCounter, strongMemoryCache, memoryCacheService, requestService,
        systemCallbacks, callFactoryLazy.value, logger
    )

    private val isShutdown = AtomicBoolean(false)

    override fun enqueue(request: SVGARequest): Disposable {
        // Start executing the request on the main thread.
        val job = scope.launch {
            val result = executeMain(request, REQUEST_TYPE_ENQUEUE)
            if (result is ErrorResult) throw result.throwable
        }

        // Update the current request attached to the view and return a new disposable.
        return if (request.target is ViewTarget<*>) {
            val requestId = request.target.view.requestManager.setCurrentRequestJob(job)
            ViewTargetDisposable(requestId, request.target)
        } else {
            BaseTargetDisposable(job)
        }
    }

    override suspend fun execute(request: SVGARequest): SVGAResult {
        // Update the current request attached to the view synchronously.
        if (request.target is ViewTarget<*>) {
            request.target.view.requestManager.setCurrentRequestJob(coroutineContext.job)
        }

        // Start executing the request on the main thread.
        return withContext(Dispatchers.Main.immediate) {
            executeMain(request, REQUEST_TYPE_EXECUTE)
        }
    }

    @MainThread
    private suspend fun executeMain(initialRequest: SVGARequest, type: Int): SVGAResult {
        // Ensure this image loader isn't shutdown.
        check(!isShutdown.get()) { "The svga loader is shutdown." }

        // Apply this image loader's defaults to this request.
        val request = initialRequest.newBuilder().defaults(defaults).build()

        // Create a new event listener.
        val eventListener = eventListenerFactory.create(request)

        // Wrap the target to support bitmap pooling.
        val targetDelegate =
            delegateService.createTargetDelegate(request.target, type, eventListener)

        // Wrap the request to manage its lifecycle.
        val requestDelegate =
            delegateService.createRequestDelegate(request, targetDelegate, coroutineContext.job)

        try {
            // Fail before starting if data is null.
            if (request.data == NullRequestData) throw NullRequestDataException()

            // Enqueued requests suspend until the lifecycle is started.
            if (type == REQUEST_TYPE_ENQUEUE) request.lifecycle.awaitStarted()

            // Set the placeholder on the target.
            val cached = null
            try {
                targetDelegate.metadata = null
                targetDelegate.start(cached)
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
            val result = executeChain(request, type, size, cached, eventListener)

            // Set the result on the target.
            when (result) {
                is SuccessResult -> onSuccess(result, targetDelegate, eventListener)
                is ErrorResult -> onError(result, targetDelegate, eventListener)
            }
            return result
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                onCancel(request, eventListener)
                throw throwable
            } else {
                // Create the default error result if there's an uncaught exception.
                val result = requestService.errorResult(request, throwable)
                onError(result, targetDelegate, eventListener)
                return result
            }
        } finally {
            requestDelegate.complete()
        }
    }

    /** Called by [SystemCallbacks.onTrimMemory]. */
    fun onTrimMemory(level: Int) {
        strongMemoryCache.trimMemory(level)
        weakMemoryCache.trimMemory(level)
    }

    override fun shutdown() {
        if (isShutdown.getAndSet(true)) return

        // Order is important.
        scope.cancel()
        systemCallbacks.shutdown()
        strongMemoryCache.clearMemory()
        weakMemoryCache.clearMemory()
    }

    private suspend inline fun executeChain(
        request: SVGARequest,
        type: Int,
        size: Size,
        cached: SVGAVideoEntity?,
        eventListener: EventListener
    ): SVGAResult {
        val chain =
            RealInterceptorChain(request, type, interceptors, 0, request, size, eventListener)
        return if (launchInterceptorChainOnMainThread) {
            chain.proceed(request)
        } else {
            withContext(request.dispatcher) {
                chain.proceed(request)
            }
        }
    }

    private suspend inline fun onSuccess(
        result: SuccessResult,
        targetDelegate: TargetDelegate,
        eventListener: EventListener
    ) {
        try {
            val request = result.request
            val metadata = result.metadata
            val dataSource = metadata.dataSource
            logger?.log(TAG, Log.INFO) { "Successful (${dataSource.name}) - ${request.data}" }
            targetDelegate.metadata = metadata
            targetDelegate.success(result)
            eventListener.onSuccess(request, metadata)
            request.listener?.onSuccess(request, metadata)
        } finally {
            referenceCounter.decrement(result.drawable)
        }
    }

    private suspend inline fun onError(
        result: ErrorResult,
        targetDelegate: TargetDelegate,
        eventListener: EventListener
    ) {
        val request = result.request
        logger?.log(TAG, Log.INFO) { "Failed - ${request.data} - ${result.throwable}" }
        targetDelegate.metadata = null
        targetDelegate.error(result)
        eventListener.onError(request, result.throwable)
        request.listener?.onError(request, result.throwable)
    }

    private fun onCancel(request: SVGARequest, eventListener: EventListener) {
        logger?.log(TAG, Log.INFO) { "Cancelled - ${request.data}" }
        eventListener.onCancel(request)
        request.listener?.onCancel(request)
    }

    companion object {
        private const val TAG = "RealImageLoader"
    }
}