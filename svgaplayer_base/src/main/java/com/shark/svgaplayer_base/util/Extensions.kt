@file:JvmName("-Extensions")
@file:Suppress("NOTHING_TO_INLINE")

package com.shark.svgaplayer_base.util

import android.app.ActivityManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Looper
import android.os.StatFs
import android.view.View
import android.webkit.MimeTypeMap
import androidx.core.view.ViewCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.shark.svgaplayer_base.memory.MemoryCache
import com.shark.svgaplayer_base.memory.TargetDelegate
import com.shark.svgaplayer_base.memory.ViewTargetRequestManager
import com.shark.svgaplayer_base.recycle.VideoEntityRefCounter
import com.shark.svgaplayer_base.request.Parameters
import com.shark.svgaplayer_base.request.SVGAResult
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.target.ViewTarget
import com.shark.svgaplayer_base.R
import com.opensource.svgaplayer.SVGADrawable
import com.opensource.svgaplayer.SVGAVideoEntity
import kotlinx.coroutines.Job
import okhttp3.Call
import okhttp3.Headers
import okio.ByteString.Companion.encodeUtf8

import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

internal inline val ActivityManager.isLowRamDeviceCompat: Boolean
    get() = SDK_INT < 19 || isLowRamDevice

@Suppress("DEPRECATION")
internal inline val StatFs.blockCountCompat: Long
    get() = if (SDK_INT >= 18) blockCountLong else blockCount.toLong()

@Suppress("DEPRECATION")
internal inline val StatFs.blockSizeCompat: Long
    get() = if (SDK_INT >= 18) blockSizeLong else blockSize.toLong()

internal val View.requestManager: ViewTargetRequestManager
    get() {
        var manager = getTag(R.id.svga_request_manager) as? ViewTargetRequestManager
        if (manager == null) {
            manager = synchronized(this) {
                // Check again in case coil_request_manager was just set.
                (getTag(R.id.svga_request_manager) as? ViewTargetRequestManager)
                    ?.let { return@synchronized it }

                ViewTargetRequestManager().apply {
                    addOnAttachStateChangeListener(this)
                    setTag(R.id.svga_request_manager, this)
                }
            }
        }
        return manager
    }

internal inline val View.isAttachedToWindowCompat: Boolean
    get() = ViewCompat.isAttachedToWindow(this)

internal val Drawable.width: Int
    get() = (this as? BitmapDrawable)?.bitmap?.width ?: intrinsicWidth

internal val Drawable.height: Int
    get() = (this as? BitmapDrawable)?.bitmap?.height ?: intrinsicHeight

internal val Drawable.isVector: Boolean
    get() = (this is VectorDrawableCompat) || (SDK_INT > 21 && this is VectorDrawable)

internal fun Closeable.closeQuietly() {
    try {
        close()
    } catch (exception: RuntimeException) {
        throw exception
    } catch (_: Exception) {
    }
}

/**
 * Wrap a [Call.Factory] factory as a [Call.Factory] instance.
 * [initializer] is called only once the first time [Call.Factory.newCall] is called.
 */
internal fun lazyCallFactory(initializer: () -> Call.Factory): Call.Factory {
    val lazy: Lazy<Call.Factory> = lazy(initializer)
    return Call.Factory { lazy.value.newCall(it) } // Intentionally not a method reference.
}

/** Modified from [MimeTypeMap.getFileExtensionFromUrl] to be more permissive with special characters. */
internal fun MimeTypeMap.getMimeTypeFromUrl(url: String?): String? {
    if (url.isNullOrBlank()) {
        return null
    }

    val extension = url
        .substringBeforeLast('#') // Strip the fragment.
        .substringBeforeLast('?') // Strip the query.
        .substringAfterLast('/') // Get the last path segment.
        .substringAfterLast('.', missingDelimiterValue = "") // Get the file extension.

    return getMimeTypeFromExtension(extension)
}

internal val Uri.firstPathSegment: String?
    get() = pathSegments.firstOrNull()

internal val Configuration.nightMode: Int
    get() = uiMode and Configuration.UI_MODE_NIGHT_MASK

/** Required for compatibility with API 25 and below. */
internal val NULL_COLOR_SPACE: ColorSpace? = null

internal val EMPTY_HEADERS = Headers.Builder().build()

internal fun Headers?.orEmpty() = this ?: EMPTY_HEADERS

internal fun Parameters?.orEmpty() = this ?: Parameters.EMPTY

internal fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()

fun String.toAssetUri(): Uri {
    return Uri.parse("file:///android_asset/$this")
}

internal inline val Any.identityHashCode: Int
    get() = System.identityHashCode(this)

inline fun <reified R : Any> Array<*>.findInstance(): R? {
    for (element in this) if (element is R) return element
    return null
}

internal inline fun AtomicInteger.loop(action: (Int) -> Unit) {
    while (true) action(get())
}

internal inline val CoroutineContext.job: Job get() = get(Job)!!

internal var TargetDelegate.metadata: SVGAResult.Metadata?
    get() = (target as? ViewTarget<*>)?.view?.requestManager?.metadata
    set(value) {
        (target as? ViewTarget<*>)?.view?.requestManager?.metadata = value
    }

internal inline operator fun MemoryCache.Key.Companion.invoke(
    base: String,
    size: Size?,
    parameters: Parameters
): MemoryCache.Key {
    return MemoryCache.Key.Complex(
        base = base,
        size = size,
        parameters = parameters.cacheKeys()
    )
}

internal inline fun VideoEntityRefCounter.decrement(entity: SVGAVideoEntity?) {
    if (entity != null) decrement(entity)
}

internal inline fun VideoEntityRefCounter.decrement(drawable: SVGADrawable?) {
    drawable?.videoItem?.let(::decrement)
}

internal inline fun VideoEntityRefCounter.setValid(entity: SVGAVideoEntity?, isValid: Boolean) {
    if (entity != null) setValid(entity, isValid)
}

internal inline fun SVGAVideoEntity.toDrawable(): SVGADrawable = SVGADrawable(this)

internal val Bitmap.Config?.bytesPerPixel: Int
    get() = when {
        this == Bitmap.Config.ALPHA_8 -> 1
        this == Bitmap.Config.RGB_565 -> 2
        this == Bitmap.Config.ARGB_4444 -> 2
        SDK_INT >= 26 && this == Bitmap.Config.RGBA_F16 -> 8
        else -> 4
    }

/**
 * Returns the in memory size of this [Bitmap] in bytes.
 * This value will not change over the lifetime of a bitmap.
 */
internal val Bitmap.allocationByteCountCompat: Int
    get() {
        check(!isRecycled) { "Cannot obtain size for recycled bitmap: $this [$width x $height] + $config" }

        return try {
            if (SDK_INT >= 19) {
                allocationByteCount
            } else {
                rowBytes * height
            }
        } catch (_: Exception) {
            Utils.calculateAllocationByteCount(width, height, config)
        }
    }

internal val SVGAVideoEntity.allocationByteCountCompat: Int
    get() {
        var bytes = 0
        imageMap.forEach {
            bytes += it.value.allocationByteCountCompat
        }

        return bytes
    }

internal fun String.md5Key(): String = this.encodeUtf8().md5().hex()
