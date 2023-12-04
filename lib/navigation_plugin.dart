import 'navigation_plugin_platform_interface.dart';
import 'models/navmode.dart';
import 'models/options.dart';
import 'models/route_event.dart';
import 'models/voice_units.dart';
import 'models/way_point.dart';
import 'package:flutter/widgets.dart';

class VietMapNavigationPlugin {
  static final VietMapNavigationPlugin _instance = VietMapNavigationPlugin();

  /// get current instance of this class
  static VietMapNavigationPlugin get instance => _instance;

  MapOptions _defaultOptions = MapOptions(
    apiKey: '',
    mapStyle: '',
    zoom: 18,
    tilt: 0,
    bearing: 0,
    enableRefresh: false,
    alternatives: true,
    voiceInstructionsEnabled: true,
    bannerInstructionsEnabled: true,
    allowsUTurnAtWayPoints: true,
    mode: MapNavigationMode.drivingWithTraffic,
    units: VoiceUnits.imperial,
    simulateRoute: true,
    trackCameraPosition: true,
    animateBuildRoute: true,
    longPressDestinationEnabled: true,
    language: 'vi',
  );

  /// setter to set default options
  void setDefaultOptions(MapOptions options) {
    _defaultOptions = options;
  }

  /// Getter to retriev default options
  MapOptions getDefaultOptions() {
    return _defaultOptions;
  }

  ///Current Device OS Version
  Future<String?> getPlatformVersion() {
    return VietmapNavigationPluginPlatform.instance.getPlatformVersion();
  }

  ///Total distance remaining in meters along route.
  Future<double?> getDistanceRemaining() {
    return VietmapNavigationPluginPlatform.instance.getDistanceRemaining();
  }

  ///Total seconds remaining on all legs.
  Future<double?> getDurationRemaining() {
    return VietmapNavigationPluginPlatform.instance.getDurationRemaining();
  }

  ///Adds waypoints or stops to an on-going navigation
  ///
  /// [wayPoints] must not be null and have at least 1 item. The way points will
  /// be inserted after the currently navigating waypoint
  /// in the existing navigation
  Future<dynamic> addWayPoints({required List<WayPoint> wayPoints}) async {
    return VietmapNavigationPluginPlatform.instance
        .addWayPoints(wayPoints: wayPoints);
  }

  /// Free-drive mode is a unique Mapbox Navigation SDK feature that allows
  /// drivers to navigate without a set destination.
  /// This mode is sometimes referred to as passive navigation.
  /// Begins to generate Route Progress
  ///
  Future<bool?> startFreeDrive({MapOptions? options}) async {
    options ??= _defaultOptions;
    return VietmapNavigationPluginPlatform.instance.startFreeDrive(options);
  }

  Future<bool?> startNavigation({MapOptions? options}) async {
    return VietmapNavigationPluginPlatform.instance.startNavigation();
  }

  ///Ends Navigation and Closes the Navigation View
  Future<bool?> finishNavigation() async {
    return VietmapNavigationPluginPlatform.instance.finishNavigation();
  }

  /// Will download the navigation engine and the user's region
  /// to allow offline routing
  Future<bool?> enableOfflineRouting() async {
    return VietmapNavigationPluginPlatform.instance.enableOfflineRouting();
  }

  /// Event listener for RouteEvents
  Future<dynamic> registerRouteEventListener(
    ValueSetter<RouteEvent> listener,
  ) async {
    return VietmapNavigationPluginPlatform.instance
        .registerRouteEventListener(listener);
  }
}
