@file:Suppress("unused")

package com.shark.svgaplayer_base.request

import android.graphics.drawable.Drawable
import com.shark.svgaplayer_base.size.Precision
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class DefaultRequestOptions(
    val interceptorDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    val fetcherDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val decoderDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val precision: Precision = Precision.AUTOMATIC,
    val allowHardware: Boolean = true,
    val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val networkCachePolicy: CachePolicy = CachePolicy.ENABLED,
) {

    fun copy(
        interceptorDispatcher: CoroutineDispatcher = this.interceptorDispatcher,
        fetcherDispatcher: CoroutineDispatcher = this.fetcherDispatcher,
        decoderDispatcher: CoroutineDispatcher = this.decoderDispatcher,
        precision: Precision = this.precision,
        allowHardware: Boolean = this.allowHardware,
        memoryCachePolicy: CachePolicy = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy = this.diskCachePolicy,
        networkCachePolicy: CachePolicy = this.networkCachePolicy,
    ) = DefaultRequestOptions(
        interceptorDispatcher = interceptorDispatcher,
        fetcherDispatcher = fetcherDispatcher,
        decoderDispatcher = decoderDispatcher,
        precision = precision,
        allowHardware = allowHardware,
        memoryCachePolicy = memoryCachePolicy,
        diskCachePolicy = diskCachePolicy,
        networkCachePolicy = networkCachePolicy,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is DefaultRequestOptions &&
                interceptorDispatcher == other.interceptorDispatcher &&
                fetcherDispatcher == other.fetcherDispatcher &&
                decoderDispatcher == other.decoderDispatcher &&
                precision == other.precision &&
                allowHardware == other.allowHardware &&
                memoryCachePolicy == other.memoryCachePolicy &&
                diskCachePolicy == other.diskCachePolicy &&
                networkCachePolicy == other.networkCachePolicy
    }

    override fun hashCode(): Int {
        var result = interceptorDispatcher.hashCode()
        result = 31 * result + fetcherDispatcher.hashCode()
        result = 31 * result + decoderDispatcher.hashCode()
        result = 31 * result + precision.hashCode()
        result = 31 * result + allowHardware.hashCode()
        result = 31 * result + memoryCachePolicy.hashCode()
        result = 31 * result + diskCachePolicy.hashCode()
        result = 31 * result + networkCachePolicy.hashCode()
        return result
    }
}
