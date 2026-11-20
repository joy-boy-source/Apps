package com.example.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): Boolean {
        return try {
            cancel()
            val dir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
            val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = file

            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(64000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            cancel()
            false
        }
    }

    fun stopAndReadBytes(): ByteArray? {
        val currentRecorder = recorder ?: return null
        val file = outputFile
        recorder = null
        return try {
            currentRecorder.stop()
            currentRecorder.release()
            if (file != null && file.exists() && file.length() > 0L) {
                val bytes = file.readBytes()
                file.delete()
                bytes
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop audio recording", e)
            runCatching { currentRecorder.release() }
            runCatching { file?.delete() }
            null
        } finally {
            outputFile = null
        }
    }

    fun cancel() {
        recorder?.let { r ->
            runCatching { r.stop() }
            runCatching { r.release() }
        }
        recorder = null
        outputFile?.let { runCatching { it.delete() } }
        outputFile = null
    }

    companion object {
        private const val TAG = "VoiceRecorder"
    }
}
