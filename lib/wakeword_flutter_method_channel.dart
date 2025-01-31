import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'wakeword_flutter_platform_interface.dart';

/// An implementation of [WakewordFlutterPlatform] that uses method channels.
class MethodChannelWakewordFlutter extends WakewordFlutterPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('wakeword_flutter');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
