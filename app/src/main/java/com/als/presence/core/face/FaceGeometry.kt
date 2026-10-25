package com.als.presence.core.face

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure 2-D geometry helpers used by the on-device face pipeline.
 *
 * Keeping these calculations free of Android graphics types makes the
 * alignment and NMS logic unit-testable on the JVM.
 */
internal data class FacePoint(val x: Double, val y: Double)

internal data class FaceBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val area: Float get() = width * height
}

internal data class FaceDetection(
    val box: FaceBox,
    val score: Float,
    val landmarks: List<FacePoint>
)

/**
 * Similarity transform:
 *
 *     x' = a*x + b*y + tx
 *     y' = c*x + d*y + ty
 */
internal data class SimilarityTransform(
    val a: Double,
    val b: Double,
    val tx: Double,
    val c: Double,
    val d: Double,
    val ty: Double
) {
    fun apply(point: FacePoint): FacePoint = FacePoint(
        x = a * point.x + b * point.y + tx,
        y = c * point.x + d * point.y + ty
    )

    /** Row-major 3x3 values accepted by Android's [android.graphics.Matrix.setValues]. */
    fun matrixValues(): FloatArray = floatArrayOf(
        a.toFloat(), b.toFloat(), tx.toFloat(),
        c.toFloat(), d.toFloat(), ty.toFloat(),
        0f, 0f, 1f
    )
}

internal object FaceGeometry {
    /**
     * ArcFace/SFace reference landmarks for a 112x112 aligned crop.
     * Order: right eye, left eye, nose, right mouth corner, left mouth corner.
     */
    val SFACE_REFERENCE: List<FacePoint> = listOf(
        FacePoint(38.2946, 51.6963),
        FacePoint(73.5318, 51.5014),
        FacePoint(56.0252, 71.7366),
        FacePoint(41.5493, 92.3655),
        FacePoint(70.7299, 92.2041)
    )

    /**
     * Least-squares similarity transform (uniform scale + rotation + translation)
     * mapping [src] onto [dst]. A proper rotation is used, so a mirrored face is
     * not silently accepted.
     */
    fun similarityTransform(
        src: List<FacePoint>,
        dst: List<FacePoint> = SFACE_REFERENCE
    ): SimilarityTransform {
        require(src.size == dst.size && src.size >= 3) {
            "Need at least three matched point pairs"
        }

        val count = src.size.toDouble()
        val srcMeanX = src.sumOf { it.x } / count
        val srcMeanY = src.sumOf { it.y } / count
        val dstMeanX = dst.sumOf { it.x } / count
        val dstMeanY = dst.sumOf { it.y } / count

        var sumDxSx = 0.0
        var sumDxSy = 0.0
        var sumDySx = 0.0
        var sumDySy = 0.0
        var sumSrcSquared = 0.0

        for (index in src.indices) {
            val sx = src[index].x - srcMeanX
            val sy = src[index].y - srcMeanY
            val dx = dst[index].x - dstMeanX
            val dy = dst[index].y - dstMeanY
            sumDxSx += dx * sx
            sumDxSy += dx * sy
            sumDySx += dy * sx
            sumDySy += dy * sy
            sumSrcSquared += sx * sx + sy * sy
        }

        if (sumSrcSquared < 1e-12) {
            return SimilarityTransform(1.0, 0.0, dstMeanX - srcMeanX, 0.0, 1.0, dstMeanY - srcMeanY)
        }

        val angle = atan2(sumDySx - sumDxSy, sumDxSx + sumDySy)
        val cosine = cos(angle)
        val sine = sin(angle)
        val numerator = cosine * (sumDxSx + sumDySy) + sine * (sumDySx - sumDxSy)
        val scale = (numerator / sumSrcSquared).coerceAtLeast(1e-9)

        val a = scale * cosine
        val b = -scale * sine
        val c = scale * sine
        val d = scale * cosine
        val tx = dstMeanX - (a * srcMeanX + b * srcMeanY)
        val ty = dstMeanY - (c * srcMeanX + d * srcMeanY)

        return SimilarityTransform(a, b, tx, c, d, ty)
    }

    fun intersectionOverUnion(first: FaceBox, second: FaceBox): Float {
        val left = maxOf(first.left, second.left)
        val top = maxOf(first.top, second.top)
        val right = minOf(first.right, second.right)
        val bottom = minOf(first.bottom, second.bottom)
        val intersectionWidth = (right - left).coerceAtLeast(0f)
        val intersectionHeight = (bottom - top).coerceAtLeast(0f)
        val intersection = intersectionWidth * intersectionHeight
        val union = first.area + second.area - intersection
        return if (union <= 0f) 0f else intersection / union
    }

    fun nonMaxSuppression(
        detections: List<FaceDetection>,
        iouThreshold: Float,
        topK: Int
    ): List<FaceDetection> {
        if (topK <= 0) return emptyList()
        val kept = ArrayList<FaceDetection>(minOf(topK, detections.size))
        for (candidate in detections.sortedByDescending { it.score }) {
            if (kept.size >= topK) break
            if (kept.none { intersectionOverUnion(it.box, candidate.box) > iouThreshold }) {
                kept += candidate
            }
        }
        return kept
    }
}
