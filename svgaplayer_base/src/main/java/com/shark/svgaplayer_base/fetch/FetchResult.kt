package com.shark.svgaplayer_base.fetch

import android.graphics.drawable.Drawable
import com.shark.svgaplayer_base.decode.DataSource
import com.opensource.svgaplayer.SVGAVideoEntity
import com.shark.svgaplayer_base.decode.SVGASource
import okio.BufferedSource

/** The result of [Fetcher.fetch]. */
sealed class FetchResult

/**
 * A raw [BufferedSource] result, which will be consumed by the relevant [Decoder].
 *
 * @param source An unconsumed [BufferedSource] that will be decoded by a [Decoder].
 * @param mimeType An optional MIME type for the [source].
 * @param dataSource Where [source] was loaded from.
 */
class SourceResult(
    val source: SVGASource,
    val mimeType: String?,
    val dataSource: DataSource
) : FetchResult() {
    fun copy(
        source: SVGASource = this.source,
        mimeType: String? = this.mimeType,
        dataSource: DataSource = this.dataSource,
    ) = SourceResult(
        source = source,
        mimeType = mimeType,
        dataSource = dataSource
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SourceResult

        if (source != other.source) return false
        if (mimeType != other.mimeType) return false
        if (dataSource != other.dataSource) return false

        return true
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + dataSource.hashCode()
        return result
    }


}

/**
 * A direct [Drawable] result. Return this from a [Fetcher] if its data cannot be converted into a [BufferedSource].
 *
 * @param drawable The loaded [Drawable].
 * @param isSampled True if [drawable] is sampled (i.e. not loaded into memory at full size).
 * @param dataSource The source that [drawable] was fetched from.
 */
class SVGAEntityResult(
    val videoEntity: SVGAVideoEntity,
    val isSampled: Boolean,
    val dataSource: DataSource
) : FetchResult() {

    fun copy(
        videoEntity: SVGAVideoEntity = this.videoEntity,
        isSampled: Boolean = this.isSampled,
        dataSource: DataSource = this.dataSource,
    ) = SVGAEntityResult(
        videoEntity = videoEntity,
        isSampled = isSampled,
        dataSource = dataSource
    )


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SVGAEntityResult

        if (videoEntity != other.videoEntity) return false
        if (isSampled != other.isSampled) return false
        if (dataSource != other.dataSource) return false

        return true
    }

    override fun hashCode(): Int {
        var result = videoEntity.hashCode()
        result = 31 * result + isSampled.hashCode()
        result = 31 * result + dataSource.hashCode()
        return result
    }
}
