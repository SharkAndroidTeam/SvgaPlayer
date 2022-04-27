package com.shark.svgaplayer_base


import android.content.Context
import com.shark.svgaplayer_base.request.Disposable
import com.shark.svgaplayer_base.request.SVGARequest
import com.shark.svgaplayer_base.request.SVGAResult

/**

 * @Author svenj
 * @Date 2020/11/27
 * @Email svenjzm@gmail.com
 */
object IkCosler {
    private var imageLoader: SvgaLoader? = null
    private var imageLoaderFactory: SVGALoaderFactory? = null

    @JvmStatic
    fun svgaLoader(context: Context): SvgaLoader = imageLoader ?: newSvgaLoader(context)

    @JvmStatic
    inline fun enqueue(request: SVGARequest): Disposable {
        return svgaLoader(request.context).enqueue(request)
    }

    @JvmStatic
    suspend inline fun execute(request: SVGARequest): SVGAResult {
        return svgaLoader(request.context).execute(request)
    }

    @JvmStatic
    @Synchronized
    fun setSvgaLoader(imageLoader: SvgaLoader) {
        this.imageLoaderFactory = null
        this.imageLoader = imageLoader
    }

    @JvmStatic
    @Synchronized
    fun setSvgaLoader(factory: SVGALoaderFactory) {
        imageLoaderFactory = factory
        imageLoader = null
    }

    /** Create and set the new singleton [SvgaLoader]. */
    @Synchronized
    private fun newSvgaLoader(context: Context): SvgaLoader {
        // Check again in case imageLoader was just set.
        imageLoader?.let { return it }

        // Create a new ImageLoader.
        val newImageLoader = imageLoaderFactory?.newSvgaLoader()
            ?: (context.applicationContext as? SVGALoaderFactory)?.newSvgaLoader()
            ?: SvgaLoader(context)
        imageLoaderFactory = null
        imageLoader = newImageLoader
        return newImageLoader
    }

    /** Reset the internal state. */
    @Synchronized
    internal fun reset() {
        imageLoader = null
        imageLoaderFactory = null
    }
}