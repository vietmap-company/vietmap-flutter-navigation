//
//  EventType.swift
//  demo_plugin
//
//  Created by NhatPV on 19/06/2023.
//

import Foundation

enum MapEventType: String, Codable
{
    case mapReady
    case onMapRendered
    case routeBuilding
    case routeBuilt
    case routeBuildFailed
    case progressChange
    case userOffRoute
    case milestoneEvent
    case navigationRunning
    case navigationCancelled
    case navigationFinished
    case fasterRouteFound
    case speechAnnouncement
    case bannerInstruction
    case onArrival
    case failedToReroute
    case rerouteAlong
    case onMapMove
    case onMapMoveEnd
    case onMapLongClick
    case onMapClick
    case onNewRouteSelected
}

