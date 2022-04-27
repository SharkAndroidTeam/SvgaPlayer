package com.shark.svgaplayer_base

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.FloatRange
import com.shark.svgaplayer_base.memory.EmptyWeakMemoryCache
import com.shark.svgaplayer_base.memory.MemoryCache
import com.shark.svgaplayer_base.memory.RealWeakMemoryCache
import com.shark.svgaplayer_base.memory.StrongMemoryCache
import com.shark.svgaplayer_base.recycle.RealVideoEntityRefCounter
import com.shark.svgaplayer_base.request.CachePolicy
import com.shark.svgaplayer_base.request.*
import com.shark.svgaplayer_base.size.Precision
import com.shark.svgaplayer_base.util.DefaultLogger
import com.shark.svgaplayer_base.util.Logger
import com.shark.svgaplayer_base.util.Utils
import com.shark.svgaplayer_base.util.lazyCallFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import java.io.File

/**

 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
interface SvgaLoader {

    /**
     * The default options that are used to fill in unset [SVGARequest] values.
     */
    val defaults: DefaultRequestOptions

    /**
     * An in-memory cache of recently loaded images.
     */
    val memoryCache: MemoryCache

    /**
     * Enqueue the [request] to be executed asynchronously.
     *
     * @param request The request to execute.
     * @return A [Disposable] which can be used to cancel or check the status of the request.
     */
    fun enqueue(request: SVGARequest): Disposable

    /**
     * Execute the [request] in the current coroutine scope.
     *
     * NOTE: If [SVGARequest.target] is a [ViewTarget], the job will automatically be cancelled
     * if its view is detached.
     *
     * @param request The request to execute.
     * @return A [SuccessResult] if the request completes successfully. Else, returns an [ErrorResult].
     */
    suspend fun execute(request: SVGARequest): SVGAResult

    /**
     * Shutdown this image loader.
     *
     * All associated resources will be freed and any new requests will fail before starting.
     *
     * In progress [enqueue] requests will be cancelled immediately.
     * In progress [execute] requests will continue until complete.
     */
    fun shutdown()

    class Builder(context: Context) {
        private val applicationContext = context.applicationContext

        private var callFactory: Call.Factory? = null
        private var eventListenerFactory: EventListener.Factory? = null
        private var registry: ComponentRegistry? = null
        private var logger: Logger? = DefaultLogger()
        private var defaults = DefaultRequestOptions.INSTANCE

        private var availableMemoryPercentage = Utils.getDefaultAvailableMemoryPercentage(applicationContext)
        private var addLastModifiedToFileCacheKey = true
        private var launchInterceptorChainOnMainThread = true
        private var trackWeakReferences = true

        /**
         * Set the [OkHttpClient] used for network requests.
         *
         * This is a convenience function for calling `callFactory(Call.Factory)`.
         *
         * NOTE: You must set [OkHttpClient.cache] to enable disk caching. A default
         * Coil disk cache instance can be created using [Utils.createDefaultCache].
         */
        fun okHttpClient(okHttpClient: OkHttpClient) = callFactory(okHttpClient)

        /**
         * Set a lazy callback to create the [OkHttpClient] used for network requests.
         *
         * This is a convenience function for calling `callFactory(() -> Call.Factory)`.
         *
         * NOTE: You must set [OkHttpClient.cache] to enable disk caching. A default
         * Coil disk cache instance can be created using [Utils.createDefaultCache].
         */
        fun okHttpClient(initializer: () -> OkHttpClient) = callFactory(initializer)

        /**
         * Set the [Call.Factory] used for network requests.
         *
         * Calling [okHttpClient] automatically sets this value.
         *
         * NOTE: You must set [OkHttpClient.cache] to enable disk caching. A default
         * Coil disk cache instance can be created using [Utils.createDefaultCache].
         */
        fun callFactory(callFactory: Call.Factory) = apply {
            this.callFactory = callFactory
        }

        /**
         * Set a lazy callback to create the [Call.Factory] used for network requests.
         *
         * This allows lazy creation of the [Call.Factory] on a background thread.
         * [initializer] is guaranteed to be called at most once.
         *
         * Prefer using this instead of `callFactory(Call.Factory)`.
         *
         * Calling [okHttpClient] automatically sets this value.
         *
         * NOTE: You must set [OkHttpClient.cache] to enable disk caching. A default
         * Coil disk cache instance can be created using [Utils.createDefaultCache].
         */
        fun callFactory(initializer: () -> Call.Factory) = apply {
            this.callFactory = lazyCallFactory(initializer)
        }

        /**
         * Build and set the [ComponentRegistry].
         */
        @JvmSynthetic
        inline fun componentRegistry(
            builder: ComponentRegistry.Builder.() -> Unit
        ) = componentRegistry(ComponentRegistry.Builder().apply(builder).build())

        /**
         * Set the [ComponentRegistry].
         */
        fun componentRegistry(registry: ComponentRegistry) = apply {
            this.registry = registry
        }

        /**
         * Set the percentage of available memory to devote to this [SvgaLoader]'s memory cache and bitmap pool.
         *
         * Setting this to 0 disables memory caching and bitmap pooling.
         *
         * Default: [Utils.getDefaultAvailableMemoryPercentage]
         */
        fun availableMemoryPercentage(@FloatRange(from = 0.0, to = 1.0) percent: Double) = apply {
            require(percent in 0.0..1.0) { "Percent must be in the range [0.0, 1.0]." }
            this.availableMemoryPercentage = percent
        }

        /**
         * The default [CoroutineDispatcher] to run image requests on.
         *
         * Default: [Dispatchers.IO]
         */
        fun dispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(dispatcher = dispatcher)
        }

        /**
         * Allow the use of [Bitmap.Config.HARDWARE].
         *
         * If false, any use of [Bitmap.Config.HARDWARE] will be treated as [Bitmap.Config.ARGB_8888].
         *
         * NOTE: Setting this to false this will reduce performance on API 26 and above. Only disable if necessary.
         *
         * Default: true
         */
        fun allowHardware(enable: Boolean) = apply {
            this.defaults = this.defaults.copy(allowHardware = enable)
        }

        /**
         * Enables adding [File.lastModified] to the memory cache key when loading an image from a [File].
         *
         * This allows subsequent requests that load the same file to miss the memory cache if the file has been updated.
         * However, if the memory cache check occurs on the main thread (see [launchInterceptorChainOnMainThread])
         * calling [File.lastModified] will cause a strict mode violation.
         *
         * Default: true
         */
        fun addLastModifiedToFileCacheKey(enable: Boolean) = apply {
            this.addLastModifiedToFileCacheKey = enable
        }

        /**
         * Enables launching the [Interceptor] chain on the main thread.
         *
         * If true, the [Interceptor] chain will be launched from [MainCoroutineDispatcher.immediate]. This allows
         * the [ImageLoader] to check its memory cache and return a cached value synchronously if the request is
         * started from the main thread. However, [Mapper.map] and [Fetcher.key] operations will be executed on the
         * main thread as well, which has a performance cost.
         *
         * If false, the [Interceptor] chain will be launched from the request's [SVGARequest.dispatcher].
         * This will result in better UI performance, but values from the memory cache will not be resolved
         * synchronously.
         *
         * The actual fetch + decode process always occurs on [SVGARequest.dispatcher] and is unaffected by this flag.
         *
         * It's worth noting that [Interceptor]s can also control which [CoroutineDispatcher] the
         * memory cache is checked on by calling [Interceptor.Chain.proceed] inside a [withContext] block.
         * Therefore if you set [launchInterceptorChainOnMainThread] to true, you can control which [SVGARequest]s
         * check the memory cache synchronously at runtime.
         *
         * Default: true
         */
        fun launchInterceptorChainOnMainThread(enable: Boolean) = apply {
            this.launchInterceptorChainOnMainThread = enable
        }

        /**
         * Enables weak reference tracking of loaded images.
         *
         * This allows the image loader to hold weak references to loaded images.
         * This ensures that if an image is still in memory it will be returned from the memory cache.
         *
         * Default: true
         */
        fun trackWeakReferences(enable: Boolean) = apply {
            this.trackWeakReferences = enable
        }

        /**
         * Set a single [EventListener] that will receive all callbacks for requests launched by this image loader.
         */
        fun eventListener(listener: EventListener) = eventListener(EventListener.Factory(listener))

        /**
         * Set the [EventListener.Factory] to create per-request [EventListener]s.
         *
         * @see eventListener
         */
        fun eventListener(factory: EventListener.Factory) = apply {
            this.eventListenerFactory = factory
        }

        /**
         * Set the default precision for a request. [Precision] controls whether the size of the
         * loaded image must match the request's size exactly or not.
         *
         * Default: [Precision.AUTOMATIC]
         */
        fun precision(precision: Precision) = apply {
            this.defaults = this.defaults.copy(precision = precision)
        }

        /**
         * Set the default memory cache policy.
         */
        fun memoryCachePolicy(policy: CachePolicy) = apply {
            this.defaults = this.defaults.copy(memoryCachePolicy = policy)
        }

        /**
         * Set the default disk cache policy.
         */
        fun diskCachePolicy(policy: CachePolicy) = apply {
            this.defaults = this.defaults.copy(diskCachePolicy = policy)
        }

        /**
         * Set the default network cache policy.
         *
         * NOTE: Disabling writes has no effect.
         */
        fun networkCachePolicy(policy: CachePolicy) = apply {
            this.defaults = this.defaults.copy(networkCachePolicy = policy)
        }

        /**
         * Set the [Logger] to write logs to.
         *
         * NOTE: Setting a [Logger] can reduce performance and should be avoided in release builds.
         */
        fun logger(logger: Logger?) = apply {
            this.logger = logger
        }

        /**
         * Create a new [SvgaLoader] instance.
         */
        fun build(): SvgaLoader {
            val availableMemorySize = Utils.calculateAvailableMemorySize(applicationContext, availableMemoryPercentage)

            val weakMemoryCache = if (trackWeakReferences) {
                RealWeakMemoryCache(logger)
            } else {
                EmptyWeakMemoryCache
            }
            val referenceCounter = RealVideoEntityRefCounter(weakMemoryCache, logger)
            val strongMemoryCache = StrongMemoryCache(weakMemoryCache, referenceCounter, availableMemorySize.toInt(), logger)

            return RealSvgaLoader(
                context = applicationContext,
                defaults = defaults,
                referenceCounter = referenceCounter,
                strongMemoryCache = strongMemoryCache,
                weakMemoryCache = weakMemoryCache,
                callFactory = callFactory ?: buildDefaultCallFactory(),
                eventListenerFactory = eventListenerFactory ?: EventListener.Factory.NONE,
                componentRegistry = registry ?: ComponentRegistry(),
                addLastModifiedToFileCacheKey = addLastModifiedToFileCacheKey,
                launchInterceptorChainOnMainThread = launchInterceptorChainOnMainThread,
                logger = logger
            )
        }

        private fun buildDefaultCallFactory() = lazyCallFactory {
            OkHttpClient.Builder()
                .cache(Utils.createDefaultCache(applicationContext))
                .build()
        }
    }

    companion object {
        /** Create a new [SvgaLoader] without configuration. */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(context: Context) = Builder(context).build()
    }
}