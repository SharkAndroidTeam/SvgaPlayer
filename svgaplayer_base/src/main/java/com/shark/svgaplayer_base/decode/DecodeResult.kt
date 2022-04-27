package com.shark.svgaplayer_base.decode

import com.opensource.svgaplayer.SVGAVideoEntity


/**
 * The result of [Decoder.decode].
 *
 * @param entity The decoded [SVGAVideoEntity].
 *
 * @see Decoder
 */
data class DecodeResult(
    val entity: SVGAVideoEntity,
    val isSampled: Boolean
)
