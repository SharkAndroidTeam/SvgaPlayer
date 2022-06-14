package com.shark.svgaplayer_base.target

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.opensource.svgaplayer.SVGADrawable
import com.opensource.svgaplayer.SVGAImageView

/** A [Target] that handles setting images on an [SVGAImageView]. */
open class SVGAImageViewTarget(
    override val view: SVGAImageView
) : PoolableViewTarget<SVGAImageView>, DefaultLifecycleObserver {

    private var isStarted = false

    // override fun onStart() = setDrawable(null)

    override fun onError() = setDrawable(null)

    override fun onSuccess(result: SVGADrawable) = setDrawable(result)

    override fun onClear() = setDrawable(null)

    override fun onStart(owner: LifecycleOwner) {
        isStarted = true
        updateAnimation()
    }

    override fun onStop(owner: LifecycleOwner) {
        isStarted = false
        updateAnimation()
    }

    /** Replace the [SVGAImageView]'s current drawable with [drawable]. */
    protected open fun setDrawable(drawable: Drawable?) {
        (view.drawable as? SVGADrawable)?.stop()
        view.setImageDrawable(drawable)
        view.drawable?.let {
            Log.i("SVGAImageViewTarget", "SVGAVideoEntity prepare start")
//            (it as? SVGADrawable)?.videoItem?.prepare({
//                Log.i("SVGAImageViewTarget", "SVGAVideoEntity prepare success to updateAnimation")
//                updateAnimation()
//            }, null) ?: run {
//                Log.i("SVGAImageViewTarget", "SVGAVideoEntity updateAnimation")
//                updateAnimation()
//            }
            updateAnimation()
        }
    }

    /** Start/stop the current [SVGADrawable]'s animation based on the current lifecycle state. */
    protected open fun updateAnimation() {
        if (view.drawable !is SVGADrawable) return
        if (isStarted) {
            view.stepToFrame(0, view.mAutoPlay)
//            (view.drawable as? SVGADrawable)?.resume()
        }
    }

    override fun toString() = "SVGAImageViewTarget(view=$view)"
}
