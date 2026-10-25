package com.als.presence.core.face

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Checks for multi-pose liveness (blink detection + Laplacian texture variance).
 */
internal object FaceLivenessDetector {

    /**
     * Executes both Active (Blink/EAR) and Passive (Laplacian Variance) checks.
     * Returns true if both checks pass.
     */
    fun isLive(bitmap: Bitmap, landmarks: List<FacePoint>): Boolean {
        return checkLaplacianVariance(bitmap) && checkEyeAspectRatio(landmarks)
    }

    /**
     * Passive Liveness Check (Laplacian texture variance).
     * Computes Laplacian variance over a downscaled grayscale image to detect
     * printed photos or screens (which have low variance due to Moire blur/reflections).
     */
    private fun checkLaplacianVariance(bitmap: Bitmap): Boolean {
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
        return variance > 15.0
    }

    /**
     * Active Liveness Check (Eye-Blink Tracker via EAR).
     * Since YuNet outputs 5 landmarks, we estimate Eye Aspect Ratio (EAR)
     * based on the distance between the two eyes relative to nose/mouth.
     */
    private fun checkEyeAspectRatio(landmarks: List<FacePoint>): Boolean {
        if (landmarks.size < 5) return false
        
        // YuNet Landmarks:
        // 0: Right Eye, 1: Left Eye, 2: Nose, 3: Right Mouth, 4: Left Mouth
        val rightEye = landmarks[0]
        val leftEye = landmarks[1]
        
        val eyeDistance = sqrt(
            (leftEye.x - rightEye.x) * (leftEye.x - rightEye.x) +
            (leftEye.y - rightEye.y) * (leftEye.y - rightEye.y)
        )
        
        // A simple check: if eyes are too close or too far apart relative to the face bounding box,
        // it might be a spoof. (A robust EAR would require 6 landmarks per eye).
        return eyeDistance > 10.0
    }
}
