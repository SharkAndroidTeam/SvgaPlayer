package com.shark.svgaplayer

import android.app.Application
import com.shark.svgaplayer_base.DefaultSVGALoaderFactory
import com.shark.svgaplayer_base.SVGALoaderFactory
import com.shark.svgaplayer_base.SvgaLoader

class SVGASampleApplication : Application(), SVGALoaderFactory {

    override fun newSvgaLoader() = DefaultSVGALoaderFactory(this).newSvgaLoader()
}
