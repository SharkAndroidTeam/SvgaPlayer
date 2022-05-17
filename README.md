# SvgaPlayer

#### 1.基本用法

##### 1.1 简单使用

```
binding.svgaImg.load("xxx.svga") //使用url
binding.svgaImg.loadAsset("xxx.svga") //使用asset路径
binding.svgaing.load(File("xxx.svga"))//使用file
```

##### 1.2 自定义配置

在Applicaition实现SVGALoaderFactory接口，配置自己需要的SvgaLoader，本库还提供一个默认的DefaultSVGALoaderFactory

```
class SVGASampleApplication : Application(), SVGALoaderFactory {

    override fun newSvgaLoader() = DefaultSVGALoaderFactory(this).newSvgaLoader()
}
```

```
class DefaultSVGALoaderFactory(private val context: Context) : SVGALoaderFactory {
    override fun newSvgaLoader(): SvgaLoader {
        return SvgaLoader.Builder(context)
            .okHttpClient { //OkHttp配置
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
            .diskCache { //磁盘缓存配置
                DiskCache.Builder()
                    .directory(context.filesDir.resolve("svga_cache"))
                    .maxSizeBytes(512L * 1024 * 1024) // 512MB
                    .build()
            }
            .memoryCache { //内存缓存配置
                MemoryCache.Builder(context)
                    // Set the max size to 25% of the app's available memory.
                    .maxSizePercent(0.25)
                    .build()
            }
            .respectCacheHeaders(true)
            .build()
    }
}
```

#### 1.3 SVGARequest

如果用户有监听SVGA加载回调的需求，可以在SVGARequest里添加listener，代码如下：

```
@JvmSynthetic
inline fun SVGAImageView.loadAny(
    data: Any?,
    imageLoader: SvgaLoader = context.svgaLoader,
    builder: SVGARequest.Builder.() -> Unit = {}
): Disposable {
    val request = SVGARequest.Builder(context)
        .data(data)
        .target(this)
        .apply(builder)
        .listener(onStart = {
          ...

        },onError = { request, throwable ->  
          ...
  
        },onSuccess = { request, result ->  
          ...
  
        })
        .build()
    return imageLoader.enqueue(request)
}
```
