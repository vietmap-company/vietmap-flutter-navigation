import 'dart:convert';

import 'package:vietmap_flutter_navigation/helpers.dart';
import 'package:vietmap_flutter_navigation/models/way_point.dart';
import 'package:flutter/material.dart';

import '../embedded/controller.dart';
import '../embedded/view.dart';
import '../models/direction_route.dart';
import '../models/events.dart';
import '../models/options.dart';
import '../models/route_event.dart';
import '../models/route_progress_event.dart';

class NavigationView extends StatefulWidget {
  /// NavigationView is a widget that show a map with navigation
  /// it will response all events from the map and navigation
  /// and return all information about the route
  const NavigationView(
      {super.key,
      required this.mapOptions,
      required this.onMapCreated,
      this.onRouteProgressChange,
      this.onRouteBuilding,
      this.onRouteBuilt,
      this.onRouteBuildFailed,
      this.onNavigationRunning,
      this.onArrival,
      this.onNavigationFinished,
      this.onNavigationCancelled,
      this.onMapMove,
      this.userOffRoute,
      this.onMapMoveEnd,
      this.onMapReady,
      this.onMapRendered,
      this.onMapLongClick,
      this.onMapClick});

  /// Setting navigation options for the map
  final MapOptions mapOptions;

  /// This callback will called whenever user change GPS location
  /// it will response a [RouteProgressEvent] object, which contains all information about current route
  final Function(RouteProgressEvent)? onRouteProgressChange;

  /// This callback will called when map is created
  final Function(MapNavigationViewController) onMapCreated;

  /// This callback will called when the route is building
  final VoidCallback? onRouteBuilding;

  /// This callback will called when the route is built successfully and response a [DirectionRoute] object
  /// which contains all information about the route
  final Function(DirectionRoute)? onRouteBuilt;

  /// This callback will called when the route is built failed and response a message
  final Function(String?)? onRouteBuildFailed;

  /// This callback will called when the navigation is running
  final VoidCallback? onNavigationRunning;

  /// This callback will called when the user is arrival to the destination
  final VoidCallback? onArrival;

  /// This callback will called when the navigation is finished (user arrived destination)
  final VoidCallback? onNavigationFinished;

  /// This callback will called when the navigation is cancelled
  /// (user click on the cancel button on the navigation view)
  final VoidCallback? onNavigationCancelled;

  /// This callback will called when the map is start move
  final VoidCallback? onMapMove;

  /// This callback will called when the map is rendered successfully and complete show all elements of the map
  final VoidCallback? onMapRendered;

  /// This callback will called when the map is moved end
  final VoidCallback? onMapMoveEnd;

  /// This callback will called when the map is ready
  final VoidCallback? onMapReady;

  /// This callback will called when the user long click on the map and response a [WayPoint] object
  /// which contains all information about the location where user long click
  final Function(WayPoint?)? onMapLongClick;

  /// This callback will called when the user click on the map and response a [WayPoint] object
  /// which contains all information about the location where user click
  final Function(WayPoint?)? onMapClick;

  /// This callback will called when the user is off route and response a [WayPoint] object
  /// which contains all information about the location where user off route
  /// (user is off route when user is not follow the route)
  final Function(WayPoint?)? userOffRoute;

  @override
  State<NavigationView> createState() => _NavigationViewState();
}

class _NavigationViewState extends State<NavigationView> {
  @override
  void initState() {
    assert(widget.mapOptions.apiKey != '', 'apikey không được để trống');
    assert(widget.mapOptions.mapStyle != '', 'mapStyle không được để trống');
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MapNavigationView(
        options: widget.mapOptions,
        onRouteEvent: _onEmbeddedRouteEvent,
        onCreated: (MapNavigationViewController controller) async {
          widget.onMapCreated(controller);
          controller.initialize();
        });
  }

  /// handle all events from the native side and response to the user callback function
  Future<void> _onEmbeddedRouteEvent(RouteEvent e) async {
    switch (e.eventType) {
      case MapEvent.progressChange:
        if (e.data != null) {
          var progressEvent = e.data as RouteProgressEvent;
          if (widget.onRouteProgressChange != null) {
            widget.onRouteProgressChange!(progressEvent);
          }
        }
        break;
      case MapEvent.routeBuilding:
        if (widget.onRouteBuilding != null) widget.onRouteBuilding!();
        break;
      case MapEvent.routeBuilt:
        Map<String, dynamic> map = decodeJson(data: e.data);
        var data = DirectionRoute.fromJson(map);
        if (widget.onRouteBuilt != null) widget.onRouteBuilt!(data);
        break;
      case MapEvent.routeBuildFailed:
        var message = jsonDecode(e.data);
        if (widget.onRouteBuildFailed != null) {
          widget.onRouteBuildFailed!(message);
        }
        break;
      case MapEvent.navigationRunning:
        if (widget.onNavigationRunning != null) widget.onNavigationRunning!();
        break;
      case MapEvent.onArrival:
        if (widget.onArrival != null) widget.onArrival!();
        break;
      case MapEvent.navigationFinished:
        if (widget.onNavigationFinished != null) widget.onNavigationFinished!();
        break;
      case MapEvent.navigationCancelled:
        if (widget.onNavigationCancelled != null) {
          widget.onNavigationCancelled!();
        }
        break;
      case MapEvent.milestoneEvent:
        break;
      case MapEvent.onMapClick:
        if (widget.onMapClick != null) {
          var data = decodeJson(data: e.data);
          WayPoint wayPoint = WayPoint(
              name: 'map_long_click',
              latitude: data['latitude'],
              longitude: data['longitude']);
          widget.onMapClick!(wayPoint);
        }
        break;
      case MapEvent.onMapLongClick:
        if (widget.onMapLongClick != null) {
          var data = decodeJson(data: e.data);
          WayPoint wayPoint = WayPoint(
              name: 'map_long_click',
              latitude: data['latitude'],
              longitude: data['longitude']);
          widget.onMapLongClick!(wayPoint);
        }
        break;
      case MapEvent.onMapMoveEnd:
        if (widget.onMapMoveEnd != null) widget.onMapMoveEnd!();
        break;
      case MapEvent.onMapMove:
        if (widget.onMapMove != null) widget.onMapMove!();
        break;
      case MapEvent.mapReady:
        if (widget.onMapReady != null) widget.onMapReady!();
        break;
      case MapEvent.userOffRoute:
        if (widget.userOffRoute != null) {
          var data = jsonDecode(e.data);
          WayPoint wayPoint = WayPoint(
              name: 'user_off_route',
              latitude: data['latitude'],
              longitude: data['longitude']);
          widget.userOffRoute!(wayPoint);
        }
        break;
      case MapEvent.onMapRendered:
        if (widget.onMapRendered != null) widget.onMapRendered!();
        break;
      default:
        break;
    }
  }
}
