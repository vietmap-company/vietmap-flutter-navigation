//
//  FlutterMapboxNavigationPlugin.swift
//  demo_plugin
//
//  Created by NhatPV on 16/06/2023.
//
import Flutter
import UIKit
import Mapbox
import MapboxDirections
import MapboxCoreNavigation
import MapboxNavigation

private typealias RouteRequestSuccess = (([Route]) -> Void)
private typealias RouteRequestFailure = ((NSError) -> Void)

public class FlutterMapNavigationView : NavigationFactory, FlutterPlatformView
{
    let frame: CGRect
    let viewId: Int64

    let messenger: FlutterBinaryMessenger
    let channel: FlutterMethodChannel
    let eventChannel: FlutterEventChannel

    var navigationMapView: NavigationMapView!
    var arguments: NSDictionary?
    var routes: [Route]? {
        didSet {
//            guard let routes = routes,
//                  let current = routes.first else { mapView?.removeRoutes(); return }
//
//            mapView?.showRoutes(routes)
//            mapView?.showWaypoints(current)
        }
    }
    var wayPoints = [Waypoint]()
    var routeResponse: Route?
    var selectedRouteIndex = 0
    var routeOptions: NavigationRouteOptions?
//    var navigationService: NavigationService!

    var _mapInitialized = false;
    var locationManager = CLLocationManager()

//    private let passiveLocationManager = PassiveLocationManager()
//    private lazy var passiveLocationProvider = PassiveLocationProvider(locationManager: passiveLocationManager)

    init(messenger: FlutterBinaryMessenger, frame: CGRect, viewId: Int64, args: Any?)
    {
        self.frame = frame
        self.viewId = viewId
        self.arguments = args as! NSDictionary?

        self.messenger = messenger
        self.channel = FlutterMethodChannel(name: "flutter_map_navigation/\(viewId)", binaryMessenger: messenger)
        self.eventChannel = FlutterEventChannel(name: "flutter_map_navigation/\(viewId)/events", binaryMessenger: messenger)

        super.init()

        self.eventChannel.setStreamHandler(self)

        self.channel.setMethodCallHandler { [weak self](call, result) in

            guard let strongSelf = self else { return }

            let arguments = call.arguments as? NSDictionary

            if(call.method == "getPlatformVersion")
            {
                result("iOS " + UIDevice.current.systemVersion)
            }
            else if(call.method == "buildRoute")
            {
//                strongSelf.buildRoute(arguments: arguments, flutterResult: result)
            }
            else if(call.method == "clearRoute")
            {
//                strongSelf.clearRoute(arguments: arguments, result: result)
            }
            else if(call.method == "getDistanceRemaining")
            {
                result(strongSelf._distanceRemaining)
            }
            else if(call.method == "getDurationRemaining")
            {
                result(strongSelf._durationRemaining)
            }
            else if(call.method == "finishNavigation")
            {
//                strongSelf.endNavigation(result: result)
            }
            else if(call.method == "startFreeDrive")
            {
//                strongSelf.startEmbeddedFreeDrive(arguments: arguments, result: result)
            }
            else if(call.method == "startNavigation")
            {
//                strongSelf.startEmbeddedNavigation(arguments: arguments, result: result)
            }
            else if(call.method == "reCenter"){
                //used to recenter map from user action during navigation
//                strongSelf.navigationMapView.navigationCamera.follow()
            }
            else
            {
                result("method is not implemented");
            }

        }
    }

    public func view() -> UIView
    {
        if(_mapInitialized)
        {
            return navigationMapView
        }

        setupMapView()

        return navigationMapView
    }

    private func configureMapView() {
        navigationMapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        navigationMapView.styleURL = URL(string: _url);
        navigationMapView.routeLineColor = UIColor.yellow
        navigationMapView.userTrackingMode = .follow
        navigationMapView.showsUserHeadingIndicator = true
    }
    
    private func setupMapView()
    {
        navigationMapView = NavigationMapView(frame: frame)
        navigationMapView.delegate = self

        if(self.arguments != nil)
        {

            parseFlutterArguments(arguments: arguments)
            configureMapView()

            var currentLocation: CLLocation!

            locationManager.requestWhenInUseAuthorization()

            if(CLLocationManager.authorizationStatus() == .authorizedWhenInUse ||
                CLLocationManager.authorizationStatus() == .authorizedAlways) {
                currentLocation = locationManager.location

            }

            let initialLatitude = arguments?["initialLatitude"] as? Double ?? currentLocation?.coordinate.latitude
            let initialLongitude = arguments?["initialLongitude"] as? Double ?? currentLocation?.coordinate.longitude
            if(initialLatitude != nil && initialLongitude != nil)
            {
                moveCameraToCoordinates(latitude: initialLatitude!, longitude: initialLongitude!)
            }

        }

        if _longPressDestinationEnabled
        {
            let gesture = UILongPressGestureRecognizer(target: self, action: #selector(handleLongPress(_:)))
            gesture.delegate = self
            navigationMapView?.addGestureRecognizer(gesture)
        }

    }

    func moveCameraToCoordinates(latitude: Double, longitude: Double) {
        let camera = MGLMapCamera(
            lookingAtCenter: CLLocationCoordinate2D(latitude: latitude, longitude: longitude),
            acrossDistance: calculateDistance(zoomLevel: _zoom),
            pitch: 75,
            heading: (navigationMapView.userLocation?.heading?.trueHeading) ?? 0
        )
        navigationMapView.setCamera(camera, animated: true)
    }
    
    func calculateDistance(zoomLevel: Double) -> CLLocationDistance {
        let earthRadius = 6378137.0 // Earth's radius in meters
        let tileSize = 512.0 // Map tile size in pixels
        let zoomFactor = pow(2, zoomLevel)
        let distance = (2 * .pi * earthRadius) * (tileSize / zoomFactor)
        return distance
    }
    
    fileprivate lazy var defaultSuccess: RouteRequestSuccess = { [weak self] (routes) in
        guard let strongSelf = self else { return }
        guard let current = routes.first else { return }
        strongSelf.routeResponse = current
        strongSelf.sendEvent(eventType: MapEventType.routeBuilt, data: strongSelf.encodeRoute(route: current))
        strongSelf._routes = routes
        strongSelf._wayPoints = current.routeOptions.waypoints
        strongSelf.navigationMapView.showRoutes(routes)
        strongSelf.navigationMapView.showWaypoints(current)
    }

    fileprivate lazy var defaultFailure: RouteRequestFailure = { [weak self] (error) in
        guard let strongSelf = self else { return }
        strongSelf._routes = nil //clear routes from the map
    }

}

extension FlutterMapNavigationView : NavigationMapViewDelegate {
    public func navigationMapView(_ mapView: NavigationMapView, didSelect route: Route) {
        guard let routes = routes else { return }
        guard let index = routes.firstIndex(where: { $0 == route }) else { return }
        self.routes!.remove(at: index)
        self.routes!.insert(route, at: 0)
    }

    public func mapViewDidFinishLoadingMap(_ mapView: NavigationMapView) {
        self.navigationMapView.recenterMap()
    }

}

extension FlutterMapNavigationView : MGLMapViewDelegate {
    
}

extension FlutterMapNavigationView : UIGestureRecognizerDelegate {

    public func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        return true
    }

    @objc func handleLongPress(_ gesture: UILongPressGestureRecognizer) {
        guard gesture.state == .ended else { return }
        let location = navigationMapView.convert(gesture.location(in: navigationMapView), toCoordinateFrom: navigationMapView)
        requestRoute(destination: location)
    }

    func requestRoute(destination: CLLocationCoordinate2D) {
        isEmbeddedNavigation = true
        sendEvent(eventType: MapEventType.routeBuilding)

        guard let userLocation = navigationMapView.userLocation?.location else { return }
        let location = CLLocation(latitude: userLocation.coordinate.latitude,
                                  longitude: userLocation.coordinate.longitude)
        let userWaypoint = Waypoint(location: location, name: "Current Location")
        let destinationWaypoint = Waypoint(coordinate: destination)

        let routeOptions = NavigationRouteOptions(waypoints: [userWaypoint, destinationWaypoint])
        routeOptions.shapeFormat = .polyline6
        routeOptions.locale = Locale(identifier: "vi")
        requestRoute(with: routeOptions, success: defaultSuccess, failure: defaultFailure)
    }
    
    fileprivate func requestRoute(with options: RouteOptions, success: @escaping RouteRequestSuccess, failure: RouteRequestFailure?) {
        let handler: Directions.RouteCompletionHandler = {(waypoints, potentialRoutes, potentialError) in
            if let error = potentialError, let fail = failure { return fail(error) }
            guard let routes = potentialRoutes else { return }
            return success(routes)
        }
        let apiUrl = Directions.shared.url(forCalculating: options)
        print("API Request URL: \(apiUrl)")
        Directions.shared.calculate(options, completionHandler: handler)
    }
}

