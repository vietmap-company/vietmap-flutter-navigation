import 'package:flutter/material.dart';
import 'package:vietmap_gl_platform_interface/vietmap_gl_platform_interface.dart';
export 'package:vietmap_gl_platform_interface/vietmap_gl_platform_interface.dart'
    show LatLng;

class MarkerWidget {
  /// The image path of the marker, allow only image in [png], [jpeg], [jpg] format
  final Widget child;
  final LatLng latLng;
  final String? title;
  final String? snippet;
  int? markerId;

  MarkerWidget({
    required this.child,
    required this.latLng,
    this.title,
    this.snippet,
  });
  toJson() {
    return {
      'latitude': latLng.latitude,
      'longitude': latLng.longitude,
      'title': title,
      'snippet': snippet
    };
  }
}
