package com.opensource.svgaplayer.utils

import android.util.Log
import java.io.File

/**

 * @Author svenj
 * @Date 2021/1/11
 * @Email svenjzm@gmail.com
 */
object VirtualBoxUtils {
    private var mIsLeidian: Boolean? = null

    fun isLeidianBox(): Boolean {
        if (mIsLeidian == null) {
            mIsLeidian = hasLeidianSo()
        }

        return mIsLeidian!!
    }

    private fun hasLeidianSo(): Boolean {
        return try {
            val leidianSo = File("/system/lib/libldutils.so")
            leidianSo.exists()
        } catch (t : Throwable) {
            Log.e("VirtualBoxUtils", "Check Leidian so failed", t)
            false
        }
    }
}