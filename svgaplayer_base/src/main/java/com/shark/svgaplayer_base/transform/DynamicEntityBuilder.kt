package com.shark.svgaplayer_base.transform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.text.BoringLayout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.shark.svgaplayer_base.request.Options
import com.shark.svgaplayer_base.network.HttpException
import com.shark.svgaplayer_base.util.await
import com.opensource.svgaplayer.SVGADynamicEntity
import com.shark.svgaplayer_base.fetch.HttpUriFetcher
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Request

/**
 * @Author svenj
 * @Date 2021/1/9
 * @Email svenjzm@gmail.com
 */
class DynamicEntityBuilder {
    private val hiddenMap: HashMap<String, Boolean> = hashMapOf()

    private val bitmapMap: HashMap<String, Bitmap> = hashMapOf()

    private val textMap: HashMap<String, String> = hashMapOf()

    private val textPaintMap: HashMap<String, TextPaint> = hashMapOf()

    private val staticLayoutMap: HashMap<String, StaticLayout> = hashMapOf()

    private val boringLayoutMap: HashMap<String, BoringLayout> = hashMapOf()

    private val drawerMap: HashMap<String, (canvas: Canvas, frameIndex: Int) -> Boolean> =
        hashMapOf()

    private val dynamicClickKey: MutableList<String> = mutableListOf()

    private val drawerSizedMap: HashMap<String, (canvas: Canvas, frameIndex: Int, width: Int, height: Int) -> Boolean> =
        hashMapOf()

    private val dynamicImageUrl: HashMap<String, String> = hashMapOf()

    fun setHidden(key: String, hide: Boolean): DynamicEntityBuilder {
        hiddenMap[key] = hide
        return this
    }

    fun setImage(key: String, bitmap: Bitmap): DynamicEntityBuilder {
        bitmapMap[key] = bitmap
        return this
    }

    fun setImage(key: String, url: String): DynamicEntityBuilder {
        dynamicImageUrl[key] = url
        return this
    }

    fun setTextPaint(key: String, text: String, paint: TextPaint): DynamicEntityBuilder {
        textMap[key] = text
        textPaintMap[key] = paint
        return this
    }

    fun setStaticLayout(key: String, layout: StaticLayout): DynamicEntityBuilder {
        staticLayoutMap[key] = layout
        return this
    }

    fun setBoringLayout(key: String, layout: BoringLayout): DynamicEntityBuilder {
        boringLayoutMap[key] = layout
        return this
    }

    fun setDrawer(
        key: String,
        drawer: (canvas: Canvas, frameIndex: Int) -> Boolean
    ): DynamicEntityBuilder {
        drawerMap[key] = drawer
        return this
    }

    fun setDrawerSized(
        key: String,
        drawer: (canvas: Canvas, frameIndex: Int, width: Int, height: Int) -> Boolean
    ): DynamicEntityBuilder {
        drawerSizedMap[key] = drawer
        return this
    }

    fun setClickKey(key: String): DynamicEntityBuilder {
        dynamicClickKey.add(key)
        return this
    }

    suspend fun create(callFactory: Call.Factory, options: Options): SVGADynamicEntity =
        SVGADynamicEntity().apply {
            // setHidden
            for ((forKey, hide) in hiddenMap) {
                setHidden(hide, forKey)
            }

            // setDynamicImage
            for ((forKey, bitmap) in bitmapMap) {
                setDynamicImage(bitmap, forKey)
            }

            // get bitmap from urls
            for ((forKey, url) in dynamicImageUrl) {
                val bitmap = fetchBitmapFromUrl(url, callFactory, options)
                if (bitmap != null) {
                    setDynamicImage(bitmap, forKey)
                } else {
                    Log.e("DynamicEntityBuilder", "fetch empty bitmap for $forKey")
                }
            }

            // setDynamicText
            for ((forKey, text) in textMap) {
                textPaintMap[forKey]?.let { textPaint ->
                    setDynamicText(text, textPaint, forKey)
                }
            }

            // setDynamicText
            for ((forKey, layout) in staticLayoutMap) {
                setDynamicText(layout, forKey)
            }

            for ((forKey, layout) in boringLayoutMap) {
                setDynamicText(layout, forKey)
            }

            // setDynamicDrawer
            for ((forKey, drawer) in drawerMap) {
                setDynamicDrawer(drawer, forKey)
            }

            for ((forKey, drawer) in drawerSizedMap) {
                setDynamicDrawerSized(drawer, forKey)
            }

            // set click
            setClickArea(dynamicClickKey)
        }

    private suspend fun fetchBitmapFromUrl(
        url: String,
        callFactory: Call.Factory,
        options: Options
    ): Bitmap? {
        val request = Request.Builder().url(url).headers(options.headers)
        val networkRead = options.networkCachePolicy.readEnabled
        val diskRead = options.diskCachePolicy.readEnabled
        when {
            !networkRead && diskRead -> {
                request.cacheControl(CacheControl.FORCE_CACHE)
            }
            networkRead && !diskRead -> if (options.diskCachePolicy.writeEnabled) {
                request.cacheControl(CacheControl.FORCE_NETWORK)
            } else {
                request.cacheControl(HttpUriFetcher.CACHE_CONTROL_FORCE_NETWORK_NO_CACHE)
            }
            !networkRead && !diskRead -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                request.cacheControl(HttpUriFetcher.CACHE_CONTROL_NO_NETWORK_NO_CACHE)
            }
        }
        return try {
            val response = callFactory.newCall(request.build()).await()

            if (!response.isSuccessful) {
                response.body?.close()
                throw HttpException(response)
            }
            val body = checkNotNull(response.body) { "Null response body!" }
            BitmapFactory.decodeStream(body.byteStream())
        } catch (e : Throwable) {
            Log.e("DynamicEntityBuilder", "Fetch bitmap failed from $url", e)
            null
        }
    }
}