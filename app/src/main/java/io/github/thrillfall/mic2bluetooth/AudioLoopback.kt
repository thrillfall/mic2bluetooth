package io.github.thrillfall.mic2bluetooth

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlin.math.max

enum class LoopbackMode { A2DP_HIGH_QUALITY, SCO_LOW_LATENCY }

class AudioLoopback(private val ctx: Context) {

    @Volatile private var running = false
    private var thread: Thread? = null
    private var savedAudioMode: Int = AudioManager.MODE_NORMAL
    private var routedComm = false

    val isRunning: Boolean get() = running

    @SuppressLint("MissingPermission")
    fun start(mode: LoopbackMode, onError: (String) -> Unit) {
        if (running) return

        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val sampleRate: Int
        val source: Int
        val usage: Int
        val contentType: Int

        if (mode == LoopbackMode.SCO_LOW_LATENCY) {
            val scoDev = am.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
            if (scoDev == null) {
                onError("No Bluetooth HFP/SCO device available. Pair a headset/earbuds (most pure speakers don't support SCO).")
                return
            }
            savedAudioMode = am.mode
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            val ok = am.setCommunicationDevice(scoDev)
            if (!ok) {
                am.mode = savedAudioMode
                onError("Failed to route audio to Bluetooth SCO")
                return
            }
            routedComm = true
            sampleRate = 16_000
            source = MediaRecorder.AudioSource.VOICE_COMMUNICATION
            usage = AudioAttributes.USAGE_VOICE_COMMUNICATION
            contentType = AudioAttributes.CONTENT_TYPE_SPEECH
        } else {
            sampleRate = 44_100
            source = MediaRecorder.AudioSource.MIC
            usage = AudioAttributes.USAGE_MEDIA
            contentType = AudioAttributes.CONTENT_TYPE_MUSIC
        }

        running = true
        thread = Thread({
            val inChannel = AudioFormat.CHANNEL_IN_MONO
            val outChannel = AudioFormat.CHANNEL_OUT_MONO
            val encoding = AudioFormat.ENCODING_PCM_16BIT

            val minIn = AudioRecord.getMinBufferSize(sampleRate, inChannel, encoding)
            val minOut = AudioTrack.getMinBufferSize(sampleRate, outChannel, encoding)
            if (minIn <= 0 || minOut <= 0) {
                running = false
                cleanupRouting()
                onError("Unsupported audio config")
                return@Thread
            }
            val bufSize = max(minIn, minOut) * 2

            var record: AudioRecord? = null
            var track: AudioTrack? = null
            try {
                record = AudioRecord(source, sampleRate, inChannel, encoding, bufSize)
                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    onError("AudioRecord init failed")
                    return@Thread
                }

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(usage)
                            .setContentType(contentType)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(encoding)
                            .setChannelMask(outChannel)
                            .build()
                    )
                    .setBufferSizeInBytes(bufSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                record.startRecording()
                track.play()

                val buf = ByteArray(bufSize)
                while (running) {
                    val n = record.read(buf, 0, buf.size)
                    if (n > 0) {
                        track.write(buf, 0, n, AudioTrack.WRITE_BLOCKING)
                    } else if (n < 0) {
                        Log.w(TAG, "AudioRecord.read returned $n")
                        break
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Loopback error", t)
                onError(t.message ?: t.javaClass.simpleName)
            } finally {
                runCatching { record?.stop() }
                runCatching { record?.release() }
                runCatching { track?.stop() }
                runCatching { track?.release() }
                cleanupRouting()
                running = false
            }
        }, "audio-loopback").also {
            it.priority = Thread.MAX_PRIORITY
            it.start()
        }
    }

    fun stop() {
        running = false
        thread?.join(1500)
        thread = null
    }

    private fun cleanupRouting() {
        if (!routedComm) return
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        runCatching { am.clearCommunicationDevice() }
        runCatching { am.mode = savedAudioMode }
        routedComm = false
    }

    companion object { private const val TAG = "AudioLoopback" }
}
