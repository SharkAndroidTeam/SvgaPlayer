package com.shark.svgaplayer_base.map

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.shark.svgaplayer_base.request.Options

/**
 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
internal class ResourceUriMapper(private val context: Context) : Mapper<Uri, Uri> {

    override fun map(data: Uri, options: Options): Uri? {
        if (!isApplicable(data)) return null

        val packageName = data.authority.orEmpty()
        val resources = options.context.packageManager.getResourcesForApplication(packageName)
        val (type, name) = data.pathSegments
        val id = resources.getIdentifier(name, type, packageName)
        check(id != 0) { "Invalid ${ContentResolver.SCHEME_ANDROID_RESOURCE} URI: $data" }

        return "${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/$id".toUri()
    }

    private fun isApplicable(data: Uri): Boolean {
        return data.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE &&
                !data.authority.isNullOrBlank() &&
                data.pathSegments.count() == 2
    }
}