//
//  NavigationViewFactory.swift
//  demo_plugin
//
//  Created by NhatPV on 21/06/2023.
//

import Flutter
import UIKit
import Mapbox
import MapboxDirections
import MapboxCoreNavigation
import MapboxNavigation

public class FlutterMapNavigationViewFactory : NSObject, FlutterPlatformViewFactory
{
    let messenger: FlutterBinaryMessenger

    init(messenger: FlutterBinaryMessenger) {
        self.messenger = messenger
    }

    public func create(withFrame frame: CGRect, viewIdentifier viewId: Int64, arguments args: Any?) -> FlutterPlatformView {
        return FlutterMapNavigationView(messenger: self.messenger, frame: frame, viewId: viewId, args: args)
    }

    public func createArgsCodec() -> FlutterMessageCodec & NSObjectProtocol {
        return FlutterStandardMessageCodec.sharedInstance()
    }
}
