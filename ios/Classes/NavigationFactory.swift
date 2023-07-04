import Flutter
import UIKit
import Mapbox
import MapboxDirections
import MapboxCoreNavigation
import MapboxNavigation

private typealias RouteRequestSuccess = (([Route]) -> Void)
private typealias RouteRequestFailure = ((NSError) -> Void)

public class NavigationFactory : NSObject, FlutterStreamHandler
{
    let _url = Bundle.main.object(forInfoDictionaryKey: "VietMapURL") as! String
    var _navigationViewController: NavigationViewController? = nil
    var _eventSink: FlutterEventSink? = nil
    
    let ALLOW_ROUTE_SELECTION = false
    let IsMultipleUniqueRoutes = false
    var isEmbeddedNavigation = false
    
    var _distanceRemaining: Double?
    var _durationRemaining: Double?
    var _navigationMode: String?
    var _routes: [Route]?
    var _wayPointOrder = [Int:Waypoint]()
    var _wayPoints = [Waypoint]()
    var _lastKnownLocation: CLLocation?
    
    var _options: NavigationRouteOptions?
    var _simulateRoute = false
    var _allowsUTurnAtWayPoints: Bool?
    var _isOptimized = false
    var _language = "en"
    var _voiceUnits = "imperial"
    var _mapStyleUrlDay: String?
    var _mapStyleUrlNight: String?
    var _zoom: Double = 13.0
    var _tilt: Double = 0.0
    var _bearing: Double = 0.0
    var _animateBuildRoute = true
    var _longPressDestinationEnabled = true
    var _shouldReRoute = true
    var _showReportFeedbackButton = true
    var _showEndOfRouteFeedback = true
    var navigationDirections: Directions?
    
    
    // MARK: Directions Request Handlers

    fileprivate lazy var defaultSuccess: RouteRequestSuccess = { [weak self] (routes) in
        guard let strongSelf = self else { return }
        guard let current = routes.first else { return }
        strongSelf._routes = routes
        strongSelf._wayPoints = current.routeOptions.waypoints
        strongSelf.startNavigationWithRoute(simulated: false)
    }

    fileprivate lazy var defaultFailure: RouteRequestFailure = { [weak self] (error) in
        guard let strongSelf = self else { return }
        strongSelf._routes = nil //clear routes from the map
    }

    func getLocationsFromFlutterArgument(arguments: NSDictionary?) {
        _ = [Route]()
        guard let oWayPoints = arguments?["wayPoints"] as? NSDictionary else {return}
        for item in oWayPoints as NSDictionary
        {
            let point = item.value as! NSDictionary
            guard point["Name"] is String else {return }
            guard let oLatitude = point["Latitude"] as? Double else {return}
            guard let oLongitude = point["Longitude"] as? Double else {return}
            _ = point["IsSilent"] as? Bool ?? false
            _ = point["Order"] as? Int
            let coordinate = CLLocationCoordinate2D(latitude: oLatitude, longitude: oLongitude)
            let waypoint = Waypoint(coordinate: coordinate)
            _wayPoints.append(waypoint)
        }
        
        let routeOptions = NavigationRouteOptions(waypoints: _wayPoints, profileIdentifier: .automobile)
        routeOptions.shapeFormat = .polyline6
        routeOptions.locale = Locale(identifier: "vi")
        requestRoute(with: routeOptions, success: defaultSuccess, failure: defaultFailure)
    }
    
    // MARK: - Public Methods
    // MARK: Route Requests
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
    
    func startNavigation(arguments: NSDictionary?, result: @escaping FlutterResult) {
        _wayPoints.removeAll()
        getLocationsFromFlutterArgument(arguments: arguments)
        registerNotifications()
    }
    
    func startNavigationWithRoute(simulated: Bool = false) {
        guard let route = _routes?.first else { return }
        
        _navigationViewController = NavigationViewController(
            for: route,
            styles: [NightStyle()],
            locationManager: getNavigationLocationManager(simulated: simulated)
        )
        _navigationViewController?.delegate = self
        configureMapView()
        let flutterViewController = UIApplication.shared.delegate?.window??.rootViewController as! FlutterViewController
        flutterViewController.present(_navigationViewController!, animated: true, completion: nil)
    }
    
    private func configureMapView() {
        _navigationViewController?.mapView?.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        _navigationViewController?.routeController.reroutesProactively = true
        _navigationViewController?.mapView?.styleURL = URL(string: _url);
        _navigationViewController?.mapView?.userTrackingMode = .follow
        _navigationViewController?.mapView?.showsUserHeadingIndicator = true
    }
    
    private func getNavigationLocationManager(simulated: Bool) -> NavigationLocationManager {
        guard let route = _routes?.first else { return NavigationLocationManager() }
        let simulatedLocationManager = SimulatedLocationManager(route: route)
        simulatedLocationManager.speedMultiplier = 2
        return simulated ? simulatedLocationManager : NavigationLocationManager()
    }
    
    func parseFlutterArguments(arguments: NSDictionary?) {
        _language = arguments?["language"] as? String ?? _language
        _voiceUnits = arguments?["units"] as? String ?? _voiceUnits
        _simulateRoute = arguments?["simulateRoute"] as? Bool ?? _simulateRoute
        _isOptimized = arguments?["isOptimized"] as? Bool ?? _isOptimized
        _allowsUTurnAtWayPoints = arguments?["allowsUTurnAtWayPoints"] as? Bool
        _navigationMode = arguments?["mode"] as? String ?? "drivingWithTraffic"
        _showReportFeedbackButton = arguments?["showReportFeedbackButton"] as? Bool ?? _showReportFeedbackButton
        _showEndOfRouteFeedback = arguments?["showEndOfRouteFeedback"] as? Bool ?? _showEndOfRouteFeedback
        _mapStyleUrlDay = arguments?["mapStyleUrlDay"] as? String
        _mapStyleUrlNight = arguments?["mapStyleUrlNight"] as? String
        _zoom = arguments?["zoom"] as? Double ?? _zoom
        _bearing = arguments?["bearing"] as? Double ?? _bearing
        _tilt = arguments?["tilt"] as? Double ?? _tilt
        _animateBuildRoute = arguments?["animateBuildRoute"] as? Bool ?? _animateBuildRoute
        _longPressDestinationEnabled = arguments?["longPressDestinationEnabled"] as? Bool ?? _longPressDestinationEnabled
    }
    
    func sendEvent(eventType: MapEventType, data: String = "") {
        let routeEvent = MapRouteEvent(eventType: eventType, data: data)
        
        let jsonEncoder = JSONEncoder()
        let jsonData = try! jsonEncoder.encode(routeEvent)
        let eventJson = String(data: jsonData, encoding: String.Encoding.utf8)
        if(_eventSink != nil) {
            _eventSink!(eventJson)
        }
    }
    
    func endNavigation(result: FlutterResult?)
    {
        sendEvent(eventType: MapEventType.navigationFinished)
        if(self._navigationViewController != nil)
        {
            endNotifications()
            self._navigationViewController?.routeController.endNavigation()
            self._navigationViewController?.dismiss(animated: true, completion: {
                self._navigationViewController = nil
                if(result != nil)
                {
                    result!(true)
                }
            })
        }
    }

    func registerNotifications() {
        NotificationCenter.default.addObserver(self, selector: #selector(reRouted(_:)), name: .routeControllerDidReroute, object: nil)
    }
    
    func endNotifications() {
        NotificationCenter.default.removeObserver(self, name: .routeControllerWillReroute, object: nil)
    }
    
    @objc func reRouted(_ notification: NSNotification) {
        if let userInfo = notification.object as? RouteController {
            self._navigationViewController?.mapView?.showRoutes([userInfo.routeProgress.route])
            self._navigationViewController?.mapView?.tracksUserCourse = true
            self._navigationViewController?.mapView?.recenterMap()
        }
    }
    
    //MARK: EventListener Delegates
    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        _eventSink = events
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        _eventSink = nil
        return nil
    }
    
    // MARK: convert json
    func encodeRoute(route: Route) -> String {
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
}

extension NavigationFactory: NavigationViewControllerDelegate {
    public func navigationViewController(_ navigationViewController: NavigationViewController, didUpdate progress: RouteProgress, with location: CLLocation, rawLocation: CLLocation) {
        _lastKnownLocation = location
        _distanceRemaining = progress.distanceRemaining
        _durationRemaining = progress.durationRemaining
        sendEvent(eventType: MapEventType.navigationRunning)
        //_currentLegDescription =  progress.currentLeg.description
        if(_eventSink != nil)
        {
            let routeDictionary: [String: Any] = [
                "description": progress.description
            ]
            var progressEventJson = "{}"
            if let jsonData = try? JSONSerialization.data(withJSONObject: routeDictionary, options: []) {
                if let jsonData = String(data: jsonData, encoding: .utf8) {
                    progressEventJson = jsonData
                }
            }
            
            _eventSink!(progressEventJson)
            
            if(progress.isFinalLeg && progress.currentLegProgress.userHasArrivedAtWaypoint && !_showEndOfRouteFeedback)
            {
                _eventSink = nil
            }
        }
    }

    public func navigationViewController(_ navigationViewController: NavigationViewController, didArriveAt waypoint: Waypoint) -> Bool {
        sendEvent(eventType: MapEventType.onArrival, data: "true")
        return true
    }
    
    // Called when the user hits the exit button.
    // If implemented, you are responsible for also dismissing the UI.
    public func navigationViewControllerDidDismiss(_ navigationViewController: NavigationViewController, byCanceling canceled: Bool) {
        if(canceled)
        {
           sendEvent(eventType: MapEventType.navigationCancelled)
        }
        endNavigation(result: nil)
    }
}
