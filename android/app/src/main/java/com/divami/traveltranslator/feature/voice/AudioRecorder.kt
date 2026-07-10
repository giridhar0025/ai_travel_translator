package com.divami.traveltranslator.feature.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Records microphone audio to an AAC/MP4 file the backend STT endpoint accepts
 * (`audio/mp4`). Single responsibility: manage one MediaRecorder lifecycle and
 * hand back the recorded file. Callers own permission checks.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    /** Begins recording to a fresh temp file in the app cache. */
    fun start() {
        stopQuietly()
        val file = File.createTempFile("voice_", ".m4a", context.cacheDir)
        outputFile = file
        recorder = newRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
    }

    /** Stops recording and returns the file, or null if nothing usable was captured. */
    fun stop(): File? {
        val file = outputFile
        try {
            recorder?.stop()
        } catch (_: RuntimeException) {
            // stop() throws if start() captured no audio (too short). Treat as no-op.
            file?.delete()
            return null
        } finally {
            recorder?.release()
            recorder = null
        }
        return file?.takeIf { it.length() > 0 }
    }

    /** Current peak amplitude (0..32767) since the last read — drives the live
     *  waveform. Returns 0 when not recording. */
    fun amplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    /** Aborts an in-progress recording and discards the file. */
    fun cancel() {
        stopQuietly()
        outputFile?.delete()
        outputFile = null
    }

    private fun stopQuietly() {
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
}
