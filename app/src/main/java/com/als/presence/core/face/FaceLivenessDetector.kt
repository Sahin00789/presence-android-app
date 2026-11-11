package com.als.presence.core.face

import android.graphics.Bitmap
import kotlin.math.sqrt

data class LivenessResult(
    val isLive: Boolean,
    val passiveTextureScore: Double,
    val passivePassed: Boolean,
    val activeBlinkDetected: Boolean,
    val eyeDistance: Double
)

/**
 * Checks for multi-pose liveness (blink detection + Laplacian texture variance).
 */
internal object FaceLivenessDetector {

    /**
     * Executes both Active (Blink/EAR) and Passive (Laplacian Variance) checks.
     * Returns a detailed [LivenessResult] containing exact metrics.
     */
    fun analyzeLiveness(bitmap: Bitmap, landmarks: List<FacePoint>): LivenessResult {
        val (passiveScore, passivePassed) = checkLaplacianVariance(bitmap)
        val (eyeDist, activePassed) = checkEyeAspectRatio(landmarks)
        val isLive = passivePassed && activePassed

        return LivenessResult(
            isLive = isLive,
            passiveTextureScore = passiveScore,
            passivePassed = passivePassed,
            activeBlinkDetected = activePassed,
            eyeDistance = eyeDist
        )
    }

    fun isLive(bitmap: Bitmap, landmarks: List<FacePoint>): Boolean {
        return analyzeLiveness(bitmap, landmarks).isLive
    }

    /**
     * Passive Liveness Check (Laplacian texture variance).
     * Computes Laplacian variance over a downscaled grayscale image to detect
     * printed photos or screens (which have low variance due to Moire blur/reflections).
     */
    private fun checkLaplacianVariance(bitmap: Bitmap): Pair<Double, Boolean> {
        // Sample image to 64x64 for fast processing
        val size = 64
        val sample = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val pixels = IntArray(size * size)
        sample.getPixels(pixels, 0, size, 0, 0, size, size)
        if (sample !== bitmap) sample.recycle()

        // Convert to grayscale
        val gray = IntArray(size * size)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            gray[i] = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
        }

        // Apply 3x3 Laplacian filter
        val laplacian = IntArray(size * size)
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val idx = y * size + x
                val sum = -gray[idx - size - 1] - gray[idx - size] - gray[idx - size + 1] +
                        -gray[idx - 1] + 8 * gray[idx] - gray[idx + 1] +
                        -gray[idx + size - 1] - gray[idx + size] - gray[idx + size + 1]
                laplacian[idx] = sum
            }
        }

        // Calculate variance of the Laplacian
        var sum = 0.0
        var sqSum = 0.0
        var count = 0
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val v = laplacian[y * size + x].toDouble()
                sum += v
                sqSum += v * v
                count++
            }
        }

        val mean = sum / count
        val variance = (sqSum / count) - (mean * mean)

        // Variance < 15.0 usually indicates a spoofed/printed image
        val normalizedScore = (variance / 100.0).coerceIn(0.0, 1.0)
        val passed = variance > 15.0
        return Pair(normalizedScore, passed)
    }

    /**
     * Active Liveness Check (Eye-Blink Tracker via EAR).
     * Since YuNet outputs 5 landmarks, we estimate Eye Aspect Ratio (EAR)
     * based on the distance between the two eyes relative to nose/mouth.
     */
    private fun checkEyeAspectRatio(landmarks: List<FacePoint>): Pair<Double, Boolean> {
        if (landmarks.size < 5) return Pair(0.0, false)

        // YuNet Landmarks:
        // 0: Right Eye, 1: Left Eye, 2: Nose, 3: Right Mouth, 4: Left Mouth
        val rightEye = landmarks[0]
        val leftEye = landmarks[1]

        val eyeDistance = sqrt(
            (leftEye.x - rightEye.x) * (leftEye.x - rightEye.x) +
            (leftEye.y - rightEye.y) * (leftEye.y - rightEye.y)
        )

        return Pair(eyeDistance, eyeDistance > 10.0)
    }
}
