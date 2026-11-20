package com.vea.cementbagcounter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object ImageUtil {

    /**
     * تبدیل ImageProxy دوربین به Bitmap
     *
     * پشتیبانی:
     * - YUV_420_888
     * - RowStride
     * - PixelStride
     * - چرخش تصویر
     * - مدیریت حافظه
     */
    fun toBitmap(image: ImageProxy): Bitmap {

        val width = image.width
        val height = image.height

        require(width > 0 && height > 0) {
            "Invalid camera image dimensions"
        }

        require(width % 2 == 0 && height % 2 == 0) {
            "YUV image dimensions must be even"
        }

        require(image.format == ImageFormat.YUV_420_888) {
            "Unsupported image format: ${image.format}"
        }

        val planes = image.planes

        require(planes.size >= 3) {
            "Invalid YUV image planes"
        }

        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuffer = yPlane.buffer.duplicate()
        val uBuffer = uPlane.buffer.duplicate()
        val vBuffer = vPlane.buffer.duplicate()

        val ySize = width * height
        val uvSize = ySize / 2

        val nv21 = ByteArray(ySize + uvSize)

        var outputIndex = 0

        // -----------------------------
        // Y Channel
        // -----------------------------

        val yBasePosition = yBuffer.position()

        for (row in 0 until height) {

            val rowStart =
                yBasePosition + row * yPlane.rowStride

            for (col in 0 until width) {

                val index =
                    rowStart + col * yPlane.pixelStride

                if (index < yBuffer.position() || index >= yBuffer.limit()) {
                    throw IllegalStateException(
                        "Y plane buffer overflow"
                    )
                }

                nv21[outputIndex++] = yBuffer.get(index)
            }
        }

        // -----------------------------
        // UV Channels
        // NV21 = V followed by U
        // -----------------------------

        val uBasePosition = uBuffer.position()
        val vBasePosition = vBuffer.position()

        for (row in 0 until height / 2) {

            val uRowStart =
                uBasePosition + row * uPlane.rowStride

            val vRowStart =
                vBasePosition + row * vPlane.rowStride

            for (col in 0 until width / 2) {

                val uIndex =
                    uRowStart + col * uPlane.pixelStride

                val vIndex =
                    vRowStart + col * vPlane.pixelStride

                if (
                    uIndex < uBuffer.position() ||
                    uIndex >= uBuffer.limit() ||
                    vIndex < vBuffer.position() ||
                    vIndex >= vBuffer.limit()
                ) {
                    throw IllegalStateException(
                        "UV plane buffer overflow"
                    )
                }

                nv21[outputIndex++] = vBuffer.get(vIndex)
                nv21[outputIndex++] = uBuffer.get(uIndex)
            }
        }

        // -----------------------------
        // NV21 -> JPEG -> Bitmap
        // -----------------------------

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            width,
            height,
            null
        )

        val jpegOutput = ByteArrayOutputStream()

        val compressed = yuvImage.compressToJpeg(
            Rect(0, 0, width, height),
            85,
            jpegOutput
        )

        if (!compressed) {
            throw IllegalStateException(
                "YUV to JPEG conversion failed"
            )
        }

        val jpegBytes = jpegOutput.toByteArray()

        var bitmap = BitmapFactory.decodeByteArray(
            jpegBytes,
            0,
            jpegBytes.size
        ) ?: throw IllegalStateException(
            "Bitmap decoding failed"
        )

        // -----------------------------
        // Rotate image
        // -----------------------------

        val rotation = image.imageInfo.rotationDegrees

        if (rotation != 0) {

            val matrix = Matrix().apply {
                postRotate(rotation.toFloat())
            }

            val rotatedBitmap = try {

                Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )

            } catch (e: Exception) {

                bitmap.recycle()

                throw IllegalStateException(
                    "Image rotation failed",
                    e
                )
            }

            if (rotatedBitmap !== bitmap) {
                bitmap.recycle()
                bitmap = rotatedBitmap
            }
        }

        return bitmap
    }

    /**
     * تبدیل فریم دوربین به Bitmap با رزولوشن کنترل‌شده
     *
     * برای کاهش مصرف RAM و پردازش،
     * ابعاد تصویر از maxDimension بیشتر نمی‌شود.
     */
    fun toBitmapScaled(
        image: ImageProxy,
        maxDimension: Int = 1280
    ): Bitmap {

        val bitmap = toBitmap(image)

        if (maxDimension <= 0) {
            return bitmap
        }

        val largestDimension = max(
            bitmap.width,
            bitmap.height
        )

        if (largestDimension <= maxDimension) {
            return bitmap
        }

        val scale =
            maxDimension.toFloat() / largestDimension

        val newWidth =
            (bitmap.width * scale)
                .toInt()
                .coerceAtLeast(1)

        val newHeight =
            (bitmap.height * scale)
                .toInt()
                .coerceAtLeast(1)

        val scaledBitmap = try {

            Bitmap.createScaledBitmap(
                bitmap,
                newWidth,
                newHeight,
                true
            )

        } catch (e: Exception) {

            bitmap.recycle()

            throw IllegalStateException(
                "Bitmap scaling failed",
                e
            )
        }

        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
        }

        return scaledBitmap
    }

    /**
     * محاسبه بزرگ‌ترین مقدار
     */
    private fun max(
        a: Int,
        b: Int
    ): Int {

        return if (a > b) a else b
    }
}
