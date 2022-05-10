package com.shark.svgaplayer_base.util

import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.shark.svgaplayer_base.RealSvgaLoader
import com.shark.svgaplayer_base.network.EmptyNetworkObserver
import com.shark.svgaplayer_base.network.NetworkObserver
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

internal class SystemCallbacks(
    imageLoader: RealSvgaLoader,
    private val context: Context,
    isNetworkObserverEnabled: Boolean
) : ComponentCallbacks2, NetworkObserver.Listener {

    internal val imageLoader = WeakReference(imageLoader)

    private val networkObserver = if (isNetworkObserverEnabled) {
        NetworkObserver(context, this, imageLoader.logger)
    } else {
        EmptyNetworkObserver()
    }



    @Volatile
    private var _isOnline = networkObserver.isOnline
    private val _isShutdown = AtomicBoolean(false)

    val isOnline get() = _isOnline
    val isShutdown get() = _isShutdown.get()

    init {
        context.registerComponentCallbacks(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        imageLoader.get() ?: shutdown()
    }

    override fun onTrimMemory(level: Int) {
        imageLoader.get()?.onTrimMemory(level) ?: shutdown()
    }

    override fun onLowMemory() = onTrimMemory(TRIM_MEMORY_COMPLETE)

    override fun onConnectivityChange(isOnline: Boolean) {
        val imageLoader = imageLoader.get()
        if (imageLoader == null) {
            shutdown()
            return
        }

        _isOnline = isOnline
        imageLoader.logger?.log(TAG, Log.INFO) { if (isOnline) ONLINE else OFFLINE }
    }

    fun shutdown() {
        if (_isShutdown.getAndSet(true)) return
        context.unregisterComponentCallbacks(this)
        networkObserver.shutdown()
    }

    companion object {
        private const val TAG = "NetworkObserver"
        private const val ONLINE = "ONLINE"
        private const val OFFLINE = "OFFLINE"
    }
}
