package com.shark.svgaplayer_base.fetch

import com.shark.svgaplayer_base.SvgaLoader
import com.shark.svgaplayer_base.request.Options
import com.shark.svgaplayer_base.size.Size

/**
 *
 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
interface Fetcher {

//    fun handles(data: T): Boolean = true
//
//    fun key(data: T): String
//
//    suspend fun fetch(data: T, size: Size, options: Options): FetchResult


    /**
     * Fetch the data provided by [Factory.create] or return 'null' to delegate to the next
     * [Factory] in the component registry.
     */
    suspend fun fetch(): FetchResult?

    fun interface Factory<T : Any> {

        /**
         * Return a [Fetcher] that can fetch [data] or 'null' if this factory cannot create a
         * fetcher for the data.
         *
         * @param data The data to fetch.
         * @param options A set of configuration options for this request.
         * @param imageLoader The [ImageLoader] that's executing this request.
         */
        fun create(data: T, options: Options, imageLoader: SvgaLoader): Fetcher?
    }
}