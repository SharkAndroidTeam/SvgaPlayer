@file:Suppress("unused")

package com.shark.svgaplayer_base.request

import androidx.lifecycle.Lifecycle
import com.shark.svgaplayer_base.request.CachePolicy
import com.shark.svgaplayer_base.size.Precision
import com.shark.svgaplayer_base.size.SizeResolver
import kotlinx.coroutines.CoroutineDispatcher

class DefinedRequestOptions(
    val lifecycle: Lifecycle?,
    val sizeResolver: SizeResolver?,
    val interceptorDispatcher: CoroutineDispatcher?,
    val fetcherDispatcher: CoroutineDispatcher?,
    val decoderDispatcher: CoroutineDispatcher?,
    val precision: Precision?,
    val allowHardware: Boolean?,
    val memoryCachePolicy: CachePolicy?,
    val diskCachePolicy: CachePolicy?,
    val networkCachePolicy: CachePolicy?
) {

    fun copy(
        lifecycle: Lifecycle? = this.lifecycle,
        sizeResolver: SizeResolver? = this.sizeResolver,
        interceptorDispatcher: CoroutineDispatcher? = this.interceptorDispatcher,
        fetcherDispatcher: CoroutineDispatcher? = this.fetcherDispatcher,
        decoderDispatcher: CoroutineDispatcher? = this.decoderDispatcher,
        precision: Precision? = this.precision,
        allowHardware: Boolean? = this.allowHardware,
        memoryCachePolicy: CachePolicy? = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy? = this.diskCachePolicy,
        networkCachePolicy: CachePolicy? = this.networkCachePolicy
    ) = DefinedRequestOptions(
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
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DefinedRequestOptions

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

        return true
    }

    override fun hashCode(): Int {
        var result = lifecycle?.hashCode() ?: 0
        result = 31 * result + (sizeResolver?.hashCode() ?: 0)
        result = 31 * result + (interceptorDispatcher?.hashCode() ?: 0)
        result = 31 * result + (fetcherDispatcher?.hashCode() ?: 0)
        result = 31 * result + (decoderDispatcher?.hashCode() ?: 0)
        result = 31 * result + (precision?.hashCode() ?: 0)
        result = 31 * result + (allowHardware?.hashCode() ?: 0)
        result = 31 * result + (memoryCachePolicy?.hashCode() ?: 0)
        result = 31 * result + (diskCachePolicy?.hashCode() ?: 0)
        result = 31 * result + (networkCachePolicy?.hashCode() ?: 0)
        return result
    }


}
