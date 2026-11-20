package com.vea.cementbagcounter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.DashPathEffect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DetectionOverlay(context: Context) : View(context) {

    var isSetup = true
        set(value) {
            field = value
            invalidate()
        }

    var lineTop = 150f
        set(value) {
            field = value.coerceIn(0f, imageH)
            invalidate()
        }

    var lineBottom = 411f
        set(value) {
            field = value.coerceIn(0f, imageH)
            invalidate()
        }

    var roiLeft = 80f
        set(value) {
            field = value.coerceIn(0f, imageW)
            invalidate()
        }

    var roiRight = 560f
        set(value) {
            field = value.coerceIn(0f, imageW)
            invalidate()
        }

    private var imageW = 640f
    private var imageH = 640f

    val sourceWidth: Float
        get() = imageW

    val sourceHeight: Float
        get() = imageH

    private val isEnglish: Boolean
        get() = context.getSharedPreferences(
            "app_settings",
            Context.MODE_PRIVATE
        ).getString("language", "fa") == "en"

    private var detections: List<Detection> = emptyList()
    private var trackedDetections: List<TrackedDetection> = emptyList()

    private var showingTracked = false
    private var dragging = 0

    private val density = resources.displayMetrics.density
    private val touchThreshold = 40f * density
    private val minimumRoiWidth = 40f

    private val gold = Color.rgb(212, 175, 55)
    private val lightGold = Color.rgb(245, 215, 130)
    private val cyan = Color.rgb(40, 220, 230)
    private val green = Color.rgb(75, 220, 130)
    private val orange = Color.rgb(255, 165, 55)

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = green
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }

    private val predictedBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = orange
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        pathEffect = DashPathEffect(
            floatArrayOf(10f * density, 6f * density),
            0f
        )
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gold
        strokeWidth = 3.5f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val roiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cyan
        strokeWidth = 3f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 13f * density
        typeface = Typeface.DEFAULT_BOLD
    }

    private val labelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xE6191919.toInt()
        style = Paint.Style.FILL
    }

    private val lineLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 12f * density
        typeface = Typeface.DEFAULT_BOLD
    }

    private val lineLabelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gold
        style = Paint.Style.FILL
    }

    private val roiLabelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cyan
        style = Paint.Style.FILL
    }

    private val roiLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 12f * density
        typeface = Typeface.DEFAULT_BOLD
    }

    private val setupMessageBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCC151515.toInt()
        style = Paint.Style.FILL
    }

    private val activeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lightGold
        strokeWidth = 5f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val roiFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x1830DCE6
        style = Paint.Style.FILL
    }

    private val setupHintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gold
        textSize = 11f * density
        typeface = Typeface.DEFAULT_BOLD
    }

    fun setDetections(
        list: List<Detection>,
        w: Int,
        h: Int
    ) {
        detections = list
        updateImageSize(w, h)

        trackedDetections = emptyList()
        showingTracked = false

        invalidate()
    }

    fun setTrackedDetections(
        list: List<TrackedDetection>,
        w: Int,
        h: Int
    ) {
        trackedDetections = list
        updateImageSize(w, h)

        showingTracked = true

        invalidate()
    }

    fun clearDetections() {
        detections = emptyList()
        trackedDetections = emptyList()
        showingTracked = false
        invalidate()
    }

    fun setImageSize(w: Int, h: Int) {
        updateImageSize(w, h)
        invalidate()
    }

    private fun updateImageSize(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return

        val newW = w.toFloat()
        val newH = h.toFloat()

        if (imageW > 0f && imageH > 0f &&
            (newW != imageW || newH != imageH)
        ) {
            val scaleX = newW / imageW
            val scaleY = newH / imageH

            lineTop *= scaleY
            lineBottom *= scaleY
            roiLeft *= scaleX
            roiRight *= scaleX
        }

        imageW = newW
        imageH = newH

        lineTop = lineTop.coerceIn(0f, imageH)
        lineBottom = lineBottom.coerceIn(0f, imageH)
        roiLeft = roiLeft.coerceIn(0f, imageW)
        roiRight = roiRight.coerceIn(0f, imageW)

        if (roiRight - roiLeft < minimumRoiWidth) {
            roiRight = (roiLeft + minimumRoiWidth).coerceAtMost(imageW)
            roiLeft = (roiRight - minimumRoiWidth).coerceAtLeast(0f)
        }
    }

    private fun imageScale(): Float {
        if (
            imageW <= 0f ||
            imageH <= 0f ||
            width <= 0 ||
            height <= 0
        ) {
            return 1f
        }

        return max(
            width / imageW,
            height / imageH
        )
    }

    private fun offsetX(scale: Float): Float {
        return (width - imageW * scale) / 2f
    }

    private fun offsetY(scale: Float): Float {
        return (height - imageH * scale) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width <= 0 || height <= 0) return

        val scale = imageScale()
        val dx = offsetX(scale)
        val dy = offsetY(scale)

        drawRoiArea(canvas, scale, dx, dy)

        if (showingTracked) {
            trackedDetections.forEach { tracked ->
                drawDetection(
                    canvas,
                    tracked.detection,
                    scale,
                    dx,
                    dy,
                    tracked.id,
                    tracked.isPredicted
                )
            }
        } else {
            detections.forEach { detection ->
                drawDetection(
                    canvas,
                    detection,
                    scale,
                    dx,
                    dy,
                    null,
                    false
                )
            }
        }

        drawGuides(canvas, scale, dx, dy)
        drawStatusMessage(canvas)
    }

    private fun drawRoiArea(
        canvas: Canvas,
        scale: Float,
        dx: Float,
        dy: Float
    ) {
        val leftX = roiLeft * scale + dx
        val rightX = roiRight * scale + dx

        canvas.drawRect(
            leftX,
            0f,
            rightX,
            height.toFloat(),
            roiFillPaint
        )
    }

    private fun drawGuides(
        canvas: Canvas,
        scale: Float,
        dx: Float,
        dy: Float
    ) {
        val topY = lineTop * scale + dy
        val bottomY = lineBottom * scale + dy

        val leftX = roiLeft * scale + dx
        val rightX = roiRight * scale + dx

        val currentTopPaint = if (dragging == 1) {
            activeLinePaint
        } else {
            linePaint
        }

        val currentBottomPaint = if (dragging == 2) {
            activeLinePaint
        } else {
            linePaint
        }

        val currentLeftPaint = if (dragging == 3) {
            activeLinePaint
        } else {
            roiPaint
        }

        val currentRightPaint = if (dragging == 4) {
            activeLinePaint
        } else {
            roiPaint
        }

        canvas.drawLine(
            0f,
            topY,
            width.toFloat(),
            topY,
            currentTopPaint
        )

        canvas.drawLine(
            0f,
            bottomY,
            width.toFloat(),
            bottomY,
            currentBottomPaint
        )

        canvas.drawLine(
            leftX,
            0f,
            leftX,
            height.toFloat(),
            currentLeftPaint
        )

        canvas.drawLine(
            rightX,
            0f,
            rightX,
            height.toFloat(),
            currentRightPaint
        )

        drawLineLabel(
            canvas,
            if (isEnglish) "COUNT LINE 1" else "خط شمارش ۱",
            topY
        )

        drawLineLabel(
            canvas,
            if (isEnglish) "COUNT LINE 2" else "خط شمارش ۲",
            bottomY
        )

        drawRoiLabel(
            canvas,
            if (isEnglish) "ROI START" else "شروع محدوده",
            leftX,
            true
        )

        drawRoiLabel(
            canvas,
            if (isEnglish) "ROI END" else "پایان محدوده",
            rightX,
            false
        )

        if (isSetup) {
            drawSetupHint(canvas)
        }
    }

    private fun drawSetupHint(canvas: Canvas) {
        val hint = if (isEnglish) {
            "DRAG LINES TO ADJUST"
        } else {
            "خطوط را لمس کنید و جابه‌جا کنید"
        }

        val textWidth = setupHintPaint.measureText(hint)
        val padding = 10f * density
        val heightHint = 28f * density

        val left = (width - textWidth - padding * 2f) / 2f
        val top = (height - heightHint - 12f * density)
            .coerceAtLeast(0f)

        canvas.drawRoundRect(
            left,
            top,
            left + textWidth + padding * 2f,
            top + heightHint,
            8f * density,
            8f * density,
            setupMessageBackground
        )

        canvas.drawText(
            hint,
            left + padding,
            top + heightHint / 2f + 4f * density,
            setupHintPaint
        )
    }

    private fun drawStatusMessage(canvas: Canvas) {
        val message = if (isEnglish) {
            if (isSetup) {
                "SETUP MODE"
            } else {
                "COUNTING ACTIVE"
            }
        } else {
            if (isSetup) {
                "حالت تنظیم"
            } else {
                "شمارش فعال"
            }
        }

        val padding = 12f * density
        val messageWidth = textPaint.measureText(message)

        val left = 8f * density
        val top = 8f * density
        val messageHeight = 34f * density

        canvas.drawRoundRect(
            left,
            top,
            left + messageWidth + padding * 2f,
            top + messageHeight,
            8f * density,
            8f * density,
            setupMessageBackground
        )

        canvas.drawText(
            message,
            left + padding,
            top + messageHeight / 2f + 5f * density,
            textPaint
        )
    }

    private fun drawLineLabel(
        canvas: Canvas,
        label: String,
        y: Float
    ) {
        val padding = 8f * density
        val textWidth = lineLabelPaint.measureText(label)
        val labelHeight = 25f * density
        val left = 8f * density

        val top = (y - labelHeight / 2f).coerceIn(
            0f,
            (height - labelHeight).coerceAtLeast(0f)
        )

        canvas.drawRoundRect(
            left,
            top,
            left + textWidth + padding * 2f,
            top + labelHeight,
            7f * density,
            7f * density,
            lineLabelBackgroundPaint
        )

        canvas.drawText(
            label,
            left + padding,
            top + labelHeight / 2f + 4f * density,
            lineLabelPaint
        )
    }

    private fun drawRoiLabel(
        canvas: Canvas,
        label: String,
        x: Float,
        isLeft: Boolean
    ) {
        val padding = 8f * density
        val textWidth = roiLabelPaint.measureText(label)
        val labelHeight = 25f * density
        val labelWidth = textWidth + padding * 2f

        val desiredLeft = if (isLeft) {
            x + 8f * density
        } else {
            x - labelWidth - 8f * density
        }

        val left = desiredLeft.coerceIn(
            0f,
            (width - labelWidth).coerceAtLeast(0f)
        )

        val top = 48f * density

        canvas.drawRoundRect(
            left,
            top,
            left + labelWidth,
            top + labelHeight,
            7f * density,
            7f * density,
            roiLabelBackgroundPaint
        )

        canvas.drawText(
            label,
            left + padding,
            top + labelHeight / 2f + 4f * density,
            roiLabelPaint
        )
    }

    private fun drawDetection(
        canvas: Canvas,
        detection: Detection,
        scale: Float,
        dx: Float,
        dy: Float,
        trackId: Int?,
        isPredicted: Boolean
    ) {
        val left = detection.left * scale + dx
        val top = detection.top * scale + dy
        val right = detection.right * scale + dx
        val bottom = detection.bottom * scale + dy

        val currentPaint = if (isPredicted) {
            predictedBoxPaint
        } else {
            boxPaint
        }

        canvas.drawRect(
            left,
            top,
            right,
            bottom,
            currentPaint
        )

        val label = when {
            trackId != null && isPredicted ->
                if (isEnglish) {
                    "ID $trackId | Predicted"
                } else {
                    "ID $trackId | پیش‌بینی"
                }

            trackId != null ->
                "ID $trackId | ${(detection.confidence * 100).toInt()}%"

            else ->
                "${(detection.confidence * 100).toInt()}%"
        }

        val labelWidth =
            textPaint.measureText(label) + 16f * density

        val labelHeight = 27f * density

        val labelTop =
            (top - labelHeight - 4f * density).coerceAtLeast(0f)

        val safeLeft = left.coerceIn(
            0f,
            (width - labelWidth).coerceAtLeast(0f)
        )

        canvas.drawRoundRect(
            safeLeft,
            labelTop,
            safeLeft + labelWidth,
            labelTop + labelHeight,
            6f * density,
            6f * density,
            labelBackgroundPaint
        )

        canvas.drawText(
            label,
            safeLeft + 8f * density,
            labelTop + 18.5f * density,
            textPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isSetup) return false

        val scale = imageScale()

        if (scale <= 0f) return false

        val dx = offsetX(scale)
        val dy = offsetY(scale)

        val imageX = (event.x - dx) / scale
        val imageY = (event.y - dy) / scale

        val topScreenY = lineTop * scale + dy
        val bottomScreenY = lineBottom * scale + dy

        val leftScreenX = roiLeft * scale + dx
        val rightScreenX = roiRight * scale + dx

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                val distances = listOf(
                    abs(event.y - topScreenY),
                    abs(event.y - bottomScreenY),
                    abs(event.x - leftScreenX),
                    abs(event.x - rightScreenX)
                )

                val closest = distances.indices.minByOrNull {
                    distances[it]
                } ?: return false

                if (distances[closest] > touchThreshold) {
                    dragging = 0
                    return false
                }

                dragging = closest + 1

                parent?.requestDisallowInterceptTouchEvent(true)

                invalidate()

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (dragging == 0) return false

                when (dragging) {
                    1 -> {
                        lineTop = imageY.coerceIn(
                            0f,
                            imageH
                        )
                    }

                    2 -> {
                        lineBottom = imageY.coerceIn(
                            0f,
                            imageH
                        )
                    }

                    3 -> {
                        roiLeft = imageX.coerceIn(
                            0f,
                            (roiRight - minimumRoiWidth).coerceAtLeast(0f)
                        )
                    }

                    4 -> {
                        roiRight = imageX.coerceIn(
                            (roiLeft + minimumRoiWidth).coerceAtMost(imageW),
                            imageW
                        )
                    }
                }

                invalidate()

                return true
            }

            MotionEvent.ACTION_UP -> {
                val wasDragging = dragging != 0

                dragging = 0

                parent?.requestDisallowInterceptTouchEvent(false)

                invalidate()

                performClick()

                return wasDragging
            }

            MotionEvent.ACTION_CANCEL -> {
                val wasDragging = dragging != 0

                dragging = 0

                parent?.requestDisallowInterceptTouchEvent(false)

                invalidate()

                return wasDragging
            }
        }

        return dragging != 0
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
