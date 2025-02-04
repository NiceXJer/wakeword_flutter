package com.example.wakeword_flutter

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class WakewordFlutterPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var speechService: SpeechService? = null
    private var recognizer: Recognizer? = null
    private var wakeWords: List<String> = listOf("hey threesixty", "hey three sixty", "hello 360", "hi", "hello")

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.d("WakeWord", "Plugin attached to engine")
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "wakeword_flutter")
        channel.setMethodCallHandler(this)
        requestMicrophonePermission()
    }

    private fun requestMicrophonePermission() {
        Log.d("WakeWord", "Requesting microphone permission")
        val activity = context as? Activity ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            initializeVosk()
        }
    }

    private fun initializeVosk() {
        Log.d("WakeWord", "Initializing Vosk")
        try {
            val modelPath = context.filesDir.absolutePath + "/vosk-model"
            val modelDir = File(modelPath)

            if (!modelDir.exists() || modelDir.list()?.isEmpty() == true) {
                Log.d("WakeWord", "Copying Vosk model to $modelPath")
                copyAssetsToFilesDir("vosk-model-small-en-us-0.15", modelPath)
            }

            val model = Model(modelPath)
            recognizer = Recognizer(model, 16000.0f)
            startListening()
        } catch (e: Exception) {
            Log.e("WakeWord", "Failed to initialize Vosk: ${e.message}")
        }
    }

    private fun copyAssetsToFilesDir(assetPath: String, outputPath: String) {
        val assetManager = context.assets
        try {
            val files = assetManager.list(assetPath) ?: return
            val outFile = File(outputPath)

            if (files.isEmpty()) {
                val inputStream: InputStream = assetManager.open(assetPath)
                val outputStream = FileOutputStream(outFile)
                val buffer = ByteArray(1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                inputStream.close()
                outputStream.flush()
                outputStream.close()
            } else {
                if (!outFile.exists()) outFile.mkdirs()
                for (file in files) {
                    copyAssetsToFilesDir("$assetPath/$file", "$outputPath/$file")
                }
            }
        } catch (e: Exception) {
            Log.e("WakeWord", "Error copying assets: ${e.message}")
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "checkPermission" -> {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                result.success(granted)
                if (granted) initializeVosk()
            }
            "startListening" -> {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    initializeVosk()
                    startListening()
                }
                result.success(null)
            }
            "stopListening" -> {
                stopListening()
                result.success(null)
            }
            "setWakeWords" -> {
                val words = call.arguments as? List<String>
                if (words != null) {
                    wakeWords = words
                    Log.d("WakeWord", "Updated wake words: $wakeWords")
                    result.success(null)
                } else {
                    result.error("INVALID_ARGUMENT", "Wake words must be a list of strings", null)
                }
            }
            else -> result.notImplemented()
        }
    }

    private fun startListening() {
        if (speechService == null && recognizer != null) {
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(object : RecognitionListener {
                override fun onResult(hypothesis: String?) {
                    Log.d("WakeWord", "Raw Recognition Output: $hypothesis")
                }

                override fun onPartialResult(hypothesis: String?) {
                    if (hypothesis != null) {
                        val jsonObject = JSONObject(hypothesis)
                        val text = jsonObject.optString("partial", "").trim()
                        Log.d("WakeWord", "Partial result: $text")
                        channel.invokeMethod("recognizedWords", text)
                        
                        if (isWakeWordDetected(text)) {
                            Log.d("WakeWord", "Wake word detected in partial: $text")
                            channel.invokeMethod("wakeWordDetected", text)
                        }
                    }
                }

                override fun onFinalResult(hypothesis: String?) {
                    if (hypothesis != null) {
                        val jsonObject = JSONObject(hypothesis)
                        val text = jsonObject.optString("text", "").trim()
                        Log.d("WakeWord", "Final result: $text")
                        channel.invokeMethod("recognizedWords", text)
                        
                        if (isWakeWordDetected(text)) {
                            Log.d("WakeWord", "Wake word detected: $text")
                            channel.invokeMethod("wakeWordDetected", text)
                        }
                    }
                }

                override fun onError(e: Exception?) {
                    Log.e("WakeWord", "SpeechService error: ${e?.message}")
                }

                override fun onTimeout() {
                    Log.d("WakeWord", "SpeechService timeout")
                }
            })
        }
    }

    private fun stopListening() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
    }

    private fun isWakeWordDetected(text: String): Boolean {
        val trimmedText = text.trim()
        Log.d("WakeWord", "Checking if '$trimmedText' matches any wake words: $wakeWords")
        return wakeWords.any { it.equals(trimmedText, ignoreCase = true) }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        stopListening()
    }
}