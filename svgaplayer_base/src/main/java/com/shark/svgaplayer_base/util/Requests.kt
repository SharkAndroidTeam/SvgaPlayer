@file:JvmName("-Requests")
@file:Suppress("NOTHING_TO_INLINE")

package com.shark.svgaplayer_base.util

import com.shark.svgaplayer_base.fetch.Fetcher
import com.shark.svgaplayer_base.request.SVGARequest

/** Ensure [SVGARequest.fetcher] is valid for [data]. */
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> SVGARequest.fetcher(data: T): Fetcher<T>? {
    val (fetcher, type) = fetcher ?: return null
    check(type.isAssignableFrom(data::class.java)) {
        "${fetcher.javaClass.name} cannot handle data with type ${data.javaClass.name}."
    }
    return fetcher as Fetcher<T>
}
