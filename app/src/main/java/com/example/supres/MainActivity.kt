package com.example.supres

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var originalImageView: com.example.supres.ZoomableImageView
    private lateinit var upscaledImageView: com.example.supres.ZoomableImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var trainingDataStatus: TextView
    private lateinit var galleryButton: Button
    private lateinit var cameraButton: Button
    private var fsrcnnHelper: FsrcnnHelper? = null
    private lateinit var upscaleButton: Button
    private var originalBitmap: android.graphics.Bitmap? = null
    private var upscaledBitmap: android.graphics.Bitmap? = null
    private val PICK_IMAGE_REQUEST = 1
    private val CAMERA_REQUEST = 2
    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        originalImageView = findViewById(R.id.originalImageView)
        upscaledImageView = findViewById(R.id.upscaledImageView)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        trainingDataStatus = findViewById(R.id.trainingDataStatus)
        galleryButton = findViewById(R.id.galleryButton)
        cameraButton = findViewById(R.id.cameraButton)
        upscaleButton = findViewById(R.id.upscaleButton)

        // Initialize FsrcnnHelper
        fsrcnnHelper = try {
            FsrcnnHelper(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading model: ${e.message}", Toast.LENGTH_LONG).show()
            statusText.text = "Error loading model. Upscaling will use fallback."
            statusText.visibility = android.view.View.VISIBLE
            null
        }

        // Add click listeners
        galleryButton.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                startActivityForResult(intent, PICK_IMAGE_REQUEST)
            } catch (e: Exception) {
                Toast.makeText(this, "Error opening gallery: ${e.message}", Toast.LENGTH_LONG).show()
                statusText.text = "Error opening gallery."
                statusText.visibility = android.view.View.VISIBLE
            }
        }

        cameraButton.setOnClickListener {
            try {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val photoFile = createImageFile()
                photoFile?.let {
                    val photoURI = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.provider", it)
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(intent, CAMERA_REQUEST)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_LONG).show()
                statusText.text = "Error opening camera."
                statusText.visibility = android.view.View.VISIBLE
            }
        }

        upscaleButton.setOnClickListener {
            if (originalBitmap == null) {
                statusText.text = "Please select an image first."
                statusText.visibility = android.view.View.VISIBLE
                return@setOnClickListener
            }
            if (fsrcnnHelper == null) {
                Toast.makeText(this, "Model not loaded. Using fallback.", Toast.LENGTH_LONG).show()
            }
            progressBar.visibility = android.view.View.VISIBLE
            upscaleButton.isEnabled = false
            statusText.text = "Upscaling..."
            statusText.visibility = android.view.View.VISIBLE
            // Start upscaling in background
            Thread {
                try {
                    val result = fsrcnnHelper?.upscale(originalBitmap!!) ?: android.graphics.Bitmap.createScaledBitmap(originalBitmap!!, originalBitmap!!.width * 2, originalBitmap!!.height * 2, true)
                    runOnUiThread {
                        upscaledBitmap = result
                        upscaledImageView.setImageBitmap(result)
                        progressBar.visibility = android.view.View.GONE
                        upscaleButton.isEnabled = true
                        statusText.text = if (fsrcnnHelper?.isModelLoaded == false) "Upscaling complete (fallback used)." else "Upscaling complete."
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        progressBar.visibility = android.view.View.GONE
                        upscaleButton.isEnabled = true
                        statusText.text = "Error during upscaling: ${e.message}"
                        Toast.makeText(this, "Error during upscaling: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }
    }

    private fun createImageFile(): File? {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    data?.data?.let { uri ->
                        try {
                            originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            originalImageView.setImageBitmap(originalBitmap)
                            statusText.visibility = android.view.View.GONE
                        } catch (e: Exception) {
                            statusText.text = "Error loading image: ${e.message}"
                            statusText.visibility = android.view.View.VISIBLE
                            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                CAMERA_REQUEST -> {
                    currentPhotoPath?.let { path ->
                        try {
                            originalBitmap = android.graphics.BitmapFactory.decodeFile(path)
                            originalImageView.setImageBitmap(originalBitmap)
                            statusText.visibility = android.view.View.GONE
                        } catch (e: Exception) {
                            statusText.text = "Error loading image: ${e.message}"
                            statusText.visibility = android.view.View.VISIBLE
                            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}