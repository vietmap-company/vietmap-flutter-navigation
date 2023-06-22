import Flutter
import UIKit
import Mapbox

public class DemoPlugin:  NavigationFactory, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "demo_plugin", binaryMessenger: registrar.messenger())
    let instance = DemoPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
      
    let viewFactory = FlutterMapNavigationViewFactory(messenger: registrar.messenger())
    registrar.register(viewFactory, withId: "FlutterMapNavigationView")
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
      
    let arguments = call.arguments as? NSDictionary

    switch call.method {
        case "getPlatformVersion":
          result("iOS " + UIDevice.current.systemVersion)
        case "startNavigation":
          startNavigation(arguments: arguments, result: result)
        default:
          result(FlutterMethodNotImplemented)
        }
    }
}
