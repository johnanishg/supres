package com.example.supres

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class FsrcnnHelper(private val context: Context) {
    private var interpreter: Interpreter? = null

    companion object {
        private const val MODEL_FILE = "fsrcnn_x2.tflite"
        private const val TAG = "FsrcnnHelper"
    }

    init {
        Log.d(TAG, "FsrcnnHelper initialized")
        try {
            // Check if model file exists
            val modelFile = context.assets.list("")?.find { it == MODEL_FILE }
            if (modelFile == null) {
                Log.e(TAG, "Model file $MODEL_FILE not found in assets")
                throw RuntimeException("Model file $MODEL_FILE not found in assets")
            }
            Log.d(TAG, "Model file found in assets")

            // Load model file
            val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            Log.d(TAG, "Model file loaded successfully")

            // Initialize interpreter
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                Log.d(TAG, "Set interpreter to use 4 threads")
            }
            interpreter = Interpreter(buffer, options)
            Log.d(TAG, "Interpreter initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FsrcnnHelper: ${e.message}", e)
            interpreter = null
        }
    }

    fun upscale(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        return try {
            Log.d(TAG, "Upscaling image of size ${bitmap.width}x${bitmap.height}")
            if (interpreter == null) {
                Log.e(TAG, "Interpreter is null, using fallback resize")
                return android.graphics.Bitmap.createScaledBitmap(bitmap, bitmap.width * 2, bitmap.height * 2, true)
            }
            // Ensure input buffer size matches model's expected input shape (1xHxWx3)
            val inputBuffer = ByteBuffer.allocateDirect(1 * bitmap.height * bitmap.width * 3 * 4)
            inputBuffer.order(java.nio.ByteOrder.nativeOrder())
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            for (pixel in pixels) {
                inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
                inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
                inputBuffer.putFloat((pixel and 0xFF) / 255.0f)
            }
            inputBuffer.rewind() // Ensure the buffer is ready for reading
            val outputBuffer = ByteBuffer.allocateDirect(1 * bitmap.height * 2 * bitmap.width * 2 * 3 * 4)
            outputBuffer.order(java.nio.ByteOrder.nativeOrder())
            interpreter?.run(inputBuffer, outputBuffer)
            val outputPixels = IntArray(bitmap.width * 2 * bitmap.height * 2)
            outputBuffer.rewind()
            for (i in outputPixels.indices) {
                val r = (outputBuffer.float * 255).toInt().coerceIn(0, 255)
                val g = (outputBuffer.float * 255).toInt().coerceIn(0, 255)
                val b = (outputBuffer.float * 255).toInt().coerceIn(0, 255)
                outputPixels[i] = (r shl 16) or (g shl 8) or b
            }
            android.graphics.Bitmap.createBitmap(outputPixels, bitmap.width * 2, bitmap.height * 2, android.graphics.Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            Log.e(TAG, "Upscaling failed: ${e.message}", e)
            // Fallback: simple resize
            android.graphics.Bitmap.createScaledBitmap(bitmap, bitmap.width * 2, bitmap.height * 2, true)
        }
    }

    val isModelLoaded: Boolean
        get() = interpreter != null
} 