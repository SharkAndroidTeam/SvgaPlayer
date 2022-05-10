package com.shark.svgaplayer_base.map

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toFile
import com.shark.svgaplayer_base.fetch.AssetUriFetcher
import com.shark.svgaplayer_base.map.Mapper
import com.shark.svgaplayer_base.request.Options
import com.shark.svgaplayer_base.util.firstPathSegment
import com.shark.svgaplayer_base.util.isAssetUri
import java.io.File

/**

 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
internal class FileUriMapper : Mapper<Uri, File> {


    override fun map(data: Uri, options: Options): File? {
        if (!isApplicable(data)) return null
        return  data.toFile()
    }

    private fun isApplicable(data: Uri): Boolean {
        return !isAssetUri(data) &&
                data.scheme.let { it == null || it == ContentResolver.SCHEME_FILE } &&
                data.path.orEmpty().startsWith('/') && data.firstPathSegment != null
    }
}