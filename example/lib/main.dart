import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:wakeword_flutter/wakeword_flutter.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String detectedWord = "Listening...";

  @override
  void initState() {
    super.initState();
    _requestPermission();
  }

  Future<void> _requestPermission() async {
    bool granted = await WakewordFlutter.checkPermission();
    if (!granted) {
      var status = await Permission.microphone.request();
      if (status.isGranted) {
        print("Permission granted, starting wake word detection...");
        await WakewordFlutter.setWakeWords(
            ["Hey", "Hi", "Hello", "Hey AI", "Hello Jarvis"]);
        startListening();
      } else {
        setState(() {
          detectedWord = "Microphone permission needed.";
        });
      }
    } else {
      print("Microphone permission already granted.");
      await WakewordFlutter.setWakeWords(
          ["Hey", "Hi", "Hello", "Hey AI", "Hello Jarvis"]);
      startListening();
    }
  }

  void startListening() {
    WakewordFlutter.setWakeWordCallback((word) {
      setState(() {
        detectedWord = "Wake word detected: $word";
      });
      print("Wake word detected: $word");
    });

    WakewordFlutter.startListening();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        appBar: AppBar(title: const Text("Wake Word Test")),
        body: Center(
          child: Text(detectedWord, style: TextStyle(fontSize: 20)),
        ),
      ),
    );
  }
}
