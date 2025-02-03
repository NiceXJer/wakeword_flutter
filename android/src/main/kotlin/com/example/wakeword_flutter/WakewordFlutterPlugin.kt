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
import org.vosk.android.SpeechService
import org.vosk.android.RecognitionListener
import org.json.JSONObject
import android.content.res.AssetManager
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
        Log.d("WakeWord", "onAttachedToEngine called")
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "wakeword_flutter")
        channel.setMethodCallHandler(this)

        requestMicrophonePermission()
    }

    private fun requestMicrophonePermission() {
        Log.d("WakeWord", "Requesting microphone permission")
        val activity = context as Activity
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.d("WakeWord", "Microphone permission not granted, requesting permission")
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            Log.d("WakeWord", "Microphone permission already granted, initializing Vosk")
            initializeVosk()
        }
    }

    private fun initializeVosk() {
        Log.d("WakeWord", "Initializing Vosk")
        try {
            val modelPath = context.filesDir.absolutePath + "/vosk-model"
            val modelDir = File(modelPath)
Log.d("WakeWord", "Model path: $modelPath")
if (!modelDir.exists() || modelDir.list()?.isEmpty() == true) {
    Log.d("WakeWord", "Model folder is empty or not found, copying...")
    copyAssetsToFilesDir(context, "vosk-model-small-en-us-0.15", modelPath)
}


            if (modelDir.exists()) {
                Log.d("WakeWord", "Deleting existing model folder...")
                modelDir.deleteRecursively()
            }

            if (!modelDir.exists() || modelDir.list()?.isEmpty() == true) {
                Log.d("WakeWord", "Copying Vosk model to $modelPath")
                copyAssetsToFilesDir(context, "vosk-model-small-en-us-0.15", modelPath)
            } else {
                Log.d("WakeWord", "Vosk model already exists at $modelPath")
            }

            if (!modelDir.exists()) {
                Log.e("WakeWord", "Error: Model directory does NOT exist!")
            }

            val model = Model(modelPath)
            recognizer = Recognizer(model, 16000.0f)
            Log.d("WakeWord", "Vosk initialized successfully")
            startListening()
        } catch (e: Exception) {
            Log.e("WakeWord", "Failed to initialize Vosk: ${e.message}")
        }
    }

    private fun copyAssetsToFilesDir(context: Context, assetPath: String, outputPath: String) {
        Log.d("WakeWord", "Copying assets from $assetPath to $outputPath")
        val assetManager: AssetManager = context.assets
        try {
            val files = assetManager.list(assetPath)
            val outFile = File(outputPath)

            if (files.isNullOrEmpty()) {
                Log.d("WakeWord", "No subdirectories, copying file directly")
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
                Log.d("WakeWord", "Copied file: $assetPath → $outputPath")
            } else {
                Log.d("WakeWord", "Subdirectories found, recursively copying files")
                if (!outFile.exists()) outFile.mkdirs()
                for (file in files) {
                    copyAssetsToFilesDir(context, "$assetPath/$file", "$outputPath/$file")
                }
            }
        } catch (e: Exception) {
            Log.e("WakeWord", "Error copying assets: ${e.message}")
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Log.d("WakeWord", "onMethodCall: ${call.method}")
        when (call.method) {
            "checkPermission" -> {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                Log.d("WakeWord", "checkPermission result: $granted")
                result.success(granted)
            }
            "startListening" -> {
                Log.d("WakeWord", "startListening called")
                startListening()
                result.success(null)
            }
            "stopListening" -> {
                Log.d("WakeWord", "stopListening called")
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
                    Log.e("WakeWord", "Invalid argument for wake words")
                    result.error("INVALID_ARGUMENT", "Wake words must be a list of strings", null)
                }
            }
            else -> {
                Log.d("WakeWord", "Method not implemented: ${call.method}")
                result.notImplemented()
            }
        }
    }

    private fun startListening() {
    Log.d("WakeWord", "startListening")
    if (speechService == null && recognizer != null) {
        Log.d("WakeWord", "SpeechService not running, starting it")
        try {
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(object : RecognitionListener {
                override fun onResult(hypothesis: String?) {
                    Log.d("WakeWord", "Raw Recognition Output: $hypothesis")
                }

                override fun onPartialResult(hypothesis: String?) {
                    Log.d("WakeWord", "onPartialResult: $hypothesis")
                    if (hypothesis != null) {
                        try {
                            val jsonObject = JSONObject(hypothesis)
                            val text = jsonObject.optString("partial", "")
                            Log.d("WakeWord", "Partial result: $text")
                            channel.invokeMethod("recognizedWords", text)
                        } catch (e: Exception) {
                            Log.e("WakeWord", "Error parsing partial result: ${e.message}")
                        }
                    }
                }

                override fun onFinalResult(hypothesis: String?) {
                    Log.d("WakeWord", "onFinalResult: $hypothesis")
                    if (hypothesis != null) {
                        try {
                            val jsonObject = JSONObject(hypothesis)
                            val text = jsonObject.optString("text", "")
                            Log.d("WakeWord", "Final result: $text")
                            channel.invokeMethod("recognizedWords", text)

                            if (isWakeWordDetected(text)) {
                                Log.d("WakeWord", "Wake word detected: $text")
                                channel.invokeMethod("wakeWordDetected", text)
                            }
                        } catch (e: Exception) {
                            Log.e("WakeWord", "Error parsing final result: ${e.message}")
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
        } catch (e: Exception) {
            Log.e("WakeWord", "Error starting SpeechService: ${e.message}")
        }
    } else {
        Log.d("WakeWord", "SpeechService is already running.")
    }
}


    private fun stopListening() {
    Log.d("WakeWord", "Stopping listening")
    speechService?.stop()
    speechService = null
}


    private fun isWakeWordDetected(text: String): Boolean {
        Log.d("WakeWord", "Checking detected text: $text")
        val detected = wakeWords.any { it.equals(text, ignoreCase = true) }
        Log.d("WakeWord", "Wake word matched: $detected")
        return detected
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d("WakeWord", "onDetachedFromEngine called")
        channel.setMethodCallHandler(null)
        stopListening()
    }
}
