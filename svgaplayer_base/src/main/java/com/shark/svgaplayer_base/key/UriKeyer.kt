package com.shark.svgaplayer_base.key

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.net.Uri
import com.shark.svgaplayer_base.request.Options
import com.shark.svgaplayer_base.util.nightMode

internal class UriKeyer : Keyer<Uri> {

    override fun key(data: Uri, options: Options): String {
        // 'android.resource' uris can change if night mode is enabled/disabled.
        return if (data.scheme == SCHEME_ANDROID_RESOURCE) {
            "$data-${options.context.resources.configuration.nightMode}"
        } else {
            data.toString()
        }
    }
}
