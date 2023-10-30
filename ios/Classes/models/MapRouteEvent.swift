//
//  MapRouteEvent.swift
//  demo_plugin
//
//  Created by NhatPV on 19/06/2023.
//

import Foundation

public class MapRouteEvent : Codable
{
    let eventType: MapEventType
    let data: String

    init(eventType: MapEventType, data: String) {
        self.eventType = eventType
        self.data = data
    }
}
