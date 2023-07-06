import 'embedded/controller.dart';

extension ProfileExtension on DrivingProfile {
  String getValue() {
    switch (this) {
      case DrivingProfile.drivingTraffic:
        return 'driving-traffic';
      case DrivingProfile.cycling:
        return 'cycling';
      case DrivingProfile.walking:
        return 'walking';
      case DrivingProfile.motorcycle:
        return 'motorcycle';
    }
  }
}
