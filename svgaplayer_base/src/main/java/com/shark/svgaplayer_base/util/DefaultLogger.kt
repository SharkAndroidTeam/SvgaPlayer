package com.shark.svgaplayer_base.util


import android.util.Log
import com.shark.svgaplayer_base.util.Logger

/**

 * @Author svenj
 * @Date 2020/11/27
 * @Email svenjzm@gmail.com
 */
class DefaultLogger: Logger {
    override var level: Int = Log.ERROR
    override fun log(tag: String, priority: Int, message: String?, throwable: Throwable?) {
        Log.i(tag, message, throwable)
    }
}