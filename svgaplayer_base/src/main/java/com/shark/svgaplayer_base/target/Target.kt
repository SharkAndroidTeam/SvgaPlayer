package com.shark.svgaplayer_base.target

import androidx.annotation.MainThread
import com.opensource.svgaplayer.SVGADrawable

/**
 * A listener that accepts the result of an image request.
 */
interface Target {

    /**
     * Called when the request starts.
     */
    @MainThread
    fun onStart() {}

    /**
     * Called if an error occurs while executing the request.
     */
    @MainThread
    fun onError() {}

    /**
     * Called if the request completes successfully.
     */
    @MainThread
    fun onSuccess(result: SVGADrawable) {}
}
