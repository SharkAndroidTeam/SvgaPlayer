package com.shark.svgaplayer_base.fetch

import com.shark.svgaplayer_base.decode.Options
import com.shark.svgaplayer_base.size.Size

/**
 *
 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
interface Fetcher<T : Any> {

    fun handles(data: T): Boolean = true

    fun key(data: T): String

    suspend fun fetch(data: T, size: Size, options: Options): FetchResult
}