@file:JvmName("SVGAImageViews")
@file:Suppress("unused")
package com.shark.svgaplayer_base.util

import android.net.Uri
import com.shark.svgaplayer_base.SvgaLoader
import com.shark.svgaplayer_base.request.Disposable
import com.shark.svgaplayer_base.request.SVGARequest
import com.shark.svgaplayer_base.request.SVGAResult
import com.opensource.svgaplayer.SVGAImageView
import okhttp3.HttpUrl
import java.io.File

/**
 * SVGAImageView 的扩展函数
 * @Author svenj
 * @Date 2020/11/27
 * @Email svenjzm@gmail.com
 */
/** @see SVGAImageView.loadAny */
@JvmSynthetic
inline fun SVGAImageView.load(
    uri: String?,
    imageLoader: SvgaLoader = context.svgaLoader,
    builder: SVGARequest.Builder.() -> Unit = {}
): Disposable = loadAny(uri, imageLoader, builder)

/** @see SVGAImageView.loadAny */
@JvmSynthetic
inline fun SVGAImageView.load(
    url: HttpUrl?,
    imageLoader: SvgaLoader = context.svgaLoader,
    builder: SVGARequest.Builder.() -> Unit = {}
): Disposable = loadAny(url, imageLoader, builder)

/** @see SVGAImageView.loadAny */
@JvmSynthetic
inline fun SVGAImageView.load(
    uri: Uri?,
    imageLoader: SvgaLoader = context.svgaLoader,
    builder: SVGARequest.Builder.() -> Unit = {}
): Disposable = loadAny(uri, imageLoader, builder)

/** @see SVGAImageView.loadAny */
@JvmSynthetic
inline fun SVGAImageView.load(
    file: File?,
    imageLoader: SvgaLoader = context.svgaLoader,
    builder: SVGARequest.Builder.() -> Unit = {}
): Disposable = loadAny(file, imageLoader, builder)

@JvmSynthetic
inline fun SVGAImageView.loadAsset(
    assetName: String,
    svgaLoader: SvgaLoader = context.svgaLoader,
    builder: SVGARequest.Builder.() -> Unit = {}
): Disposable = loadAny(assetName.toAssetUri(), svgaLoader, builder)

/**
 * Load the image referenced by [data] and set it on this [SVGAImageView].
 *
 * [SVGAImageView.loadAny] is the type-unsafe version of [SVGAImageView.load].
 *
 *
 * @param data The data to load.
 * @param imageLoader The [SvgaLoader] that will be used to enqueue the [SVGARequest].
 * @param builder An optional lambda to configure the request before it is enqueued.
 */
@JvmSynthetic
inline fun SVGAImageView.loadAny(
    data: Any?,
    imageLoader: SvgaLoader = context.svgaLoader,
    builder: SVGARequest.Builder.() -> Unit = {}
): Disposable {
    val request = SVGARequest.Builder(context)
        .data(data)
        .target(this)
        .apply(builder)
        .build()
    return imageLoader.enqueue(request)
}

/**
 * Cancel any in progress requests and clear all resources associated with this [SVGAImageView].
 */
//fun SVGAImageView.clearRequests() {
//    Utils.clear(this)
//}

/**
 * Get the metadata of the successful request attached to this view.
 */
//val SVGAImageView.metadata: SVGAResult.Metadata?
//    @JvmName("metadata") get() = Utils.metadata(this)