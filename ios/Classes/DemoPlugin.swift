import Flutter
import UIKit
import Mapbox

public class DemoPlugin:  NavigationFactory, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "demo_plugin", binaryMessenger: registrar.messenger())
        let eventChannel = FlutterEventChannel(name: "demo_plugin/events", binaryMessenger: registrar.messenger())
        let instance = DemoPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
        eventChannel.setStreamHandler(instance)
        
        let viewFactory = FlutterMapNavigationViewFactory(messenger: registrar.messenger())
        registrar.register(viewFactory, withId: "DemoPluginView")
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
        let arguments = call.arguments as? NSDictionary
        
        if(call.method == "getPlatformVersion")
        {
            result("iOS " + UIDevice.current.systemVersion)
        }
        else if(call.method == "getDistanceRemaining")
        {
            result(_distanceRemaining)
        }
        else if(call.method == "getDurationRemaining")
        {
            result(_durationRemaining)
        }
        else if(call.method == "startFreeDrive")
        {
            //          startFreeDrive(arguments: arguments, result: result)
        }
        else if(call.method == "startNavigation")
        {
            startNavigation(arguments: arguments, result: result)
        }
        else if(call.method == "addWayPoints")
        {
            //          addWayPoints(arguments: arguments, result: result)
        }
        else if(call.method == "finishNavigation")
        {
                      endNavigation(result: result)
        }
        else if(call.method == "enableOfflineRouting")
        {
            //          downloadOfflineRoute(arguments: arguments, flutterResult: result)
        }
        else
        {
            result(FlutterMethodNotImplemented)
        }
    }
}
