package com.opensource.svgaplayer

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.opensource.svgaplayer.drawer.SVGACanvasDrawer

class SVGADrawable(val videoItem: SVGAVideoEntity, val dynamicItem: SVGADynamicEntity) :
    Drawable() {

    constructor(videoItem: SVGAVideoEntity) : this(videoItem, SVGADynamicEntity())

    var cleared = true
        internal set(value) {
            if (field == value) {
                return
            }
            field = value
            invalidateSelf()
        }

    var currentFrame = 0
        internal set(value) {
            if (field == value) {
                return
            }
            field = value
            invalidateSelf()
        }

    var scaleType: ImageView.ScaleType = ImageView.ScaleType.MATRIX

    private val drawer = SVGACanvasDrawer(videoItem, dynamicItem)

    override fun draw(canvas: Canvas) {
        if (cleared) {
            return
        }
        canvas.let {
            drawer.drawFrame(it, currentFrame, scaleType)
        }
    }

    override fun setAlpha(alpha: Int) {

    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {

    }

    fun resume() {
        videoItem.audioList.forEach { audio ->
            audio.playID?.let {
                videoItem.soundPool?.resume(it)
            }
        }
    }

    fun pause() {
        videoItem.audioList.forEach { audio ->
            audio.playID?.let {
                videoItem.soundPool?.pause(it)
            }
        }
    }

    fun stop() {
        videoItem.audioList.forEach { audio ->
            audio.playID?.let {
                videoItem.soundPool?.stop(it)
            }
        }
    }

    fun clear() {
        videoItem.audioList.forEach { audio ->
            audio.playID?.let {
                videoItem.soundPool?.stop(it)
            }
            audio.playID = null
        }
        // SvgaLoader中使用了引用记数缓存方案，引用记数为0时才会clear videoEntity
        // VideoEntityRefCounter
        // videoItem.clear()
    }

//    override fun getIntrinsicWidth(): Int =
//        if (videoItem.mFrameWidth > 0 && videoItem.mFrameHeight > 0) {
//            videoItem.mFrameWidth
//        } else {
//            videoItem.videoSize.width.toInt()
//        }
//
//    override fun getIntrinsicHeight(): Int =
//        if (videoItem.mFrameWidth > 0 && videoItem.mFrameHeight > 0) {
//            videoItem.mFrameHeight
//        } else {
//            videoItem.videoSize.height.toInt()
//        }
}