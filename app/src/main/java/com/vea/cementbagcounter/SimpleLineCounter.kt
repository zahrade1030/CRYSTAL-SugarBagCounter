package com.vea.cementbagcounter

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class TrackedDetection(
    val id: Int,
    val detection: Detection,
    val isPredicted: Boolean = false
)

data class TrackingUpdate(
    val newCounts: Int,
    val trackedDetections: List<TrackedDetection>
)

/**
 * Lightweight tracker for a single-class conveyor counter.
 *
 * Features:
 * - Track ID persistence
 * - Motion prediction
 * - Detection association
 * - ROI filtering
 * - Counting between two horizontal lines
 * - Prevention of duplicate counting
 * - Temporary tracking during missed detections
 */
class SimpleLineCounter {

    private data class Track(
        var x: Float,
        var y: Float,

        var left: Float,
        var top: Float,
        var right: Float,
        var bottom: Float,

        var velocityX: Float = 0f,
        var velocityY: Float = 0f,

        var missed: Int = 0,
        var hits: Int = 1,
        var age: Int = 1,

        var armed: Boolean = false,
        var counted: Boolean = false,

        var lastDetection: Detection? = null,
        var previousY: Float = 0f,
        var confidence: Float = 0f
    )

    private val tracks = mutableMapOf<Int, Track>()

    private var nextId = 1

    // Counting lines
    private var topLine = 150f
    private var bottomLine = 411f

    // Region of interest
    private var roiLeft = 0f
    private var roiRight = Float.MAX_VALUE

    // Tracking configuration
    private val maxMissedFrames = 15
    private val maxAssociationDistance = 140f
    private val maxVelocity = 100f

    private val predictionLimit = 5
    private val minimumHitsToCount = 2

    /**
     * Configure counting lines.
     */
    fun setLines(
        top: Float,
        bottom: Float
    ) {

        if (!top.isFinite() || !bottom.isFinite()) {
            return
        }

        topLine = min(top, bottom)
        bottomLine = max(top, bottom)
    }

    /**
     * Configure horizontal ROI.
     */
    fun setRoi(
        left: Float,
        right: Float
    ) {

        if (!left.isFinite() || !right.isFinite()) {
            return
        }

        roiLeft = min(left, right)
        roiRight = max(left, right)
    }

    /**
     * Reset tracker and counting state.
     */
    fun reset() {

        tracks.clear()
        nextId = 1
    }

    /**
     * Simple counting API.
     */
    fun update(
        detections: List<Detection>
    ): Int {

        return updateWithTracks(detections).newCounts
    }

    /**
     * Update tracks and return detected objects.
     */
    fun updateWithTracks(
        detections: List<Detection>
    ): TrackingUpdate {

        // Update track age and missed-frame count
        tracks.values.forEach { track ->
            track.missed++
            track.age++
        }

        val matchedIds = mutableSetOf<Int>()

        val output = mutableListOf<TrackedDetection>()

        var newCounts = 0

        // Filter invalid detections
        val validDetections = detections
            .filter { detection ->
                detectionInsideRoi(detection) &&
                    detection.confidence.isFinite() &&
                    detection.confidence > 0f &&
                    detection.left.isFinite() &&
                    detection.top.isFinite() &&
                    detection.right.isFinite() &&
                    detection.bottom.isFinite() &&
                    detection.cx.isFinite() &&
                    detection.cy.isFinite() &&
                    detection.width > 0f &&
                    detection.height > 0f
            }
            .sortedByDescending {
                it.confidence
            }

        // -----------------------------------------
        // Associate detections with existing tracks
        // -----------------------------------------

        for (detection in validDetections) {

            val candidate = tracks.entries
                .filter { (id, track) ->

                    id !in matchedIds &&
                        track.missed <= 3 &&
                        isAssociationPossible(
                            track,
                            detection
                        )
                }
                .minByOrNull { entry ->

                    associationCost(
                        entry.value,
                        detection
                    )
                }

            val track: Track
            val trackId: Int

            if (candidate != null) {

                // ---------------------------------
                // Existing track
                // ---------------------------------

                trackId = candidate.key
                track = candidate.value

                matchedIds.add(trackId)

                val previousY = track.y

                val dx = detection.cx - track.x
                val dy = detection.cy - track.y

                val velocityWeight =
                    if (track.missed <= 1) {
                        0.55f
                    } else {
                        0.35f
                    }

                // Smooth horizontal velocity
                track.velocityX = (
                    track.velocityX * (1f - velocityWeight) +
                        dx * velocityWeight
                    ).coerceIn(
                    -maxVelocity,
                    maxVelocity
                )

                // Smooth vertical velocity
                track.velocityY = (
                    track.velocityY * (1f - velocityWeight) +
                        dy * velocityWeight
                    ).coerceIn(
                    -maxVelocity,
                    maxVelocity
                )

                track.previousY = previousY

                // Update position
                track.x = detection.cx
                track.y = detection.cy

                // Update bounding box
                track.left = detection.left
                track.top = detection.top
                track.right = detection.right
                track.bottom = detection.bottom

                // Update track state
                track.missed = 0
                track.hits++

                track.lastDetection = detection
                track.confidence = detection.confidence

                // ---------------------------------
                // Counting logic
                // ---------------------------------

                val wasInsideBand =
                    isInsideBand(previousY)

                val isInsideBand =
                    isInsideBand(track.y)

                val crossedTop =
                    previousY > topLine &&
                        track.y <= topLine

                val crossedBottom =
                    previousY < bottomLine &&
                        track.y >= bottomLine

                val crossedEntireBand =
                    (
                        previousY > bottomLine &&
                            track.y < topLine
                        ) ||
                        (
                            previousY < topLine &&
                                track.y > bottomLine
                            )

                // Arm track when it enters counting area
                if (
                    wasInsideBand ||
                    isInsideBand ||
                    crossedEntireBand
                ) {
                    track.armed = true
                }

                // Count only once per track
                if (
                    track.armed &&
                    !track.counted &&
                    track.hits >= minimumHitsToCount &&
                    (
                        crossedTop ||
                            crossedBottom ||
                            crossedEntireBand
                        )
                ) {

                    track.counted = true

                    newCounts++
                }

            } else {

                // ---------------------------------
                // New track
                // ---------------------------------

                trackId = nextId++

                track = Track(
                    x = detection.cx,
                    y = detection.cy,

                    left = detection.left,
                    top = detection.top,
                    right = detection.right,
                    bottom = detection.bottom,

                    lastDetection = detection,

                    previousY = detection.cy,

                    confidence = detection.confidence,

                    armed = isInsideBand(detection.cy)
                )

                tracks[trackId] = track

                matchedIds.add(trackId)
            }

            output.add(
                TrackedDetection(
                    id = trackId,
                    detection = detection,
                    isPredicted = false
                )
            )
        }

        // -----------------------------------------
        // Predict temporarily missing tracks
        // -----------------------------------------

        tracks.forEach { (id, track) ->

            if (
                id !in matchedIds &&
                track.missed in 1..predictionLimit &&
                track.lastDetection != null
            ) {

                val predicted =
                    predictedDetection(track)

                if (detectionInsideRoi(predicted)) {

                    output.add(
                        TrackedDetection(
                            id = id,
                            detection = predicted,
                            isPredicted = true
                        )
                    )
                }
            }
        }

        // -----------------------------------------
        // Remove expired tracks
        // -----------------------------------------

        tracks.entries.removeAll { entry ->

            entry.value.missed > maxMissedFrames
        }

        return TrackingUpdate(
            newCounts = newCounts,
            trackedDetections = output
        )
    }

    /**
     * Check whether an object is inside the counting band.
     */
    private fun isInsideBand(
        y: Float
    ): Boolean {

        return y > topLine &&
            y < bottomLine
    }

    /**
     * Check whether detection center is inside ROI.
     */
    private fun detectionInsideRoi(
        detection: Detection
    ): Boolean {

        return detection.cx >= roiLeft &&
            detection.cx <= roiRight
    }

    /**
     * Determine whether a detection can be associated
     * with an existing track.
     */
    private fun isAssociationPossible(
        track: Track,
        detection: Detection
    ): Boolean {

        val distance =
            associationDistance(
                track,
                detection
            )

        if (
            !distance.isFinite() ||
            distance > maxAssociationDistance
        ) {
            return false
        }

        val oldWidth =
            max(
                1f,
                track.right - track.left
            )

        val oldHeight =
            max(
                1f,
                track.bottom - track.top
            )

        val newWidth =
            max(
                1f,
                detection.width
            )

        val newHeight =
            max(
                1f,
                detection.height
            )

        val widthRatio =
            newWidth / oldWidth

        val heightRatio =
            newHeight / oldHeight

        // Reject implausible size changes
        return widthRatio in 0.35f..2.8f &&
            heightRatio in 0.35f..2.8f
    }

    /**
     * Calculate distance between predicted track position
     * and incoming detection.
     */
    private fun associationDistance(
        track: Track,
        detection: Detection
    ): Float {

        val predictionFrames =
            track.missed.coerceIn(1, 3)

        val predictedX =
            track.x +
                track.velocityX * predictionFrames

        val predictedY =
            track.y +
                track.velocityY * predictionFrames

        val dx =
            predictedX - detection.cx

        val dy =
            predictedY - detection.cy

        return sqrt(
            dx * dx + dy * dy
        )
    }

    /**
     * Calculate association cost.
     *
     * Lower cost means a better candidate.
     */
    private fun associationCost(
        track: Track,
        detection: Detection
    ): Float {

        val distance =
            associationDistance(
                track,
                detection
            )

        val oldWidth =
            max(
                1f,
                track.right - track.left
            )

        val oldHeight =
            max(
                1f,
                track.bottom - track.top
            )

        val newWidth =
            max(
                1f,
                detection.width
            )

        val newHeight =
            max(
                1f,
                detection.height
            )

        val sizePenalty =
            abs(oldWidth - newWidth) +
                abs(oldHeight - newHeight)

        val confidencePenalty =
            (1f - detection.confidence) * 20f

        return distance +
            sizePenalty * 0.15f +
            confidencePenalty
    }

    /**
     * Predict the location of a temporarily missing object.
     */
    private fun predictedDetection(
        track: Track
    ): Detection {

        val predictionFrames =
            track.missed.coerceAtMost(
                predictionLimit
            )

        val predictedX =
            track.x +
                track.velocityX * predictionFrames

        val predictedY =
            track.y +
                track.velocityY * predictionFrames

        val shiftX =
            predictedX - track.x

        val shiftY =
            predictedY - track.y

        val original =
            track.lastDetection
            ?: throw IllegalStateException(
                "Track has no previous detection"
            )

        return Detection(
            left = original.left + shiftX,
            top = original.top + shiftY,
            right = original.right + shiftX,
            bottom = original.bottom + shiftY,
            confidence = original.confidence
        )
    }
}
