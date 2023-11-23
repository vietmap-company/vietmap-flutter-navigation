class CurrentLocation {
  final num? latitude;
  final num? longitude;
  final String? provider;
  final num? speed;
  final num? bearing;
  final num? altitude;
  final num? accuracy;

  /// This is not available on Android devices below API 26 (Android O version).
  /// On android devices below API 26, this will return [speed] value.
  /// We don't require the minimum API level to be 26 because we want to support
  /// as many devices as possible.
  final num? speedAccuracyMetersPerSecond;

  CurrentLocation(
      {this.latitude,
      this.longitude,
      this.provider,
      this.speed,
      this.bearing,
      this.altitude,
      this.accuracy,
      this.speedAccuracyMetersPerSecond});

  factory CurrentLocation.fromJson(Map<String, dynamic> json) {
    return CurrentLocation(
        latitude: json['latitude'],
        longitude: json['longitude'],
        provider: json['provider'],
        speed: json['speed'],
        bearing: json['bearing'],
        altitude: json['altitude'],
        accuracy: json['accuracy'],
        speedAccuracyMetersPerSecond: json['speedAccuracyMetersPerSecond']);
  }
}
