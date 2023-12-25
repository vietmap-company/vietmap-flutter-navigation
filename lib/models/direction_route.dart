import 'package:vietmap_gl_platform_interface/vietmap_gl_platform_interface.dart';

class DirectionRoute {
  String? routeIndex;
  num? distance;
  num? duration;

  /// This is the encoded string of the route, you can decode it to get the list of coordinates
  /// using [VietmapPolylineDecoder.decodePolyline] method from [VietmapPolylineDecoder] class
  String? geometry;
  num? weight;
  String? weightName;
  List<Legs>? legs;
  RouteOptions? routeOptions;
  String? voiceLocale;

  DirectionRoute(
      {this.routeIndex,
      this.distance,
      this.duration,
      this.geometry,
      this.weight,
      this.weightName,
      this.legs,
      this.routeOptions,
      this.voiceLocale});

  DirectionRoute.fromJson(Map<String, dynamic> json) {
    routeIndex = json['routeIndex'];
    distance = json['distance'];
    duration = json['duration'];
    geometry = json['geometry'];
    weight = json['weight'];
    weightName = json['weight_name'];
    if (json['legs'] != null) {
      legs = <Legs>[];
      json['legs'].forEach((v) {
        legs!.add(Legs.fromJson(v));
      });
    }
    routeOptions = json['routeOptions'] != null
        ? RouteOptions.fromJson(json['routeOptions'])
        : null;
    voiceLocale = json['voiceLocale'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = <String, dynamic>{};
    data['routeIndex'] = routeIndex;
    data['distance'] = distance;
    data['duration'] = duration;
    data['geometry'] = geometry;
    data['weight'] = weight;
    data['weight_name'] = weightName;
    if (legs != null) {
      data['legs'] = legs!.map((v) => v.toJson()).toList();
    }
    if (routeOptions != null) {
      data['routeOptions'] = routeOptions!.toJson();
    }
    data['voiceLocale'] = voiceLocale;
    return data;
  }

  // get list of coordinates
  List<LatLng> get getCoordinates {
    var coordinates = <LatLng>[];
    for (var leg in legs!) {
      for (var step in leg.steps!) {
        if (step.maneuver?.location != null &&
            step.maneuver!.location!.length == 2) {
          coordinates.add(LatLng(step.maneuver!.location!.first.toDouble(),
              step.maneuver!.location!.last.toDouble()));
        }
      }
    }

    return coordinates;
  }
}

class Legs {
  num? distance;
  num? duration;
  String? summary;
  List<Steps>? steps;

  Legs({this.distance, this.duration, this.summary, this.steps});

  Legs.fromJson(Map<String, dynamic> json) {
    distance = json['distance'];
    duration = json['duration'];
    summary = json['summary'];
    if (json['steps'] != null) {
      steps = <Steps>[];
      json['steps'].forEach((v) {
        steps!.add(Steps.fromJson(v));
      });
    }
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = <String, dynamic>{};
    data['distance'] = distance;
    data['duration'] = duration;
    data['summary'] = summary;
    if (steps != null) {
      data['steps'] = steps!.map((v) => v.toJson()).toList();
    }
    return data;
  }
}

class Steps {
  num? distance;
  num? duration;
  String? geometry;
  String? name;
  String? mode;
  Maneuver? maneuver;
  List<VoiceInstructions>? voiceInstructions;
  List<BannerInstructions>? bannerInstructions;
  String? drivingSide;
  num? weight;
  List<Intersections>? intersections;
  String? exits;

  Steps(
      {this.distance,
      this.duration,
      this.geometry,
      this.name,
      this.mode,
      this.maneuver,
      this.voiceInstructions,
      this.bannerInstructions,
      this.drivingSide,
      this.weight,
      this.intersections,
      this.exits});

  Steps.fromJson(Map<String, dynamic> json) {
    distance = json['distance'];
    duration = json['duration'];
    geometry = json['geometry'];
    name = json['name'];
    mode = json['mode'];
    maneuver =
        json['maneuver'] != null ? Maneuver.fromJson(json['maneuver']) : null;
    if (json['voiceInstructions'] != null) {
      voiceInstructions = <VoiceInstructions>[];
      json['voiceInstructions'].forEach((v) {
        voiceInstructions!.add(VoiceInstructions.fromJson(v));
      });
    }
    if (json['bannerInstructions'] != null) {
      bannerInstructions = <BannerInstructions>[];
      json['bannerInstructions'].forEach((v) {
        bannerInstructions!.add(BannerInstructions.fromJson(v));
      });
    }
    drivingSide = json['driving_side'];
    weight = json['weight'];
    if (json['intersections'] != null) {
      intersections = <Intersections>[];
      json['intersections'].forEach((v) {
        intersections!.add(Intersections.fromJson(v));
      });
    }
    exits = json['exits'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = <String, dynamic>{};
    data['distance'] = distance;
    data['duration'] = duration;
    data['geometry'] = geometry;
    data['name'] = name;
    data['mode'] = mode;
    if (maneuver != null) {
      data['maneuver'] = maneuver!.toJson();
    }
    if (voiceInstructions != null) {
      data['voiceInstructions'] =
          voiceInstructions!.map((v) => v.toJson()).toList();
    }
    if (bannerInstructions != null) {
      data['bannerInstructions'] =
          bannerInstructions!.map((v) => v.toJson()).toList();
    }
    data['driving_side'] = drivingSide;
    data['weight'] = weight;
    if (intersections != null) {
      data['intersections'] = intersections!.map((v) => v.toJson()).toList();
    }
    data['exits'] = exits;
    return data;
  }
}

class Maneuver {
  List<num>? location;
  num? bearingBefore;
  num? bearingAfter;
  String? instruction;
  String? type;
  String? modifier;
  num? exit;

  Maneuver(
      {this.location,
      this.bearingBefore,
      this.bearingAfter,
      this.instruction,
      this.type,
      this.modifier,
      this.exit});

  Maneuver.fromJson(Map<String, dynamic> json) {
    location = json['location'].cast<num>();
    bearingBefore = json['bearing_before'];
    bearingAfter = json['bearing_after'];
    instruction = json['instruction'];
    type = json['type'];
    modifier = json['modifier'];
    exit = json['exit'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = <String, dynamic>{};
    data['location'] = location;
    data['bearing_before'] = bearingBefore;
    data['bearing_after'] = bearingAfter;
    data['instruction'] = instruction;
    data['type'] = type;
    data['modifier'] = modifier;
    data['exit'] = exit;
    return data;
  }
}

class VoiceInstructions {
  num? distanceAlongGeometry;
  String? announcement;
  String? ssmlAnnouncement;

  VoiceInstructions(
      {this.distanceAlongGeometry, this.announcement, this.ssmlAnnouncement});

  VoiceInstructions.fromJson(Map<String, dynamic> json) {
    distanceAlongGeometry = json['distanceAlongGeometry'];
    announcement = json['announcement'];
    ssmlAnnouncement = json['ssmlAnnouncement'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = <String, dynamic>{};
    data['distanceAlongGeometry'] = distanceAlongGeometry;
    data['announcement'] = announcement;
    data['ssmlAnnouncement'] = ssmlAnnouncement;
    return data;
  }
}

class BannerInstructions {
  num? distanceAlongGeometry;
  Primary? primary;

  BannerInstructions({this.distanceAlongGeometry, this.primary});

  BannerInstructions.fromJson(Map<String, dynamic> json) {
    distanceAlongGeometry = json['distanceAlongGeometry'];
    primary =
        json['primary'] != null ? Primary.fromJson(json['primary']) : null;
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = <String, dynamic>{};
    data['distanceAlongGeometry'] = distanceAlongGeometry;
    if (primary != null) {
      data['primary'] = primary!.toJson();
    }
    return data;
  }
}

class Primary {
  String? text;
  List<Components>? components;
  String? type;
  String? modifier;

  Primary({this.text, this.components, this.type, this.modifier});

  Primary.fromJson(Map<String, dynamic> json) {
    text = json['text'];
    if (json['components'] != null) {
      components = <Components>[];
      json['components'].forEach((v) {
        components!.add(Components.fromJson(v));
      });
    }
    type = json['type'];
    modifier = json['modifier'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = <String, dynamic>{};
    data['text'] = text;
    if (components != null) {
      data['components'] = components!.map((v) => v.toJson()).toList();
    }
    data['type'] = type;
    data['modifier'] = modifier;
    return data;
  }
}

class Components {
  String? text;
  String? type;
  num? abbrPriority;

  Components({this.text, this.type, this.abbrPriority});

  Components.fromJson(Map<String, dynamic> json) {
    text = json['text'];
    type = json['type'];
    abbrPriority = json['abbr_priority'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = <String, dynamic>{};
    data['text'] = text;
    data['type'] = type;
    data['abbr_priority'] = abbrPriority;
    return data;
  }
}

class Intersections {
  List<num>? location;
  List<num>? bearings;
  List<bool>? entry;
  num? outIntersection;
  num? inIntersection;

  Intersections(
      {this.location,
      this.bearings,
      this.entry,
      this.outIntersection,
      this.inIntersection});

  Intersections.fromJson(Map<String, dynamic> json) {
    location = json['location'].cast<num>();
    bearings = json['bearings'].cast<num>();
    entry = json['entry'] != null ? json['entry'].cast<bool>() : [];
    outIntersection = json['out'];
    inIntersection = json['in'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = <String, dynamic>{};
    data['location'] = location;
    data['bearings'] = bearings;
    data['entry'] = entry;
    data['out'] = outIntersection;
    data['in'] = inIntersection;
    return data;
  }
}

class RouteOptions {
  String? baseUrl;

  String? profile;
  List<List>? coordinates;
  bool? alternatives;
  String? language;
  String? bearings;
  bool? continueStraight;
  bool? roundaboutExits;
  String? geometries;
  String? overview;
  bool? steps;
  String? annotations;
  bool? voiceInstructions;
  bool? bannerInstructions;
  String? voiceUnits;

  RouteOptions({
    this.baseUrl,
    this.profile,
    this.coordinates,
    this.alternatives,
    this.language,
    this.bearings,
    this.continueStraight,
    this.roundaboutExits,
    this.geometries,
    this.overview,
    this.steps,
    this.annotations,
    this.voiceInstructions,
    this.bannerInstructions,
    this.voiceUnits,
  });

  RouteOptions.fromJson(Map<String, dynamic> json) {
    baseUrl = json['baseUrl'];
    profile = json['profile'];
    alternatives = json['alternatives'];
    language = json['language'];
    bearings = json['bearings'];
    continueStraight = json['continue_straight'];
    roundaboutExits = json['roundabout_exits'];
    geometries = json['geometries'];
    overview = json['overview'];
    steps = json['steps'];
    annotations = json['annotations'];
    voiceInstructions = json['voice_instructions'];
    bannerInstructions = json['banner_instructions'];
    voiceUnits = json['voice_units'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = <String, dynamic>{};
    data['baseUrl'] = baseUrl;
    data['profile'] = profile;

    data['alternatives'] = alternatives;
    data['language'] = language;
    data['bearings'] = bearings;
    data['continue_straight'] = continueStraight;
    data['roundabout_exits'] = roundaboutExits;
    data['geometries'] = geometries;
    data['overview'] = overview;
    data['steps'] = steps;
    data['annotations'] = annotations;
    data['voice_instructions'] = voiceInstructions;
    data['banner_instructions'] = bannerInstructions;
    data['voice_units'] = voiceUnits;
    return data;
  }
}
