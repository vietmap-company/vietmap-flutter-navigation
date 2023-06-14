import 'demo_plugin_platform_interface.dart';
import 'models/options.dart';
import 'models/way_point.dart';

class DemoPlugin {
  Future<String?> getPlatformVersion() {
    return DemoPluginPlatform.instance.getPlatformVersion();
  }

  Future<bool?> startNavigation(
      List<WayPoint> wayPoints, MapOptions options) async {
    return DemoPluginPlatform.instance.startNavigation(wayPoints, options);
  }
}
