# SvgaPlayer

## 1.基本用法

### 1.1 简单使用

```
binding.svgaImg.load("xxx.svga") //使用url
binding.svgaImg.loadAsset("xxx.svga") //使用asset路径
binding.svgaing.load(File("xxx.svga"))//使用file
```

### 1.2 自定义配置

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

## 1.3 SVGARequest

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

## 2.SVGALoader的工作流程

SVGALoader的实现类为RealSVGALoader，里面包含了所有加载SVGA所需要的类。SVGARequest从加载到展示到SVGAImageView的流程比较简单，在协程里对SVGARequest进行Map、Fecth、Decode处理之后就能得到能显示的SuccessResult。

### 2.1 enqueue()&excuteMain()

SVGARequest入队之后就会在协程体内执行excuteMain，而在excuteMain方法里有一个RealInterceptorChian，这里通过类似于Okhttp获取respone的方法来获取SVGAResult，但如果用户没有自定义拦截器的话，就只有EngineInterceptor一个拦截器。

```
override fun enqueue(request: SVGARequest): Disposable {
        // Start executing the request on the main thread.
        val job = scope.async {
            executeMain(request, REQUEST_TYPE_ENQUEUE).also { result ->
                if (result is ErrorResult) logger?.log(TAG, result.throwable)
            }
        }

       ...
    }
```

```
@MainThread
    private suspend fun executeMain(initialRequest: SVGARequest, type: Int): SVGAResult {
        ...

            // Execute the interceptor chain.
            val result = withContext(request.interceptorDispatcher) {
                RealInterceptorChain(
                    initialRequest = request,
                    interceptors = interceptors,
                    index = 0,
                    request = request,
                    size = size,
                    eventListener = eventListener,
                ).proceed(request)
            }

     ...
    }
```

### 2.2 EngineInterceptor

在拦截器里，先对request进行map操作，然后在MemoryCacheService里寻找缓存，如果有缓存，就返回缓存，主流程结束

```
 eventListener.mapStart(request, data)
            val mappedData = imageLoader.components.map(data, options)
            // Check the memory cache.
            val cacheKey =
                memoryCacheService.newCacheKey(request, mappedData, options, eventListener)
            val cacheValue = cacheKey?.let { memoryCacheService.getCacheValue(request, it, size) }

            //检查缓存，如果缓存不为空，返回缓存，流程结束
            if (cacheValue != null) {
                return memoryCacheService.newResult(
                    request,
                    cacheKey,
                    cacheValue,
                    callFactory,
                    options
                )
            }
```

如果没有缓存，则走fecth流程

```
val result = execute(request, mappedData, options, eventListener)
```

```
fetchResult = fetch(components, request, mappedData, _options, eventListener)
```

#### 2.2.1 HttpUriFetcher

首先检查磁盘里是否缓存，如果有缓存而且符合条件的，则返回缓存，Fectch流程结束

```
private fun readFromDiskCache(): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled) {
            diskCache.value?.get(diskCacheKey)
        } else {
            null
        }
    }
```

```
return SourceResult(
                        source = snapshot.toSVGASource(),
                        mimeType = getMimeType(url, xxx),
                        dataSource = DataSource.DISK
                    )
```

如果没有缓存，则用okhttp下载文件并写入磁盘，构造SourceResult返回结果

```

private suspend fun executeNetworkRequest(request: Request): Response {
        val response = if (isMainThread()) {
            if (options.networkCachePolicy.readEnabled) {
                // Prevent executing requests on the main thread that could block due to a
                // networking operation.
                throw NetworkOnMainThreadException()
            } else {
                // Work around: https://github.com/Kotlin/kotlinx.coroutines/issues/2448
                callFactory.value.newCall(request).execute()
            }
        } else {
            // Suspend and enqueue the request on one of OkHttp's dispatcher threads.
            callFactory.value.newCall(request).await()
        }
        if (!response.isSuccessful && response.code != HttpURLConnection.HTTP_NOT_MODIFIED) {
            response.body?.closeQuietly()
            throw HttpException(response)
        }
        return response
    }
```

```
private fun writeToDiskCache(
        snapshot: DiskCache.Snapshot?,
        request: Request,
        response: Response,
        cacheResponse: CacheResponse?
    ): DiskCache.Snapshot? {
        ...
        try {
            // Write the response to the disk cache.
            if (response.code == HttpURLConnection.HTTP_NOT_MODIFIED && cacheResponse != null) {
                // Only update the metadata.
                val combinedResponse = response.newBuilder()
                    .headers(combineHeaders(cacheResponse.responseHeaders, response.headers))
                    .build()
                fileSystem.write(editor.metadata) {
                    CacheResponse(combinedResponse).writeTo(this)
                }
            } else {
                // Update the metadata and the image data.
                fileSystem.write(editor.metadata) {
                    CacheResponse(response).writeTo(this)
                }
                fileSystem.write(editor.data) {
                    response.body!!.source().readAll(this)
                }
            }
            return editor.commitAndGet()
        } catch (e: Exception) {
            editor.abortQuietly()
            throw e
        } finally {
            response.closeQuietly()
        }
    }
```

```
return SourceResult(
    source = responseBody.toSVGASource(),
    mimeType = getMimeType(this.url, responseBody.contentType()),
    dataSource = response.toDataSource())
```

#### 2.2.3 SVGAVideoEntityDecoder

通过Fecth得到的SourceResult，里面有个soucre字段，是SVGASource类型，Decoder使用source解析出ExecuteResult，decode流程结束。

### 2.3 流程结尾

回到RealSvgaLoader，拿到结果decode之后的结果之后，只需要回调到SVGAImange上，整个主流程就结束了

```
val result = withContext(request.interceptorDispatcher) {
                RealInterceptorChain(
                    initialRequest = request,
                    interceptors = interceptors,
                    index = 0,
                    request = request,
                    size = size,
                    eventListener = eventListener,
                ).proceed(request)
            }
```

```
private fun onSuccess(
        result: SuccessResult,
        target: Target?,
        eventListener: EventListener
    ) {
        val request = result.request
        val dataSource = result.dataSource
        logger?.log(TAG, Log.INFO) { "Successful (${dataSource.name}) - ${request.data}" }
//        transition(result, target, eventListener) { target?.onSuccess(result.drawable) }
        target?.onSuccess(result.drawable)
        eventListener.onSuccess(request, result)
        request.listener?.onSuccess(request, result)
    }
```
