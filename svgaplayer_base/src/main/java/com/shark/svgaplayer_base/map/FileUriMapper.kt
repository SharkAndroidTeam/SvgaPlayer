package com.shark.svgaplayer_base.map

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toFile
import com.shark.svgaplayer_base.fetch.AssetUriFetcher
import com.shark.svgaplayer_base.map.Mapper
import com.shark.svgaplayer_base.util.firstPathSegment
import java.io.File

/**

 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
internal class FileUriMapper : Mapper<Uri, File> {

    override fun handles(data: Uri): Boolean {
        return data.scheme == ContentResolver.SCHEME_FILE &&
                data.firstPathSegment.let { it != null && it != AssetUriFetcher.ASSET_FILE_PATH_ROOT }
    }

    override fun map(data: Uri) = data.toFile()
}