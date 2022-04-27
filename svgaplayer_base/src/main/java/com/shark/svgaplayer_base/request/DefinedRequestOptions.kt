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
    val dispatcher: CoroutineDispatcher?,
    val precision: Precision?,
    val allowHardware: Boolean?,
    val memoryCachePolicy: CachePolicy?,
    val diskCachePolicy: CachePolicy?,
    val networkCachePolicy: CachePolicy?
) {

    fun copy(
        lifecycle: Lifecycle? = this.lifecycle,
        sizeResolver: SizeResolver? = this.sizeResolver,
        dispatcher: CoroutineDispatcher? = this.dispatcher,
        precision: Precision? = this.precision,
        allowHardware: Boolean? = this.allowHardware,
        memoryCachePolicy: CachePolicy? = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy? = this.diskCachePolicy,
        networkCachePolicy: CachePolicy? = this.networkCachePolicy
    ) = DefinedRequestOptions(
        lifecycle, sizeResolver, dispatcher, precision, allowHardware, memoryCachePolicy,
        diskCachePolicy, networkCachePolicy
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is DefinedRequestOptions &&
                lifecycle == other.lifecycle &&
                sizeResolver == other.sizeResolver &&
                dispatcher == other.dispatcher &&
                precision == other.precision &&
                allowHardware == other.allowHardware &&
                memoryCachePolicy == other.memoryCachePolicy &&
                diskCachePolicy == other.diskCachePolicy &&
                networkCachePolicy == other.networkCachePolicy
    }

    override fun hashCode(): Int {
        var result = lifecycle?.hashCode() ?: 0
        result = 31 * result + (sizeResolver?.hashCode() ?: 0)
        result = 31 * result + (dispatcher?.hashCode() ?: 0)
        result = 31 * result + (precision?.hashCode() ?: 0)
        result = 31 * result + (allowHardware?.hashCode() ?: 0)
        result = 31 * result + (memoryCachePolicy?.hashCode() ?: 0)
        result = 31 * result + (diskCachePolicy?.hashCode() ?: 0)
        result = 31 * result + (networkCachePolicy?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "DefinedRequestOptions(lifecycle=$lifecycle, sizeResolver=$sizeResolver, " +
                "dispatcher=$dispatcher, precision=$precision, allowHardware=$allowHardware, " +
                "memoryCachePolicy=$memoryCachePolicy, diskCachePolicy=$diskCachePolicy, " +
                "networkCachePolicy=$networkCachePolicy)"
    }

}
