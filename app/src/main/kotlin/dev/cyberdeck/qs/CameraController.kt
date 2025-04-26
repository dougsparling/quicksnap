package dev.cyberdeck.qs

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration

class CameraController(
    private val context: Context,
    private val directory: File,
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(executor.asCoroutineDispatcher() + SupervisorJob())

    private var captureJob: Job? = null

    fun capture(selector: CameraSelector, delay: Duration, count: Int) {
        captureJob?.cancel()
        captureJob = scope.launchAsLifecycle { lifecycle ->
            val createProvider = async { createCameraProvider() }

            val provider = createProvider.await()
            val capture = quickJpgCapture()
            withContext(Dispatchers.Main) {
                provider.bindToLifecycle(lifecycle, selector, capture)
            }

            delay(delay)
            val output = outputFileWithTs()
            val res = capture.takePicture(output)
            Log.i("CameraController", "Photo capture succeeded (${output.file?.absolutePath}: $res")
        }
    }

    private fun quickJpgCapture() = ImageCapture.Builder()
        .setJpegQuality(100)
        .setFlashMode(ImageCapture.FLASH_MODE_OFF)
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setIoExecutor(executor)
        .build()

    private fun outputFileWithTs(): ImageCapture.OutputFileOptions {
        val name =
            SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())
        val filename = File(directory, "$name.jpg")
        return ImageCapture.OutputFileOptions.Builder(filename).build()
    }

    private suspend fun createCameraProvider() = suspendCoroutine<ProcessCameraProvider> { cont ->
        val instance = ProcessCameraProvider.getInstance(context)
        instance.addListener({ cont.resumeWith(runCatching { instance.get() }) }, executor)
    }

    private suspend fun ImageCapture.takePicture(
        outputFileOptions: ImageCapture.OutputFileOptions
    ) = suspendCoroutine { cont ->
        takePicture(outputFileOptions, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                cont.resumeWithException(exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                cont.resume(output)
            }
        })
    }
}


private fun CoroutineScope.launchAsLifecycle(block: suspend CoroutineScope.(LifecycleOwner) -> Unit): Job {
    val lifecycle = CustomLifecycle()
    return launch {
        try {
            lifecycle.start()
            block(lifecycle)
        } finally {
            lifecycle.stop()
        }
    }
}

class CustomLifecycle : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    suspend fun start() = withContext(Dispatchers.Main) {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    suspend fun stop() = withContext(Dispatchers.Main) {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override val lifecycle = lifecycleRegistry
}
