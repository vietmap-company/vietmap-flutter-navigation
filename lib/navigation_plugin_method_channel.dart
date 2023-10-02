import 'dart:async';
import 'dart:convert';
import 'dart:developer';
import 'dart:io';

import 'package:vietmap_flutter_navigation/models/events.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'navigation_plugin_platform_interface.dart';
import 'models/navmode.dart';
import 'models/options.dart';
import 'models/route_event.dart';
import 'models/route_progress_event.dart';
import 'models/way_point.dart';

/// An implementation of [VietmapNavigationPluginPlatform] that uses method channels.
class MethodChannelVietmapNavigationPlugin
    extends VietmapNavigationPluginPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('navigation_plugin');

  @visibleForTesting
  final eventChannel = const EventChannel('navigation_plugin/events');

  late StreamSubscription<RouteEvent> _routeEventSubscription;
  late ValueSetter<RouteEvent>? _onRouteEvent;

  @override
  Future<String?> getPlatformVersion() async {
    final version =
        await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<double?> getDistanceRemaining() async {
    final distance =
        await methodChannel.invokeMethod<double?>('getDistanceRemaining');
    return distance;
  }

  @override
  Future<double?> getDurationRemaining() async {
    final duration =
        await methodChannel.invokeMethod<double?>('getDurationRemaining');
    return duration;
  }

  @override
  Future<bool?> startFreeDrive(MapOptions options) async {
    _routeEventSubscription = routeEventsListener!.listen(_onProgressData);
    final args = options.toMap();
    final result = await methodChannel.invokeMethod('startFreeDrive', args);
    if (result is bool) return result;
    log(result.toString());
    return false;
  }

  @override
  Future<bool?> startNavigation({MapOptions? options}) async {
    // assert(wayPoints.length > 1, 'Error: WayPoints must be at least 2');
    // if (Platform.isIOS && wayPoints.length > 3) {
    //   assert(options.mode != MapNavigationMode.drivingWithTraffic, '''
    //         Error: Cannot use drivingWithTraffic Mode when you have more than 3 Stops
    //       ''');
    // }

    // final pointList = _getPointListFromWayPoints(wayPoints);
    // var i = 0;
    // final wayPointMap = {for (var e in pointList) i++: e};
    final args = options?.toMap();
    // args['wayPoints'] = wayPointMap;
    _routeEventSubscription = routeEventsListener!.listen(_onProgressData);
    final result = await methodChannel.invokeMethod('startNavigation', args);
    if (result is bool) return result;
    log(result.toString());
    return false;
  }

  @override
  Future<dynamic> addWayPoints({required List<WayPoint> wayPoints}) async {
    assert(wayPoints.isNotEmpty, 'Error: WayPoints must be at least 1');
    final pointList = _getPointListFromWayPoints(wayPoints);
    var i = 0;
    final wayPointMap = {for (var e in pointList) i++: e};
    final args = <String, dynamic>{};
    args['wayPoints'] = wayPointMap;
    await methodChannel.invokeMethod('addWayPoints', args);
  }

  @override
  Future<bool?> finishNavigation() async {
    final success = await methodChannel.invokeMethod<bool?>('finishNavigation');
    return success;
  }

  /// Will download the navigation engine and the user's region
  /// to allow offline routing
  @override
  Future<bool?> enableOfflineRouting() async {
    final success =
        await methodChannel.invokeMethod<bool?>('enableOfflineRouting');
    return success;
  }

  @override
  Future<dynamic> registerRouteEventListener(
    ValueSetter<RouteEvent> listener,
  ) async {
    _onRouteEvent = listener;
  }

  /// Events Handling
  Stream<RouteEvent>? get routeEventsListener {
    return eventChannel
        .receiveBroadcastStream()
        .map((dynamic event) => _parseRouteEvent(event as String));
  }

  void _onProgressData(RouteEvent event) {
    if (_onRouteEvent != null) _onRouteEvent?.call(event);
    switch (event.eventType) {
      case MapEvent.navigationFinished:
        _routeEventSubscription.cancel();
        break;
      // ignore: no_default_cases
      default:
        break;
    }
  }

  RouteEvent _parseRouteEvent(String jsonString) {
    RouteEvent event;
    final map = json.decode(jsonString);
    final progressEvent =
        RouteProgressEvent.fromJson(map as Map<String, dynamic>);
    if (progressEvent.isProgressEvent!) {
      event = RouteEvent(
        eventType: MapEvent.progressChange,
        data: progressEvent,
      );
    } else {
      event = RouteEvent.fromJson(map);
    }
    return event;
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
