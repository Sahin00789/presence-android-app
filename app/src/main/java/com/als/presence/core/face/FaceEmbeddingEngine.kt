package com.als.presence.core.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * On-device face pipeline backed by ONNX Runtime Mobile.
 *
 * Two models are bundled in `assets/`:
 *
 *  * `yunet.onnx` — YuNet face detector (bounding box + five landmarks);
 *  * `sface.onnx` — SFace recogniser (112x112 aligned crop -> 128-D embedding).
 *
 * The frame is aligned with the five detected landmarks before recognition, so
 * the embedding is stable across camera angles and device rotations. Only the
 * normalised numeric embedding leaves the device; the captured frame is
 * discarded immediately.
 *
 * If either model is missing the engine falls back to a low-fidelity luminance
 * signature **only for debuggable builds** so the end-to-end flow can still be
 * exercised. Release builds refuse to produce a template until both models are
 * provisioned (`docs/FACE_VERIFICATION.md`).
 */
@Singleton
class FaceEmbeddingEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val DETECTOR_ASSET = "yunet.onnx"
        private const val RECOGNIZER_ASSET = "sface.onnx"
        private const val DETECTOR_INPUT_SIZE = 640
        private const val RECOGNIZER_INPUT_SIZE = 112
        private const val DETECTION_SCORE_THRESHOLD = 0.70f
        private const val NMS_IOU_THRESHOLD = 0.30f
        private const val MAX_FACE_CANDIDATES = 5
        private const val MIN_QUALITY_SCORE = 0.12
        private const val TAG = "FaceEmbeddingEngine"

        /**
         * Bump whenever the bundled detector/recogniser pair or preprocessing
         * changes. The backend stores the version alongside each template.
         */
        const val MODEL_VERSION = "sface-yunet-v1"
        const val FALLBACK_MODEL_VERSION = "fallback-dev-v1"

        private val YUNET_STRIDES = intArrayOf(8, 16, 32)
    }

    data class Embedding(
        val vector: List<Double>,
        val modelVersion: String,
        val qualityScore: Double
    )

    class FaceModelUnavailableException(message: String) : IllegalStateException(message)

    private data class Models(
        val detector: OrtSession,
        val recognizer: OrtSession
    )

    private data class LetterboxedImage(
        val bitmap: Bitmap,
        val scaleX: Float,
        val scaleY: Float,
        val offsetX: Float,
        val offsetY: Float
    ) {
        fun recycle() {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private val environment: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private val models: Models? by lazy { loadModels() }

    private fun loadModels(): Models? {
        return try {
            val detector = loadSession(DETECTOR_ASSET)
            try {
                val recognizer = loadSession(RECOGNIZER_ASSET)
                Models(detector, recognizer)
            } catch (error: Exception) {
                runCatching { detector.close() }
                throw error
            }
        } catch (error: Exception) {
            Log.w(TAG, "ONNX face models are not available: ${error.message}")
            null
        }
    }

    private fun loadSession(assetName: String): OrtSession {
        val bytes = context.assets.open(assetName).use { it.readBytes() }
        return OrtSession.SessionOptions().use { options ->
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            options.setIntraOpNumThreads(2)
            options.setInterOpNumThreads(1)
            environment.createSession(bytes, options)
        }
    }

    /**
     * Internal method called by the auto-capture wizard to process a single frame 
     * against a specific target pose. Throws specific feedback exceptions if the 
     * conditions aren't met, allowing the UI to guide the user.
     */
    suspend fun captureSpecificPose(bitmap: Bitmap, targetPose: String): Embedding = withContext(Dispatchers.Default) {
        val frameQuality = qualityScore(bitmap)
        if (frameQuality < MIN_QUALITY_SCORE) {
            throw FaceModelUnavailableException("Lighting is too poor. Move to a well-lit area.")
        }

        val activeModels = models
        if (activeModels == null) {
            if (!isDebuggable()) {
                throw FaceModelUnavailableException("Face verification models are not provisioned.")
            }
            return@withContext Embedding(
                vector = fallbackEmbedding(bitmap),
                modelVersion = FALLBACK_MODEL_VERSION,
                qualityScore = frameQuality
            )
        }

        val detection = detectFace(activeModels.detector, bitmap)
            ?: throw FaceModelUnavailableException("No face detected in frame. Please look at the camera.")

        // 1. Pose Estimation Check
        val estimatedPose = FacePoseEstimator.estimatePose(detection.landmarks)
        if (estimatedPose != targetPose) {
            throw FaceModelUnavailableException("Please look $targetPose. (Currently looking $estimatedPose)")
        }

        // 2. Liveness Check
        val isLive = FaceLivenessDetector.isLive(bitmap, detection.landmarks)
        if (!isLive) {
            throw FaceModelUnavailableException("Liveness check failed. Blink naturally.")
        }

        // 3. Align and Extract 128-D Embedding
        val aligned = alignFace(bitmap, detection.landmarks)
        try {
            val cropQuality = qualityScore(aligned)
            val quality = minOf(frameQuality, cropQuality, detection.score.toDouble()).coerceIn(0.0, 1.0)
            if (quality < MIN_QUALITY_SCORE) {
                throw FaceModelUnavailableException("Face is not clear enough. Hold steady.")
            }
            val vector = runRecognizer(activeModels.recognizer, aligned)
            Embedding(vector, MODEL_VERSION, quality)
        } finally {
            if (!aligned.isRecycled) aligned.recycle()
        }
    }

    suspend fun embed(bitmap: Bitmap): Embedding = withContext(Dispatchers.Default) {
        val frameQuality = qualityScore(bitmap)
        if (frameQuality < MIN_QUALITY_SCORE) {
            throw FaceModelUnavailableException(
                "Face frame is too dark or blurry. Move to a well-lit area and try again."
            )
        }

        val activeModels = models
        if (activeModels == null) {
            if (!isDebuggable()) {
                throw FaceModelUnavailableException(
                    "Face verification models are not provisioned. Contact your administrator."
                )
            }
            return@withContext Embedding(
                vector = fallbackEmbedding(bitmap),
                modelVersion = FALLBACK_MODEL_VERSION,
                qualityScore = frameQuality
            )
        }

        val detection = detectFace(activeModels.detector, bitmap)
            ?: throw FaceModelUnavailableException(
                "No face detected. Centre your face in the frame and try again."
            )
            
        // Check for Multi-Pose Liveness
        val isLive = FaceLivenessDetector.isLive(bitmap, detection.landmarks)
        if (!isLive) {
            throw FaceModelUnavailableException(
                "Liveness check failed. Please blink naturally and avoid using photos/screens."
            )
        }

        val aligned = alignFace(bitmap, detection.landmarks)
        try {
            val cropQuality = qualityScore(aligned)
            val quality = minOf(frameQuality, cropQuality, detection.score.toDouble())
                .coerceIn(0.0, 1.0)
            if (quality < MIN_QUALITY_SCORE) {
                throw FaceModelUnavailableException(
                    "Face is not clear enough. Improve lighting and hold the camera steady."
                )
            }
            Embedding(
                vector = runRecognizer(activeModels.recognizer, aligned),
                modelVersion = MODEL_VERSION,
                qualityScore = quality
            )
        } finally {
            if (!aligned.isRecycled) aligned.recycle()
        }
    }

    private fun isDebuggable(): Boolean =
        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

    // ---------------------------------------------------------------------
    // YuNet detection
    // ---------------------------------------------------------------------

    private fun detectFace(session: OrtSession, bitmap: Bitmap): FaceDetection? {
        val letterboxed = letterbox(bitmap, DETECTOR_INPUT_SIZE)
        try {
            val side = DETECTOR_INPUT_SIZE
            val plane = side * side
            val pixels = IntArray(plane)
            letterboxed.bitmap.getPixels(pixels, 0, side, 0, 0, side, side)

            // YuNet was exported from OpenCV and expects raw BGR values (0..255).
            val data = FloatArray(3 * plane)
            for (index in pixels.indices) {
                val pixel = pixels[index]
                data[index] = (pixel and 0xFF).toFloat()
                data[plane + index] = ((pixel shr 8) and 0xFF).toFloat()
                data[2 * plane + index] = ((pixel shr 16) and 0xFF).toFloat()
            }

            val inputName = session.inputNames.first()
            val tensor = OnnxTensor.createTensor(
                environment,
                FloatBuffer.wrap(data),
                longArrayOf(1, 3, side.toLong(), side.toLong())
            )

            val detections = ArrayList<FaceDetection>(32)
            tensor.use {
                session.run(mapOf(inputName to tensor)).use { result ->
                    for (stride in YUNET_STRIDES) {
                        val cols = side / stride
                        val rows = side / stride
                        val scores = readFloats(result, "cls_$stride")
                        val objectness = readFloats(result, "obj_$stride")
                        val boxes = readFloats(result, "bbox_$stride")
                        val keypoints = readFloats(result, "kps_$stride")

                        for (row in 0 until rows) {
                            for (col in 0 until cols) {
                                val index = row * cols + col
                                val classification = scores[index].coerceIn(0f, 1f)
                                val objectScore = objectness[index].coerceIn(0f, 1f)
                                val score = sqrt(classification * objectScore)
                                if (score < DETECTION_SCORE_THRESHOLD) continue

                                val boxIndex = index * 4
                                val centerX = (col + boxes[boxIndex]) * stride
                                val centerY = (row + boxes[boxIndex + 1]) * stride
                                val width = exp(boxes[boxIndex + 2]) * stride
                                val height = exp(boxes[boxIndex + 3]) * stride

                                val inputBox = FaceBox(
                                    left = centerX - width / 2f,
                                    top = centerY - height / 2f,
                                    right = centerX + width / 2f,
                                    bottom = centerY + height / 2f
                                )
                                val inputLandmarks = ArrayList<FacePoint>(5)
                                for (landmark in 0 until 5) {
                                    val base = index * 10 + landmark * 2
                                    inputLandmarks += FacePoint(
                                        x = (keypoints[base] + col).toDouble() * stride,
                                        y = (keypoints[base + 1] + row).toDouble() * stride
                                    )
                                }

                                detections += FaceDetection(
                                    box = mapBox(inputBox, letterboxed, bitmap),
                                    score = score,
                                    landmarks = inputLandmarks.map {
                                        mapPoint(it, letterboxed, bitmap)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            return FaceGeometry.nonMaxSuppression(
                detections = detections,
                iouThreshold = NMS_IOU_THRESHOLD,
                topK = MAX_FACE_CANDIDATES
            ).maxByOrNull { rank(it, bitmap) }
        } finally {
            letterboxed.recycle()
        }
    }

    private fun readFloats(result: OrtSession.Result, name: String): FloatArray {
        val tensor = result.get(name).orElseThrow {
            IllegalStateException("Face detector output '$name' is missing")
        } as OnnxTensor
        val buffer = tensor.floatBuffer
        val values = FloatArray(buffer.remaining())
        buffer.get(values)
        return values
    }

    private fun letterbox(source: Bitmap, side: Int): LetterboxedImage {
        val scale = min(side.toFloat() / source.width, side.toFloat() / source.height)
        val drawWidth = (source.width * scale).roundToInt().coerceIn(1, side)
        val drawHeight = (source.height * scale).roundToInt().coerceIn(1, side)
        val offsetX = (side - drawWidth) / 2f
        val offsetY = (side - drawHeight) / 2f

        val output = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)
        val scaled = Bitmap.createScaledBitmap(source, drawWidth, drawHeight, true)
        canvas.drawBitmap(
            scaled,
            offsetX,
            offsetY,
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        )
        if (scaled !== source) scaled.recycle()

        return LetterboxedImage(
            bitmap = output,
            scaleX = drawWidth.toFloat() / source.width,
            scaleY = drawHeight.toFloat() / source.height,
            offsetX = offsetX,
            offsetY = offsetY
        )
    }

    private fun mapBox(
        box: FaceBox,
        letterbox: LetterboxedImage,
        target: Bitmap
    ): FaceBox = FaceBox(
        left = ((box.left - letterbox.offsetX) / letterbox.scaleX)
            .coerceIn(0f, target.width.toFloat()),
        top = ((box.top - letterbox.offsetY) / letterbox.scaleY)
            .coerceIn(0f, target.height.toFloat()),
        right = ((box.right - letterbox.offsetX) / letterbox.scaleX)
            .coerceIn(0f, target.width.toFloat()),
        bottom = ((box.bottom - letterbox.offsetY) / letterbox.scaleY)
            .coerceIn(0f, target.height.toFloat())
    )

    private fun mapPoint(
        point: FacePoint,
        letterbox: LetterboxedImage,
        target: Bitmap
    ): FacePoint = FacePoint(
        x = ((point.x - letterbox.offsetX) / letterbox.scaleX)
            .coerceIn(0.0, target.width.toDouble()),
        y = ((point.y - letterbox.offsetY) / letterbox.scaleY)
            .coerceIn(0.0, target.height.toDouble())
    )

    /** Favour the largest, most central face when several people are in frame. */
    private fun rank(detection: FaceDetection, bitmap: Bitmap): Float {
        val centerX = (detection.box.left + detection.box.right) / 2f
        val centerY = (detection.box.top + detection.box.bottom) / 2f
        val normalizedX = (centerX - bitmap.width / 2f) / max(1f, bitmap.width / 2f)
        val normalizedY = (centerY - bitmap.height / 2f) / max(1f, bitmap.height / 2f)
        val distance = sqrt(normalizedX * normalizedX + normalizedY * normalizedY)
            .coerceIn(0f, 1f)
        return detection.score * (1f - 0.35f * distance)
    }

    // ---------------------------------------------------------------------
    // SFace alignment + embedding
    // ---------------------------------------------------------------------

    private fun alignFace(source: Bitmap, landmarks: List<FacePoint>): Bitmap {
        val transform = FaceGeometry.similarityTransform(landmarks)
        val matrix = Matrix().apply { setValues(transform.matrixValues()) }
        val aligned = Bitmap.createBitmap(
            RECOGNIZER_INPUT_SIZE,
            RECOGNIZER_INPUT_SIZE,
            Bitmap.Config.ARGB_8888
        )
        Canvas(aligned).apply {
            drawColor(Color.BLACK)
            drawBitmap(
                source,
                matrix,
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            )
        }
        return aligned
    }

    private fun runRecognizer(session: OrtSession, aligned: Bitmap): List<Double> {
        val side = RECOGNIZER_INPUT_SIZE
        val plane = side * side
        val pixels = IntArray(plane)
        aligned.getPixels(pixels, 0, side, 0, 0, side, side)

        // SFace expects RGB in 0..255; the graph itself performs
        // (pixel - 127.5) / 128 normalisation.
        val data = FloatArray(3 * plane)
        for (index in pixels.indices) {
            val pixel = pixels[index]
            data[index] = ((pixel shr 16) and 0xFF).toFloat()
            data[plane + index] = ((pixel shr 8) and 0xFF).toFloat()
            data[2 * plane + index] = (pixel and 0xFF).toFloat()
        }

        val inputName = session.inputNames.first()
        val tensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(data),
            longArrayOf(1, 3, side.toLong(), side.toLong())
        )
        tensor.use {
            session.run(mapOf(inputName to tensor)).use { result ->
                val output = result[0] as OnnxTensor
                val buffer = output.floatBuffer
                val values = DoubleArray(buffer.remaining())
                for (index in values.indices) values[index] = buffer.get(index).toDouble()
                return normalise(values)
            }
        }
    }

    private fun normalise(values: DoubleArray): List<Double> {
        val magnitude = sqrt(values.sumOf { it * it })
        if (magnitude <= 1e-9) {
            throw FaceModelUnavailableException("Face embedding could not be computed. Try again.")
        }
        return values.map { it / magnitude }
    }

    /**
     * Debug-only fallback: a 16x8 luminance signature. It is deliberately not
     * presented as biometric-grade verification and is never used in release.
     */
    private fun fallbackEmbedding(bitmap: Bitmap): List<Double> {
        val width = 8
        val height = 16
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        if (scaled !== bitmap) scaled.recycle()
        val values = DoubleArray(pixels.size) { index ->
            val pixel = pixels[index]
            val red = ((pixel shr 16) and 0xFF)
            val green = ((pixel shr 8) and 0xFF)
            val blue = (pixel and 0xFF)
            (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
        }
        return normalise(values)
    }

    private fun qualityScore(bitmap: Bitmap): Double {
        val sample = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        val pixels = IntArray(32 * 32)
        sample.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        if (sample !== bitmap) sample.recycle()
        val luminance = DoubleArray(pixels.size) { index ->
            val pixel = pixels[index]
            val red = ((pixel shr 16) and 0xFF)
            val green = ((pixel shr 8) and 0xFF)
            val blue = (pixel and 0xFF)
            (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
        }
        val mean = luminance.average()
        val variance = luminance.sumOf { (it - mean) * (it - mean) } / luminance.size
        val contrast = sqrt(variance)
        val brightnessPenalty = if (mean < 0.12 || mean > 0.95) 0.0 else 1.0
        return (contrast * 3.5).coerceIn(0.0, 1.0) * brightnessPenalty
    }
}
