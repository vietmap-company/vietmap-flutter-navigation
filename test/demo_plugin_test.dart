import 'package:vietmap_flutter_navigation/models/options.dart';
import 'package:vietmap_flutter_navigation/models/route_event.dart';
import 'package:vietmap_flutter_navigation/models/way_point.dart';
import 'package:flutter/src/foundation/basic_types.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:vietmap_flutter_navigation/demo_plugin.dart';
import 'package:vietmap_flutter_navigation/demo_plugin_platform_interface.dart';
import 'package:vietmap_flutter_navigation/demo_plugin_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockDemoPluginPlatform
    with MockPlatformInterfaceMixin
    implements DemoPluginPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<bool?> startNavigation(List<WayPoint> wayPoints, MapOptions options) =>
      Future.value(true);

  @override
  Future addWayPoints({required List<WayPoint> wayPoints}) {
    // TODO: implement addWayPoints
    throw UnimplementedError();
  }

  @override
  Future<bool?> enableOfflineRouting() {
    // TODO: implement enableOfflineRouting
    throw UnimplementedError();
  }

  @override
  Future<bool?> finishNavigation() {
    // TODO: implement finishNavigation
    throw UnimplementedError();
  }

  @override
  Future<double?> getDistanceRemaining() {
    // TODO: implement getDistanceRemaining
    throw UnimplementedError();
  }

  @override
  Future<double?> getDurationRemaining() {
    // TODO: implement getDurationRemaining
    throw UnimplementedError();
  }

  @override
  Future registerRouteEventListener(ValueSetter<RouteEvent> listener) {
    // TODO: implement registerRouteEventListener
    throw UnimplementedError();
  }

  @override
  Future<bool?> startFreeDrive(MapOptions options) {
    // TODO: implement startFreeDrive
    throw UnimplementedError();
  }
}

void main() {
  final DemoPluginPlatform initialPlatform = DemoPluginPlatform.instance;

  test('$MethodChannelDemoPlugin is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelDemoPlugin>());
  });

  test('getPlatformVersion', () async {
    DemoPlugin demoPlugin = DemoPlugin();
    MockDemoPluginPlatform fakePlatform = MockDemoPluginPlatform();
    DemoPluginPlatform.instance = fakePlatform;

    expect(await demoPlugin.getPlatformVersion(), '42');
  });
}
