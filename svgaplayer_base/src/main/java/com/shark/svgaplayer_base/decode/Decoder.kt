package com.shark.svgaplayer_base.decode

import com.opensource.svgaplayer.SVGAVideoEntity
import com.shark.svgaplayer_base.size.Size
import okio.BufferedSource

/**
 * Converts a [BufferedSource] into a [SVGAVideoEntity].
 *
 * Use this interface to add support for custom file formats (e.g. GIF, SVG, TIFF, etc.).
 */
interface Decoder {

    fun handles(source: BufferedSource, mimeType: String?): Boolean

    /**
     * Decode [source] as a [SVGAVideoEntity].
     *
     * NOTE: Implementations are responsible for closing [source] when finished with it.
     *
     * @param source The [BufferedSource] to read from.
     * @param key The cache key fot the request
     * @param size The requested dimensions for the image.
     * @param options A set of configuration options for this request.
     */
    suspend fun decode(
        source: BufferedSource,
        key: String,
        size: Size,
        options: Options
    ): DecodeResult
}
