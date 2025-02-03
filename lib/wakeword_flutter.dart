import 'package:flutter/services.dart';

class WakewordFlutter {
  static const MethodChannel _channel = MethodChannel('wakeword_flutter');

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

  static void setWakeWordCallback(Function(String) callback) {
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
}


  static Future<void> startListening() async {
    try {
      await _channel.invokeMethod('startListening');
    } on PlatformException catch (e) {
      print("Failed to start listening: ${e.message}");
    }
  }

  static Future<void> stopListening() async {
    try {
      await _channel.invokeMethod('stopListening');
    } on PlatformException catch (e) {
      print("Failed to stop listening: ${e.message}");
    }
  }
}
