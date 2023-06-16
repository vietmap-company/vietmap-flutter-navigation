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
        var routes = [Route]()
        guard let oWayPoints = arguments?["wayPoints"] as? NSDictionary else {return}
        for item in oWayPoints as NSDictionary
        {
            let point = item.value as! NSDictionary
            guard let oName = point["Name"] as? String else {return }
            guard let oLatitude = point["Latitude"] as? Double else {return}
            guard let oLongitude = point["Longitude"] as? Double else {return}
            let oIsSilent = point["IsSilent"] as? Bool ?? false
            let order = point["Order"] as? Int
            let coordinate = CLLocationCoordinate2D(latitude: oLatitude, longitude: oLongitude)
            let waypoint = Waypoint(coordinate: coordinate, name: "Điểm đến của bạn")
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
        _navigationViewController?.mapView?.routeLineColor = UIColor.yellow
        _navigationViewController?.mapView?.userTrackingMode = .follow
        _navigationViewController?.mapView?.showsUserHeadingIndicator = true
    }
    
    private func getNavigationLocationManager(simulated: Bool) -> NavigationLocationManager {
        guard let route = _routes?.first else { return NavigationLocationManager() }
        let simulatedLocationManager = SimulatedLocationManager(route: route)
        simulatedLocationManager.speedMultiplier = 2
        return simulated ? simulatedLocationManager : NavigationLocationManager()
    }

    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        return nil
    }
}

extension NavigationFactory: NavigationViewControllerDelegate {
    // By default, when the user arrives at a waypoint, the next leg starts immediately.
    // If you implement this method, return true to preserve this behavior.
    // Return false to remain on the current leg, for example to allow the user to provide input.
    // If you return false, you must manually advance to the next leg. See the example above in `confirmationControllerDidConfirm(_:)`.
    public func navigationViewController(_ navigationViewController: NavigationViewController, didArriveAt waypoint: Waypoint) -> Bool {
        return true
    }
    
    // Called when the user hits the exit button.
    // If implemented, you are responsible for also dismissing the UI.
    public func navigationViewControllerDidDismiss(_ navigationViewController: NavigationViewController, byCanceling canceled: Bool) {
    }
}
