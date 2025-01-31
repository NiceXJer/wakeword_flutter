package com.example.wakeword_flutter

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class WakewordFlutterPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "wakeword_flutter")
        channel.setMethodCallHandler(this)

        requestMicrophonePermission()
    }

    private fun requestMicrophonePermission() {
        val activity = context as Activity
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            startListening()
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    when (call.method) {
        "checkPermission" -> {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            result.success(granted)
        }
        "startListening" -> {
            startListening()
            result.success(null)
        }
        "stopListening" -> {
            speechRecognizer?.stopListening()
            result.success(null)
        }
        else -> result.notImplemented()
    }
}

    private fun startListening() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.let {
                        for (text in it) {
                            Log.d("WakeWord", "Final Result: $text")
                            if (isWakeWordDetected(text)) {
                                channel.invokeMethod("wakeWordDetected", text)
                            }
                        }
                    }
                    startListening()  // Restart to continuously listen
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.let {
                        for (text in it) {
                            Log.d("WakeWord", "Listening (Partial Result): $text")
                        }
                    }
                }

                override fun onError(error: Int) {
                    Log.e("WakeWord", "Error: $error")
                    startListening()  // Restart on error
                }

                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("WakeWord", "Ready to listen...")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("WakeWord", "Speech started...")
                }

                override fun onEndOfSpeech() {
                    Log.d("WakeWord", "Speech ended...")
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        speechRecognizer?.startListening(intent)
    }

    private fun isWakeWordDetected(text: String): Boolean {
        val wakeWords = listOf("hey threesixty", "hey three sixty", "hello 360", "hi", "Hi")
        return wakeWords.any { it.equals(text, ignoreCase = true) }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        speechRecognizer?.destroy()
    }
}