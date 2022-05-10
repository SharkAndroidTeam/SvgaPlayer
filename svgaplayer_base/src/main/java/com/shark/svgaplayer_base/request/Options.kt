package com.shark.svgaplayer_base.request

import android.content.Context
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.util.EMPTY_HEADERS
import okhttp3.Headers

/**

 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
class Options(
    val context: Context,
    val allowInexactSize: Boolean = false,

    val headers: Headers = EMPTY_HEADERS,

    /**
     * A map of custom objects. These are used to attach custom data to a request.
     */
    val tags: Tags = Tags.EMPTY,
    val parameters: Parameters = Parameters.EMPTY,
    val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val networkCachePolicy: CachePolicy = CachePolicy.ENABLED,
    /**
     * The requested output size for the image request.
     */
    val size: Size = Size.ORIGINAL,
    /**
     * The cache key to use when persisting images to the disk cache or 'null' if the component can
     * compute its own.
     */
    val diskCacheKey: String? = null,

    ) {
    fun copy(
        context: Context = this.context,
        allowInexactSize: Boolean = this.allowInexactSize,
        size: Size = this.size,
        headers: Headers = this.headers,
        tags: Tags = this.tags,
        parameters: Parameters = this.parameters,
        memoryCachePolicy: CachePolicy = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy = this.diskCachePolicy,
        networkCachePolicy: CachePolicy = this.networkCachePolicy,
        diskCacheKey: String? = this.diskCacheKey,
    ) = Options(
        context, allowInexactSize, headers, tags, parameters, memoryCachePolicy, diskCachePolicy,
        networkCachePolicy, size, diskCacheKey
    )


}