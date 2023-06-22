import 'demo_plugin_platform_interface.dart';
import 'models/navmode.dart';
import 'models/options.dart';
import 'models/voice_units.dart';
import 'models/way_point.dart';

class DemoPlugin {
  static final DemoPlugin _instance = DemoPlugin();

  /// get current instance of this class
  static DemoPlugin get instance => _instance;

  late MapOptions _defaultOptions = MapOptions(
    zoom: 15,
    tilt: 0,
    bearing: 0,
    enableRefresh: false,
    alternatives: true,
    voiceInstructionsEnabled: true,
    bannerInstructionsEnabled: true,
    allowsUTurnAtWayPoints: true,
    mode: MapNavigationMode.drivingWithTraffic,
    units: VoiceUnits.imperial,
    simulateRoute: false,
    animateBuildRoute: true,
    longPressDestinationEnabled: true,
    language: 'en',
  );

  /// setter to set default options
  void setDefaultOptions(MapOptions options) {
    _defaultOptions = options;
  }

  /// Getter to retriev default options
  MapOptions getDefaultOptions() {
    return _defaultOptions;
  }

  Future<String?> getPlatformVersion() {
    return DemoPluginPlatform.instance.getPlatformVersion();
  }

  Future<bool?> startNavigation(
      List<WayPoint> wayPoints, MapOptions options) async {
    return DemoPluginPlatform.instance.startNavigation(wayPoints, options);
  }
}
