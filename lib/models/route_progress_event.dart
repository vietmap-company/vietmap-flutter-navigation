import 'current_location.dart';
import 'route_leg.dart';
import '../helpers.dart';

///This class contains all progress information at any given time during a navigation session.
///This progress includes information for the current route, leg and step the user is traversing along.
///With every new valid location update, a new route progress will be generated using the latest information.
class RouteProgressEvent {
  bool? arrived;
  // Khoảng cách còn lại (total)
  double? distanceRemaining;
  // Thời gian ước tính còn lại (total)
  double? durationRemaining;
  // Khoảng cách đã đi được
  double? distanceTraveled;
  // Khỏang cách đã đi được của tuyến đường hiện tại
  double? currentLegDistanceTraveled;
  // Khoảng cách còn lại của tuyến đường hiện tại
  double? currentLegDistanceRemaining;

  /// Text instruction for the current step the user is on.
  String? currentStepInstruction;

  /// Current leg the user is traversing along.
  RouteLeg? currentLeg;

  RouteLeg? priorLeg;

  List<RouteLeg>? remainingLegs;

  int? legIndex;

  int? stepIndex;

  bool? isProgressEvent;

  /// Distance to the next turn, in meters.
  double? distanceToNextTurn;

  ///  Current modifier of the step the user is on.
  /// currentModifierType and currentModifier is an additional instruction on how to complete the current step.
  String? currentModifierType;

  /// currentModifierType and currentModifier is an additional instruction on how to complete the current step.
  String? currentModifier;

  /// Current location of the user, this is the raw location from the location provider
  CurrentLocation? currentLocation;

  /// Current snapped location of the user, this is the location snapped to the route
  /// This value will stable if the user is not moving
  /// The location is always on the route, if the user is off route, the location will be snapped to the nearest point on the route
  CurrentLocation? snappedLocation;
  RouteProgressEvent(
      {this.arrived,
      this.distanceRemaining,
      this.durationRemaining,
      this.distanceTraveled,
      this.currentLegDistanceTraveled,
      this.currentLegDistanceRemaining,
      this.currentStepInstruction,
      this.currentLeg,
      this.priorLeg,
      this.remainingLegs,
      this.legIndex,
      this.stepIndex,
      this.isProgressEvent,
      this.currentModifier,
      this.currentModifierType,
      this.distanceToNextTurn});

  RouteProgressEvent.fromJson(Map<String, dynamic> json) {
    currentLocation = json['location'] == null
        ? null
        : CurrentLocation.fromJson(json['location'] as Map<String, dynamic>);
    snappedLocation = json['snappedLocation'] == null
        ? null
        : CurrentLocation.fromJson(
            json['snappedLocation'] as Map<String, dynamic>);
    currentModifier = json['currentModifier'];
    currentModifierType = json['currentModifierType'];
    distanceToNextTurn = json['distanceToNextTurn'];
    isProgressEvent = json['arrived'] != null;
    arrived = json['arrived'] == null ? false : json['arrived'] as bool?;
    distanceRemaining = isNullOrZero(json['distanceRemaining'])
        ? 0.0
        : json["distanceRemaining"] + .0;
    durationRemaining = isNullOrZero(json['durationRemaining'])
        ? 0.0
        : json["durationRemaining"] + .0;
    distanceTraveled = isNullOrZero(json['distanceTraveled'])
        ? 0.0
        : json["distanceTraveled"] + .0;
    currentLegDistanceTraveled =
        isNullOrZero(json['currentLegDistanceTraveled'])
            ? 0.0
            : json["currentLegDistanceTraveled"] + .0;
    currentLegDistanceRemaining =
        isNullOrZero(json['currentLegDistanceRemaining'])
            ? 0.0
            : json["currentLegDistanceRemaining"] + .0;
    currentStepInstruction = json['currentStepInstruction'];
    currentLeg = json['currentLeg'] == null
        ? null
        : RouteLeg.fromJson(json['currentLeg'] as Map<String, dynamic>);
    priorLeg = json['priorLeg'] == null
        ? null
        : RouteLeg.fromJson(json['priorLeg'] as Map<String, dynamic>);
    remainingLegs = (json['remainingLegs'] as List?)
        ?.map((e) =>
            e == null ? null : RouteLeg.fromJson(e as Map<String, dynamic>))
        .cast<RouteLeg>()
        .toList();
    legIndex = json['legIndex'];
    stepIndex = json['stepIndex'];
  }
}
