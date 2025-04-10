# SvgaPlayer

## 0.导入

在根目录gradle文件添加jitpack仓库
```
repositories {
    mavenCentral()
	maven { url 'https://jitpack.io' }
}

```
在对应module.gradle文件添加依赖
```
dependencies {
    implementation 'com.github.SharkAndroidTeam:SvgaPlayer:1.0.3'
}
```

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
## SVGA播放流程解析

### 1.SVGAImageView

播放svga的方法是`startAnimation()`，里面默认调用了`startAnimation(range = null,reverse = false)`，range的主要作用是裁剪，当用户需要只播放中间某几帧的时候，会用到这个方法，这里不作讨论，reverse是用来控制是否倒放的，现在来到`play()`方法

#### 1.1 play()

在`play()`方法中可以看到`ValueAnimator`类，可以看出SVGA的播放实际是一个属性动画在播放

```
private fun play(range: SVGARange?, reverse: Boolean) {
        val drawable = getSVGADrawable() ?: return //获取SVGADrawable
        setupDrawable()
        mStartFrame = 0
        val videoItem = drawable.videoItem 
        mEndFrame = videoItem.frames - 1 //拿到最后一帧
        val animator = ValueAnimator.ofInt(mStartFrame, mEndFrame)
        animator.duration =
            ((mEndFrame - mStartFrame + 1) * (1000 / videoItem.FPS) / generateScale()).toLong() //使用帧数和FPS计算SVGA动画播放时长
        animator.addUpdateListener(mAnimatorUpdateListener)
       ...
       animator.start()
  
    }
```

接下来再看一下`mAnimatorUpdateListener`，里面调用了SVGAImageView里的`onAnimatorUpdate`

```
private class AnimatorUpdateListener(view: SVGAImageView) :
        ValueAnimator.AnimatorUpdateListener {
        private val weakReference = WeakReference<SVGAImageView>(view)

        override fun onAnimationUpdate(animation: ValueAnimator?) {
            weakReference.get()?.onAnimatorUpdate(animation)
        }
    } 
```

其中调用`drawable.currentFrame`的方法会绘制当前帧

```
private fun onAnimatorUpdate(animator: ValueAnimator?) {
        val drawable = getSVGADrawable() ?: return
        drawable.currentFrame = animator?.animatedValue as Int
        ...
    }
```

### 2.SVGADrawable

调用`setCurrentFrame`会调用`invalidateSelf`会重绘自身

```
var currentFrame = 0
        internal set(value) {
            if (field == value) {
                return
            }
            field = value
            invalidateSelf()
        }
```

```
override fun draw(canvas: Canvas) {
        if (cleared) {
            return
        }
        canvas?.let {
            drawer.drawFrame(it, currentFrame, scaleType)
        }
    }
```

```
    private val drawer = SVGACanvasDrawer(videoItem, dynamicItem)

```

可以看到drawFrame的工作都是由SVGACanvasDrawer完成的

### 2.SVGACanvasDrawer
`drawFrame`的主要步骤分为三个：
1.`requestFrameSprites`根据当前帧获取spriteList，为什么拿到的是一个list，那是因为一帧可能包含多种信息，比如matte(蒙层)
2.遍历spriteList调用`drawSprite`绘制sprite
3.释放spriteList `releaseFrameSprites`
PS.当spriteList中有matte(蒙层)时，会先判断matte是否需要draw，这里不作分析
```
override fun drawFrame(canvas: Canvas, frameIndex: Int, scaleType: ImageView.ScaleType) {
        ...
        val sprites = requestFrameSprites(frameIndex) //根据帧Index获取spriteList

        if (sprites.count() <= 0) return //没有sprite，return
       
        ...
        
        sprites.forEachIndexed { index, svgaDrawerSprite ->
            svgaDrawerSprite.imageKey?.let {
                 ...
                 drawSprite(svgaDrawerSprite, canvas, frameIndex)
                 ...
            }
        releaseFrameSprites(sprites) //释放sprites
    }
```
```
internal fun requestFrameSprites(frameIndex: Int): List<SVGADrawerSprite> {
        return videoItem.spriteList.mapNotNull {
            if (frameIndex >= 0 && frameIndex < it.frames.size) {
                it.imageKey?.let { imageKey ->
                    if (!imageKey.endsWith(".matte") && it.frames[frameIndex].alpha <= 0.0) {
                        return@mapNotNull null
                    }
                    return@mapNotNull (spritePool.acquire() ?: SVGADrawerSprite()).apply {
                        _matteKey = it.matteKey
                        _imageKey = it.imageKey
                        _frameEntity = it.frames[frameIndex]
                    }
                }
            }
            return@mapNotNull null
        }
    }
```
#### 2.1 drawSprite()
绘制Sprite也是分三步，分别是画图、边界、动态设置，三步的方法大同小异，都是根据SVGADrawerSprite的属性去绘制，这里只分析drawImage
```
private fun drawSprite(sprite: SVGADrawerSprite, canvas: Canvas, frameIndex: Int) {
        drawImage(sprite, canvas) //画图
        drawShape(sprite, canvas) //画边界
        drawDynamic(sprite, canvas, frameIndex) //动态设置
    }
```
根据imagekey获取bitmap，然后drawBitmap
```
    private fun drawImage(sprite: SVGADrawerSprite, canvas: Canvas) {
        val imageKey = sprite.imageKey ?: return
        ...
       
        val drawingBitmap = (dynamicItem.dynamicImage[imageKey] ?: videoItem.imageMap[imageKey])
            ?: return
            
        val frameMatrix = shareFrameMatrix(sprite.frameEntity.transform)
        val paint = this.sharedValues.sharedPaint()
        ...
        if (sprite.frameEntity.maskPath != null) { // 如果maskPath不为空后面就会执行clipPath
            val maskPath = sprite.frameEntity.maskPath ?: return
            canvas.save()
            val path = this.sharedValues.sharedPath()
            maskPath.buildPath(path)
            path.transform(frameMatrix)
            canvas.clipPath(path)
            frameMatrix.preScale(
                (sprite.frameEntity.layout.width / drawingBitmap.width).toFloat(),
                (sprite.frameEntity.layout.height / drawingBitmap.height).toFloat()
            )
            if (!drawingBitmap.isRecycled) {
                canvas.drawBitmap(drawingBitmap, frameMatrix, paint)
            }
            canvas.restore()
        } else {
            frameMatrix.preScale(
                (sprite.frameEntity.layout.width / drawingBitmap.width).toFloat(),
                (sprite.frameEntity.layout.height / drawingBitmap.height).toFloat()
            )
            if (!drawingBitmap.isRecycled) {
                canvas.drawBitmap(drawingBitmap, frameMatrix, paint)
            }
        }
       ...
        drawTextOnBitmap(canvas, drawingBitmap, sprite, frameMatrix)
    }

```