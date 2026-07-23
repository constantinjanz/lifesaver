package com.lifesaver.service

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Short voice notes the user records to their future self, one per pause length (rung 1/2/3 =
 * 1st / 2nd / 3rd+ open ≈ 5s / 15s / 30s). The matching note plays on the pause screen and is cut
 * off when the countdown ends. If a rung has no note, the nearest shorter one is used.
 */
object FutureSelf {
    /** Standard pause seconds per rung, for labelling the recorder. */
    val RUNG_SECONDS = mapOf(1 to 5, 2 to 15, 3 to 30)

    fun file(context: Context, rung: Int): File =
        File(context.filesDir, "future_self_${rung.coerceIn(1, 3)}.m4a")

    fun exists(context: Context, rung: Int): Boolean =
        file(context, rung).let { it.exists() && it.length() > 0 }

    fun anyExists(context: Context): Boolean = (1..3).any { exists(context, it) }

    fun delete(context: Context, rung: Int): Boolean = file(context, rung).delete()

    /** The note to play for [rung]: exact, else the nearest recorded shorter rung, else null. */
    fun noteFor(context: Context, rung: Int): File? {
        for (r in rung.coerceIn(1, 3) downTo 1) if (exists(context, r)) return file(context, r)
        return null
    }

    fun play(context: Context, rung: Int): MediaPlayer? {
        val f = noteFor(context, rung) ?: return null
        return runCatching {
            MediaPlayer().apply {
                setDataSource(f.absolutePath)
                setOnCompletionListener { runCatching { release() } }
                prepare()
                start()
            }
        }.getOrNull()
    }
}

/** MIC recorder writing to a specific file. Call [start] then [stop]. */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null

    fun start(target: File): Boolean = runCatching {
        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setOutputFile(target.absolutePath)
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
