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
      this.onMapLongClick,
      this.onMapClick});
  final MapOptions mapOptions;
  final Function(RouteProgressEvent)? onRouteProgressChange;
  final Function(MapNavigationViewController) onMapCreated;
  final VoidCallback? onRouteBuilding;
  final Function(DirectionRoute)? onRouteBuilt;
  final Function(String?)? onRouteBuildFailed;
  final VoidCallback? onNavigationRunning;
  final VoidCallback? onArrival;
  final VoidCallback? onNavigationFinished;
  final VoidCallback? onNavigationCancelled;
  final VoidCallback? onMapMove;
  final VoidCallback? onMapMoveEnd;
  final VoidCallback? onMapReady;
  final Function(WayPoint?)? onMapLongClick;
  final Function(WayPoint?)? onMapClick;
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
      default:
        break;
    }
  }
}
