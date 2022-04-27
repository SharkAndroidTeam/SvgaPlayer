@file:Suppress("unused")

package com.shark.svgaplayer_base.request

import com.shark.svgaplayer_base.request.CachePolicy
import com.shark.svgaplayer_base.size.Precision
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class DefaultRequestOptions(
    val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val precision: Precision = Precision.AUTOMATIC,
    val allowHardware: Boolean = true,
    val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val networkCachePolicy: CachePolicy = CachePolicy.ENABLED
) {

    fun copy(
        dispatcher: CoroutineDispatcher = this.dispatcher,
        precision: Precision = this.precision,
        allowHardware: Boolean = this.allowHardware,
        memoryCachePolicy: CachePolicy = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy = this.diskCachePolicy,
        networkCachePolicy: CachePolicy = this.networkCachePolicy
    ) = DefaultRequestOptions(
        dispatcher,
        precision,
        allowHardware,
        memoryCachePolicy,
        diskCachePolicy,
        networkCachePolicy
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is DefaultRequestOptions &&
                dispatcher == other.dispatcher &&
                precision == other.precision &&
                allowHardware == other.allowHardware &&
                memoryCachePolicy == other.memoryCachePolicy &&
                diskCachePolicy == other.diskCachePolicy &&
                networkCachePolicy == other.networkCachePolicy
    }

    override fun hashCode(): Int {
        var result = dispatcher.hashCode()
        result = 31 * result + precision.hashCode()
        result = 31 * result + allowHardware.hashCode()
        result = 31 * result + memoryCachePolicy.hashCode()
        result = 31 * result + diskCachePolicy.hashCode()
        result = 31 * result + networkCachePolicy.hashCode()
        return result
    }

    override fun toString(): String {
        return "DefaultRequestOptions(dispatcher=$dispatcher, precision=$precision, " +
                "allowHardware=$allowHardware, memoryCachePolicy=$memoryCachePolicy, " +
                "diskCachePolicy=$diskCachePolicy, networkCachePolicy=$networkCachePolicy)"
    }


    companion object {
        @JvmField
        val INSTANCE = DefaultRequestOptions()
    }
}
