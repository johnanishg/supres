package com.example.supres

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import kotlin.math.max
import kotlin.math.min

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    private var scaleDetector: ScaleGestureDetector
    private var matrix: Matrix = Matrix()
    private var mode = NONE
    private var oldDist = 1f
    private var oldScale = 1f
    private var matrixValues = FloatArray(9)
    private var saveScale = 1f
    private var viewWidth = 0
    private var viewHeight = 0
    private var origWidth = 0
    private var origHeight = 0
    private var right = 0f
    private var bottom = 0f
    private var minScale = 1f
    private var maxScale = 4f

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    init {
        scaleType = ScaleType.MATRIX
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        imageMatrix = matrix
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mode = DRAG
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    mode = ZOOM
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    matrix.getValues(matrixValues)
                    val x = matrixValues[Matrix.MTRANS_X]
                    val y = matrixValues[Matrix.MTRANS_Y]
                    val newX = x + (event.x - event.x)
                    val newY = y + (event.y - event.y)
                    if (newX <= 0 && newX >= -right) {
                        matrix.postTranslate(event.x - event.x, 0f)
                    }
                    if (newY <= 0 && newY >= -bottom) {
                        matrix.postTranslate(0f, event.y - event.y)
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(x * x + y * y)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            matrix.getValues(matrixValues)
            val x = matrixValues[Matrix.MTRANS_X]
            val y = matrixValues[Matrix.MTRANS_Y]
            val scale = matrixValues[Matrix.MSCALE_X]

            val newScale = scale * detector.scaleFactor
            if (newScale < maxScale && newScale > minScale) {
                matrix.postScale(detector.scaleFactor, detector.scaleFactor, detector.focusX, detector.focusY)
                matrix.getValues(matrixValues)
                val newX = matrixValues[Matrix.MTRANS_X]
                val newY = matrixValues[Matrix.MTRANS_Y]
                if (newX > 0) matrix.postTranslate(-newX, 0f)
                if (newY > 0) matrix.postTranslate(0f, -newY)
                if (newX < -right) matrix.postTranslate(-(newX + right), 0f)
                if (newY < -bottom) matrix.postTranslate(0f, -(newY + bottom))
                invalidate()
            }
            return true
        }
    }

    override fun setImageBitmap(bm: android.graphics.Bitmap?) {
        super.setImageBitmap(bm)
        if (bm != null) {
            origWidth = bm.width
            origHeight = bm.height
            fitToScreen()
        }
    }

    private fun fitToScreen() {
        val scale: Float
        val scaleX = viewWidth.toFloat() / origWidth.toFloat()
        val scaleY = viewHeight.toFloat() / origHeight.toFloat()
        scale = min(scaleX, scaleY)
        matrix.setScale(scale, scale)
        saveScale = 1f

        // Center the image
        val redundantXSpace = viewWidth - (scale * origWidth)
        val redundantYSpace = viewHeight - (scale * origHeight)
        matrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2)

        right = scale * origWidth - viewWidth
        bottom = scale * origHeight - viewHeight
        invalidate()
    }
} 