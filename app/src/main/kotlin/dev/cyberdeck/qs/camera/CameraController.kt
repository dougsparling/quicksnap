package dev.cyberdeck.qs.camera

import android.content.Context
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Finalize
import androidx.camera.video.VideoRecordEvent.Start
import dev.cyberdeck.qs.common.debug
import dev.cyberdeck.qs.common.launchWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class CameraController(
    private val context: Context,
    private val directory: File,
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(executor.asCoroutineDispatcher() + SupervisorJob())
    private val vibrator = context.getSystemService(VibratorManager::class.java)
    private var captureJob: Job? = null

    fun capture(spec: CaptureSpec) {
        captureJob?.cancel()
        captureJob = scope.launchWithLifecycle { lifecycle ->
            val createProvider = async {
                val provider = createCameraProvider()
                val capture = when (spec) {
                    is PhotoSpec -> quickJpgCapture()
                    is VideoSpec -> videoCapture()
                }
                withContext(Dispatchers.Main) {
                    provider.bindToLifecycle(lifecycle, spec.camera, capture)
                }
                provider to capture
            }

            delay(spec.delay)

            val (provider, capture) = createProvider.await()

            when (spec) {
                is PhotoSpec -> capturePhoto(spec, capture as ImageCapture)
                is VideoSpec -> captureVideo(spec, capture as VideoCapture<*>)
            }

            withContext(Dispatchers.Main) {
                provider.unbindAll()
            }
        }
    }

    private suspend fun capturePhoto(
        spec: PhotoSpec,
        capture: ImageCapture
    ) = repeat(spec.count) {
        val file = outputFileWithTs(ext = "jpg")
        val output = ImageCapture.OutputFileOptions
            .Builder(file)
            .build()

        capture.takePicture(output)
        buzz(150)
        debug("Photo capture succeeded (${file.absolutePath}")
        delay(1.seconds)
    }

    private suspend fun captureVideo(
        spec: VideoSpec,
        capture: VideoCapture<*>,
    ) = coroutineScope {
        val file = outputFileWithTs(ext = "mp4")
        val output = FileOutputOptions.Builder(file).build()

        val (events, recorder) = (capture.output as Recorder)
            .prepareRecording(context, output)
            .start()

        buzz(50, 100)

        // TODO: why are there multiple start events
        var stop: Job? = null

        for (event in events) {
            when (event) {
                is VideoRecordEvent.Status -> {
                    debug("Recorded ${event.recordingStats.numBytesRecorded} bytes over ${event.recordingStats.recordedDurationNanos.nanoseconds.inWholeMilliseconds} millis")
                }

                is Start -> {
                    stop = stop ?: launch {
                        delay(spec.duration)
                        debug("calling stop()")
                        recorder.stop()
                        buzz(100, 150)
                    }
                }

                is Finalize -> {
                    debug("Finalize (stopped): error = ${event.error}, cause ${event.cause}")
                }
            }
        }

        // just in case we stopped early
        stop?.cancel()

        debug("Recorded video to ${file.absolutePath}")
    }

    private fun quickJpgCapture() = ImageCapture.Builder()
        .setJpegQuality(80)
        .setFlashMode(ImageCapture.FLASH_MODE_OFF)
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setIoExecutor(executor)
        .build()

    private fun videoCapture() = VideoCapture.withOutput(
        Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.FHD,
                    FallbackStrategy.lowerQualityThan(Quality.FHD)
                )
            )
            .setExecutor(executor)
            .build()
    )

    private fun outputFileWithTs(ext: String): File {
        val name = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
            .format(System.currentTimeMillis())
        return File(directory, "$name.$ext")
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

    private fun PendingRecording.start(): Pair<Channel<VideoRecordEvent>, Recording> {
        val events = Channel<VideoRecordEvent>(capacity = 16)
        val recorder = start(executor) { event ->
            events.trySend(event)
            if (event is Finalize) {
                events.close(event.cause)
            }
        }
        return events to recorder
    }

    private suspend fun buzz(vararg timingsMs: Long) {
        for (timingMs in timingsMs) {
            val effect = VibrationEffect.createOneShot(timingMs, VibrationEffect.EFFECT_TICK)
            withContext(Dispatchers.Main) {
                vibrator.vibrate(CombinedVibration.createParallel(effect))
            }
            delay(100.milliseconds)
        }
    }

    sealed class CaptureSpec(
        val delay: Duration,
        val camera: CameraSelector
    )

    class PhotoSpec(
        delay: Duration,
        val count: Int,
        camera: CameraSelector,
    ) : CaptureSpec(delay, camera)

    class VideoSpec(
        delay: Duration,
        val duration: Duration,
        camera: CameraSelector,
    ) : CaptureSpec(delay, camera)
}


