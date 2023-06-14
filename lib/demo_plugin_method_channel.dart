import 'dart:developer';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'demo_plugin_platform_interface.dart';
import 'models/navmode.dart';
import 'models/options.dart';
import 'models/way_point.dart';

/// An implementation of [DemoPluginPlatform] that uses method channels.
class MethodChannelDemoPlugin extends DemoPluginPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('demo_plugin');

  @override
  Future<String?> getPlatformVersion() async {
    final version =
        await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<bool?> startNavigation(
      List<WayPoint> wayPoints, MapOptions options) async {
    assert(wayPoints.length > 1, 'Error: WayPoints must be at least 2');
    if (Platform.isIOS && wayPoints.length > 3) {
      assert(options.mode != MapNavigationMode.drivingWithTraffic, '''
            Error: Cannot use drivingWithTraffic Mode when you have more than 3 Stops
          ''');
    }

    final pointList = _getPointListFromWayPoints(wayPoints);
    var i = 0;
    final wayPointMap = {for (var e in pointList) i++: e};
    final args = options.toMap();
    args['wayPoints'] = wayPointMap;
    final result = await methodChannel.invokeMethod('startNavigation', args);
    if (result is bool) return result;
    log(result.toString());
    return false;
  }

  List<Map<String, Object?>> _getPointListFromWayPoints(
    List<WayPoint> wayPoints,
  ) {
    final pointList = <Map<String, Object?>>[];

    for (var i = 0; i < wayPoints.length; i++) {
      final wayPoint = wayPoints[i];
      assert(wayPoint.name != null, 'Error: waypoints need name');
      assert(wayPoint.latitude != null, 'Error: waypoints need latitude');
      assert(wayPoint.longitude != null, 'Error: waypoints need longitude');

      final pointMap = <String, dynamic>{
        'Order': i,
        'Name': wayPoint.name,
        'Latitude': wayPoint.latitude,
        'Longitude': wayPoint.longitude,
        'IsSilent': wayPoint.isSilent,
      };
      pointList.add(pointMap);
    }
    return pointList;
  }
}
