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
data class Size(
    val width: Dimension,
    val height: Dimension,
) {

    companion object {
        /**
         * A [Size] whose width and height are not scaled.
         */
        @JvmField val ORIGINAL = Size(Dimension.Undefined, Dimension.Undefined)
    }
}

/** Create a [Size] with a pixel value for width. */
fun Size(@Px width: Int, height: Dimension) = Size(Dimension(width), height)

/** Create a [Size] with a pixel value for height. */
fun Size(width: Dimension, @Px height: Int) = Size(width, Dimension(height))

/** Create a [Size] with pixel values for both width and height. */
fun Size(@Px width: Int, @Px height: Int) = Size(Dimension(width), Dimension(height))

/** Return true if this size is equal to [Size.ORIGINAL]. Else, return false. */
val Size.isOriginal: Boolean get() = this == Size.ORIGINAL

@Deprecated(
    message = "Migrate to 'coil.size.Size.ORIGINAL'.",
    replaceWith = ReplaceWith("Size.ORIGINAL", "coil.size.Size")
)
val OriginalSize: Size get() = Size.ORIGINAL