//
//  Route.swift
//  demo_plugin
//
//  Created by NhatPV on 05/07/2023.
//

import Foundation
import MapboxDirections
import MapboxCoreNavigation
import MapboxNavigation

// MARK: encode route
public func encodeRoute(route: Route) -> String {
    // TODO: Add parameter on routeDictionary.
    let routeDictionary: [String: Any] = [
        "routeIndex": "",
        "distance": route.distance,
        "duration": route.expectedTravelTime,
        "geometry": "",
        "weight": 0,
        "weight_name": "",
        "voiceLocale": route.speechLocale?.identifier ?? "",
        "legs": convertLeg(legs: route.legs),
        "routeOptions": convertRouteOption(route: route)
    ]

    if let jsonData = try? JSONSerialization.data(withJSONObject: routeDictionary, options: []) {
        if let jsonData = String(data: jsonData, encoding: .utf8) {
            return jsonData
        }
    }
    return "{}"
}
    
func convertRouteOption(route: Route) -> [String: Any] {
    return [
        "baseUrl": route.apiEndpoint?.description ?? "",
        "user": "vietmap",
        "profile": route.directionsOptions.profileIdentifier,
        "alternatives": true,
        "language": route.directionsOptions.locale.identifier,
        "bearings": "",
        "continue_straight": true,
        "roundabout_exits": true,
        "geometries": route.directionsOptions.shapeFormat.description,
        "overview": "",
        "annotations": "",
        "voice_instructions": true,
        "banner_instructions": true,
        "voice_units": "metric",
        "access_token": route.accessToken ?? "",
        "uuid": route.routeIdentifier ?? ""
    ] as [String: Any]
}

func convertLeg(legs: [MapboxDirections.RouteLeg]) -> Array<Any> {
    var result: [Any] = []
    
    for item in legs {
        let itemResult: [String: Any] = [
            "distance": item.distance,
            "duration": item.expectedTravelTime,
            "summary": item.name,
            "steps": convertSteps(steps: item.steps)
        ]
        result.append(itemResult)
    }

    return result
}

func convertSteps(steps: [MapboxDirections.RouteStep]) -> Array<Any> {
    var result: [Any] = []
    
    for item in steps {
        let itemResult: [String: Any] = [
            "distance": item.distance,
            "duration": item.expectedTravelTime,
            "geometry": "",
            "name": item.instructions,
            "mode": item.transportType.description,
            "driving_side": item.drivingSide.description,
            "weight": 0,
            "exits": item.exitCodes?.first ?? "",
            "maneuver": [
                "location" : [item.maneuverLocation.longitude, item.maneuverLocation.latitude],
                "bearing_before": item.initialHeading ?? 0,
                "bearing_after": item.finalHeading ?? 0,
                "instruction": item.instructions,
                "type": item.maneuverType.description,
                "modifier": item.maneuverDirection.description,
                "exit": item.exitIndex ?? 0,
            ] as [String : Any],
            "voiceInstructions": convertVoiceInstructions(voices: item.instructionsSpokenAlongStep ?? []),
            "bannerInstructions": convertBannerInstructions(banners: item.instructionsDisplayedAlongStep ?? []),
            "intersections": convertIntersections(intersections: item.intersections ?? [])
        ]
        
        result.append(itemResult)
    }
    
    return result
}

func convertIntersections(intersections: [MapboxDirections.Intersection]) -> Array<Any> {
    var result: [Any] = []
    
    for item in intersections {
        let itemResult: [String: Any] = [
            "location": [
                item.location.longitude,
                item.location.latitude
             ],
            "bearings": [
                item.headings.first
              ],
//                "entry": [],
            "out": item.outletIndex
        ]
        
        result.append(itemResult)
    }
    
    return result
}

func convertVoiceInstructions(voices: [MapboxDirections.SpokenInstruction]) -> Array<Any> {
    var result: [Any] = []
    
    for item in voices {
        let itemResult: [String: Any]  = [
            "distanceAlongGeometry": item.distanceAlongStep,
            "announcement": item.text,
            "ssmlAnnouncement": item.ssmlText
        ]
        result.append(itemResult)
    }
    
    return result
}

func convertBannerInstructions(banners: [MapboxDirections.VisualInstructionBanner]) -> Array<Any> {
    var result: [Any] = []
    
    for item in banners {
        let itemResult: [String: Any] = [
              "distanceAlongGeometry": item.distanceAlongStep,
              "primary": [
                "text": item.primaryInstruction.text ?? "",
                "type": item.primaryInstruction.maneuverType.description,
                "modifier": item.primaryInstruction.maneuverDirection.description,
                "components": convertComponent(components: item.primaryInstruction.components)
              ] as [String: Any]
        ]
        result.append(itemResult)
    }
    
    return result
}

func convertComponent(components: [MapboxDirections.ComponentRepresentable]) -> Array<Any> {
    var result: [Any] = []
    
    for object in components {
        let item: MapboxDirections.VisualInstructionComponent = object as! VisualInstructionComponent
        let itemResult: [String: Any] = [
            "text": item.text ?? "",
            "type": item.type.description,
            "abbr_priority": item.abbreviationPriority
        ]
        
        result.append(itemResult)
    }
    
    return result
}

// MARK: encode route progress
func encodeRouteProgress(routeProgress: RouteProgress) -> String {
    // TODO: Add parameter on routeDictionary.
    let routeDictionary: [String: Any] = [
        "distanceRemaining": routeProgress.distanceRemaining,
        "durationRemaining": routeProgress.durationRemaining,
        "distanceTraveled": routeProgress.distanceTraveled,
        "legIndex": routeProgress.legIndex,
        "currentLegDistanceRemaining": routeProgress.currentLegProgress.distanceRemaining,
        "currentLegDistanceTraveled": routeProgress.currentLegProgress.distanceTraveled,
        "currentStepInstruction": routeProgress.currentLegProgress.currentStep.instructions,
        "distanceToNextTurn": routeProgress.currentLegProgress.currentStepProgress.distanceRemaining,
        "currentModifier": routeProgress.currentLegProgress.currentStepProgress.step.instructionsDisplayedAlongStep?.last?.primaryInstruction.maneuverDirection.description ?? "",
        "currentModifierType": routeProgress.currentLegProgress.currentStepProgress.step.instructionsDisplayedAlongStep?.last?.primaryInstruction.maneuverType.description ?? "",
        "currentLeg": [
            "distance": routeProgress.currentLeg.distance,
            "expectedTravelTime": routeProgress.currentLeg.expectedTravelTime,
            "steps": convertStep(steps: routeProgress.currentLeg.steps)
        ] as [String : Any]
    ]

    if let jsonData = try? JSONSerialization.data(withJSONObject: routeDictionary, options: []) {
        if let jsonData = String(data: jsonData, encoding: .utf8) {
            return jsonData
        }
    }
    return "{}"
}

func convertStep(steps: [MapboxDirections.RouteStep]) -> Array<Any> {
    var result: [Any] = []
    
    for item in steps {
        let itemResult : [String: Any] = [
              "instructions": "",
              "distance": item.distance,
              "expectedTravelTime":item.expectedTravelTime
        ]
        
        result.append(itemResult)
    }
    
    return result
}

// MARK: encode long click, click, reroute
func encodeLocation(location: CLLocationCoordinate2D) -> String {
    // TODO: Add parameter on routeDictionary.
    let routeDictionary: [String: Any] = [
        "latitude": location.latitude,
        "longitude": location.longitude
    ]

    if let jsonData = try? JSONSerialization.data(withJSONObject: routeDictionary, options: []) {
        if let jsonData = String(data: jsonData, encoding: .utf8) {
            return jsonData
        }
    }
    return "{}"
}
