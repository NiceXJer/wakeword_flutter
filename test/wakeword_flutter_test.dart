// import 'package:flutter_test/flutter_test.dart';
// import 'package:wakeword_flutter/wakeword_flutter.dart';
// import 'package:wakeword_flutter/wakeword_flutter_platform_interface.dart';
// import 'package:wakeword_flutter/wakeword_flutter_method_channel.dart';
// import 'package:plugin_platform_interface/plugin_platform_interface.dart';

// class MockWakewordFlutterPlatform
//     with MockPlatformInterfaceMixin
//     implements WakewordFlutterPlatform {

//   @override
//   Future<String?> getPlatformVersion() => Future.value('42');
// }

// void main() {
//   final WakewordFlutterPlatform initialPlatform = WakewordFlutterPlatform.instance;

//   test('$MethodChannelWakewordFlutter is the default instance', () {
//     expect(initialPlatform, isInstanceOf<MethodChannelWakewordFlutter>());
//   });

//   test('getPlatformVersion', () async {
//     WakewordFlutter wakewordFlutterPlugin = WakewordFlutter();
//     MockWakewordFlutterPlatform fakePlatform = MockWakewordFlutterPlatform();
//     WakewordFlutterPlatform.instance = fakePlatform;

//     // expect(await wakewordFlutterPlugin.noSuchMethod(  ), '42');
//   });
// }
