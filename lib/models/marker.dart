import 'package:vietmap_gl_platform_interface/vietmap_gl_platform_interface.dart';
export 'package:vietmap_gl_platform_interface/vietmap_gl_platform_interface.dart'
    show LatLng;

class NavigationMarker {
  /// The image path of the marker, allow only image in [png], [jpeg], [jpg] format
  final String imagePath;
  final LatLng latLng;
  final String? title;
  final String? snippet;
  int? markerId;
  final int? width;
  final int? height;

  NavigationMarker({
    required this.imagePath,
    required this.latLng,
    this.title,
    this.snippet,
    this.width,
    this.height,
  })  : assert(width != null || height == null,
            'Width and height must be both provided or both null'),
        assert(height != null || width == null,
            'Width and height must be both provided or both null');
  toJson() {
    return {
      'latitude': latLng.latitude,
      'longitude': latLng.longitude,
      'title': title,
      'snippet': snippet,
      'width': width,
      'height': height,
    };
  }
}
