package com.shark.svgaplayer_base.target

import android.graphics.drawable.Drawable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.shark.svgaplayer_base.target.PoolableViewTarget
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
        updateAnimation()
    }

    /** Start/stop the current [SVGADrawable]'s animation based on the current lifecycle state. */
    protected open fun updateAnimation() {
        if (view.drawable !is SVGADrawable) return
        if (isStarted) {
            view.stepToFrame(0, view.mAutoPlay)
        }
    }

    override fun toString() = "SVGAImageViewTarget(view=$view)"
}
