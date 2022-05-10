package com.shark.svgaplayer_base.decode

import com.opensource.svgaplayer.SVGAVideoEntity
import com.shark.svgaplayer_base.SvgaLoader
import com.shark.svgaplayer_base.fetch.SourceResult
import com.shark.svgaplayer_base.request.Options
import com.shark.svgaplayer_base.size.Size
import okio.BufferedSource

/**
 * Converts a [BufferedSource] into a [SVGAVideoEntity].
 *
 * Use this interface to add support for custom file formats (e.g. GIF, SVG, TIFF, etc.).
 */
interface Decoder {

//    fun handles(source: BufferedSource, mimeType: String?): Boolean
//
//    /**
//     * Decode [source] as a [SVGAVideoEntity].
//     *
//     * NOTE: Implementations are responsible for closing [source] when finished with it.
//     *
//     * @param source The [BufferedSource] to read from.
//     * @param key The cache key fot the request
//     * @param size The requested dimensions for the image.
//     * @param options A set of configuration options for this request.
//     */
//    suspend fun decode(
//        source: BufferedSource,
//        key: String,
//        size: Size,
//        options: Options
//    ): DecodeResult


    /**
     * Decode the [SourceResult] provided by [Factory.create] or return 'null' to delegate to the
     * next [Factory] in the component registry.
     */
    suspend fun decode(): DecodeResult?

    fun interface Factory {

        /**
         * Return a [Decoder] that can decode [result] or 'null' if this factory cannot
         * create a decoder for the source.
         *
         * Implementations **must not** consume [result]'s [ImageSource], as this can cause calls
         * to subsequent decoders to fail. [ImageSource]s should only be consumed in [decode].
         *
         * Prefer using [BufferedSource.peek], [BufferedSource.rangeEquals], or other
         * non-consuming methods to check for the presence of header bytes or other markers.
         * Implementations can also rely on [SourceResult.mimeType], however it is not guaranteed
         * to be accurate (e.g. a file that ends with .png, but is encoded as a .jpg).
         *
         * @param result The result from the [Fetcher].
         * @param options A set of configuration options for this request.
         * @param imageLoader The [ImageLoader] that's executing this request.
         */
        fun create(result: SourceResult, options: Options, imageLoader: SvgaLoader): Decoder?
    }


}
