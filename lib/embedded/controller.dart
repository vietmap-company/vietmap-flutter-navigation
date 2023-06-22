import 'dart:async';
import 'dart:convert';
import 'dart:developer';
import 'dart:io';

import 'package:demo_plugin/models/events.dart';
import 'package:demo_plugin/models/navmode.dart';
import 'package:demo_plugin/models/options.dart';
import 'package:demo_plugin/models/route_event.dart';
import 'package:demo_plugin/models/route_progress_event.dart';
import 'package:demo_plugin/models/way_point.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Controller for a single Map Navigation instance running on the host platform.
class MapNavigationViewController {
  late MethodChannel _methodChannel;
  late EventChannel _eventChannel;

  ValueSetter<RouteEvent>? _routeEventNotifier;

  Stream<RouteEvent>? _onRouteEvent;
  late StreamSubscription<RouteEvent> _routeEventSubscription;

  MapNavigationViewController(int id, ValueSetter<RouteEvent>? eventNotifier) {
    _methodChannel = MethodChannel('demo_plugin/$id');
    _methodChannel.setMethodCallHandler(_handleMethod);

    _eventChannel = EventChannel('demo_plugin/$id/events');
    _routeEventNotifier = eventNotifier;
  }

  ///Current Device OS Version
  Future<String> get platformVersion => _methodChannel
      .invokeMethod('getPlatformVersion')
      .then<String>((dynamic result) => result);

  ///Total distance remaining in meters along route.
  Future<double> get distanceRemaining => _methodChannel
      .invokeMethod<double>('getDistanceRemaining')
      .then<double>((dynamic result) => result);

  ///Total seconds remaining on all legs.
  Future<double> get durationRemaining => _methodChannel
      .invokeMethod<double>('getDurationRemaining')
      .then<double>((dynamic result) => result);

  ///Build the Route Used for the Navigation
  ///
  /// [wayPoints] must not be null. A collection of [WayPoint](longitude, latitude and name). Must be at least 2 or at most 25. Cannot use drivingWithTraffic mode if more than 3-waypoints.
  /// [options] options used to generate the route and used while navigating
  ///
  Future<bool> buildRoute(
      {required List<WayPoint> wayPoints, MapOptions? options}) async {
    assert(wayPoints.length > 1);
    if (Platform.isIOS && wayPoints.length > 3 && options?.mode != null) {
      assert(options!.mode != MapNavigationMode.drivingWithTraffic,
          "Error: Cannot use drivingWithTraffic Mode when you have more than 3 Stops");
    }
    List<Map<String, Object?>> pointList = [];

    for (int i = 0; i < wayPoints.length; i++) {
      var wayPoint = wayPoints[i];
      assert(wayPoint.name != null);
      assert(wayPoint.latitude != null);
      assert(wayPoint.longitude != null);

      final pointMap = <String, dynamic>{
        "Order": i,
        "Name": wayPoint.name,
        "Latitude": wayPoint.latitude,
        "Longitude": wayPoint.longitude,
      };
      pointList.add(pointMap);
    }
    var i = 0;
    var wayPointMap = {for (var e in pointList) i++: e};

    Map<String, dynamic> args = <String, dynamic>{};
    if (options != null) args = options.toMap();
    args["wayPoints"] = wayPointMap;

    return await _methodChannel
        .invokeMethod('buildRoute', args)
        .then<bool>((dynamic result) => result);
  }

  /// starts listening for events
  Future<void> initialize() async {
    _routeEventSubscription = _streamRouteEvent!.listen(_onProgressData);
  }

  /// Clear the built route and resets the map
  Future<bool?> clearRoute() async {
    return _methodChannel.invokeMethod('clearRoute', null);
  }

  /// Starts Free Drive Mode
  Future<bool?> startFreeDrive({MapOptions? options}) async {
    Map<String, dynamic>? args;
    if (options != null) args = options.toMap();
    return _methodChannel.invokeMethod('startFreeDrive', args);
  }

  /// Starts the Navigation
  Future<bool?> startNavigation({MapOptions? options}) async {
    Map<String, dynamic>? args;
    if (options != null) args = options.toMap();
    log('heree');
    _routeEventSubscription = _streamRouteEvent!.listen(_onProgressData);
    final result = await _methodChannel.invokeMethod('startNavigation', args);
    if (result is bool) return result;
    log(result.toString());
    return result;
  }

  ///Ends Navigation and Closes the Navigation View
  Future<bool?> finishNavigation() async {
    var success = await _methodChannel.invokeMethod('finishNavigation', null);
    return success;
  }

  /// Generic Handler for Messages sent from the Platform
  Future<dynamic> _handleMethod(MethodCall call) async {
    switch (call.method) {
      case 'sendFromNative':
        String? text = call.arguments as String?;
        return Future.value("Text from native: $text");
    }
  }

  void _onProgressData(RouteEvent event) {
    if (_routeEventNotifier != null) _routeEventNotifier!(event);

    if (event.eventType == MapEvent.onArrival) {
      _routeEventSubscription.cancel();
    }
  }

  Stream<RouteEvent>? get _streamRouteEvent {
    _onRouteEvent ??= _eventChannel
        .receiveBroadcastStream()
        .map((dynamic event) => _parseRouteEvent(event));
    return _onRouteEvent;
  }

  RouteEvent _parseRouteEvent(String jsonString) {
    RouteEvent event;
    print(jsonString);
    var map = json.decode(jsonString);
    var progressEvent = RouteProgressEvent.fromJson(map);
    if (progressEvent.isProgressEvent!) {
      event =
          RouteEvent(eventType: MapEvent.progressChange, data: progressEvent);
    } else {
      event = RouteEvent.fromJson(map);
    }
    return event;
  }
}
