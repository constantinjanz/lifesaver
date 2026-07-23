package com.lifesaver.service

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * A short voice note the user records to their future self; it plays at the intervention, in their
 * own voice, at the moment they reach for the feed. Stored as one file in app storage.
 */
object FutureSelf {
    fun file(context: Context): File = File(context.filesDir, "future_self.m4a")
    fun exists(context: Context): Boolean = file(context).let { it.exists() && it.length() > 0 }
    fun delete(context: Context): Boolean = file(context).delete()

    fun play(context: Context, onDone: () -> Unit = {}): MediaPlayer? = runCatching {
        MediaPlayer().apply {
            setDataSource(file(context).absolutePath)
            setOnCompletionListener { runCatching { release() }; onDone() }
            prepare()
            start()
        }
    }.getOrNull()
}

/** Simple MIC recorder that writes to [FutureSelf.file]. Call [start] then [stop]. */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null

    fun start(): Boolean = runCatching {
        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setOutputFile(FutureSelf.file(context).absolutePath)
        r.prepare()
        r.start()
        recorder = r
        true
    }.getOrDefault(false)

    fun stop() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
    }
}
