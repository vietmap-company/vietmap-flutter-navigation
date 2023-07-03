import 'dart:convert';
import 'events.dart';
import 'route_progress_event.dart';

/// Represents an event sent by the navigation service
class RouteEvent {
  MapEvent? eventType;
  dynamic data;

  RouteEvent({this.eventType, this.data});

  RouteEvent.fromJson(Map<String, dynamic> json) {
    if (json['eventType'] is int) {
      eventType = MapEvent.values[json['eventType']];
    } else {
      try {
        eventType = MapEvent.values.firstWhere(
            (e) => e.toString().split(".").last == json['eventType']);
      } on StateError {
        //When the list is empty or eventType not found (Bad State: No Element)
      } catch (e) {
        // TODO handle the error
      }
    }
    try {
      var dataJson = json['data'];
      if (eventType == MapEvent.progressChange) {
        data = RouteProgressEvent.fromJson(dataJson);
      } else {
        data = jsonEncode(dataJson);
      }
    } catch (e) {}
  }
}
