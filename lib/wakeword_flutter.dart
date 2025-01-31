import 'package:flutter/services.dart';

class WakewordFlutter {
  static const MethodChannel _channel = MethodChannel('wakeword_flutter');

  /// Checks if the microphone permission is granted.
  static Future<bool> checkPermission() async {
    try {
      final bool granted = await _channel.invokeMethod('checkPermission');
      return granted;
    } on PlatformException catch (e) {
      print("Failed to check permission: ${e.message}");
      return false;
    }
  }

  /// Sets a callback to be invoked when the wake word is detected.
  static void setWakeWordCallback(Function(String) callback) {
    _channel.setMethodCallHandler((call) async {
      if (call.method == "wakeWordDetected") {
        final String wakeWord = call.arguments;
        callback(wakeWord);
      }
    });
  }

  /// Starts listening for the wake word.
  static Future<void> startListening() async {
    try {
      await _channel.invokeMethod('startListening');
    } on PlatformException catch (e) {
      print("Failed to start listening: ${e.message}");
    }
  }

  /// Stops listening for the wake word.
  static Future<void> stopListening() async {
    try {
      await _channel.invokeMethod('stopListening');
    } on PlatformException catch (e) {
      print("Failed to stop listening: ${e.message}");
    }
  }
}
