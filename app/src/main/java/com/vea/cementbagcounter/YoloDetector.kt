package com.vea.cementbagcounter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import ai.onnxruntime.*
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

data class Detection(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float
) {
    val cx get() = (left + right) / 2f
    val cy get() = (top + bottom) / 2f
    val width get() = right - left
    val height get() = bottom - top
}

class YoloDetector(context: Context) {

    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val inputName: String

    private val inputSize = 416

    companion object {
        private const val TAG = "YoloDetector"
        private const val CONFIDENCE_THRESHOLD = 0.30f
        private const val NMS_THRESHOLD = 0.45f
        private const val MAX_DETECTIONS = 60
        private const val EXPECTED_CLASSES = 1
    }

    init {
        val modelBytes = context.assets.open("best.onnx").use {
            it.readBytes()
        }

        val options = OrtSession.SessionOptions().apply {
            setOptimizationLevel(
                OrtSession.SessionOptions.OptLevel.ALL_OPT
            )
            setIntraOpNumThreads(2)
            setInterOpNumThreads(1)
        }

        session = try {
            env.createSession(modelBytes, options)
        } finally {
            options.close()
        }

        inputName = session.inputNames.firstOrNull()
            ?: throw IllegalStateException("مدل ورودی معتبر ندارد")

        Log.i(TAG, "Model loaded successfully")
        Log.i(TAG, "Input: $inputName")
        Log.i(TAG, "Outputs: ${session.outputNames}")
    }

    fun detect(source: Bitmap): List<Detection> {

        if (
            source.isRecycled ||
            source.width <= 0 ||
            source.height <= 0
        ) {
            return emptyList()
        }

        var letterboxed: Bitmap? = null
        var resizeRatio = 1f
        var padX = 0f
        var padY = 0f

        try {
            // YOLO-style models are normally trained/inferred with letterboxing.
            // Preserve the source aspect ratio instead of stretching the frame.
            resizeRatio = min(
                inputSize.toFloat() / source.width,
                inputSize.toFloat() / source.height
            )

            val resizedWidth = (source.width * resizeRatio)
                .toInt()
                .coerceIn(1, inputSize)

            val resizedHeight = (source.height * resizeRatio)
                .toInt()
                .coerceIn(1, inputSize)

            padX = ((inputSize - resizedWidth) / 2).toFloat()
            padY = ((inputSize - resizedHeight) / 2).toFloat()

            letterboxed = Bitmap.createBitmap(
                inputSize,
                inputSize,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(letterboxed)
            canvas.drawColor(Color.rgb(114, 114, 114))

            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(
                source,
                null,
                Rect(
                    padX.toInt(),
                    padY.toInt(),
                    padX.toInt() + resizedWidth,
                    padY.toInt() + resizedHeight
                ),
                paint
            )

            val pixels = IntArray(inputSize * inputSize)

            letterboxed.getPixels(
                pixels,
                0,
                inputSize,
                0,
                0,
                inputSize,
                inputSize
            )

            val plane = inputSize * inputSize
            val input = FloatArray(3 * plane)

            for (i in pixels.indices) {
                val pixel = pixels[i]
                input[i] = ((pixel shr 16) and 255) / 255f
                input[plane + i] = ((pixel shr 8) and 255) / 255f
                input[2 * plane + i] = (pixel and 255) / 255f
            }

            OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(input),
                longArrayOf(
                    1,
                    3,
                    inputSize.toLong(),
                    inputSize.toLong()
                )
            ).use { tensor ->

                session.run(
                    mapOf(inputName to tensor)
                ).use { result ->

                    if (result.size() == 0) {
                        return emptyList()
                    }

                    val outputTensor =
                        result[0] as? OnnxTensor
                            ?: return emptyList()

                    // اصلاح خطای shape
                    val tensorInfo =
                        outputTensor.info as TensorInfo

                    val shape = tensorInfo.shape

                    Log.d(
                        TAG,
                        "Output shape: ${shape.contentToString()}"
                    )

                    val output = flatten(
                        outputTensor.value
                    )

                    return decode(
                        output,
                        shape,
                        source.width.toFloat(),
                        source.height.toFloat(),
                        resizeRatio,
                        padX,
                        padY
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Detection failed", e)
            return emptyList()

        } finally {
            letterboxed?.let {
                if (!it.isRecycled) it.recycle()
            }
        }
    }

    private fun flatten(value: Any): FloatArray {

        val output = ArrayList<Float>()

        fun walk(v: Any?) {
            when (v) {
                is FloatArray -> {
                    for (item in v) {
                        output.add(item)
                    }
                }

                is DoubleArray -> {
                    for (item in v) {
                        output.add(item.toFloat())
                    }
                }

                is Array<*> -> {
                    for (item in v) {
                        if (item != null) {
                            walk(item)
                        }
                    }
                }

                is Number -> {
                    output.add(v.toFloat())
                }
            }
        }

        walk(value)

        return output.toFloatArray()
    }

    private fun decode(
        data: FloatArray,
        shape: LongArray,
        srcW: Float,
        srcH: Float,
        resizeRatio: Float,
        padX: Float,
        padY: Float
    ): List<Detection> {

        if (
            data.isEmpty() ||
            srcW <= 0f ||
            srcH <= 0f
        ) {
            return emptyList()
        }

        if (shape.size < 2) {
            Log.w(TAG, "Unsupported output shape")
            return emptyList()
        }

        val dimensions = shape.map {
            it.toInt()
        }

        val dim1 = dimensions[dimensions.size - 2]
        val dim2 = dimensions[dimensions.size - 1]

        val channelFirst = dim1 <= 100 && dim2 > dim1

        val channels = if (channelFirst) {
            dim1
        } else {
            dim2
        }

        val candidates = if (channelFirst) {
            dim2
        } else {
            dim1
        }

        if (
            channels < 5 ||
            candidates <= 0 ||
            data.size < channels * candidates
        ) {
            Log.w(
                TAG,
                "Unsupported dimensions: ${shape.contentToString()}"
            )
            return emptyList()
        }

        // Single-class YOLOv8 exports use 4 box values + class score (5 channels).
        // Single-class YOLOv5 exports use 4 box values + objectness + class score (6).
        val hasObjectness = channels == 5 + EXPECTED_CLASSES

        val classCount = when {
            channels == 4 + EXPECTED_CLASSES -> EXPECTED_CLASSES
            hasObjectness -> EXPECTED_CLASSES
            channels > 4 -> channels - 4
            else -> 1
        }

        val out = mutableListOf<Detection>()

        for (i in 0 until candidates) {

            fun at(c: Int): Float {
                return if (channelFirst) {
                    data[c * candidates + i]
                } else {
                    data[i * channels + c]
                }
            }

            val cx = at(0)
            val cy = at(1)
            val w = at(2)
            val h = at(3)

            if (
                !cx.isFinite() ||
                !cy.isFinite() ||
                !w.isFinite() ||
                !h.isFinite() ||
                w <= 0f ||
                h <= 0f
            ) {
                continue
            }

            var confidence = 0f

            if (channels == 5) {

                confidence = at(4)

            } else if (hasObjectness) {

                val objectness = at(4)

                var bestClass = 0f

                for (c in 0 until classCount) {
                    val score = at(5 + c)

                    if (
                        score.isFinite() &&
                        score > bestClass
                    ) {
                        bestClass = score
                    }
                }

                confidence = objectness * bestClass

            } else {

                var bestClass = 0f

                for (c in 0 until classCount) {
                    val score = at(4 + c)

                    if (
                        score.isFinite() &&
                        score > bestClass
                    ) {
                        bestClass = score
                    }
                }

                confidence = bestClass
            }

            if (
                !confidence.isFinite() ||
                confidence < CONFIDENCE_THRESHOLD ||
                confidence > 1.01f
            ) {
                continue
            }

            // Remove letterbox padding, then map model-space pixels back
            // into the original frame coordinates.
            val sourceCx = (cx - padX) / resizeRatio
            val sourceCy = (cy - padY) / resizeRatio
            val sourceW = w / resizeRatio
            val sourceH = h / resizeRatio

            val left = max(0f, sourceCx - sourceW / 2f)
            val top = max(0f, sourceCy - sourceH / 2f)
            val right = min(srcW, sourceCx + sourceW / 2f)
            val bottom = min(srcH, sourceCy + sourceH / 2f)

            if (
                right <= left ||
                bottom <= top
            ) {
                continue
            }

            out.add(
                Detection(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    confidence = confidence
                )
            )
        }

        return nms(
            out,
            NMS_THRESHOLD
        ).take(MAX_DETECTIONS)
    }

    private fun nms(
        items: List<Detection>,
        threshold: Float
    ): List<Detection> {

        if (items.isEmpty()) {
            return emptyList()
        }

        val sorted = items
            .sortedByDescending {
                it.confidence
            }
            .toMutableList()

        val kept = mutableListOf<Detection>()

        while (
            sorted.isNotEmpty() &&
            kept.size < MAX_DETECTIONS
        ) {
            val best = sorted.removeAt(0)

            kept.add(best)

            val iterator = sorted.iterator()

            while (iterator.hasNext()) {
                val candidate = iterator.next()

                if (iou(best, candidate) > threshold) {
                    iterator.remove()
                }
            }
        }

        return kept
    }

    private fun iou(
        a: Detection,
        b: Detection
    ): Float {

        val x1 = max(a.left, b.left)
        val y1 = max(a.top, b.top)

        val x2 = min(a.right, b.right)
        val y2 = min(a.bottom, b.bottom)

        val interWidth = max(
            0f,
            x2 - x1
        )

        val interHeight = max(
            0f,
            y2 - y1
        )

        val inter = interWidth * interHeight

        val areaA =
            max(0f, a.right - a.left) *
            max(0f, a.bottom - a.top)

        val areaB =
            max(0f, b.right - b.left) *
            max(0f, b.bottom - b.top)

        val union = areaA + areaB - inter

        return if (union <= 0f) {
            0f
        } else {
            inter / union
        }
    }

    fun close() {
        try {
            session.close()
            Log.i(TAG, "Session closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing session", e)
        }
    }
}
