@file:Suppress("unused")

package com.shark.svgaplayer_base.request

import android.content.Context
import android.net.Uri
import android.widget.ImageView.ScaleType.CENTER
import android.widget.ImageView.ScaleType.MATRIX
import androidx.annotation.MainThread
import androidx.annotation.Px
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.shark.svgaplayer_base.size.DisplaySizeResolver
import com.shark.svgaplayer_base.target.ViewTarget
import com.shark.svgaplayer_base.decode.Decoder
import com.shark.svgaplayer_base.fetch.Fetcher
import com.shark.svgaplayer_base.memory.MemoryCache
import com.shark.svgaplayer_base.size.*
import com.shark.svgaplayer_base.target.SVGAImageViewTarget
import com.shark.svgaplayer_base.transform.DynamicEntityBuilder
import com.shark.svgaplayer_base.util.getLifecycle
import com.shark.svgaplayer_base.util.orEmpty
import com.opensource.svgaplayer.SVGADrawable
import com.opensource.svgaplayer.SVGAImageView
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.Headers
import okhttp3.HttpUrl
import java.io.File
import com.shark.svgaplayer_base.target.Target
import com.shark.svgaplayer_base.util.DEFAULT_REQUEST_OPTIONS

/**
 * An immutable value object that represents a request for an image.
 *
 * @see ImageLoader.enqueue
 * @see ImageLoader.execute
 */
class SVGARequest private constructor(
    val context: Context,

    /** @see Builder.data */
    val data: Any,

    /** @see Builder.target */
    val target: Target?,

    /** @see Builder.listener */
    val listener: Listener?,

    /** @see Builder.memoryCacheKey */
    val memoryCacheKey: MemoryCache.Key?,

    /** @see Builder.diskCacheKey */
    val diskCacheKey: String?,

    /** @see Builder.fetcherFactory */
    val fetcherFactory: Pair<Fetcher.Factory<*>, Class<*>>?,

    /** @see Builder.decoderFactory */
    val decoderFactory: Decoder.Factory?,

    /** @see Builder.setDynamicEntity */
    val dynamicEntityBuilder: DynamicEntityBuilder?,

    /** @see Builder.headers */
    val headers: Headers,

    /** @see Builder.tags */
    val tags: Tags,

    /** @see Builder.parameters */
    val parameters: Parameters,

    /** @see Builder.lifecycle */
    val lifecycle: Lifecycle,

    /** @see Builder.sizeResolver */
    val sizeResolver: SizeResolver,

    /** @see Builder.interceptorDispatcher */
    val interceptorDispatcher: CoroutineDispatcher,

    /** @see Builder.fetcherDispatcher */
    val fetcherDispatcher: CoroutineDispatcher,

    /** @see Builder.decoderDispatcher */
    val decoderDispatcher: CoroutineDispatcher,

    /** @see Builder.precision */
    val precision: Precision,

    /** @see Builder.allowHardware */
    val allowHardware: Boolean,

    /** @see Builder.memoryCachePolicy */
    val memoryCachePolicy: CachePolicy,

    /** @see Builder.diskCachePolicy */
    val diskCachePolicy: CachePolicy,

    /** @see Builder.networkCachePolicy */
    val networkCachePolicy: CachePolicy,

    /** The raw values set on [Builder]. */
    val defined: DefinedRequestOptions,

    /** The defaults used to fill unset values. */
    val defaults: DefaultRequestOptions
) {
    @JvmOverloads
    fun newBuilder(context: Context = this.context) = Builder(this, context)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SVGARequest

        if (context != other.context) return false
        if (data != other.data) return false
        if (target != other.target) return false
        if (listener != other.listener) return false
        if (memoryCacheKey != other.memoryCacheKey) return false
        if (diskCacheKey != other.diskCacheKey) return false
        if (fetcherFactory != other.fetcherFactory) return false
        if (decoderFactory != other.decoderFactory) return false
        if (dynamicEntityBuilder != other.dynamicEntityBuilder) return false
        if (headers != other.headers) return false
        if (tags != other.tags) return false
        if (parameters != other.parameters) return false
        if (lifecycle != other.lifecycle) return false
        if (sizeResolver != other.sizeResolver) return false
        if (interceptorDispatcher != other.interceptorDispatcher) return false
        if (fetcherDispatcher != other.fetcherDispatcher) return false
        if (decoderDispatcher != other.decoderDispatcher) return false
        if (precision != other.precision) return false
        if (allowHardware != other.allowHardware) return false
        if (memoryCachePolicy != other.memoryCachePolicy) return false
        if (diskCachePolicy != other.diskCachePolicy) return false
        if (networkCachePolicy != other.networkCachePolicy) return false
        if (defined != other.defined) return false
        if (defaults != other.defaults) return false

        return true
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + (target?.hashCode() ?: 0)
        result = 31 * result + (listener?.hashCode() ?: 0)
        result = 31 * result + (memoryCacheKey?.hashCode() ?: 0)
        result = 31 * result + (diskCacheKey?.hashCode() ?: 0)
        result = 31 * result + (fetcherFactory?.hashCode() ?: 0)
        result = 31 * result + (decoderFactory?.hashCode() ?: 0)
        result = 31 * result + (dynamicEntityBuilder?.hashCode() ?: 0)
        result = 31 * result + headers.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + lifecycle.hashCode()
        result = 31 * result + sizeResolver.hashCode()
        result = 31 * result + interceptorDispatcher.hashCode()
        result = 31 * result + fetcherDispatcher.hashCode()
        result = 31 * result + decoderDispatcher.hashCode()
        result = 31 * result + precision.hashCode()
        result = 31 * result + allowHardware.hashCode()
        result = 31 * result + memoryCachePolicy.hashCode()
        result = 31 * result + diskCachePolicy.hashCode()
        result = 31 * result + networkCachePolicy.hashCode()
        result = 31 * result + defined.hashCode()
        result = 31 * result + defaults.hashCode()
        return result
    }

    override fun toString(): String {
        return "SVGARequest(context=$context, data=$data, target=$target, listener=$listener, memoryCacheKey=$memoryCacheKey, diskCacheKey=$diskCacheKey, fetcherFactory=$fetcherFactory, decoderFactory=$decoderFactory, dynamicEntityBuilder=$dynamicEntityBuilder, headers=$headers, tags=$tags, parameters=$parameters, lifecycle=$lifecycle, sizeResolver=$sizeResolver, interceptorDispatcher=$interceptorDispatcher, fetcherDispatcher=$fetcherDispatcher, decoderDispatcher=$decoderDispatcher, precision=$precision, allowHardware=$allowHardware, memoryCachePolicy=$memoryCachePolicy, diskCachePolicy=$diskCachePolicy, networkCachePolicy=$networkCachePolicy, defined=$defined, defaults=$defaults)"
    }


    /**
     * A set of callbacks for an [SVGARequest].
     */
    interface Listener {

        /**
         * Called immediately after [Target.onStart].
         */
        @MainThread
        fun onStart(request: SVGARequest) {
        }

        /**
         * Called if the request is cancelled.
         */
        @MainThread
        fun onCancel(request: SVGARequest) {
        }

        /**
         * Called if an error occurs while executing the request.
         */
        @MainThread
        fun onError(request: SVGARequest, throwable: Throwable) {
        }

        /**
         * Called if the request completes successfully.
         */
        @MainThread
        fun onSuccess(request: SVGARequest, metadata: SVGAResult.Metadata) {
        }
    }

    class Builder {
        private val context: Context
        private var defaults: DefaultRequestOptions
        private var data: Any?

        private var target: Target?
        private var listener: Listener?
        private var memoryCacheKey: MemoryCache.Key?
        private var diskCacheKey: String?

        private var fetcherFactory: Pair<Fetcher.Factory<*>, Class<*>>?
        private var decoderFactory: Decoder.Factory?
        private var dynamicEntityBuilder: DynamicEntityBuilder?

        private var headers: Headers.Builder?
        private var tags: MutableMap<Class<*>, Any>?

        private var parameters: Parameters.Builder?

        private var lifecycle: Lifecycle?
        private var sizeResolver: SizeResolver?

        private var precision: Precision?
        private var allowHardware: Boolean?
        private var memoryCachePolicy: CachePolicy?
        private var diskCachePolicy: CachePolicy?
        private var networkCachePolicy: CachePolicy?

        private var interceptorDispatcher: CoroutineDispatcher?
        private var fetcherDispatcher: CoroutineDispatcher?
        private var decoderDispatcher: CoroutineDispatcher?

        private var resolvedLifecycle: Lifecycle?
        private var resolvedSizeResolver: SizeResolver?

        constructor(context: Context) {
            this.context = context
            defaults = DEFAULT_REQUEST_OPTIONS
            data = null
            target = null
            listener = null
            memoryCacheKey = null
            diskCacheKey = null
            fetcherFactory = null
            decoderFactory = null
            dynamicEntityBuilder = null
            headers = null
            tags = null
            parameters = null
            lifecycle = null
            sizeResolver = null
            precision = null
            allowHardware = null
            memoryCachePolicy = null
            diskCachePolicy = null
            networkCachePolicy = null
            interceptorDispatcher = null
            fetcherDispatcher = null
            decoderDispatcher = null
            resolvedLifecycle = null
            resolvedSizeResolver = null
            interceptorDispatcher = null
            fetcherDispatcher = null
            decoderDispatcher = null
        }

        @JvmOverloads
        constructor(request: SVGARequest, context: Context = request.context) {
            this.context = context
            defaults = request.defaults
            data = request.data
            target = request.target
            listener = request.listener
            memoryCacheKey = request.memoryCacheKey
            diskCacheKey = request.diskCacheKey
            fetcherFactory = request.fetcherFactory
            decoderFactory = request.decoderFactory
            dynamicEntityBuilder = request.dynamicEntityBuilder
            headers = request.headers.newBuilder()
            tags = request.tags.asMap().toMutableMap()
            parameters = request.parameters.newBuilder()
            lifecycle = request.defined.lifecycle
            sizeResolver = request.defined.sizeResolver
            precision = request.defined.precision
            allowHardware = request.defined.allowHardware
            memoryCachePolicy = request.defined.memoryCachePolicy
            diskCachePolicy = request.defined.diskCachePolicy
            networkCachePolicy = request.defined.networkCachePolicy
            interceptorDispatcher = request.defined.interceptorDispatcher
            fetcherDispatcher = request.defined.fetcherDispatcher
            decoderDispatcher = request.defined.decoderDispatcher
            // If the context changes, recompute the resolved values.
            if (request.context === context) {
                resolvedLifecycle = request.lifecycle
                resolvedSizeResolver = request.sizeResolver
            } else {
                resolvedLifecycle = null
                resolvedSizeResolver = null
            }
        }

        /**
         * Set the data to load.
         *
         * The default supported data types are:
         * - [String] (mapped to a [Uri])
         * - [Uri] ("android.resource", "content", "file", "http", and "https" schemes only)
         * - [HttpUrl]
         * - [File]
         */
        fun data(data: Any?) = apply {
            this.data = data
        }

        /**
         * Set the memory cache key for this request.
         *
         * If this is null or is not set the [ImageLoader] will compute a memory cache key.
         */
        fun memoryCacheKey(key: String?) = memoryCacheKey(key?.let { MemoryCache.Key(it) })

        /**
         * Set the memory cache key for this request.
         *
         * If this is null or is not set the [ImageLoader] will compute a memory cache key.
         */
        fun memoryCacheKey(key: MemoryCache.Key?) = apply {
            this.memoryCacheKey = key
        }

        /**
         * Set the disk cache key for this request.
         *
         * If this is null or is not set, the [ImageLoader] will compute a disk cache key.
         */
        fun diskCacheKey(key: String?) = apply {
            this.diskCacheKey = key
        }

        /**
         * Convenience function to create and set the [Listener].
         */
        inline fun listener(
            crossinline onStart: (request: SVGARequest) -> Unit = {},
            crossinline onCancel: (request: SVGARequest) -> Unit = {},
            crossinline onError: (request: SVGARequest, throwable: Throwable) -> Unit = { _, _ -> },
            crossinline onSuccess: (request: SVGARequest, metadata: SVGAResult.Metadata) -> Unit = { _, _ -> }
        ) = listener(object : Listener {
            override fun onStart(request: SVGARequest) = onStart(request)
            override fun onCancel(request: SVGARequest) = onCancel(request)
            override fun onError(request: SVGARequest, throwable: Throwable) =
                onError(request, throwable)

            override fun onSuccess(request: SVGARequest, metadata: SVGAResult.Metadata) =
                onSuccess(request, metadata)
        })

        /**
         * Set the [Listener].
         */
        fun listener(listener: Listener?) = apply {
            this.listener = listener
        }

        /**
         * @see ImageLoader.Builder.dispatcher
         */
        fun dispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.fetcherDispatcher = dispatcher
            this.decoderDispatcher = dispatcher
        }

        /**
         * @see ImageLoader.Builder.interceptorDispatcher
         */
        fun interceptorDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.interceptorDispatcher = dispatcher
        }

        /**
         * @see ImageLoader.Builder.fetcherDispatcher
         */
        fun fetcherDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.fetcherDispatcher = dispatcher
        }

        /**
         * @see ImageLoader.Builder.decoderDispatcher
         */
        fun decoderDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.decoderDispatcher = dispatcher
        }


        /**
         * Set the requested width/height.
         */
        fun size(@Px size: Int) = size(size, size)

        /**
         * Set the requested width/height.
         */
        fun size(@Px width: Int, @Px height: Int) = size(Size(width, height))

        /**
         * Set the requested width/height.
         */
        fun size(size: Size) = size(SizeResolver(size))

        /**
         * Set the [SizeResolver] to resolve the requested width/height.
         */
        fun size(resolver: SizeResolver) = apply {
            this.sizeResolver = resolver
            resetResolvedValues()
        }

        /**
         * Set the precision for the size of the loaded image.
         *
         * The default value is [Precision.AUTOMATIC], which uses the logic in [allowInexactSize]
         * to determine if output image's dimensions must match the input [size] and [scale] exactly.
         *
         * NOTE: If [size] is [OriginalSize], the returned image's size will always be equal to or greater than
         * the image's original size.
         *
         * @see Precision
         */
        fun precision(precision: Precision) = apply {
            this.precision = precision
        }

        /**
         * Use [factory] to handle fetching any image data.
         *
         * If this is null or is not set the [ImageLoader] will find an applicable fetcher in its
         * [ComponentRegistry].
         */
        inline fun <reified T : Any> fetcherFactory(factory: Fetcher.Factory<T>) =
            fetcherFactory(factory, T::class.java)

        /**
         * Use [factory] to handle fetching any image data.
         *
         * If this is null or is not set the [ImageLoader] will find an applicable fetcher in its
         * [ComponentRegistry].
         */
        fun <T : Any> fetcherFactory(factory: Fetcher.Factory<T>, type: Class<T>) = apply {
            this.fetcherFactory = factory to type
        }

        /**
         * Use [factory] to handle decoding any image data.
         *
         * If this is null or is not set the [ImageLoader] will find an applicable decoder in its
         * [ComponentRegistry].
         */
        fun decoderFactory(factory: Decoder.Factory) = apply {
            this.decoderFactory = factory
        }

        fun setDynamicEntity(builder: DynamicEntityBuilder.() -> Unit) = apply {
            this.dynamicEntityBuilder = DynamicEntityBuilder().apply { builder() }
        }

        fun setDynamicEntity(builder: DynamicEntityBuilder?) = apply {
            this.dynamicEntityBuilder = builder
        }

        /**
         * @see ImageLoader.Builder.allowHardware
         */
        fun allowHardware(enable: Boolean) = apply {
            this.allowHardware = enable
        }

        /**
         * Enable/disable reading/writing from/to the memory cache.
         */
        fun memoryCachePolicy(policy: CachePolicy) = apply {
            this.memoryCachePolicy = policy
        }

        /**
         * Enable/disable reading/writing from/to the disk cache.
         */
        fun diskCachePolicy(policy: CachePolicy) = apply {
            this.diskCachePolicy = policy
        }

        /**
         * Enable/disable reading from the network.
         *
         * NOTE: Disabling writes has no effect.
         */
        fun networkCachePolicy(policy: CachePolicy) = apply {
            this.networkCachePolicy = policy
        }

        /**
         * Set the [Headers] for any network operations performed by this request.
         */
        fun headers(headers: Headers) = apply {
            this.headers = headers.newBuilder()
        }

        /**
         * Add a header for any network operations performed by this request.
         *
         * @see Headers.Builder.add
         */
        fun addHeader(name: String, value: String) = apply {
            this.headers = (this.headers ?: Headers.Builder()).add(name, value)
        }

        /**
         * Set a header for any network operations performed by this request.
         *
         * @see Headers.Builder.set
         */
        fun setHeader(name: String, value: String) = apply {
            this.headers = (this.headers ?: Headers.Builder()).set(name, value)
        }

        /**
         * Remove all network headers with the key [name].
         */
        fun removeHeader(name: String) = apply {
            this.headers = this.headers?.removeAll(name)
        }

        /**
         * Attach [tag] to this request using [T] as the key.
         */
        inline fun <reified T : Any> tag(tag: T?) = tag(T::class.java, tag)

        /**
         * Attach [tag] to this request using [type] as the key.
         */
        fun <T : Any> tag(type: Class<in T>, tag: T?) = apply {
            if (tag == null) {
                this.tags?.remove(type)
            } else {
                val tags = this.tags ?: mutableMapOf<Class<*>, Any>().also { this.tags = it }
                tags[type] = type.cast(tag)!!
            }
        }

        /**
         * Set the tags for this request.
         */
        fun tags(tags: Tags) = apply {
            this.tags = tags.asMap().toMutableMap()
        }


        /**
         * Set the parameters for this request.
         */
        fun parameters(parameters: Parameters) = apply {
            this.parameters = parameters.newBuilder()
        }

        /**
         * Set a parameter for this request.
         *
         * @see Parameters.Builder.set
         */
        @JvmOverloads
        fun setParameter(key: String, value: Any?, cacheKey: String? = value?.toString()) = apply {
            this.parameters =
                (this.parameters ?: Parameters.Builder()).apply { set(key, value, cacheKey) }
        }

        /**
         * Remove a parameter from this request.
         *
         * @see Parameters.Builder.remove
         */
        fun removeParameter(key: String) = apply {
            this.parameters?.remove(key)
        }

        /**
         * Convenience function to set [imageView] as the [Target].
         */
        fun target(imageView: SVGAImageView) = target(SVGAImageViewTarget(imageView))

        /**
         * Convenience function to create and set the [Target].
         */
        inline fun target(
            crossinline onStart: () -> Unit = {},
            crossinline onError: () -> Unit = {},
            crossinline onSuccess: (result: SVGADrawable) -> Unit = {}
        ) = target(object : Target {
            override fun onStart() = onStart()
            override fun onError() = onError()
            override fun onSuccess(result: SVGADrawable) = onSuccess(result)
        })

        /**
         * Set the [Target].
         */
        fun target(target: Target?) = apply {
            this.target = target
            resetResolvedValues()
        }

        /**
         * Set the [Lifecycle] for this request.
         */
        fun lifecycle(owner: LifecycleOwner?) = lifecycle(owner?.lifecycle)

        /**
         * Set the [Lifecycle] for this request.
         *
         * Requests are queued while the lifecycle is not at least [Lifecycle.State.STARTED].
         * Requests are cancelled when the lifecycle reaches [Lifecycle.State.DESTROYED].
         *
         * If this is null or is not set the [ImageLoader] will attempt to find the lifecycle
         * for this request through its [context].
         */
        fun lifecycle(lifecycle: Lifecycle?) = apply {
            this.lifecycle = lifecycle
        }

        /**
         * Set the defaults for any unset request values.
         */
        fun defaults(defaults: DefaultRequestOptions) = apply {
            this.defaults = defaults
            resetResolvedScale()
        }

        /**
         * Create a new [SVGARequest].
         */
        fun build(): SVGARequest {
            return SVGARequest(
                context = context,
                data = data ?: NullRequestData,
                target = target,
                listener = listener,
                memoryCacheKey = memoryCacheKey,
                diskCacheKey = diskCacheKey,
                fetcherFactory = fetcherFactory,
                decoderFactory = decoderFactory,
                dynamicEntityBuilder = dynamicEntityBuilder,
                headers = headers?.build().orEmpty(),
                tags = tags?.let(Tags::from).orEmpty(),
                parameters = parameters?.build().orEmpty(),
                lifecycle = lifecycle ?: resolvedLifecycle ?: resolveLifecycle(),
                sizeResolver = sizeResolver ?: resolvedSizeResolver ?: resolveSizeResolver(),
                interceptorDispatcher = interceptorDispatcher ?: defaults.interceptorDispatcher,
                fetcherDispatcher = fetcherDispatcher ?: defaults.fetcherDispatcher,
                decoderDispatcher = decoderDispatcher ?: defaults.decoderDispatcher,
                precision = precision ?: defaults.precision,
                allowHardware = allowHardware ?: defaults.allowHardware,
                memoryCachePolicy = memoryCachePolicy ?: defaults.memoryCachePolicy,
                diskCachePolicy = diskCachePolicy ?: defaults.diskCachePolicy,
                networkCachePolicy = networkCachePolicy ?: defaults.networkCachePolicy,
                defined = DefinedRequestOptions(
                    lifecycle,
                    sizeResolver,
                    interceptorDispatcher,
                    fetcherDispatcher,
                    decoderDispatcher,
                    precision,
                    allowHardware,
                    memoryCachePolicy,
                    diskCachePolicy,
                    networkCachePolicy
                ),
                defaults = defaults,
            )
        }

        /** Ensure these values will be recomputed when [build] is called. */
        private fun resetResolvedValues() {
            resolvedLifecycle = null
            resolvedSizeResolver = null
        }

        /** Ensure the scale will be recomputed when [build] is called. */
        private fun resetResolvedScale() {
        }

        private fun resolveLifecycle(): Lifecycle {
            val target = target
            val context = if (target is ViewTarget<*>) target.view.context else context
            return context.getLifecycle() ?: GlobalLifecycle
        }

        private fun resolveSizeResolver(): SizeResolver {
            val target = target
            return if (target is SVGAImageViewTarget && target.view.scaleType.let { it == CENTER || it == MATRIX }) {
                SizeResolver(OriginalSize)
            } else if (target is ViewTarget<*>) {
                ViewSizeResolver(target.view)
            } else {
                DisplaySizeResolver(context)
            }
        }
    }
}
