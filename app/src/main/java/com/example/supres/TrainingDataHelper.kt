package com.example.supres

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrainingDataHelper(private val context: Context) {
    private val TAG = "TrainingDataHelper"
    private val trainingDataDir: File
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    init {
        trainingDataDir = File(context.getExternalFilesDir(null), "training_data")
        if (!trainingDataDir.exists()) {
            trainingDataDir.mkdirs()
        }
    }

    fun saveTrainingPair(originalBitmap: Bitmap, upscaledBitmap: Bitmap) {
        try {
            val timestamp = dateFormat.format(Date())
            val originalFile = File(trainingDataDir, "original_$timestamp.png")
            val upscaledFile = File(trainingDataDir, "upscaled_$timestamp.png")

            // Save original image
            FileOutputStream(originalFile).use { out ->
                originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Save upscaled image
            FileOutputStream(upscaledFile).use { out ->
                upscaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Save metadata
            val metadataFile = File(trainingDataDir, "metadata_$timestamp.txt")
            metadataFile.writeText("""
                Original Image: ${originalFile.name}
                Upscaled Image: ${upscaledFile.name}
                Original Size: ${originalBitmap.width}x${originalBitmap.height}
                Upscaled Size: ${upscaledBitmap.width}x${upscaledBitmap.height}
                Timestamp: $timestamp
            """.trimIndent())

            Log.d(TAG, "Saved training pair: $timestamp")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving training pair: ${e.message}")
        }
    }

    fun getTrainingDataCount(): Int {
        return trainingDataDir.listFiles { file -> 
            file.name.startsWith("original_") && file.name.endsWith(".png")
        }?.size ?: 0
    }

    fun clearTrainingData() {
        trainingDataDir.listFiles()?.forEach { it.delete() }
    }
} 