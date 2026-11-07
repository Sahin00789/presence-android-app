package com.als.presence.core.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FacePoseEstimatorTest {

    @Test
    fun isCenteredInFrame_returnsTrue_whenFaceIsCentered() {
        val frameWidth = 640
        val frameHeight = 480
        val centeredBox = FaceBox(
            left = 220f,
            top = 140f,
            right = 420f,
            bottom = 340f
        )
        assertTrue(FacePoseEstimator.isCenteredInFrame(centeredBox, frameWidth, frameHeight))
    }

    @Test
    fun isCenteredInFrame_returnsFalse_whenFaceIsOffCenterHorizontal() {
        val frameWidth = 640
        val frameHeight = 480
        val offCenterBox = FaceBox(
            left = 10f,
            top = 140f,
            right = 110f,
            bottom = 340f
        )
        assertFalse(FacePoseEstimator.isCenteredInFrame(offCenterBox, frameWidth, frameHeight))
    }

    @Test
    fun isCenteredInFrame_returnsFalse_whenFaceIsOffCenterVertical() {
        val frameWidth = 640
        val frameHeight = 480
        val offCenterBox = FaceBox(
            left = 220f,
            top = 10f,
            right = 420f,
            bottom = 90f
        )
        assertFalse(FacePoseEstimator.isCenteredInFrame(offCenterBox, frameWidth, frameHeight))
    }

    @Test
    fun isSymmetricallyCentered_returnsTrue_forSymmetricLandmarks() {
        val symmetricLandmarks = listOf(
            FacePoint(40.0, 50.0), // Right eye
            FacePoint(80.0, 50.0), // Left eye
            FacePoint(60.0, 70.0), // Nose (centered at X=60)
            FacePoint(45.0, 90.0), // Right mouth corner
            FacePoint(75.0, 90.0)  // Left mouth corner (centered at X=60)
        )
        assertTrue(FacePoseEstimator.isSymmetricallyCentered(symmetricLandmarks))
        assertEquals("CENTER", FacePoseEstimator.estimatePose(symmetricLandmarks))
    }

    @Test
    fun isSymmetricallyCentered_returnsFalse_forAsymmetricLandmarks() {
        val asymmetricLandmarks = listOf(
            FacePoint(40.0, 50.0), // Right eye
            FacePoint(80.0, 50.0), // Left eye
            FacePoint(75.0, 70.0), // Nose shifted far towards left eye
            FacePoint(45.0, 90.0), // Right mouth corner
            FacePoint(75.0, 90.0)  // Left mouth corner
        )
        assertFalse(FacePoseEstimator.isSymmetricallyCentered(asymmetricLandmarks))
    }

    @Test
    fun isSymmetricallyCentered_returnsFalse_forTiltedHead() {
        val tiltedLandmarks = listOf(
            FacePoint(40.0, 30.0), // Right eye high
            FacePoint(80.0, 70.0), // Left eye low (steep tilt)
            FacePoint(60.0, 50.0), // Nose
            FacePoint(45.0, 70.0), // Right mouth
            FacePoint(75.0, 90.0)  // Left mouth
        )
        assertFalse(FacePoseEstimator.isSymmetricallyCentered(tiltedLandmarks))
    }
}
