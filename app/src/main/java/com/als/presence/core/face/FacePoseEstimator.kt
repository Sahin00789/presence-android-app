package com.als.presence.core.face

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Estimates the 3D head pose (Center, Left, Right, Up, Down) based on 
 * the relative positioning of the 5 facial landmarks detected by YuNet.
 */
internal object FacePoseEstimator {

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
