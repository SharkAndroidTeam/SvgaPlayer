package com.shark.svgaplayer_base

import android.content.Context
import com.shark.svgaplayer_base.disk.DiskCache
import com.shark.svgaplayer_base.memory.MemoryCache
import com.shark.svgaplayer_base.util.Utils
import com.shark.svgaplayer_base.util.*
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.io.File

/**
 * 默认 [SVGALoaderFactory]
 * @Author svenj
 * @Date 2020/11/27
 * @Email svenjzm@gmail.com
 */
class DefaultSVGALoaderFactory(private val context: Context) : SVGALoaderFactory {
    override fun newSvgaLoader(): SvgaLoader {
        return SvgaLoader.Builder(context)
            .okHttpClient {
                // Create a disk cache with "unlimited" size. Don't do this in production.
                // To create the an optimized Coil disk cache, use CoilUtils.createDefaultCache(context).
                val cacheDirectory = Utils.getDefaultCacheDirectory(context)
                val cache = Cache(cacheDirectory, Long.MAX_VALUE)

                // Rewrite the Cache-Control header to cache all responses for a year.
                val cacheControlInterceptor =
                    ResponseHeaderInterceptor("Cache-Control", "max-age=31536000,public")

                // Don't limit concurrent network requests by host.
                val dispatcher = Dispatcher().apply { maxRequestsPerHost = maxRequests }

                // Lazily create the OkHttpClient that is used for network operations.
                OkHttpClient.Builder()
                    .cache(cache)
                    .dispatcher(dispatcher)
                    .ignoreVerify()
                    .setupSSLFactory()
                    .addNetworkInterceptor(cacheControlInterceptor)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.filesDir.resolve("svga_cache"))
                    .maxSizeBytes(512L * 1024 * 1024) // 512MB
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    // Set the max size to 25% of the app's available memory.
                    .maxSizePercent(0.25)
                    .build()
            }
            .respectCacheHeaders(true)
            .build()
    }
}