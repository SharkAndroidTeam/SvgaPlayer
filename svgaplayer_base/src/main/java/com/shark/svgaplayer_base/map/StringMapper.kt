package com.shark.svgaplayer_base.map

import android.net.Uri
import androidx.core.net.toUri
import com.shark.svgaplayer_base.request.Options

/**
 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
internal class StringMapper : Mapper<String, Uri> {

    override fun map(data: String, options: Options) = data.toUri()
}
