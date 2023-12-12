import 'dart:async';
import 'dart:convert';
import 'dart:developer';
import 'dart:io';
import 'package:vietmap_flutter_navigation/extension.dart';
import 'package:vietmap_flutter_navigation/models/events.dart';
import 'package:vietmap_flutter_navigation/models/method_channel_event.dart';
import 'package:vietmap_flutter_navigation/models/navmode.dart';
import 'package:vietmap_flutter_navigation/models/options.dart';
import 'package:vietmap_flutter_navigation/models/route_event.dart';
import 'package:vietmap_flutter_navigation/models/way_point.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import '../models/marker.dart';

enum DrivingProfile { drivingTraffic, cycling, walking, motorcycle }

/// Controller for a single Map Navigation instance running on the host platform.
class MapNavigationViewController {
  late MethodChannel _methodChannel;
  late EventChannel _eventChannel;

  ValueSetter<RouteEvent>? _routeEventNotifier;

  Stream<RouteEvent>? _onRouteEvent;
  late StreamSubscription<RouteEvent> _routeEventSubscription;

  MapNavigationViewController(int id, ValueSetter<RouteEvent>? eventNotifier) {
    _methodChannel = MethodChannel('navigation_plugin/$id');
    _methodChannel.setMethodCallHandler(_handleMethod);

    _eventChannel = EventChannel('navigation_plugin/$id/events');
    _routeEventNotifier = eventNotifier;
  }

  ///Current Device OS Version
  Future<String> get platformVersion => _methodChannel
      .invokeMethod(MethodChannelEvent.getPlatformVersion)
      .then<String>((dynamic result) => result);

  ///Total distance remaining in meters along route.
  Future<double> get distanceRemaining => _methodChannel
      .invokeMethod<double>(MethodChannelEvent.getDistanceRemaining)
      .then<double>((dynamic result) => result);

  ///Total seconds remaining on all legs.
  Future<double> get durationRemaining => _methodChannel
      .invokeMethod<double>(MethodChannelEvent.getDurationRemaining)
      .then<double>((dynamic result) => result);

  ///Build the Route Used for the Navigation
  ///
  /// [wayPoints] must not be null. A collection of [WayPoint](longitude, latitude and name). Must be at least 2 or at most 25. Cannot use drivingWithTraffic mode if more than 3-waypoints.
  /// [options] options used to generate the route and used while navigating
  ///
  Future<bool> buildRoute(
      {required List<WayPoint> wayPoints,
      MapOptions? options,
      DrivingProfile profile = DrivingProfile.drivingTraffic}) async {
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

    args['profile'] = profile.getValue();
    return await _methodChannel
        .invokeMethod(MethodChannelEvent.buildRoute, args)
        .then<bool>((dynamic result) => result);
  }

  /// starts listening for events
  Future<void> initialize() async {
    _routeEventSubscription = _streamRouteEvent!.listen(_onProgressData);
  }

  /// Clear the built route and resets the map
  Future<void> clearRoute() async {
    return _methodChannel.invokeMethod(MethodChannelEvent.clearRoute, null);
  }

  /// Starts Free Drive Mode
  // Future<bool?> startFreeDrive({MapOptions? options}) async {
  //   Map<String, dynamic>? args;
  //   if (options != null) args = options.toMap();
  //   return _methodChannel.invokeMethod(MethodChannelEvent.startFreeDrive, args);
  // }

  /// Starts the Navigation
  Future<bool?> startNavigation({MapOptions? options}) async {
    Map<String, dynamic>? args;
    if (options != null) args = options.toMap();

    final result = await _methodChannel.invokeMethod(
        MethodChannelEvent.startNavigation, args);
    if (result is bool) return result;
    log(result.toString());
    return result;
  }

  /// Add a Marker Group to the Map
  /// [imagePath] is the path to the image asset, allow only image in [png], [jpeg], [jpg] format

  Future<List<NavigationMarker>> addImageMarkers(
      List<NavigationMarker> markers) async {
    List<Map<String, dynamic>> markerList = [];
    await Future.forEach(markers, (NavigationMarker marker) async {
      try {
        var data = await rootBundle.load(marker.imagePath);
        var jsonData = marker.toJson();
        if (Platform.isAndroid) {
          var bytes =
              data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes);

          jsonData['imageBytes'] = bytes;
        } else if (Platform.isIOS) {
          var base64String = base64Encode(
              data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes));
          jsonData['imageBase64'] = base64String;
        }

        markerList.add(jsonData);
      } catch (e) {
        debugPrint(e.toString());
      }
    });
    var i = 0;
    var markerMap = {for (var e in markerList) i++: e};

    List<Object?> listMarkerId = await _methodChannel.invokeMethod(
        MethodChannelEvent.addMarkers, markerMap);
    print(listMarkerId);
    if (listMarkerId.isNotEmpty) {
      for (var element in markers) {
        element.markerId = listMarkerId[markers.indexOf(element)] as int?;
      }
      return markers;
    } else {
      return [];
    }
  }

  Future<bool?> removeMarkers(List<int> markerIds) async {
    return _methodChannel.invokeMethod(
        MethodChannelEvent.removeMarkers, {'markerIds': markerIds});
  }

  Future<bool?> removeAllMarkers() async {
    return _methodChannel.invokeMethod(MethodChannelEvent.removeAllMarkers);
  }

  /// Set Center Icon for Navigation, get by call [VietMapHelper.getBytesFromAsset('assetsPath')]
  Future<bool?> setCenterIcon(Uint8List? centerIcon) {
    return Future.value(false);
    // return _methodChannel.invokeMethod(
    //     MethodChannelEvent.setCenterIcon, centerIcon);
  }

  Future<void> recenter({MapOptions? options}) async {
    Map<String, dynamic>? args;
    if (options != null) args = options.toMap();

    return _methodChannel.invokeMethod(MethodChannelEvent.recenter, args);
  }

  Future<void> overview({MapOptions? options}) async {
    Map<String, dynamic>? args;
    if (options != null) args = options.toMap();

    return _methodChannel.invokeMethod(MethodChannelEvent.overview, args);
  }

  Future<bool?> mute(bool isMute) async {
    return _methodChannel
        .invokeMethod(MethodChannelEvent.mute, {'isMute': isMute});
  }

  Future<bool> buildAndStartNavigation(
      {required List<WayPoint> wayPoints,
      MapOptions? options,
      DrivingProfile profile = DrivingProfile.drivingTraffic}) async {
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
    args['profile'] = profile.getValue();

    return await _methodChannel
        .invokeMethod(MethodChannelEvent.buildAndStartNavigation, args)
        .then<bool>((dynamic result) => result);
  }

  ///Ends Navigation and Closes the Navigation View
  Future<bool?> finishNavigation() async {
    var success = await _methodChannel.invokeMethod(
        MethodChannelEvent.finishNavigation, null);
    return success;
  }

  Future<dynamic> onDispose() async {
    var success =
        await _methodChannel.invokeMethod(MethodChannelEvent.onDispose, null);
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
    var map = json.decode(jsonString);
    event = RouteEvent.fromJson(map);
    return event;
  }
}
