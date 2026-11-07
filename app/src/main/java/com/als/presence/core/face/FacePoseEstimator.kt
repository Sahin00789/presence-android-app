package com.als.presence.core.face

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Estimates the 3D head pose (Center, Left, Right, Up, Down) based on 
 * the relative positioning of the 5 facial landmarks detected by YuNet,
 * and verifies camera frame spatial centering and facial symmetry.
 */
internal object FacePoseEstimator {

    /**
     * Checks if the detected face bounding box is spatially centered within the camera frame.
     * [maxOffsetRatio] defines the maximum allowed offset from the frame center (default 25%).
     */
    fun isCenteredInFrame(
        box: FaceBox,
        frameWidth: Int,
        frameHeight: Int,
        maxOffsetRatio: Float = 0.25f
    ): Boolean {
        if (frameWidth <= 0 || frameHeight <= 0) return true
        val centerX = (box.left + box.right) / 2f
        val centerY = (box.top + box.bottom) / 2f
        val normOffsetX = abs(centerX - frameWidth / 2f) / (frameWidth / 2f)
        val normOffsetY = abs(centerY - frameHeight / 2f) / (frameHeight / 2f)
        return normOffsetX <= maxOffsetRatio && normOffsetY <= maxOffsetRatio
    }

    /**
     * Verifies proper 2D facial landmark symmetry for a centered face:
     * 1. Left-right nose symmetry relative to both eyes (ratio within 0.85 .. 1.18).
     * 2. Left-right nose symmetry relative to mouth corners (ratio within 0.85 .. 1.18).
     * 3. Eye level alignment / tilt ratio (horizontal levelness of eyes).
     */
    fun isSymmetricallyCentered(landmarks: List<FacePoint>): Boolean {
        if (landmarks.size < 5) return false

        val rightEye = landmarks[0]
        val leftEye = landmarks[1]
        val nose = landmarks[2]
        val rightMouth = landmarks[3]
        val leftMouth = landmarks[4]

        val distNoseToRightEye = distance(nose, rightEye)
        val distNoseToLeftEye = distance(nose, leftEye)

        if (distNoseToLeftEye <= 1e-6) return false
        val eyeSymmetryRatio = distNoseToRightEye / distNoseToLeftEye

        val distNoseToRightMouth = distance(nose, rightMouth)
        val distNoseToLeftMouth = distance(nose, leftMouth)

        if (distNoseToLeftMouth <= 1e-6) return false
        val mouthSymmetryRatio = distNoseToRightMouth / distNoseToLeftMouth

        val eyeDist = distance(rightEye, leftEye)
        if (eyeDist <= 1e-6) return false

        val eyeLevelDiff = abs(rightEye.y - leftEye.y)
        val tiltRatio = eyeLevelDiff / eyeDist

        return eyeSymmetryRatio in 0.85..1.18 &&
                mouthSymmetryRatio in 0.85..1.18 &&
                tiltRatio < 0.15
    }

    /**
     * Returns one of: "CENTER", "LEFT", "RIGHT", "UP", "DOWN"
     */
    fun estimatePose(landmarks: List<FacePoint>): String {
        if (landmarks.size < 5) return "CENTER"
        
        val rightEye = landmarks[0] // User's right eye (appears on the left side of the image if not mirrored)
        val leftEye = landmarks[1]  // User's left eye
        val nose = landmarks[2]
        val rightMouth = landmarks[3]
        val leftMouth = landmarks[4]

        val eyeCenterY = (rightEye.y + leftEye.y) / 2.0
        val mouthCenterY = (rightMouth.y + leftMouth.y) / 2.0

        val distNoseToRightEye = distance(nose, rightEye)
        val distNoseToLeftEye = distance(nose, leftEye)

        // Horizontal Checks (Left/Right)
        // If the nose is significantly closer to one eye than the other, the head is turned.
        // We use a threshold of 1.35x distance difference.
        if (distNoseToRightEye / distNoseToLeftEye > 1.35) return "LEFT"
        if (distNoseToLeftEye / distNoseToRightEye > 1.35) return "RIGHT"

        // Vertical Checks (Up/Down)
        val distNoseToEyesY = abs(nose.y - eyeCenterY)
        val distNoseToMouthY = abs(mouthCenterY - nose.y)

        // If the nose is vertically much closer to the eyes, the head is tilted UP.
        if (distNoseToEyesY / distNoseToMouthY < 0.75) return "UP"
        
        // If the nose is vertically much closer to the mouth, the head is tilted DOWN.
        if (distNoseToMouthY / distNoseToEyesY < 0.75) return "DOWN"

        return "CENTER"
    }

    private fun distance(p1: FacePoint, p2: FacePoint): Double {
        return sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y))
    }
}
