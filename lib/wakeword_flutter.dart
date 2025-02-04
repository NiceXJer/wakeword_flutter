import 'package:flutter/services.dart';

class WakewordFlutter {
  static const MethodChannel _channel = MethodChannel('wakeword_flutter');
  static bool _isHandlerSet = false;
  static void initWakeWordDetection() {
    _channel.setMethodCallHandler((call) async {
      if (call.method == "recognizedWords") {
        print("🗣 Recognized: ${call.arguments}");
      } else if (call.method == "wakeWordDetected") {
        print("🔥 Wake word detected: ${call.arguments}");
      }
    });
  }

  static Future<void> startListening() async {
    await _channel.invokeMethod('startListening');
  }

  static Future<void> stopListening() async {
    await _channel.invokeMethod('stopListening');
  }

  /// Checks if microphone permission is granted
  static Future<bool> checkPermission() async {
    try {
      final bool granted = await _channel.invokeMethod('checkPermission');
      return granted;
    } on PlatformException catch (e) {
      print("Failed to check permission: ${e.message}");
      return false;
    }
  }

  /// Sets the wake words dynamically
  static Future<void> setWakeWords(List<String> wakeWords) async {
    try {
      await _channel.invokeMethod('setWakeWords', wakeWords);
    } on PlatformException catch (e) {
      print("Failed to set wake words: ${e.message}");
    }
  }

  /// Sets a callback function that triggers when a wake word is detected
  static void setWakeWordCallback(Function(String) callback) {
    if (!_isHandlerSet) {
      _channel.setMethodCallHandler((call) async {
        if (call.method == "recognizedWords") {
          final String recognizedText = call.arguments;
          print("Recognized words: $recognizedText");
        }
        if (call.method == "wakeWordDetected") {
          final String wakeWord = call.arguments;
          print("Wake word detected: $wakeWord");
          callback(wakeWord);
        }
      });
      _isHandlerSet = true;
    }
  }

  // /// Starts wake word detection
  // static Future<void> startListening() async {
  //   try {
  //     await _channel.invokeMethod('startListening');
  //   } on PlatformException catch (e) {
  //     print("Failed to start listening: ${e.message}");
  //   }
  // }

  // /// Stops wake word detection
  // static Future<void> stopListening() async {
  //   try {
  //     await _channel.invokeMethod('stopListening');
  //   } on PlatformException catch (e) {
  //     print("Failed to stop listening: ${e.message}");
  //   }
  // }
}
