//
//  FlutterMapboxNavigationPlugin.swift
//  demo_plugin
//
//  Created by NhatPV on 16/06/2023.
//
import Flutter
import UIKit
import VietMap
import VietMapDirections
import VietMapCoreNavigation
import VietMapNavigation


private typealias RouteRequestSuccess = (([Route]) -> Void)
private typealias RouteRequestFailure = ((NSError) -> Void)

public class FlutterMapNavigationView : NavigationFactory, FlutterPlatformView
{
    let frame: CGRect
    let viewId: Int64
    let debouncer = Debouncer(delay: 0.5)
    let throttler = Throttler(minimumDelay: 1.0)
    let messenger: FlutterBinaryMessenger
    let channel: FlutterMethodChannel
    let eventChannel: FlutterEventChannel
    var markerId:Int = 0
    var navigationMapView: NavigationMapView! {
        didSet {
            oldValue?.removeFromSuperview()
            if let mapView = navigationMapView {
                configureMapView(mapView)
            }
        }
    }
    var routes: [Route]? {
        didSet {
            guard let routes = routes,
                  let current = routes.first else { navigationMapView?.removeRoutes(); return }
            navigationMapView?.showRoutes(routes)
            navigationMapView?.showWaypoints(current)
        }
    }
    var currentLocation: CLLocation?
    var routeController: RouteController?
    var arguments: NSDictionary?
    var wayPoints = [Waypoint]()
    var routeResponse: Route?
    var selectedRouteIndex = 0
    var routeOptions: NavigationRouteOptions?
    var locationManager = CLLocationManager()
    var coordinates: [CLLocationCoordinate2D]?
    var isClickOnRoute:Bool=false
    var dataCustomImage: Data?
    var markerWidth: Int?
    var markerHeight: Int?
    var listMarker = [Int : Any]()
    // MARK: - Handle Flutter method
    init(messenger: FlutterBinaryMessenger, frame: CGRect, viewId: Int64, args: Any?)
    {
        self.frame = frame
        self.viewId = viewId
        self.arguments = args as! NSDictionary?
        
        self.messenger = messenger
        self.channel = FlutterMethodChannel(name: "navigation_plugin/\(viewId)", binaryMessenger: messenger)
        self.eventChannel = FlutterEventChannel(name: "navigation_plugin/\(viewId)/events", binaryMessenger: messenger)
        
        super.init()
        
        self.eventChannel.setStreamHandler(self)
//        self.navigationMapView.logoView.isHidden = false
        self.channel.setMethodCallHandler { [weak self](call, result) in
            
            guard let strongSelf = self else { return }
            
            let arguments = call.arguments as? NSDictionary
            
            if(call.method == "getPlatformVersion")
            {
                result("iOS " + UIDevice.current.systemVersion)
            }
            else if(call.method == "buildRoute")
            {
                strongSelf.buildRoute(arguments: arguments, flutterResult: result)
            }
            else if(call.method == "clearRoute")
            {
                strongSelf.clearRoute(arguments: arguments, result: result)
            }
            else if(call.method == "getDistanceRemaining")
            {
                result(strongSelf._distanceRemaining ?? 0.0)
            }
            else if(call.method == "getDurationRemaining")
            {
                result(strongSelf._durationRemaining ?? 0.0)
            }
            else if(call.method == "finishNavigation")
            { 
                //                strongSelf.endNavigation(result: result)
                strongSelf.cancelNavigation()
            }
            else if(call.method == "moveCamera"){
                guard let arguments = call.arguments as? [String: Any] else { return }
                let latitude = arguments["latitude"] as? Double
                let longitude = arguments["longitude"] as? Double
                if(latitude != nil && longitude != nil)
                {
                    strongSelf.moveCameraToCoordinates(latitude: latitude!, longitude: longitude!)
                    
                    strongSelf.navigationMapView.tracksUserCourse = false
                    
                    strongSelf.navigationMapView.showsUserLocation = false
                }
            }
            else if (call.method == "animateCamera"){
                guard let arguments = call.arguments as? [String: Any] else { return }
                let latitude = arguments["latitude"] as? Double
                let longitude = arguments["longitude"] as? Double
                let zoom = arguments["zoom"] as? Double
                let bearing = arguments["bearing"] as? Double
                let tilt = arguments["tilt"] as? Double
                let altitude = self?.altitudeFromZoomLevel(zoom)
                if(latitude != nil && longitude != nil   )
                {
                    let camera = MLNMapCamera(lookingAtCenter: CLLocationCoordinate2D(latitude: latitude!, longitude: longitude!), acrossDistance:  altitude ?? 250, pitch: tilt ?? 0, heading: bearing ?? 0)
                    
                    strongSelf.navigationMapView.setCamera(camera, withDuration: 1, animationTimingFunction: nil)
                    strongSelf.navigationMapView.tracksUserCourse = false
                     
                    
                    strongSelf.navigationMapView.showsUserLocation = false
//                    strongSelf.navigationMapView.setOverheadCameraView(from: strongSelf._wayPoints.first!.coordinate, along: strongSelf.coordinates ?? [], for: strongSelf.overheadInsets)
                }
            } 
            else if(call.method == "endNavigation")
            {
                strongSelf.endNavigationEmbedded(result: result)
            }
            else if(call.method == "startFreeDrive")
            {
                //                strongSelf.startEmbeddedFreeDrive(arguments: arguments, result: result)
            }
            else if(call.method == "startNavigation")
            {
                UIApplication.shared.isIdleTimerDisabled = true
                strongSelf.startNavigationEmbedded(result: result)
            }
            else if(call.method == "queryRenderedFeatures")
            {
                
                guard let arguments = call.arguments as? [String: Any] else { return }
                var styleLayerIdentifiers: Set<String>?
                if let layerIds = arguments["layerIds"] as? [String] {
                    if(!layerIds.isEmpty){
                        styleLayerIdentifiers = Set<String>(layerIds)
                    }
                }
                var filterExpression: NSPredicate?
                if let filter = arguments["filter"] as? [Any] {
                    filterExpression = NSPredicate(mglJSONObject: filter)
                }
                var reply = [String: NSObject]()
                var features: [MLNFeature] = []
                if let x = arguments["x"] as? Double, let y = arguments["y"] as? Double {
                    features = self!.navigationMapView.visibleFeatures(
                        at: CGPoint(x: x, y: y),
                        styleLayerIdentifiers: styleLayerIdentifiers,
                        predicate: filterExpression
                    )
                }
                if let top = arguments["top"] as? Double,
                   let bottom = arguments["bottom"] as? Double,
                   let left = arguments["left"] as? Double,
                   let right = arguments["right"] as? Double
                {
                    var width = right - left
                    var height = bottom - top
                    features = self!.navigationMapView.visibleFeatures(in: CGRect(x: left, y: top, width: width, height: height), styleLayerIdentifiers: styleLayerIdentifiers, predicate: filterExpression)
                }
                var featuresJson = [String]()
                for feature in features {
                    let dictionary = feature.geoJSONDictionary()
                    let geometry = dictionary["geometry"] as? [String:Any]
                    if((geometry?["type"] as? String) == "Point")
                    {
                        if let theJSONData = try? JSONSerialization.data(
                            withJSONObject: dictionary,
                            options: []
                        ),
                            let theJSONText = String(data: theJSONData, encoding: .utf8)
                        {
                            featuresJson.append(theJSONText)
                        }
                    }
                }
                reply["features"] = featuresJson as NSObject
                result(reply)
            }
            else if(call.method == "recenter")
            {
                //used to recenter map from user action during navigation
                strongSelf.navigationMapView.recenterMap()
                
                strongSelf.navigationMapView.setCenter(CLLocationCoordinate2D(latitude: (strongSelf.currentLocation?.coordinate.latitude)!, longitude: (strongSelf.currentLocation?.coordinate.longitude)!), zoomLevel: self!._zoom, animated: true)
                
                strongSelf.navigationMapView.tracksUserCourse = true
                
                strongSelf.navigationMapView.showsUserLocation = true
            }
            else if(call.method == "overview")
            {
                //used to recenter map from user action during navigation
                strongSelf.navigationMapView.setOverheadCameraView(from: strongSelf._wayPoints.first!.coordinate, along: strongSelf.coordinates ?? [], for: strongSelf.overheadInsets)
            }
            else if(call.method == "navigationCancelled")
            {
                UIApplication.shared.isIdleTimerDisabled = false
                strongSelf.cancelNavigation()
            }
            else if(call.method == "buildAndStartNavigation")
            {
                UIApplication.shared.isIdleTimerDisabled = true
                strongSelf.buildAndStartNavigation(arguments: arguments, flutterResult: result)
            }
            else if(call.method == "mute")
            {
                strongSelf.mute(arguments: arguments, result: result)
            }
            else if(call.method == "addMarkers")
            {
                strongSelf.addMarker(arguments: arguments, result: result)
            }
            else if(call.method == "removeMarkers"){
                strongSelf.removeMarkers(arguments: arguments, result: result)
            }
            else if(call.method == "removeAllMarkers"){
                strongSelf.removeAllMarkers(arguments: arguments, result: result)
            }
            else
            {
                result("method is not implemented");
            }
            
        }
    }
    
    public func view() -> UIView
    {
        setupMapView()
        return navigationMapView
    }
    // MARK: - Zoom level to altitude
    func altitudeFromZoomLevel(_ zoomLevel: Double?) -> Double {
        if(zoomLevel == nil ){ return 250}
        let maxZoom: Double = 21  // Maximum zoom level
        let minAltitude: Double = 100  // Minimum altitude in meters
        let maxAltitude: Double = 20000000  // Maximum altitude in meters (roughly Earth's circumference)
        
        // Reverse the zoom level (higher zoom = lower altitude)
        let reversedZoom = maxZoom - zoomLevel!
        
        // Calculate the altitude using an exponential function
        let altitude = minAltitude * pow(maxAltitude / minAltitude, reversedZoom / maxZoom)
        
        return altitude
    }
    // MARK: - configureMapView
    private func configureMapView(_ mapView: NavigationMapView) {
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        mapView.delegate = self
        mapView.navigationMapDelegate = self
        mapView.userTrackingMode = .follow
        mapView.logoView.isHidden = false
    }
    // MARK: - Setup MapView
    private func setupMapView()
    {
        navigationMapView = NavigationMapView(frame: frame,styleURL: URL(string: _url))
        navigationMapView.delegate = self
        if(self.arguments != nil)
        {
            parseFlutterArguments(arguments: arguments)
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
            let longClick = UILongPressGestureRecognizer(target: self, action: #selector(handleLongPress(_:)))
            longClick.delegate = self
            navigationMapView?.addGestureRecognizer(longClick)
        }
        
        let onClick = UITapGestureRecognizer(target: self, action: #selector(handlePress(_:)))
        onClick.delegate = self
        navigationMapView?.addGestureRecognizer(onClick)
        
        let gestureRecognizers = navigationMapView.gestureRecognizers
        for gestureRecognizer in gestureRecognizers ?? [] where !(gestureRecognizer is UILongPressGestureRecognizer) && !(gestureRecognizer is UITapGestureRecognizer) {
            gestureRecognizer.addTarget(self, action: #selector(handlePanGesture(_:)))
        }
    }
    // MARK: - moveCameraToCoordinates
    func moveCameraToCoordinates(latitude: Double, longitude: Double) {
        navigationMapView.setCenter(CLLocationCoordinate2D(latitude: latitude, longitude: longitude), zoomLevel: self._zoom, animated: true)
    }
    
    fileprivate lazy var defaultSuccess: RouteRequestSuccess = { [weak self] (routes) in
        guard let strongSelf = self else { return }
        guard let current = routes.first else { return }
        strongSelf.routeResponse = current
        strongSelf.sendEvent(eventType: MapEventType.routeBuilt, data: encodeRoute(route: current))
        strongSelf.routes = routes
        strongSelf._routes = routes
        strongSelf._wayPoints = current.routeOptions.waypoints
        strongSelf.coordinates = current.coordinates
    }
    
    fileprivate lazy var defaultFailure: RouteRequestFailure = { [weak self] (error) in
        guard let strongSelf = self else { return }
        strongSelf._routes = nil //clear routes from the map
    }
    // MARK: - buildRoute
    func buildRoute(arguments: NSDictionary?, flutterResult: @escaping FlutterResult, startNavigation: Bool = false)
    {
        _wayPoints.removeAll()
        sendEvent(eventType: MapEventType.routeBuilding)
        
        guard let oWayPoints = arguments?["wayPoints"] as? NSDictionary else {return}
        guard let profile = arguments?["profile"] as? String else {return}
        
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
        
        parseFlutterArguments(arguments: arguments)
        
        var mode: MBDirectionsProfileIdentifier = .automobileAvoidingTraffic
        
        _navigationMode = profile
        
        if (_navigationMode == "cycling")
        {
            mode = .cycling
        }
        else if(_navigationMode == "driving-traffic")
        {
            mode = .automobileAvoidingTraffic
        }
        else if(_navigationMode == "walking")
        {
            mode = .walking
        }
        else if(_navigationMode == "motorcycle")
        {
            mode = .init("mapbox/motorcycle")
        }
        
        let routeOptions = NavigationRouteOptions(waypoints: _wayPoints, profileIdentifier: mode)
        routeOptions.shapeFormat = .polyline6
        
        if (_allowsUTurnAtWayPoints != nil)
        {
            routeOptions.allowsUTurnAtWaypoint = _allowsUTurnAtWayPoints!
        }
        
        routeOptions.distanceMeasurementSystem = _voiceUnits == "imperial" ? .imperial : .metric
        routeOptions.locale = Locale(identifier: _language)
        self.routeOptions = routeOptions
        
        // Generate the route object and draw it on the map
        _ = Directions.shared.calculate(routeOptions) { [weak self] (session, result, error) in
            guard let strongSelf = self else { return }
            if let response = result?.first {
                // Handle success case
                strongSelf.routes = result
                strongSelf._routes = result
                strongSelf._wayPoints = response.routeOptions.waypoints
                strongSelf.routeResponse = response
                strongSelf.coordinates = response.coordinates
                strongSelf.navigationMapView.setOverheadCameraView(from: strongSelf._wayPoints.first!.coordinate, along: response.coordinates!, for: strongSelf.overheadInsets)
                strongSelf.sendEvent(eventType: MapEventType.routeBuilt, data: encodeRoute(route: response))
                flutterResult(true)
                if startNavigation {
                    strongSelf.startNavigationEmbedded(result: flutterResult)
                }
            } else {
                // Handle failure case
                flutterResult(false)
                strongSelf.sendEvent(eventType: MapEventType.routeBuildFailed)
            }
        }
    }
    // MARK: - buildAndStartNavigation
    func buildAndStartNavigation(arguments: NSDictionary?, flutterResult: @escaping FlutterResult) {
        buildRoute(arguments: arguments, flutterResult: flutterResult, startNavigation: true)
    }
    
    @objc var overheadInsets: UIEdgeInsets {
        return UIEdgeInsets(top: 20, left: 20, bottom: 70, right: 20)
    }
    
    func clearRoute(arguments: NSDictionary?, result: @escaping FlutterResult) {
        if routeResponse == nil
        {
            return
        }
        navigationMapView.removeRoutes()
        navigationMapView.removeWaypoints()
        _wayPoints.removeAll()
        sendEvent(eventType: MapEventType.navigationCancelled)
        UIApplication.shared.isIdleTimerDisabled = false
    }
    // MARK: - endNavigationEmbedded
    func endNavigationEmbedded(result: FlutterResult?) {
        if (routeController != nil) {
            routeController?.endNavigation()
            navigationMapView.recenterMap()
            suspendNotifications()
            sendEvent(eventType: MapEventType.navigationCancelled)
            
            UIApplication.shared.isIdleTimerDisabled = false
            if(result != nil)
            {
                result!(true)
            }
        }
    }
    // MARK: - startNavigationEmbedded
    func startNavigationEmbedded(result: @escaping FlutterResult) {
        isEmbeddedNavigation = true
        guard let route = self.routes?.first else {
            result(false)
            return
        }
        routeController = RouteController(along: route, locationManager: self.getNavigationLocationManager(simulated: _simulateRoute))
        routeController?.delegate = self
        routeController?.reroutesProactively = true
        routeController?.resume()
        navigationMapView.recenterMap() 
        navigationMapView.showsUserLocation = true
        resumeNotifications()
        result(true)
    }
    // MARK: - startNavigationNewRoute
    func startNavigationNewRoute(route: Route) {
        isEmbeddedNavigation = true
        routeController?.endNavigation()
        routeController = RouteController(along: route, locationManager: self.getNavigationLocationManager(simulated: _simulateRoute))
        routeController?.delegate = self
        routeController?.reroutesProactively = true
        routeController?.resume()
        navigationMapView.recenterMap()
        navigationMapView.showsUserLocation = true
    }
    
    func resumeNotifications() {
        NotificationCenter.default.addObserver(self, selector: #selector(progressDidChange(_ :)), name: .routeControllerProgressDidChange, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(rerouted(_:)), name: .routeControllerDidReroute, object: nil)
    }
    
    // MARK: - add a marker
    func removeAllMarkers(arguments: NSDictionary?, result: @escaping FlutterResult){
        for(_,element) in listMarker{
            navigationMapView.removeAnnotation(element as! MLNAnnotation)
        }
        listMarker.removeAll()
        result(true)
    }
    
    func removeMarkers(arguments: NSDictionary?, result: @escaping FlutterResult) {
        
        let data:[Int]? = arguments?["markerIds"] as? [Int]
        for( _,element) in data!.enumerated(){
            if(listMarker[(element )] != nil){
                navigationMapView.removeAnnotation(listMarker[(element )] as! MLNAnnotation )
                listMarker.removeValue(forKey:  (element ) )
            }
        }
        result(true)
    }
    
    // Function to resize the UIImage
    func resizeImage(image: UIImage, targetSize: CGSize) -> UIImage {
        let size = image.size

        let widthRatio  = targetSize.width  / size.width
        let heightRatio = targetSize.height / size.height

        // Determine the scale factor that preserves aspect ratio
        let scaleFactor = min(widthRatio, heightRatio)

        // Calculate the new size that preserves aspect ratio
        let scaledImageSize = CGSize(width: size.width * scaleFactor, height: size.height * scaleFactor)

        // Create a new context for the resized image
        UIGraphicsBeginImageContextWithOptions(scaledImageSize, false, 0.0)
        image.draw(in: CGRect(origin: .zero, size: scaledImageSize))
        let resizedImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return resizedImage ?? image
    }
    
    func addMarker(arguments: NSDictionary?, result: @escaping FlutterResult) {
        let data = arguments?.allValues
        var listMarkerId = [Int]()
        for (_, element) in data!.enumerated() {
            let el: NSDictionary? = (element as? NSDictionary?) ?? nil
            let myFlutterData = (el?["imageBase64"]) as? String
            let myData = Data(base64Encoded:myFlutterData!)!
            dataCustomImage = myData
            markerWidth = el?["width"] as? Int
            markerHeight = el?["height"] as? Int
            let markerImage = UIImage(data: myData)
            var resizedImage : UIImage? = nil
            if(el?["width"] != nil && el?["height"] != nil){
                resizedImage = resizeImage(image: markerImage!, targetSize: CGSize(width: el!["width"] as! Int, height: el!["height"] as! Int))
            }
            else{
                resizedImage = markerImage
            }
 
            
            _ = MLNAnnotationImage(image: resizedImage!, reuseIdentifier: "custom-marker")
            // Create a custom MLNPointAnnotation
            let customMarker = MLNPointAnnotation()
            customMarker.coordinate = CLLocationCoordinate2D(latitude: el!["latitude"]! as! CLLocationDegrees, longitude: el!["longitude"] as! CLLocationDegrees)
            
            
            customMarker.title = el!["title"] as? String
            customMarker.subtitle = el!["snippet"] as? String
            
            listMarker.updateValue(customMarker, forKey: markerId)
            // Add the annotation to the map
            navigationMapView.addAnnotation(customMarker)
            listMarkerId.append(markerId)
            markerId+=1
        }
        result(listMarkerId)
    }
//    func snapLocationToRoute(){
//        let options = SnapToRouteOptions(route: routeController!.routeProgress.route, location: currentLocation!, distance: 50)
//        let snappedCoordinate = navigationMapView.mapView.mapboxMap.coordinate(for: currentLocation!.coordinate)
//        let snappedLocation = CLLocation(latitude: snappedCoordinate.latitude, longitude: snappedCoordinate.longitude)
//        routeController?.locationManager.updateLocation(snappedLocation)
//    }
    func suspendNotifications() {
        NotificationCenter.default.removeObserver(self, name: .routeControllerProgressDidChange, object: nil)
        NotificationCenter.default.removeObserver(self, name: .routeControllerWillReroute, object: nil)
    }
    // MARK: - onProgressChange
    @objc func progressDidChange(_ notification: NSNotification) { 
        let routeProgress = notification.userInfo![RouteControllerNotificationUserInfoKey.routeProgressKey] as! RouteProgress
        let location = notification.userInfo![RouteControllerNotificationUserInfoKey.locationKey] as! CLLocation
        currentLocation = location
        let rawLocation = notification.userInfo![RouteControllerNotificationUserInfoKey.rawLocationKey] as! CLLocation
        // Update the user puck
        let camera = MLNMapCamera(lookingAtCenter: location.coordinate, altitude: 250, pitch: 60, heading: location.course) 
        
        // Add maneuver arrow
        if routeProgress.currentLegProgress.followOnStep != nil {
            navigationMapView.addArrow(route: routeProgress.route, legIndex: routeProgress.legIndex, stepIndex: routeProgress.currentLegProgress.stepIndex + 1)
        } else {
            navigationMapView.removeArrow()
        }
        navigationMapView.updateCourseTracking(location: location, camera: camera, animated: false)
        _distanceRemaining = routeProgress.distanceRemaining
        _durationRemaining = routeProgress.durationRemaining
        sendEvent(eventType: MapEventType.navigationRunning)
        let routeProgressData = encodeRouteProgress(routeProgress: routeProgress,location:location,rawLocation:rawLocation)
        sendEvent(eventType: MapEventType.progressChange, data: routeProgressData)
    }
    
    // MARK: - reroute
    @objc func rerouted(_ notification: NSNotification) {
        self.navigationMapView.showRoutes([(routeController?.routeProgress.route)!])
        self.navigationMapView.tracksUserCourse = true
        self.navigationMapView.recenterMap()
        if let userInfo = notification.object as? RouteController {
            sendEvent(eventType: MapEventType.userOffRoute, data: encodeLocation(location: (userInfo.locationManager.location?.coordinate)!))
        }
    }
    
    // MARK: - cancelNavigation
    func cancelNavigation() {
        isEmbeddedNavigation = false
        routeController?.endNavigation()
        navigationMapView.recenterMap()
        suspendNotifications()
        UIApplication.shared.isIdleTimerDisabled = false
        sendEvent(eventType: MapEventType.navigationCancelled)
    }
    
    // MARK: - mute
    func mute(arguments: NSDictionary?, result: @escaping FlutterResult) {
        guard let muted = arguments?["isMute"] as? Bool else {return}
        NavigationSettings.shared.voiceMuted = muted
    }
}

// MARK: - NavigationMapViewDelegate
extension FlutterMapNavigationView : NavigationMapViewDelegate {
    public func navigationMapView(_ mapView: NavigationMapView, didSelect route: Route) {
        isClickOnRoute = true
        guard let routes = routes else { return }
        guard let index = routes.firstIndex(where: { $0 == route }) else { return }
        self.routes!.remove(at: index)
        self.routes!.insert(route, at: 0)
        self._routes!.remove(at: index)
        self._routes!.insert(route, at: 0)
        if isEmbeddedNavigation {
            self.startNavigationNewRoute(route: route)
        }
        
        sendEvent(eventType: MapEventType.onNewRouteSelected, data: encodeRoute(route: route))
    }
}

extension FlutterMapNavigationView : MLNMapViewDelegate {
    public func mapViewDidFinishLoadingMap(_ mapView: MLNMapView) {
        sendEvent(eventType: MapEventType.mapReady)
    }
    
    public func mapViewDidFinishRenderingMap(_ mapView: MLNMapView, fullyRendered: Bool) {
        sendEvent(eventType: MapEventType.onMapRendered)
    }
    
    public func mapView(_ mapView: MLNMapView, imageFor annotation: MLNAnnotation) -> MLNAnnotationImage? {
        guard let dataCustomImage = dataCustomImage else { return annotation as? MLNAnnotationImage}
        let img = UIImage(data: dataCustomImage)
        var image : UIImage? = nil
        if(markerWidth != nil && markerHeight != nil){
            image = resizeImage(image: img!, targetSize: CGSize(width: markerWidth!, height: markerHeight!))
        }
        else{
            image = img
        }
        
        guard let image = image else { return annotation as? MLNAnnotationImage}
        if #available(iOS 13.0, *) {
            image.withTintColor(UIColor.red)
        } else {
            // Fallback on earlier versions
        }
        let annotationImage:MLNAnnotationImage?
        if #available(iOS 13.0, *) {
            annotationImage = MLNAnnotationImage(image: image , reuseIdentifier: "customAnnotation\(markerId)")
        } else {
            // Fallback on earlier versions
            annotationImage = MLNAnnotationImage(image: image, reuseIdentifier: "customAnnotation\(markerId)")
        }
        return annotationImage
    }
    
    public func mapView(_ mapView: MLNMapView, annotationCanShowCallout annotation: MLNAnnotation) -> Bool {
        for(markerId,element) in listMarker{
            if((element as! MLNAnnotation).hash == annotation.hash){
                let mk:String = "{'markerId':\(markerId)}"
                sendEvent(eventType: MapEventType.markerClicked,data:mk)
                return false
            }
        }
        return true
        
    }
    public  func mapView(_ mapView: MLNMapView, leftCalloutAccessoryViewFor annotation: MLNAnnotation) -> UIView? {
        
        let imageView = UIImageView(image: UIImage(named: "leftAccessoryImage"))
        
        return imageView
        
    }
    
    
    
    public  func mapView(_ mapView: MLNMapView, rightCalloutAccessoryViewFor annotation: MLNAnnotation) -> UIView? {
        
        let button = UIButton(type: .detailDisclosure)
        
        return button
        
    }
    
    
    
    public func mapView(_ mapView: MLNMapView, fillColorForPolygonAnnotation annotation: MLNPolygon) -> UIColor {
        
        return UIColor.red.withAlphaComponent(0.5)
        
    }
}
// MARK: - UIGestureRecognizerDelegate
extension FlutterMapNavigationView : UIGestureRecognizerDelegate {
    
    public func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        return true
    }
    
    @objc func handleLongPress(_ gesture: UILongPressGestureRecognizer) {
        guard gesture.state == .ended else { return }
        let location = navigationMapView.convert(gesture.location(in: navigationMapView), toCoordinateFrom: navigationMapView)
        
        let screenPosition = gesture.location(in: navigationMapView)
        sendEvent(eventType: MapEventType.onMapClick, data: encodeClickPosition(location: location,position: screenPosition))
    }
    
    @objc func handlePress(_ gesture: UITapGestureRecognizer) {
        guard gesture.state == .ended else { return }
        //        if(isClickOnRoute){
        //            isClickOnRoute = false
        //            return
        //        }
        let location = navigationMapView.convert(gesture.location(in: navigationMapView), toCoordinateFrom: navigationMapView)
        let screenPosition = gesture.location(in: navigationMapView)
        sendEvent(eventType: MapEventType.onMapClick, data: encodeClickPosition(location: location,position: screenPosition))
    }
    @objc public func mapViewRegionIsChanging(_ mapView: MLNMapView) {
        navigationMapView.updateCourseTracking(location: currentLocation, animated: false) 
//        throttler.throttle {
//            print("Map is changinggggg")
//            self.sendEvent(eventType: MapEventType.onMapMove)
//        }
//        debouncer.debounce {
//            print("Map is changed")
//            self.sendEvent(eventType: MapEventType.onMapMoveEnd)
//        }
        
    }
    
    @objc func handlePanGesture(_ gestureRecognizer: UIPanGestureRecognizer) {
        if gestureRecognizer.state == .began {
        
        } else if gestureRecognizer.state == .changed {
            navigationMapView.updateCourseTracking(location: currentLocation, animated: true)
            sendEvent(eventType: MapEventType.onMapMove)
        } else if gestureRecognizer.state == .ended {
            sendEvent(eventType: MapEventType.onMapMoveEnd)
        }
    }
    
    // MARK: - requestRoute
    func requestRoute(destination: CLLocationCoordinate2D) {
        sendEvent(eventType: MapEventType.routeBuilding)
        
        guard let userLocation = navigationMapView.userLocation?.location else { return }
        let location = CLLocation(latitude: userLocation.coordinate.latitude,
                                  longitude: userLocation.coordinate.longitude)
        let userWaypoint = Waypoint(location: location, name: "Vị trí của bạn")
        let destinationWaypoint = Waypoint(coordinate: destination, name: "Điểm đến của bạn")
        
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
        Directions.shared.calculate(options, completionHandler: handler)
    }
}

// MARK: - RouteControllerDelegate
extension FlutterMapNavigationView: RouteControllerDelegate {
    public func routeController(_ routeController: RouteController, didArriveAt waypoint: Waypoint) -> Bool {
        UIApplication.shared.isIdleTimerDisabled = false
        sendEvent(eventType: MapEventType.onArrival)
        return true
    }
    
//    public override func navigationViewControllerDidDismiss(_ navigationViewController: NavigationViewController, byCanceling canceled: Bool) {
//        if(canceled)
//        {
//            UIApplication.shared.isIdleTimerDisabled = false
//            sendEvent(eventType: MapEventType.navigationCancelled)
//            sendEvent(eventType: MapEventType.navigationFinished)
//        }
//    }
}

extension UIColor {
    convenience init(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")

        var rgb: UInt64 = 0
        Scanner(string: hexSanitized).scanHexInt64(&rgb)

        let r = CGFloat((rgb & 0xFF0000) >> 16) / 255.0
        let g = CGFloat((rgb & 0x00FF00) >> 8) / 255.0
        let b = CGFloat(rgb & 0x0000FF) / 255.0

        self.init(red: r, green: g, blue: b, alpha: 1.0)
    }
}
