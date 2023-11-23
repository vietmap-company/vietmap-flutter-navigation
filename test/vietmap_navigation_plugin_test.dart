import 'package:flutter_test/flutter_test.dart';
import 'package:vietmap_flutter_navigation/navigation_plugin.dart';
import 'package:vietmap_flutter_navigation/navigation_plugin_platform_interface.dart';
import 'package:vietmap_flutter_navigation/navigation_plugin_method_channel.dart';

void main() {
  final VietmapNavigationPluginPlatform initialPlatform =
      VietmapNavigationPluginPlatform.instance;

  test('$MethodChannelVietmapNavigationPlugin is the default instance', () {
    expect(
        initialPlatform, isInstanceOf<MethodChannelVietmapNavigationPlugin>());
  });

  test('getPlatformVersion', () async {
    VietmapNavigationPlugin demoPlugin = VietmapNavigationPlugin();

    expect(await demoPlugin.getPlatformVersion(), '42');
  });
}
