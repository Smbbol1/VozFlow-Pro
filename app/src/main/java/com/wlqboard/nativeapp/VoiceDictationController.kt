package com.wlqboard.nativeapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.inputmethod.InputConnection
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import kotlin.math.sqrt

/** Offline Spanish speech-to-text controller for an InputMethodService. */
class VoiceDictationController(
    context: Context,
    private val inputConnection: () -> InputConnection?,
    private val onState: (Boolean, String) -> Unit,
    private val onAmplitude: (Float) -> Unit,
    private val onError: (String) -> Unit,
    private val onRecognized: (String) -> Unit = {},
    private val onCalibrationStats: (Double, Long) -> Unit = { _, _ -> }
) {
    private val appContext = context.applicationContext
    private val assets = context.assets
    private val modelDirectory = File(appContext.filesDir, MODEL_ASSET_DIRECTORY)
    private val preferences = appContext.getSharedPreferences(
        "${appContext.packageName}_preferences",
        Context.MODE_PRIVATE
    )
        private val audioLock = Any()
    private val modelLock = Any()
    private val uiHandler = Handler(Looper.getMainLooper())

    @Volatile private var running = false
    @Volatile private var shuttingDown = false
    @Volatile private var preloading = false
    private var worker: Thread? = null
    @Volatile private var model: Model? = null
    @Volatile private var lastVoiceTextAtMs = 0L

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null

    init {
        preload()
    }

    fun preload() {
        if (shuttingDown || model != null || preloading) return
        preloading = true
        Thread({
            try { ensureModel() } catch (_: Exception) { } finally { preloading = false }
        }, "vosk-model-preload").start()
    }

    /** Starts one recording session. Calls while a session is active are ignored. */

    fun start() {
        synchronized(audioLock) {
            if (shuttingDown || running || worker?.isAlive == true) return
            if (appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                onError("No se concedió el permiso para usar el micrófono.")
                onState(false, "Permiso de micrófono requerido")
                return
            }
                        running = true
            onState(true, "Escuchando…")
            val thread = Thread({ runSession() }, "offline-voice-dictation")
            thread.priority = Thread.MAX_PRIORITY
            worker = thread
            thread.start()
        }
    }


    /** Stops capture immediately; the worker finalizes any recognized utterance. */
    fun stop() {
        running = false
        releaseAudioResources()
        onAmplitude(0f)
        onState(false, "Deteniendo dictado…")
    }

    /** Stops capture, waits for the worker, then releases the reusable Vosk model. */
    fun shutdown() {
        val thread: Thread?
        synchronized(audioLock) {
            if (shuttingDown) return
            shuttingDown = true
            running = false
            thread = worker
        }
        releaseAudioResources()
        if (thread != null && thread !== Thread.currentThread()) {
            try {
                thread.join()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
                synchronized(modelLock) {
            try {
                model?.close()
            } catch (_: Exception) {
            } finally {
                model = null
            }
        }
        synchronized(audioLock) { worker = null }
        onAmplitude(0f)

        onState(false, "Dictado cerrado")
    }

    private fun runSession() {
        var recognizer: Recognizer? = null
        var committedAnyText = false
        var lastPartial = ""
        try {
            val record = synchronized(audioLock) {
                if (!running || shuttingDown) return
                val createdRecord = createAudioRecord()
                audioRecord = createdRecord
                try {
                    createdRecord.startRecording()
                } catch (security: SecurityException) {
                    releaseAudioResourcesLocked()
                    throw security
                } catch (failure: IllegalStateException) {
                    releaseAudioResourcesLocked()
                    throw IOException("No se pudo iniciar la captura de audio.", failure)
                }
                createdRecord
            }
            onState(true, "Micrófono activo · preparando reconocimiento offline…")
            val loadedModel = ensureModel()
            if (!running || shuttingDown) return
            val activeRecognizer = Recognizer(loadedModel, SAMPLE_RATE.toFloat())
            recognizer = activeRecognizer
            onState(true, "Escuchando…")

            val buffer = ShortArray(BUFFER_SAMPLES)
            var quietRmsSum = 0.0
            var quietRmsSamples = 0L
            var quietStartedAt = 0L
            var pauseSumMs = 0L
            var pauseCount = 0L
            val silenceTimeoutMs = preferences.getInt(SILENCE_TIMEOUT_KEY, DEFAULT_SILENCE_TIMEOUT_MS.toInt())
                .coerceAtLeast(500).toLong()
            var lastVoiceAt = SystemClock.elapsedRealtime()
            var lastAmplitudeSentAt = 0L

            while (running && !shuttingDown) {
                val capturingRecord = synchronized(audioLock) { audioRecord } ?: break
                val count = capturingRecord.read(buffer, 0, buffer.size)
                if (count < 0) {
                    if (running) throw IOException("La captura de audio terminó con error ($count).")
                    break
                }
                if (count == 0) continue

                var squareSum = 0.0
                for (i in 0 until count) {
                    val sample = buffer[i].toDouble() / 32768.0
                    squareSum += sample * sample
                }
                val rms = sqrt(squareSum / count)
                val now = SystemClock.elapsedRealtime()
                val normalizedAmplitude = (rms / AMPLITUDE_FULL_SCALE_RMS).toFloat().coerceIn(0f, 1f)
                if (now - lastAmplitudeSentAt >= AMPLITUDE_CALLBACK_INTERVAL_MS) {
                    onAmplitude(normalizedAmplitude)
                    lastAmplitudeSentAt = now
                }

                // A small RMS threshold ignores microphone self-noise while still allowing quiet speech.
                val voiceThreshold = preferences.getFloat("voice_start_rms", VOICE_RMS_THRESHOLD.toFloat()).toDouble().coerceIn(0.004, 0.05)
                if (rms >= voiceThreshold) {
                    if (quietStartedAt > 0L) {
                        val pause = now - quietStartedAt
                        if (pause in 120..2500) { pauseSumMs += pause; pauseCount++ }
                        quietStartedAt = 0L
                    }
                    lastVoiceAt = now
                } else {
                    quietRmsSum += rms
                    quietRmsSamples++
                    if (quietStartedAt == 0L) quietStartedAt = now
                }
                if (now - lastVoiceAt >= silenceTimeoutMs) {
                    running = false
                    break
                }

                val audioBytes = shortsToLittleEndianBytes(buffer, count)
                if (activeRecognizer.acceptWaveForm(audioBytes, audioBytes.size)) {
                    val finalText = extractText(activeRecognizer.getResult(), "text")
                    if (finalText.isNotBlank()) {
                        commitRecognizedText(finalText)
                        committedAnyText = true
                    }
                    lastPartial = ""
                } else {
                    val partial = extractText(activeRecognizer.getPartialResult(), "partial")
                    if (partial != lastPartial) {
                        lastPartial = partial
                        if (partial.isNotBlank()) {
                            showComposingText(partial)
                        } else {
                            onUi { inputConnection()?.setComposingText("", 1) }
                        }
                    }
                }
            }

            val remainingText = extractText(activeRecognizer.getFinalResult(), "text")
            if (remainingText.isNotBlank()) {
                commitRecognizedText(remainingText)
                committedAnyText = true
            }
            if (!committedAnyText) onUi { inputConnection()?.setComposingText("", 1) }
            val averageNoise = if (quietRmsSamples > 0) quietRmsSum / quietRmsSamples else 0.0
            val averagePause = if (pauseCount > 0) pauseSumMs / pauseCount else silenceTimeoutMs
            onUi { onCalibrationStats(averageNoise, averagePause) }
        } catch (failure: IOException) {
            if (running && !shuttingDown) reportError(failure.message ?: "Error de lectura del modelo o del audio.", failure)
        } catch (failure: SecurityException) {
            if (running && !shuttingDown) reportError(failure.message ?: "Android denegó el acceso al micrófono.", failure)
        } catch (failure: Exception) {
            if (running && !shuttingDown) reportError(failure.message ?: "No se pudo iniciar el dictado offline.", failure)
        } finally {
            try {
                recognizer?.close()
            } catch (_: Exception) {
                // Ensure audio resources are still released if native cleanup fails.
            }
            releaseAudioResources()
            running = false
            onAmplitude(0f)
            onState(false, "Dictado detenido")
            synchronized(audioLock) {
                if (worker === Thread.currentThread()) worker = null
            }
        }
    }

        @Throws(IOException::class)
    private fun ensureModel(): Model {
        model?.let { return it }
        synchronized(modelLock) {
            model?.let { return it }
            if (shuttingDown) throw IOException("El controlador de dictado ya fue cerrado.")

            val marker = File(modelDirectory, MODEL_COMPLETE_MARKER)
            if (!marker.isFile) {
                deleteRecursively(modelDirectory)
                if (!modelDirectory.mkdirs() && !modelDirectory.isDirectory) {
                    throw IOException("No se pudo crear el directorio local del modelo Vosk.")
                }
                copyAssetTree(MODEL_ASSET_DIRECTORY, modelDirectory)
                if (!marker.createNewFile()) {
                    throw IOException("No se pudo marcar la copia local del modelo Vosk.")
                }
            }
            val loaded = try {
                Model(modelDirectory.absolutePath)
            } catch (failure: Exception) {
                throw IOException("No se pudo cargar el modelo Vosk desde los assets.", failure)
            }
            model = loaded
            return loaded
        }
    }

    @Throws(IOException::class)
    private fun copyAssetTree(assetPath: String, destination: File) {
        val children = try {
            assets.list(assetPath) ?: emptyArray()
        } catch (failure: IOException) {
            throw IOException("No se pudo leer el modelo Vosk de los assets: $assetPath", failure)
        }
        if (children.isNotEmpty()) {
            if (!destination.exists() && !destination.mkdirs()) {
                throw IOException("No se pudo crear el directorio del modelo: ${destination.path}")
            }
            for (child in children) {
                copyAssetTree("$assetPath/$child", File(destination, child))
            }
        } else {
            val parent = destination.parentFile
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw IOException("No se pudo crear el directorio: ${parent.path}")
            }
            try {
                assets.open(assetPath).use { input ->
                    FileOutputStream(destination).use { output -> input.copyTo(output) }
                }
            } catch (failure: IOException) {
                throw IOException("No se pudo copiar el archivo del modelo: $assetPath", failure)
            }
        }
    }

    private fun createAudioRecord(): AudioRecord {
        if (appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("No se concedió el permiso para usar el micrófono.")
        }
        val minBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
                if (minBytes <= 0) throw IOException("El dispositivo no admite captura PCM mono a 16 kHz.")
        val bufferBytes = maxOf(minBytes, BUFFER_SAMPLES * BYTES_PER_SAMPLE * 2)
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,

            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferBytes
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IOException("No se pudo inicializar el micrófono.")
        }

        if (preferences.getBoolean(NOISE_SUPPRESSION_KEY, true)) {
            try {
                if (NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)?.also { it.enabled = true }
                }
            } catch (_: Exception) {
                noiseSuppressor?.release()
                noiseSuppressor = null
            }
            try {
                if (AutomaticGainControl.isAvailable()) {
                    automaticGainControl = AutomaticGainControl.create(record.audioSessionId)?.also { it.enabled = true }
                }
            } catch (_: Exception) {
                automaticGainControl?.release()
                automaticGainControl = null
            }
        }
        return record
    }

    private fun releaseAudioResources() {
        synchronized(audioLock) { releaseAudioResourcesLocked() }
    }

    private fun releaseAudioResourcesLocked() {
        try {
            audioRecord?.let { record ->
                try {
                    if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
                } catch (_: IllegalStateException) {
                    // It may already have stopped or failed while read() was active.
                } finally {
                    record.release()
                }
            }
        } finally {
            audioRecord = null
            try {
                noiseSuppressor?.release()
            } catch (_: Exception) {
                // Optional effect cleanup.
            } finally {
                noiseSuppressor = null
            }
            try {
                automaticGainControl?.release()
            } catch (_: Exception) {
                // Optional effect cleanup.
            } finally {
                automaticGainControl = null
            }
        }
    }

        private fun commitRecognizedText(rawText: String) {
        onUi { onRecognized(rawText) }
        
        // NUEVO: Comprobar si el usuario desactivó la corrección en dictado
        val disableDictationCorrection = preferences.getBoolean("disable_dictation_correction", false)
        
        val converted = if (disableDictationCorrection) {
            // Si está desactivado, no pasamos por el motor de corrección, pero sí aplicamos comandos
            applyVoiceCommands(rawText)
        } else {
            // Flujo normal: aplicar perfil de voz, corrección y comandos
            applyVoiceCommands(SpanishPredictionEngine.correct(applyVoiceProfile(rawText), readPersonalWords()))
        }

                if (converted.isBlank()) return
        onUi {
            if (shuttingDown) return@onUi
            inputConnection()?.let { connection ->
                val now = System.currentTimeMillis()

                val pause = if (lastVoiceTextAtMs == 0L) Long.MAX_VALUE else (now - lastVoiceTextAtMs).coerceAtLeast(0L)
                val threshold = preferences.getInt(AUTO_CAP_PAUSE_KEY, DEFAULT_AUTO_CAP_PAUSE_MS).coerceIn(0, 10_000)
                val cased = capitalizeForPause(connection, converted, pause, threshold)
                connection.commitText(addBoundarySpacing(connection, cased), 1)
                lastVoiceTextAtMs = System.currentTimeMillis()
            }
        }
    }

        private fun showComposingText(rawText: String) {
        val converted = applyVoiceCommands(rawText)
        onUi {
            if (!running || shuttingDown) return@onUi
            inputConnection()?.let { connection ->
                val now = System.currentTimeMillis()

                val pause = if (lastVoiceTextAtMs == 0L) Long.MAX_VALUE else (now - lastVoiceTextAtMs).coerceAtLeast(0L)
                val threshold = preferences.getInt(AUTO_CAP_PAUSE_KEY, DEFAULT_AUTO_CAP_PAUSE_MS).coerceIn(0, 10_000)
                connection.setComposingText(capitalizeForPause(connection, converted, pause, threshold), 1)
            }
        }
    }

    private fun onUi(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else uiHandler.post(action)
    }

    private fun capitalizeForPause(connection: InputConnection, text: String, pauseMs: Long, thresholdMs: Int): String {
        val before = connection.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        val last = before.trimEnd().lastOrNull()
        val sentenceBoundary = before.isBlank() || last == '.' || last == '?' || last == '!' || last == '\n'
        val pauseBoundary = lastVoiceTextAtMs == 0L || thresholdMs == 0 || pauseMs >= thresholdMs
        val sentenceCase = capitalizeAfterPeriods(text)
        return when {
            sentenceBoundary || pauseBoundary -> capitalizeFirstLetter(sentenceCase)
            else -> lowercaseFirstWordUnlessProper(sentenceCase)
        }
    }

    private fun lowercaseFirstWordUnlessProper(text: String): String {
        val match = Regex("^[^\\p{L}]*[\\p{L}][\\p{L}0-9_-]*").find(text) ?: return text
        val word = match.value.takeLastWhile { it.isLetterOrDigit() || it == '_' || it == '-' }
        if (SpanishPredictionEngine.isKnownProperNoun(word) || word.all { !it.isLetter() || it.isUpperCase() }) return text
        val first = text.indexOfFirst { it.isLetter() }
        if (first < 0) return text
        return text.substring(0, first) + text[first].lowercaseChar() + text.substring(first + 1)
    }

    private fun addBoundarySpacing(connection: InputConnection, input: String): String {
        if (input.isEmpty()) return input
        val before = connection.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        val after = connection.getTextAfterCursor(1, 0)?.toString().orEmpty()
        val previous = before.lastOrNull()
        val first = input.first()
        val needsLeading = previous != null && !previous.isWhitespace() &&
            ((previous.isLetterOrDigit() && first.isLetterOrDigit()) || (previous in ",.;:!?" && first.isLetterOrDigit()))
        var result = (if (needsLeading) " " else "") + input
        if (result.lastOrNull() in listOf(',', '.', ';', ':', '!', '?') && after.firstOrNull()?.isWhitespace() != true) result += " "
        return result
    }

        private fun applyVoiceCommands(raw: String): String {
        var text = raw.trim()
        for ((pattern, replacement) in COMMAND_REPLACEMENTS) text = pattern.replace(text, replacement)
        if (DELETE_LAST_WORD_REGEX.containsMatchIn(text)) {
            onUi { inputConnection()?.let { connection ->
                val before = connection.getTextBeforeCursor(120, 0)?.toString().orEmpty()
                val remove = (before.takeLastWhile { it.isWhitespace() }.length + before.trimEnd().takeLastWhile { !it.isWhitespace() }.length).coerceAtLeast(1)
                connection.deleteSurroundingText(remove, 0)
            } }
            text = DELETE_LAST_WORD_REGEX.replace(text, "")
        }
        text = SPACE_BEFORE_PUNCT_REGEX.replace(text, "$1")
        text = SPACE_AFTER_PUNCT_REGEX.replace(text, "$1 ")
        return capitalizeAfterPeriods(text)
    }


    private fun applyVoiceProfile(raw: String): String = try {
        val json = JSONObject(preferences.getString("voice_profile_rules", "{}") ?: "{}")
        var value = raw
        json.keys().forEach { wrong ->
            val correct = json.optString(wrong)
            if (wrong.isNotBlank() && correct.isNotBlank()) {
                value = value.replace(Regex("(?i)\\b${Regex.escape(wrong)}\\b"), correct)
            }
        }
        value
    } catch (_: Exception) { raw }

    private fun readPersonalWords(): List<String> = try {
        val array = org.json.JSONArray(preferences.getString("personal_dictionary", "[]") ?: "[]")
        (0 until array.length()).map { array.optString(it) }.filter(String::isNotBlank)
    } catch (_: Exception) { emptyList() }

        private fun capitalizeAfterPeriods(text: String): String {
        return CAPITALIZE_PERIOD_REGEX.replace(text) { match ->
            match.groupValues + match.groupValues.uppercase(SPANISH_LOCALE)
        }
    }

    private fun capitalizeFirstLetter(text: String): String {
        val index = text.indexOfFirst { it.isLetter() }
        if (index < 0) return text
        return text.substring(0, index) + text[index].uppercase(SPANISH_LOCALE) + text.substring(index + 1)
    }


    private fun extractText(json: String, key: String): String {
        return try {
            JSONObject(json).optString(key, "")
        } catch (_: Exception) {
            ""
        }
    }

    private fun shortsToLittleEndianBytes(samples: ShortArray, count: Int): ByteArray {
        val result = ByteArray(count * BYTES_PER_SAMPLE)
        for (index in 0 until count) {
            val sample = samples[index].toInt()
            result[index * 2] = (sample and 0xff).toByte()
            result[index * 2 + 1] = ((sample ushr 8) and 0xff).toByte()
        }
        return result
    }

    private fun reportError(message: String, failure: Throwable) {
        CrashHandler.recordException(appContext, failure, "VoiceDictationController: sesión de dictado")
        onError(message)
        onState(false, "Error de dictado")
    }

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) file.listFiles()?.forEach(::deleteRecursively)
        file.delete()
    }

        companion object {
        private const val MODEL_ASSET_DIRECTORY = "vosk-model-small-es-0.42"
        private const val MODEL_COMPLETE_MARKER = ".copy_complete"
        private const val SAMPLE_RATE = 16_000
        private const val BUFFER_SAMPLES = 1_024
        private const val BYTES_PER_SAMPLE = 2
        private const val NOISE_SUPPRESSION_KEY = "noise_suppression"
        private const val SILENCE_TIMEOUT_KEY = "silence_timeout_ms"
        private const val AUTO_CAP_PAUSE_KEY = "voice_auto_caps_pause_ms"
        private const val DEFAULT_AUTO_CAP_PAUSE_MS = 3_000
        private const val DEFAULT_SILENCE_TIMEOUT_MS = 3_500L
        private const val VOICE_RMS_THRESHOLD = 0.008
                private const val AMPLITUDE_FULL_SCALE_RMS = 0.20
        private const val AMPLITUDE_CALLBACK_INTERVAL_MS = 50L
        private val SPANISH_LOCALE = Locale("es", "ES")
        private val DELETE_LAST_WORD_REGEX = Regex("\\bborrar última palabra\\b", RegexOption.IGNORE_CASE)
        private val SPACE_BEFORE_PUNCT_REGEX = Regex("\\s+([,?.!;:])")
        private val SPACE_AFTER_PUNCT_REGEX = Regex("([,?.!;:])(?=\\S)")
        private val CAPITALIZE_PERIOD_REGEX = Regex("(^|[.!?]\\s+|\\n)([\\p{L}])")
        private val COMMAND_REPLACEMENTS = listOf(
            Regex("\\b(?:nuevo párrafo|nuevo parrafo)\\b", RegexOption.IGNORE_CASE) to "\n\n",
            Regex("\\b(?:nueva línea|salto de línea)\\b", RegexOption.IGNORE_CASE) to "\n",
            Regex("\\babre interrogación\\b", RegexOption.IGNORE_CASE) to "¿",
            Regex("\\bcierra interrogación\\b", RegexOption.IGNORE_CASE) to "?",
            Regex("\\bsigno de interrogación\\b", RegexOption.IGNORE_CASE) to "?",
            Regex("\\binterrogación\\b", RegexOption.IGNORE_CASE) to "?",
            Regex("\\bcoma\\b", RegexOption.IGNORE_CASE) to ",",
            Regex("\\bdos puntos\\b", RegexOption.IGNORE_CASE) to ":",
            Regex("\\bpunto\\b", RegexOption.IGNORE_CASE) to "."
        )
    }
}


