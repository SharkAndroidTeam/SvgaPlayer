@file:Suppress("unused")

package com.shark.svgaplayer_base.map

import com.shark.svgaplayer_base.fetch.Fetcher
import com.shark.svgaplayer_base.request.Options


/**
 * An interface to convert data of type [T] into [V].
 *
 * Use this to map custom data types to a type that can be handled by a [Fetcher].
 */
fun interface Mapper<T : Any, V : Any> {

    /**
     * Convert [data] into [V]. Return 'null' if this mapper cannot convert [data].
     *
     * @param data The data to convert.
     * @param options The options for this request.
     */
    fun map(data: T, options: Options): V?
}
