package com.shark.svgaplayer_base.decode

import com.opensource.svgaplayer.SVGAVideoEntity
import com.opensource.svgaplayer.proto.MovieEntity
import com.shark.svgaplayer_base.SvgaLoader
import com.shark.svgaplayer_base.fetch.SourceResult
import com.shark.svgaplayer_base.request.Options
import com.shark.svgaplayer_base.size.Size
import com.shark.svgaplayer_base.util.*
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.*
import org.json.JSONObject
import java.io.*
import java.util.zip.Inflater
import java.util.zip.ZipInputStream

/**
 * @Author svenj
 * @Date 2020/11/26
 * @Email svenjzm@gmail.com
 */
@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class SVGAVideoEntityDecoder(
    private val source: SVGASource,
    private val options: Options,
    private val parallelismLock: Semaphore = Semaphore(Int.MAX_VALUE)
) : Decoder {

    override suspend fun decode() = parallelismLock.withPermit {
        runInterruptible {
            decodeInterruptible(source.source(), options.diskCacheKey ?: "")
        }
    }


    private fun createCache(key: String): File {
        return File(Utils.createDefaultSVGAUnzipDir(options.context).path, key)
    }

    private fun inflate(byteArray: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(byteArray, 0, byteArray.size)
        val inflatedBytes = ByteArray(2048)
        ByteArrayOutputStream().use { inflatedOutputStream ->
            while (true) {
                val count = inflater.inflate(inflatedBytes, 0, 2048)
                if (count <= 0) {
                    break
                } else {
                    inflatedOutputStream.write(inflatedBytes, 0, count)
                }
            }
            inflater.end()
            return inflatedOutputStream.toByteArray()
        }
    }

    private fun decodeInterruptible(
        source: Source,
        key: String,
    ): DecodeResult {
        val safeSource = ExceptionCatchingSource(source)
        val safeBufferedSource = safeSource.buffer()
        val cacheDir = createCache(key)

        val decodeWidth = -1
        val decodeHeight = -1

        if (Utils.isZipFormat(safeBufferedSource)) {
            if (cacheDir.isDirectory && cacheDir.list().isNotEmpty()) {
                return decodeUnZipFile(cacheDir, decodeWidth, decodeHeight)
            } else {
                try {
                    cacheDir.makeSureExist()
                    unzip(safeBufferedSource.peek().inputStream(), cacheDir)
                } catch (e: Throwable) {
                    cacheDir.deleteRecursively()
                    throw e
                }

                safeSource.exception?.let { throw it }
            }
            return decodeUnZipFile(cacheDir, decodeWidth, decodeHeight)
        } else {
            safeSource.exception?.let { throw it }
            return decodeSource(cacheDir, safeBufferedSource, decodeWidth, decodeHeight)
        }
    }

    private fun unzip(inputStream: InputStream, cache: File) {
        ZipInputStream(inputStream).use { zipInputStream ->
            while (true) {
                val zipItem = zipInputStream.nextEntry ?: break
                if (zipItem.name.contains("../")) {
                    // 解压路径防止穿透
                    continue
                }
                if (zipItem.name.contains("/")) {
                    continue
                }
                val file = File(cache, zipItem.name)
                FileOutputStream(file).use { fileOutputStream ->
                    val buff = ByteArray(2048)
                    while (true) {
                        val readBytes = zipInputStream.read(buff)
                        if (readBytes <= 0) {
                            break
                        }
                        fileOutputStream.write(buff, 0, readBytes)
                    }
                }
                zipInputStream.closeEntry()
            }
        }
    }

    private fun decodeUnZipFile(cache: File, width: Int, height: Int): DecodeResult {
        if (cache.isSVGAUnzipFile()) {
            val binaryFile = File(cache, movieBinary)
            val jsonFile = File(cache, movieSpec)
            return if (binaryFile.isFile) {
                decodeBinaryFile(cache, binaryFile, width, height)
            } else {
                decodeSpecFile(cache, jsonFile, width, height)
            }
        } else {
            throw  IllegalStateException("Invalidate svga unzip dir, must contain movie.binary or movie.spec file.")
        }
    }

    private fun decodeSource(
        cache: File,
        source: BufferedSource,
        width: Int,
        height: Int
    ): DecodeResult {
        return DecodeResult(
            entity = SVGAVideoEntity(
                MovieEntity.ADAPTER.decode(inflate(source.peek().readByteArray())),
                cache,
                width,
                height
            ),
            isSampled = false
        )
    }

    private fun decodeBinaryFile(
        source: File,
        binary: File,
        width: Int,
        height: Int
    ): DecodeResult {
        FileInputStream(binary).use {
            return DecodeResult(
                entity = SVGAVideoEntity(MovieEntity.ADAPTER.decode(it), source, width, height),
                isSampled = false
            )
        }
    }

    private fun decodeSpecFile(
        source: File,
        jsonFile: File,
        width: Int,
        height: Int
    ): DecodeResult {
        FileInputStream(jsonFile).use { fileInputStream ->
            val buffer = ByteArray(2048)
            ByteArrayOutputStream().use { byteArrayOutputStream ->
                while (true) {
                    val size = fileInputStream.read(buffer)
                    if (size == -1) {
                        break
                    }
                    byteArrayOutputStream.write(buffer, 0, size)
                }
                val jsonObj = JSONObject(byteArrayOutputStream.toString())
                return DecodeResult(
                    entity = SVGAVideoEntity(jsonObj, source, width, height),
                    isSampled = false
                )
            }
        }
    }

    private class ExceptionCatchingSource(delegate: Source) : ForwardingSource(delegate) {

        var exception: Exception? = null
            private set

        override fun read(sink: Buffer, byteCount: Long): Long {
            try {
                return super.read(sink, byteCount)
            } catch (e: Exception) {
                exception = e
                throw e
            }
        }
    }


    class Factory @JvmOverloads constructor(
        maxParallelism: Int = DEFAULT_MAX_PARALLELISM
    ) : Decoder.Factory {

        private val parallelismLock = Semaphore(maxParallelism)

        override fun create(
            result: SourceResult,
            options: Options,
            imageLoader: SvgaLoader
        ): Decoder {
            return SVGAVideoEntityDecoder(result.source, options, parallelismLock)
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }

    internal companion object {
        internal const val DEFAULT_MAX_PARALLELISM = 4
    }


}