import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:vietmap_flutter_navigation/demo_plugin_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelDemoPlugin platform = MethodChannelDemoPlugin();
  const MethodChannel channel = MethodChannel('demo_plugin');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance?.defaultBinaryMessenger
        .setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance?.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
