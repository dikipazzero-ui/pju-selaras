package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min

object ProfilePhotoHelper {
    private const val TARGET_SIZE_PX = 192
    private const val MAX_BASE64_LENGTH = 110_000

    suspend fun encodeUriToBase64(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val decoded = decodeSampledBitmap(context, uri, 512)
                    ?: throw IllegalStateException("Gagal membaca gambar dari galeri.")
                val cropped = centerCropSquare(decoded)
                val scaled = if (cropped.width != TARGET_SIZE_PX || cropped.height != TARGET_SIZE_PX) {
                    Bitmap.createScaledBitmap(cropped, TARGET_SIZE_PX, TARGET_SIZE_PX, true)
                } else {
                    cropped
                }

                var encoded = ""
                for (quality in intArrayOf(78, 62, 48, 35)) {
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    val candidate = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    encoded = candidate
                    if (candidate.length <= MAX_BASE64_LENGTH) break
                }

                if (scaled !== cropped) scaled.recycle()
                if (cropped !== decoded) cropped.recycle()
                decoded.recycle()

                require(encoded.isNotBlank() && encoded.length <= MAX_BASE64_LENGTH) {
                    "Ukuran foto terlalu besar. Pilih foto lain yang lebih kecil."
                }
                encoded
            }
        }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val largest = max(w, h)
                if (largest > maxDim) {
                    val scale = maxDim.toFloat() / largest.toFloat()
                    decoder.setTargetSize(
                        max(1, (w * scale).toInt()),
                        max(1, (h * scale).toInt())
                    )
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            var sampleSize = 1
            val largest = max(bounds.outWidth, bounds.outHeight)
            while (largest / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        }
    }

    private fun centerCropSquare(src: Bitmap): Bitmap {
        val size = min(src.width, src.height)
        if (size <= 0) return src
        val x = (src.width - size) / 2
        val y = (src.height - size) / 2
        return Bitmap.createBitmap(src, x, y, size, size)
    }
}

object ChatPhotoHelper {
    private const val MAX_DIMENSION_PX = 960
    private const val MAX_BASE64_LENGTH = 380_000

    fun createTempCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "chat_photos").apply { mkdirs() }
        val file = File(dir, "chat_cam_${System.currentTimeMillis()}.jpg")
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun encodeUriToBase64(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val decoded = decodeSampledBitmap(context, uri, MAX_DIMENSION_PX)
                    ?: throw IllegalStateException("Gagal membaca gambar.")
                val largest = max(decoded.width, decoded.height)
                val scaled = if (largest > MAX_DIMENSION_PX) {
                    val ratio = MAX_DIMENSION_PX.toFloat() / largest.toFloat()
                    Bitmap.createScaledBitmap(
                        decoded,
                        max(1, (decoded.width * ratio).toInt()),
                        max(1, (decoded.height * ratio).toInt()),
                        true
                    )
                } else {
                    decoded
                }

                var encoded = ""
                for (quality in intArrayOf(78, 64, 50, 38)) {
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    val candidate = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    encoded = candidate
                    if (candidate.length <= MAX_BASE64_LENGTH) break
                }

                if (scaled !== decoded) scaled.recycle()
                decoded.recycle()

                require(encoded.isNotBlank() && encoded.length <= MAX_BASE64_LENGTH) {
                    "Ukuran foto terlalu besar. Silakan pilih foto lain."
                }
                encoded
            }
        }

    private fun decodeSampledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val largest = max(w, h)
                if (largest > maxDim) {
                    val scale = maxDim.toFloat() / largest.toFloat()
                    decoder.setTargetSize(
                        max(1, (w * scale).toInt()),
                        max(1, (h * scale).toInt())
                    )
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            var sampleSize = 1
            val largest = max(bounds.outWidth, bounds.outHeight)
            while (largest / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        }
    }
}

class VoiceNoteRecorder {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L

    fun start(context: Context): Result<Unit> = runCatching {
        cancel()
        val file = File(context.cacheDir, "vn_rec_${System.currentTimeMillis()}.m4a")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context.applicationContext)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mr.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioSamplingRate(16_000)
            setAudioEncodingBitRate(24_000)
            setMaxDuration(60_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mr
        outputFile = file
        startTimeMs = System.currentTimeMillis()
    }.onFailure {
        cancel()
    }

    fun stopAndEncode(): Result<Pair<String, Int>> {
        val mr = recorder
        val file = outputFile
        val elapsedSec = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt().coerceIn(1, 60)
        recorder = null
        outputFile = null

        return runCatching {
            requireNotNull(mr) { "Perekam suara belum aktif." }
            requireNotNull(file) { "File rekaman tidak ditemukan." }
            try {
                mr.stop()
            } catch (e: RuntimeException) {
                throw IllegalStateException("Rekaman suara terlalu singkat. Tahan minimal 1 detik.", e)
            } finally {
                runCatching { mr.reset() }
                runCatching { mr.release() }
            }

            require(file.exists() && file.length() > 256L) {
                "Rekaman suara kosong atau terlalu singkat."
            }
            val bytes = file.readBytes()
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            require(encoded.length <= 440_000) {
                "Voice note terlalu panjang (maksimal 60 detik)."
            }
            encoded to elapsedSec
        }.also {
            runCatching { file?.delete() }
        }
    }

    fun cancel() {
        val mr = recorder
        val file = outputFile
        recorder = null
        outputFile = null
        if (mr != null) {
            runCatching { mr.stop() }
            runCatching { mr.reset() }
            runCatching { mr.release() }
        }
        if (file != null) {
            runCatching { file.delete() }
        }
    }
}

class VoiceNotePlayer(private val appContext: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    var playingMessageId by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var progressFraction by mutableFloatStateOf(0f)
        private set
    var currentPositionSec by mutableIntStateOf(0)
        private set

    fun togglePlay(
        message: ChatMessage,
        scope: CoroutineScope,
        onError: (String) -> Unit
    ) {
        val msgKey = message.id.ifBlank { message.audioBase64.hashCode().toString() }
        if (playingMessageId == msgKey && mediaPlayer != null) {
            val mp = mediaPlayer
            if (mp != null) {
                if (mp.isPlaying) {
                    mp.pause()
                    isPlaying = false
                } else {
                    mp.start()
                    isPlaying = true
                }
            }
            return
        }

        stopPlayback()
        if (message.audioBase64.isBlank()) {
            onError("Data audio voice note tidak tersedia.")
            return
        }

        scope.launch {
            val fileResult = withContext(Dispatchers.IO) {
                runCatching {
                    val cacheDir = File(appContext.cacheDir, "vn_play").apply { mkdirs() }
                    val safeName = msgKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                    val audioFile = File(cacheDir, "$safeName.m4a")
                    if (!audioFile.exists() || audioFile.length() == 0L) {
                        val bytes = Base64.decode(message.audioBase64, Base64.DEFAULT)
                        audioFile.writeBytes(bytes)
                    }
                    audioFile
                }
            }

            fileResult.onFailure { err ->
                onError(err.localizedMessage ?: "Gagal membuka audio voice note")
            }.onSuccess { audioFile ->
                runCatching {
                    val mp = MediaPlayer().apply {
                        setDataSource(audioFile.absolutePath)
                        setOnCompletionListener {
                            stopPlayback()
                        }
                        setOnErrorListener { _, _, _ ->
                            stopPlayback()
                            onError("Audio voice note tidak dapat diputar.")
                            true
                        }
                        prepare()
                        start()
                    }
                    mediaPlayer = mp
                    playingMessageId = msgKey
                    isPlaying = true
                    progressFraction = 0f
                    currentPositionSec = 0

                    progressJob?.cancel()
                    progressJob = scope.launch {
                        while (isActive && mediaPlayer === mp) {
                            val dur = runCatching { mp.duration }.getOrDefault(0).coerceAtLeast(1)
                            val pos = runCatching { mp.currentPosition }.getOrDefault(0).coerceAtLeast(0)
                            progressFraction = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                            currentPositionSec = (pos / 1000)
                            delay(120L)
                        }
                    }
                }.onFailure { err ->
                    stopPlayback()
                    onError(err.localizedMessage ?: "Gagal memutar voice note")
                }
            }
        }
    }

    fun stopPlayback() {
        progressJob?.cancel()
        progressJob = null
        mediaPlayer?.let { mp ->
            runCatching {
                if (mp.isPlaying) mp.stop()
            }
            runCatching { mp.reset() }
            runCatching { mp.release() }
        }
        mediaPlayer = null
        playingMessageId = null
        isPlaying = false
        progressFraction = 0f
        currentPositionSec = 0
    }
}

fun formatDurationSeconds(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "%d:%02d".format(mins, secs)
}
