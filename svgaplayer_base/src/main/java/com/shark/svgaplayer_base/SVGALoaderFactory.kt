package com.shark.svgaplayer_base

import com.shark.svgaplayer_base.SvgaLoader

/**

 * @Author svenj
 * @Date 2020/11/27
 * @Email svenjzm@gmail.com
 */
fun interface SVGALoaderFactory {

    fun newSvgaLoader(): SvgaLoader
}