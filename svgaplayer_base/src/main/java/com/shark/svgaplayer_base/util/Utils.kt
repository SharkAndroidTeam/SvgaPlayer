package com.shark.svgaplayer_base.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import android.os.StatFs
import android.view.View
import androidx.annotation.Px
import com.shark.svgaplayer_base.disk.DiskCache
import com.shark.svgaplayer_base.request.SVGAResult
import com.shark.svgaplayer_base.util.*
import com.shark.svgaplayer_base.util.blockCountCompat
import com.shark.svgaplayer_base.util.blockSizeCompat
import com.shark.svgaplayer_base.util.isLowRamDeviceCompat
import com.shark.svgaplayer_base.util.requestManager
import okhttp3.Cache
import okio.BufferedSource
import okio.ByteString.Companion.toByteString
import java.io.File

/** Private utility methods for Coil. */
object Utils {

    private const val CACHE_DIRECTORY_NAME = "svga_source_cache"

    private const val SVGA_UNZIP_DIRECTORY_NAME = "svga_unzip_dir"

    private const val MIN_DISK_CACHE_SIZE_BYTES = 10L * 1024 * 1024 // 10MB
    private const val MAX_DISK_CACHE_SIZE_BYTES = 250L * 1024 * 1024 // 250MB

    private const val DISK_CACHE_PERCENTAGE = 0.02

    private const val STANDARD_MULTIPLIER = 0.2
    private const val LOW_MEMORY_MULTIPLIER = 0.15

    private const val DEFAULT_MEMORY_CLASS_MEGABYTES = 256

    const val REQUEST_TYPE_ENQUEUE = 0
    const val REQUEST_TYPE_EXECUTE = 1


    /** Return the in memory size of a [Bitmap] with the given width, height, and [Bitmap.Config]. */
    fun calculateAllocationByteCount(@Px width: Int, @Px height: Int, config: Bitmap.Config?): Int {
        return width * height * config.bytesPerPixel
    }

    fun getDefaultCacheDirectory(context: Context): File {
        return File(context.cacheDir, CACHE_DIRECTORY_NAME).apply { mkdirs() }
    }

    /** Modified from Picasso. */
    fun calculateDiskCacheSize(cacheDirectory: File): Long {
        return try {
            val cacheDir = StatFs(cacheDirectory.absolutePath)
            val size = DISK_CACHE_PERCENTAGE * cacheDir.blockCountCompat * cacheDir.blockSizeCompat
            return size.toLong().coerceIn(MIN_DISK_CACHE_SIZE_BYTES, MAX_DISK_CACHE_SIZE_BYTES)
        } catch (_: Exception) {
            MIN_DISK_CACHE_SIZE_BYTES
        }
    }

    /** Modified from Picasso. */
    fun calculateAvailableMemorySize(context: Context, percentage: Double): Long {
        val memoryClassMegabytes = try {
            val activityManager: ActivityManager = context.requireSystemService()
            val isLargeHeap = (context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
            if (isLargeHeap) activityManager.largeMemoryClass else activityManager.memoryClass
        } catch (_: Exception) {
            DEFAULT_MEMORY_CLASS_MEGABYTES
        }
        return (percentage * memoryClassMegabytes * 1024 * 1024).toLong()
    }

    fun getDefaultAvailableMemoryPercentage(context: Context): Double {
        return try {
            val activityManager: ActivityManager = context.requireSystemService()
            if (activityManager.isLowRamDeviceCompat) LOW_MEMORY_MULTIPLIER else STANDARD_MULTIPLIER
        } catch (_: Exception) {
            STANDARD_MULTIPLIER
        }
    }

    fun getDefaultBitmapPoolPercentage(): Double {
        return when {
            // Prefer immutable bitmaps (which cannot be pooled) on API 24 and greater.
            SDK_INT >= 24 -> 0.0
            // Bitmap pooling is most effective on APIs 19 to 23.
            SDK_INT >= 19 -> 0.5
            // The requirements for bitmap reuse are strict below API 19.
            else -> 0.25
        }
    }

    /** Create an OkHttp disk cache with a reasonable default size and location. */
    @JvmStatic
    fun createDefaultCache(context: Context): Cache {
        val cacheDirectory = getDefaultCacheDirectory(context)
        val cacheSize = calculateDiskCacheSize(cacheDirectory)
        return Cache(cacheDirectory, cacheSize)
    }

    /**
     * Cancel any in progress requests attached to [view] and clear any associated resources.
     *
     * NOTE: Typically you should use [Disposable.dispose] to clear any associated resources,
     * however this method is provided for convenience.
     */
    @JvmStatic
    fun clear(view: View) {
        view.requestManager.clearCurrentRequest()
    }

    /** Get the metadata of the successful request attached to this view. */
    @JvmStatic
    fun metadata(view: View): SVGAResult.Metadata? {
        return view.requestManager.metadata
    }

    @JvmStatic
    fun createDefaultSVGAUnzipDir(context: Context): File {
        return File(context.cacheDir, SVGA_UNZIP_DIRECTORY_NAME).apply { mkdirs() }
    }

    /** zip format: 80750304 */
    private val ZIP_HEADER = byteArrayOf(80.toByte(), 75.toByte(), 3.toByte(), 4.toByte())
            .toByteString(0, 4)

    @JvmStatic
    fun isZipFormat(source: BufferedSource): Boolean {
        return source.rangeEquals(0, ZIP_HEADER)
    }

}


internal val Context.safeCacheDir: File get() = cacheDir.apply { mkdirs() }

/**
 * Holds the singleton instance of the disk cache. We need to have a singleton disk cache
 * instance to support creating multiple [ImageLoader]s without specifying the disk cache
 * directory.
 *
 * @see DiskCache.Builder.directory
 */
internal object SingletonDiskCache {

    private const val FOLDER_NAME = "svga_cache"
    private var instance: DiskCache? = null

    @Synchronized
    fun get(context: Context): DiskCache {
        return instance ?: run {
            // Create the singleton disk cache instance.
            DiskCache.Builder()
                .directory(context.safeCacheDir.resolve(FOLDER_NAME))
                .build()
                .also { instance = it }
        }
    }
}
