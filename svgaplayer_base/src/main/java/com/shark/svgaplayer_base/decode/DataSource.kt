package com.shark.svgaplayer_base.decode

import androidx.annotation.DrawableRes
import java.io.File
import java.nio.ByteBuffer

/**
 * Represents the source that an image was loaded from.
 *
 * @see com.gmlive.svgaplayer.fetch.SourceResult.dataSource
 * @see com.gmlive.svgaplayer.fetch.SVGADrawableResult.dataSource
 */
enum class DataSource {

    /**
     * Represents an [SvgaLoader]'s memory cache.
     *
     * This is a special data source as it means the request was
     * short circuited and skipped the full image pipeline.
     */
    MEMORY_CACHE,

    /**
     * Represents an in-memory data source (e.g. [com.opensource.svgaplayer.SVGAVideoEntity], [ByteBuffer]).
     */
    MEMORY,

    /**
     * Represents a disk-based data source (e.g. [DrawableRes], [File]).
     */
    DISK,

    /**
     * Represents a network-based data source (e.g. [HttpUrl]).
     */
    NETWORK
}
