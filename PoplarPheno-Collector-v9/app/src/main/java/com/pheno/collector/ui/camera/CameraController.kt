package com.pheno.collector.ui.camera

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

class CameraController(private val lifecycleOwner: LifecycleOwner) {
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val ctx by lazy { lifecycleOwner as Context }
    private val mainExecutor by lazy { ContextCompat.getMainExecutor(ctx) }

    fun createPreviewView(): PreviewView {
        val previewView = PreviewView(ctx).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, mainExecutor)

        return previewView
    }

    /**
     * 拍照并自动在主线程回调结果（解决闪退问题）
     */
    fun takePicture(
        outputOptions: ImageCapture.OutputFileOptions,
        onResult: (Boolean, ImageCaptureException?) -> Unit
    ) {
        imageCapture?.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                mainExecutor.execute { onResult(true, null) }
            }
            override fun onError(exc: ImageCaptureException) {
                mainExecutor.execute { onResult(false, exc) }
            }
        })
    }

    fun release() {
        executor.shutdown()
    }
}
