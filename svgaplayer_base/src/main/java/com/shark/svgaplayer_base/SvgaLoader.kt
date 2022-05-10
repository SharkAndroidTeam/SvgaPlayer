package com.shark.svgaplayer_base

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import com.shark.svgaplayer_base.disk.DiskCache
import com.shark.svgaplayer_base.memory.EmptyWeakMemoryCache
import com.shark.svgaplayer_base.memory.MemoryCache
import com.shark.svgaplayer_base.memory.RealWeakMemoryCache
import com.shark.svgaplayer_base.memory.StrongMemoryCache
import com.shark.svgaplayer_base.recycle.RealVideoEntityRefCounter
import com.shark.svgaplayer_base.request.CachePolicy
import com.shark.svgaplayer_base.request.*
import com.shark.svgaplayer_base.size.Precision
import com.shark.svgaplayer_base.util.*
import com.shark.svgaplayer_base.util.SingletonDiskCache
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
     * The components used to fulfil image requests.
     */
    val components: ComponentRegistry

    /**
     * An in-memory cache of recently loaded images.
     */
    val memoryCache: MemoryCache?

    /**
     * An on-disk cache of previously loaded images.
     */
    val diskCache: DiskCache?

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

    fun newBuilder(): Builder

    class Builder {

        private val applicationContext: Context
        private var defaults: DefaultRequestOptions
        private var memoryCache: Lazy<MemoryCache?>?
        private var diskCache: Lazy<DiskCache?>?
        private var callFactory: Lazy<Call.Factory>?
        private var eventListenerFactory: EventListener.Factory?
        private var componentRegistry: ComponentRegistry?
        private var options: SVGALoaderOptions
        private var logger: Logger?

        constructor(context: Context) {
            applicationContext = context.applicationContext
            defaults = DEFAULT_REQUEST_OPTIONS
            memoryCache = null
            diskCache = null
            callFactory = null
            eventListenerFactory = null
            componentRegistry = null
            options = SVGALoaderOptions()
            logger = null
        }

        internal constructor(imageLoader: RealSvgaLoader) {
            applicationContext = imageLoader.context.applicationContext
            defaults = imageLoader.defaults
            memoryCache = imageLoader.memoryCacheLazy
            diskCache = imageLoader.diskCacheLazy
            callFactory = imageLoader.callFactoryLazy
            eventListenerFactory = imageLoader.eventListenerFactory
            componentRegistry = imageLoader.componentRegistry
            options = imageLoader.options
            logger = imageLoader.logger
        }

        /**
         * Set the [OkHttpClient] used for network requests.
         *
         * This is a convenience function for calling `callFactory(Call.Factory)`.
         */
        fun okHttpClient(okHttpClient: OkHttpClient) = callFactory(okHttpClient)

        /**
         * Set a lazy callback to create the [OkHttpClient] used for network requests.
         *
         * This allows lazy creation of the [OkHttpClient] on a background thread.
         * [initializer] is guaranteed to be called at most once.
         *
         * Prefer using this instead of `okHttpClient(OkHttpClient)`.
         *
         * This is a convenience function for calling `callFactory(() -> Call.Factory)`.
         */
        fun okHttpClient(initializer: () -> OkHttpClient) = callFactory(initializer)

        /**
         * Set the [Call.Factory] used for network requests.
         */
        fun callFactory(callFactory: Call.Factory) = apply {
            this.callFactory = lazyOf(callFactory)
        }

        /**
         * Set a lazy callback to create the [Call.Factory] used for network requests.
         *
         * This allows lazy creation of the [Call.Factory] on a background thread.
         * [initializer] is guaranteed to be called at most once.
         *
         * Prefer using this instead of `callFactory(Call.Factory)`.
         */
        fun callFactory(initializer: () -> Call.Factory) = apply {
            this.callFactory = lazy(initializer)
        }

        /**
         * Build and set the [ComponentRegistry].
         */
        @JvmSynthetic
        inline fun components(
            builder: ComponentRegistry.Builder.() -> Unit
        ) = components(ComponentRegistry.Builder().apply(builder).build())

        /**
         * Set the [ComponentRegistry].
         */
        fun components(components: ComponentRegistry) = apply {
            this.componentRegistry = components
        }

        /**
         * Set the [MemoryCache].
         */
        fun memoryCache(memoryCache: MemoryCache?) = apply {
            this.memoryCache = lazyOf(memoryCache)
        }

        /**
         * Set a lazy callback to create the [MemoryCache].
         *
         * Prefer using this instead of `memoryCache(MemoryCache)`.
         */
        fun memoryCache(initializer: () -> MemoryCache?) = apply {
            this.memoryCache = lazy(initializer)
        }

        /**
         * Set the [DiskCache].
         *
         * NOTE: By default, [ImageLoader]s share the same disk cache instance. This is necessary
         * as having multiple disk cache instances active in the same directory at the same time
         * can corrupt the disk cache.
         *
         * @see DiskCache.directory
         */
        fun diskCache(diskCache: DiskCache?) = apply {
            this.diskCache = lazyOf(diskCache)
        }

        /**
         * Set a lazy callback to create the [DiskCache].
         *
         * Prefer using this instead of `diskCache(DiskCache)`.
         *
         * NOTE: By default, [ImageLoader]s share the same disk cache instance. This is necessary
         * as having multiple disk cache instances active in the same directory at the same time
         * can corrupt the disk cache.
         *
         * @see DiskCache.directory
         */
        fun diskCache(initializer: () -> DiskCache?) = apply {
            this.diskCache = lazy(initializer)
        }

        /**
         * Allow the use of [Bitmap.Config.HARDWARE].
         *
         * If false, any use of [Bitmap.Config.HARDWARE] will be treated as
         * [Bitmap.Config.ARGB_8888].
         *
         * NOTE: Setting this to false this will reduce performance on API 26 and above. Only
         * disable this if necessary.
         *
         * Default: true
         */
        fun allowHardware(enable: Boolean) = apply {
            this.defaults = this.defaults.copy(allowHardware = enable)
        }


        /**
         * Enables adding [File.lastModified] to the memory cache key when loading an image from a
         * [File].
         *
         * This allows subsequent requests that load the same file to miss the memory cache if the
         * file has been updated. However, if the memory cache check occurs on the main thread
         * (see [interceptorDispatcher]) calling [File.lastModified] will cause a strict mode
         * violation.
         *
         * Default: true
         */
        fun addLastModifiedToFileCacheKey(enable: Boolean) = apply {
            this.options = this.options.copy(addLastModifiedToFileCacheKey = enable)
        }

        /**
         * Enables short circuiting network requests if the device is offline.
         *
         * If true, reading from the network will automatically be disabled if the device is
         * offline. If a cached response is unavailable the request will fail with a
         * '504 Unsatisfiable Request' response.
         *
         * If false, the image loader will attempt a network request even if the device is offline.
         *
         * Default: true
         */
        fun networkObserverEnabled(enable: Boolean) = apply {
            this.options = this.options.copy(networkObserverEnabled = enable)
        }

        /**
         * Enables support for network cache headers. If enabled, this image loader will respect the
         * cache headers returned by network responses when deciding if an image can be stored or
         * served from the disk cache. If disabled, images will always be served from the disk cache
         * (if present) and will only be evicted to stay under the maximum size.
         *
         * Default: true
         */
        fun respectCacheHeaders(enable: Boolean) = apply {
            this.options = this.options.copy(respectCacheHeaders = enable)
        }

        /**
         * Sets the maximum number of parallel [BitmapFactory] decode operations at once.
         *
         * Increasing this number will allow more parallel [BitmapFactory] decode operations,
         * however it can result in worse UI performance.
         *
         * Default: 4
         */
        fun bitmapFactoryMaxParallelism(maxParallelism: Int) = apply {
            require(maxParallelism > 0) { "maxParallelism must be > 0." }
            this.options = this.options.copy(bitmapFactoryMaxParallelism = maxParallelism)
        }

        /**
         * Set a single [EventListener] that will receive all callbacks for requests launched by
         * this image loader.
         *
         * @see eventListenerFactory
         */
        fun eventListener(listener: EventListener) = eventListenerFactory { listener }

        /**
         * Set the [EventListener.Factory] to create per-request [EventListener]s.
         */
        fun eventListenerFactory(factory: EventListener.Factory) = apply {
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
         * A convenience function to set [fetcherDispatcher], [decoderDispatcher], and
         * [transformationDispatcher] in one call.
         */
        fun dispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(
                fetcherDispatcher = dispatcher,
                decoderDispatcher = dispatcher,
            )
        }

        /**
         * The [CoroutineDispatcher] that the [Interceptor] chain will be executed on.
         *
         * Default: `Dispatchers.Main.immediate`
         */
        fun interceptorDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(interceptorDispatcher = dispatcher)
        }

        /**
         * The [CoroutineDispatcher] that [Fetcher.fetch] will be executed on.
         *
         * Default: [Dispatchers.IO]
         */
        fun fetcherDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(fetcherDispatcher = dispatcher)
        }

        /**
         * The [CoroutineDispatcher] that [Decoder.decode] will be executed on.
         *
         * Default: [Dispatchers.IO]
         */
        fun decoderDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.defaults = this.defaults.copy(decoderDispatcher = dispatcher)
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
         * Create a new [ImageLoader] instance.
         */
        fun build(): SvgaLoader {
            return RealSvgaLoader(
                context = applicationContext,
                defaults = defaults,
                memoryCacheLazy = memoryCache ?: lazy { MemoryCache.Builder(applicationContext).build() },
                diskCacheLazy = diskCache ?: lazy { SingletonDiskCache.get(applicationContext) },
                callFactoryLazy = callFactory ?: lazy { OkHttpClient() },
                eventListenerFactory = eventListenerFactory ?: EventListener.Factory.NONE,
                componentRegistry = componentRegistry ?: ComponentRegistry(),
                options = options,
                logger = logger
            )
        }
    }

    companion object {
        /** Create a new [SvgaLoader] without configuration. */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(context: Context) = Builder(context).build()
    }
}