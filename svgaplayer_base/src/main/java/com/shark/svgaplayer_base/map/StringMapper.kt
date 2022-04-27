package com.shark.svgaplayer_base.map

import android.net.Uri
import androidx.core.net.toUri

/**
 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
internal class StringMapper : Mapper<String, Uri> {

    override fun map(data: String) = data.toUri()
}
