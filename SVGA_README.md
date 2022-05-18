# SVGA播放流程解析

## 1.SVGAImageView

播放svga的方法是`startAnimation()`，里面默认调用了`startAnimation(range = null,reverse = false)`，range的主要作用是裁剪，当用户需要只播放中间某几帧的时候，会用到这个方法，这里不作讨论，reverse是用来控制是否倒放的，现在来到`play()`方法

### 1.1 play()

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

## 2.SVGADrawable

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

## 2.SVGACanvasDrawer
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
### 2.1 drawSprite()
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