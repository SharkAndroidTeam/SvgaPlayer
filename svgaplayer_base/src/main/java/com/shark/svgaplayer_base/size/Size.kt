package com.shark.svgaplayer_base.size

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.annotation.Px
import kotlinx.parcelize.Parcelize

/**

 * @Author svenj
 * @Date 2020/11/25
 * @Email svenjzm@gmail.com
 */
sealed class Size : Parcelable

/** Represents the width and height of the source image. */
@SuppressLint("ParcelCreator")
@Parcelize
object OriginalSize : Size() {
    override fun toString() = "com.gmlive.svgaplayer.size.OriginalSize"
}

/** A positive width and height in pixels. */
@SuppressLint("ParcelCreator")
@Parcelize
data class PixelSize(
    @Px val width: Int,
    @Px val height: Int
) : Size() {

    init {
        require(width > 0 && height > 0) { "width and height must be > 0." }
    }
}