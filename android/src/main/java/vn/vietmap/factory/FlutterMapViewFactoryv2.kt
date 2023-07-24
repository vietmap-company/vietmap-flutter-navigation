//package vn.vietmap.factory
//
//import android.Manifest
//import android.app.Activity
//import android.app.Application
//import android.content.Context
//import android.content.pm.PackageManager
//import android.location.Location
//import android.os.Bundle
//import android.view.View
//import androidx.annotation.NonNull
//import androidx.core.app.ActivityCompat
//import com.mapbox.android.gestures.MoveGestureDetector
//import com.mapbox.api.directions.v5.DirectionsCriteria
//import com.mapbox.api.directions.v5.models.BannerInstructions
//import com.mapbox.api.directions.v5.models.DirectionsResponse
//import com.mapbox.api.directions.v5.models.DirectionsRoute
//import com.mapbox.geojson.Point
//import com.mapbox.mapboxsdk.Mapbox
//import com.mapbox.mapboxsdk.camera.CameraPosition
//import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
//import com.mapbox.mapboxsdk.geometry.LatLng
//import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
//import com.mapbox.mapboxsdk.location.LocationComponentOptions
//import com.mapbox.mapboxsdk.location.engine.LocationEngine
//import com.mapbox.mapboxsdk.location.modes.CameraMode
//import com.mapbox.mapboxsdk.location.modes.RenderMode
//import com.mapbox.mapboxsdk.location.permissions.PermissionsManager
//import com.mapbox.mapboxsdk.maps.*
//import com.mapbox.services.android.navigation.ui.v5.NavigationPresenter
//import com.mapbox.services.android.navigation.ui.v5.NavigationView
//import com.mapbox.services.android.navigation.ui.v5.NavigationViewModel
//import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
//import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback
//import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener
//import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
//import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener
//import com.mapbox.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener
//import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
//import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement
//import com.mapbox.services.android.navigation.v5.location.engine.LocationEngineProvider
//import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
//import com.mapbox.services.android.navigation.v5.milestone.Milestone
//import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
//import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
//import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener
//import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
//import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
//import com.mapbox.services.android.navigation.v5.route.FasterRouteListener
//import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
//import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
//import io.flutter.plugin.common.BinaryMessenger
//import io.flutter.plugin.common.EventChannel
//import io.flutter.plugin.common.MethodCall
//import io.flutter.plugin.common.MethodChannel
//import io.flutter.plugin.platform.PlatformView
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import timber.log.Timber
//import vn.vietmap.models.VietMapEvents
//import vn.vietmap.models.VietMapRouteProgressEvent
//import vn.vietmap.navigation_plugin.VietMapNavigationPlugin
//import vn.vietmap.utilities.PluginUtilities
//import java.util.*
//
//class FlutterMapViewFactoryv2:
//PlatformView,
//    MethodChannel.MethodCallHandler,
//Application.ActivityLifecycleCallbacks,
//    OnMapReadyCallback,
//    ProgressChangeListener,
//    OffRouteListener,
//    MilestoneEventListener,
//    NavigationEventListener,
//    NavigationListener,
//    FasterRouteListener,
//    SpeechAnnouncementListener,
//    BannerInstructionsListener,
//    RouteListener, EventChannel.StreamHandler, MapboxMap.OnMapLongClickListener,
//MapboxMap.OnMoveListener {
//
//    private val activity: Activity
//    private val context: Context
//
//    private val methodChannel: MethodChannel
//    private val eventChannel: EventChannel
//
//    private val options: MapboxMapOptions
//
//    private var mapView: MapView
//    private var navigationMapView: MapView
//    private var mapBoxMap: MapboxMap? = null
//    private var currentRoute: DirectionsRoute? = null
//    private var navigationView: NavigationView? = null
//    private var locationEngine: LocationEngine? = null
//    private var navigationMapRoute: NavigationMapRoute? = null
//    private val navigationOptions = MapboxNavigationOptions.builder()
//        .build()
//    //    private var navigation: MapboxNavigation
//    private var mapReady = false
//    private var isDisposed = false
//    private var isRefreshing = false
//    private var isBuildingRoute = false
//    private var isNavigationInProgress = false
//    private var isNavigationCanceled = false
//    private var navigationViewModel: NavigationViewModel? = null
//
//    constructor(cxt: Context, messenger: BinaryMessenger, viewId: Int, act: Activity, args: Any?) {
//
//        Mapbox.getInstance(act.applicationContext)
//        activity = act
//        activity.application.registerActivityLifecycleCallbacks(this)
//        context = cxt
////        val arguments = args as? Map<*, *>
////        if (arguments != null)
////            setOptions(arguments)
//
//        methodChannel = MethodChannel(messenger, "navigation_plugin/${viewId}")
//        eventChannel = EventChannel(messenger, "navigation_plugin/${viewId}/events")
//        eventChannel.setStreamHandler(this)
//
//        options = MapboxMapOptions.createFromAttributes(context)
//            .compassEnabled(false)
//            .logoEnabled(true)
//        mapView = MapView(context, options)
//        navigationMapView = MapView(context, options)
//        navigationView = NavigationView(context)
//        locationEngine = LocationEngineProvider.getBestLocationEngine(context)
//
//        navigationViewModel = NavigationViewModel(activity.application)
//        navigationViewModel?.initializeLocationEngine()
//        navigationViewModel?.initializeNavigation(context, navigationOptions, locationEngine)
//        navigationViewModel?.initializeRouter()
//        navigationView?.onCreate(null, navigationViewModel)
//        methodChannel.setMethodCallHandler(this)
//        mapView.getMapAsync(this)
//        navigationMapView.getMapAsync(this)
//
//    }
//
//
//    companion object {
//
//        //Config
//        var initialLatitude: Double? = null
//        var initialLongitude: Double? = null
//
//        val wayPoints: MutableList<Point> = mutableListOf()
//        var navigationMode = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
//        var simulateRoute = false
//        var mapStyleURL: String? = null
//        var navigationLanguage = Locale("en")
//        var navigationVoiceUnits = DirectionsCriteria.IMPERIAL
//        var zoom = 18.0
//        var bearing = 10000.0
//        var tilt = 10000.0
//        var distanceRemaining: Double? = null
//        var durationRemaining: Double? = null
//
//        var alternatives = true
//
//        var allowsUTurnAtWayPoints = false
//        var enableRefresh = false
//        var voiceInstructionsEnabled = true
//        var bannerInstructionsEnabled = true
//        var longPressDestinationEnabled = true
//        var animateBuildRoute = true
//        var isOptimized = false
//
//        var originPoint: Point? = null
//        var destinationPoint: Point? = null
//    }
//
//    private fun buildRoute(methodCall: MethodCall, result: MethodChannel.Result) {
//        isNavigationCanceled = false
//        isNavigationInProgress = false
//
//        val arguments = methodCall.arguments as? Map<*, *>
//        if(arguments != null)
//            setOptions(arguments)
//
//        if (mapReady) {
//            FlutterMapViewFactoryv2.wayPoints.clear()
//            var points = arguments?.get("wayPoints") as HashMap<*, *>
//            for (item in points)
//            {
//                val point = item.value as HashMap<*, *>
//                val latitude = point["Latitude"] as Double
//                val longitude = point["Longitude"] as Double
//                FlutterMapViewFactoryv2.wayPoints.add(Point.fromLngLat(longitude, latitude))
//            }
//            getRoute(context)
//            result.success(true)
//        } else {
//            result.success(false)
//        }
//    }
//
//    private fun getRoute(context: Context) {
//
//        if (!PluginUtilities.isNetworkAvailable(context)) {
//            PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILD_FAILED, "No Internet Connection")
//            return
//        }
//
//        PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILDING)
//
//        FlutterMapViewFactoryv2.originPoint = Point.fromLngLat(FlutterMapViewFactoryv2.wayPoints[0].longitude(), FlutterMapViewFactoryv2.wayPoints[0].latitude())
//        FlutterMapViewFactoryv2.destinationPoint = Point.fromLngLat(FlutterMapViewFactoryv2.wayPoints[1].longitude(), FlutterMapViewFactoryv2.wayPoints[1].latitude())
//        val builder = NavigationRoute.builder(activity)
//            .apikey("YOUR_API_KEY_HERE")
//            .origin(FlutterMapViewFactoryv2.originPoint!!)
//            .destination(FlutterMapViewFactoryv2.destinationPoint!!)
//            .alternatives(true)
//            .build()
//        builder.getRoute(object : Callback<DirectionsResponse> {
//            override fun onResponse(call: Call<DirectionsResponse?>, response: Response<DirectionsResponse?>) {
//                if (response.body() == null || response.body()!!.routes().size < 1) {
//                    PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILD_FAILED, "No routes found")
//                    return
//                }
//                currentRoute = response.body()!!.routes()[0]
//                PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILT,"${currentRoute?.toJson()}")
//                moveCameraToOriginOfRoute()
//                navigationMapRoute?.addRoute(currentRoute)
//                isBuildingRoute = false
//
//                if (isNavigationInProgress) {
//                    startNavigation()
//                }
//            }
//
//            override fun onFailure(call: Call<DirectionsResponse?>, throwable: Throwable) {
//                isBuildingRoute = false
//                PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILD_FAILED, "${throwable.message}")
//            }
//        })
//    }
//    private fun clearRoute(methodCall: MethodCall, result: MethodChannel.Result) {
//        if (navigationMapRoute != null)
//            navigationMapRoute?.updateRouteArrowVisibilityTo(false)
//
//        PluginUtilities.sendEvent(VietMapEvents.NAVIGATION_CANCELLED)
//    }
//
//    private fun startNavigation(methodCall: MethodCall, result: MethodChannel.Result) {
//
//        val arguments = methodCall.arguments as? Map<*, *>
//        if(arguments != null)
//            setOptions(arguments)
//
//        startNavigation()
//
//        if (currentRoute != null) {
//            result.success(true)
//        } else {
//            result.success(false)
//        }
//    }
//
//    private fun startNavigation() {
//        navigationView?.initialize(
//            OnNavigationReadyCallback { return@OnNavigationReadyCallback },
//            getInitialCameraPosition()
//        )
//        navigationView?.onMapReady(mapBoxMap!!)
//        navigationView?.initializeNavigationMap(navigationMapView, mapBoxMap)
//        isNavigationCanceled = false
//        if (currentRoute != null) {
//            if (FlutterMapViewFactoryv2.simulateRoute) {
//                (locationEngine as ReplayRouteLocationEngine).assign(currentRoute)
////                navigation.locationEngine = locationEngine as ReplayRouteLocationEngine
//            }
//            val options =
//                NavigationViewOptions.builder()
//                    .progressChangeListener(this)
//                    .milestoneEventListener(this)
//                    .navigationListener(this)
//                    .speechAnnouncementListener(this)
//                    .bannerInstructionsListener(this)
//                    .routeListener(this)
//                    .locationEngine(locationEngine)
//                    .directionsRoute(currentRoute)
//                    .shouldSimulateRoute(FlutterMapViewFactoryv2.simulateRoute)
//                    .onMoveListener(object : MapboxMap.OnMoveListener {
//                        override fun onMoveBegin(moveGestureDetector: MoveGestureDetector) {}
//                        override fun onMove(moveGestureDetector: MoveGestureDetector) {}
//                        override fun onMoveEnd(moveGestureDetector: MoveGestureDetector) {}
//                    })
//                    .navigationOptions(navigationOptions)
//                    .build()
//
//            currentRoute?.let {
//
//                isNavigationInProgress = true
//                navigationView?.initViewConfig(false)
//                navigationMapRoute?.updateRouteArrowVisibilityTo(false)
//                navigationMapRoute?.updateRouteVisibilityTo(false)
//                navigationView?.retrieveNavigationMapboxMap()?.removeRoute()
//                navigationView?.startNavigation(options)
//                navigationView?.startCamera(currentRoute)
//                navigationView?.updateCameraRouteOverview()
////                navigation.startNavigation(currentRoute!!)
//                PluginUtilities.sendEvent(VietMapEvents.NAVIGATION_RUNNING)
//            }
//        }
//    }
//
//
//    private fun getInitialCameraPosition(): CameraPosition {
//        if(currentRoute == null)
//            return CameraPosition.DEFAULT;
//
//        val originCoordinate = currentRoute?.routeOptions()?.coordinates()?.get(0)
//        return CameraPosition.Builder()
//            .target(LatLng(originCoordinate!!.latitude(), originCoordinate.longitude()))
////            .zoom(VietMapNavigationPlugin.zoom)
////            .bearing(VietMapNavigationPlugin.bearing)
////            .tilt(VietMapNavigationPlugin.tilt)
//            .build()
//    }
//    private fun finishNavigation(isOffRouted: Boolean = false) {
//
//        FlutterMapViewFactoryv2.zoom = 15.0
//        FlutterMapViewFactoryv2.bearing = 0.0
//        FlutterMapViewFactoryv2.tilt = 0.0
//        isNavigationCanceled = true
//
//        if (!isOffRouted) {
//            isNavigationInProgress = false
//            moveCameraToOriginOfRoute()
//        }
//
//        if (currentRoute != null) {
//        }
//
//    }
//
//
//    private fun moveCamera(location: LatLng) {
//        val cameraPosition = CameraPosition.Builder()
//            .target(location)
//            .zoom(FlutterMapViewFactoryv2.zoom)
//            .bearing(FlutterMapViewFactoryv2.bearing)
//            .tilt(FlutterMapViewFactoryv2.tilt)
//            .build()
//        var duration = 3000
//        if(FlutterMapViewFactoryv2.animateBuildRoute)
//            duration = 1
//        mapBoxMap?.animateCamera(
//            CameraUpdateFactory
//            .newCameraPosition(cameraPosition), duration)
//    }
//
//    private fun moveCameraToOriginOfRoute() {
//        currentRoute?.let {
//            val originCoordinate = it.routeOptions()?.coordinates()?.get(0)
//            originCoordinate?.let {
//                val location = LatLng(originCoordinate.latitude(), originCoordinate.longitude())
//                moveCamera(location)
//                //addCustomMarker(location)
//            }
//        }
//    }
//
//    private fun setOptions(arguments: Map<*, *>)
//    {
//        val navMode = arguments["mode"] as? String
//        if(navMode != null)
//        {
//            if(navMode == "walking")
//                FlutterMapViewFactoryv2.navigationMode = DirectionsCriteria.PROFILE_WALKING;
//            else if(navMode == "cycling")
//                FlutterMapViewFactoryv2.navigationMode = DirectionsCriteria.PROFILE_CYCLING;
//            else if(navMode == "driving")
//                FlutterMapViewFactoryv2.navigationMode = DirectionsCriteria.PROFILE_DRIVING;
//        }
//
//        val simulated = arguments["simulateRoute"] as? Boolean
//        if (simulated != null) {
//            println(simulated)
//            println("----------------------------------------")
//            FlutterMapViewFactoryv2.simulateRoute = simulated
//        }
//
//        val language = arguments["language"] as? String
//        if(language != null)
//            FlutterMapViewFactoryv2.navigationLanguage = Locale(language)
//
//        val units = arguments["units"] as? String
//
//        if(units != null)
//        {
//            if(units == "imperial")
//                FlutterMapViewFactoryv2.navigationVoiceUnits = DirectionsCriteria.IMPERIAL
//            else if(units == "metric")
//                FlutterMapViewFactoryv2.navigationVoiceUnits = DirectionsCriteria.METRIC
//        }
//
//        FlutterMapViewFactoryv2.mapStyleURL = arguments["mapStyleURL"] as? String
//
//        FlutterMapViewFactoryv2.initialLatitude = arguments["initialLatitude"] as? Double
//        FlutterMapViewFactoryv2.initialLongitude = arguments["initialLongitude"] as? Double
//
//        val zm = arguments["zoom"] as? Double
//        if(zm != null)
//            FlutterMapViewFactoryv2.zoom = zm
//
//        val br = arguments["bearing"] as? Double
//        if(br != null)
//            FlutterMapViewFactoryv2.bearing = br
//
//        val tt = arguments["tilt"] as? Double
//        if(tt != null)
//            FlutterMapViewFactoryv2.tilt = tt
//
//        val optim = arguments["isOptimized"] as? Boolean
//        if(optim != null)
//            FlutterMapViewFactoryv2.isOptimized = optim
//
//        val anim = arguments["animateBuildRoute"] as? Boolean
//        if(anim != null)
//            FlutterMapViewFactoryv2.animateBuildRoute = anim
//
//        val altRoute = arguments["alternatives"] as? Boolean
//        if(altRoute != null)
//            FlutterMapViewFactoryv2.alternatives = altRoute
//
//        val voiceEnabled = arguments["voiceInstructionsEnabled"] as? Boolean
//        if(voiceEnabled != null)
//            FlutterMapViewFactoryv2.voiceInstructionsEnabled = voiceEnabled
//
//        val bannerEnabled = arguments["bannerInstructionsEnabled"] as? Boolean
//        if(bannerEnabled != null)
//            FlutterMapViewFactoryv2.bannerInstructionsEnabled = bannerEnabled
//
//        var longPress = arguments["longPressDestinationEnabled"] as? Boolean
//        if(longPress != null)
//            FlutterMapViewFactoryv2.longPressDestinationEnabled = longPress
//    }
//
//    private fun finishNavigation(methodCall: MethodCall, result: MethodChannel.Result) {
//
//        finishNavigation()
//
//        if (currentRoute != null) {
//            result.success(true)
//        } else {
//            result.success(false)
//        }
//    }
//
//    override fun getView(): View {
//        return if (isNavigationInProgress) {
//            println("getting navigation view")
//            navigationView!!
//        } else {
//            println("getting map view")
//            mapView
//        }
//    }
//
//    override fun dispose() {
//        TODO("Not yet implemented")
//    }
//
//    override fun onMethodCall(methodCall: MethodCall, result: MethodChannel.Result) {
//        when (methodCall.method) {
//
//            "buildRoute" -> {
//                buildRoute(methodCall, result)
//            }
//            "clearRoute" -> {
//                clearRoute(methodCall, result)
//            }
//            "startNavigation" -> {
//                startNavigation(methodCall, result)
//            }
//            "finishNavigation" -> {
//                finishNavigation(methodCall, result)
//            }
//            "getDistanceRemaining" -> {
//                result.success(FlutterMapViewFactoryv2.distanceRemaining)
//            }
//            "getDurationRemaining" -> {
//                result.success(FlutterMapViewFactoryv2.durationRemaining)
//            }
//            "recenter" -> {
//
//                val navigationPresenter: NavigationPresenter? =
//                    navigationView?.getNavigationPresenter()
//                navigationPresenter?.onRecenterClick()
//
//            }
//            "overview" -> {
//                val navigationPresenter: NavigationPresenter? =
//                    navigationView?.getNavigationPresenter()
//                navigationPresenter?.onRouteOverviewClick()
//            }
//            else -> result.notImplemented()
//        }
//    }
//
//    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
//        mapView.onCreate(savedInstanceState)
//        navigationView?.onCreate(savedInstanceState,null)}
//
//    override fun onActivityStarted(activity: Activity) {
//
//        try {
//            navigationView?.onStart()
//            mapView.onStart()
//        } catch (e: java.lang.Exception) {
//            Timber.i(String.format("onActivityStarted, %s", "Error: ${e.message}"))
//        }
//    }
//
//    override fun onActivityResumed(activity: Activity) {    navigationView?.onResume()
//        mapView.onResume()
//    }
//
//    override fun onActivityPaused(activity: Activity) {
//        navigationView?.onPause()
//        mapView.onPause()
//    }
//
//    override fun onActivityStopped(activity: Activity) {
//    }
//
//    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
//        navigationView?.onSaveInstanceState(outState)
//        mapView.onSaveInstanceState(outState)
//    }
//
//    override fun onActivityDestroyed(activity: Activity) {
//    }
//
//    override fun onMapReady(p0: MapboxMap) {
//        this.mapReady = true
//        this.mapBoxMap = p0
//        if (FlutterMapViewFactoryv2.simulateRoute) {
//            locationEngine = ReplayRouteLocationEngine()
//        }
//        mapBoxMap?.setStyle(FlutterMapViewFactoryv2.mapStyleURL) { style ->
//            context.addDestinationIconSymbolLayer(style)
//            enableLocationComponent(style)
//            initMapRoute()
//        }
//
//        if(FlutterMapViewFactoryv2.longPressDestinationEnabled)
//            mapBoxMap?.addOnMapLongClickListener(this);
//
//        if(FlutterMapViewFactoryv2.initialLatitude != null && FlutterMapViewFactoryv2.initialLongitude != null)
//            moveCamera(LatLng(FlutterMapViewFactoryv2.initialLatitude!!, FlutterMapViewFactoryv2.initialLongitude!!))
//
//        PluginUtilities.sendEvent(VietMapEvents.MAP_READY)
//    }
//
//    private fun initMapRoute() {
//        navigationMapRoute = NavigationMapRoute(mapView, mapBoxMap!!)
//    }
//    private fun enableLocationComponent(@NonNull loadedMapStyle: Style) {
//        if (PermissionsManager.areLocationPermissionsGranted(context)) {
//            val customLocationComponentOptions = LocationComponentOptions.builder(context)
//                .pulseEnabled(true)
//                .build()
//            mapBoxMap?.locationComponent?.let { locationComponent ->
//                locationComponent.activateLocationComponent(
//                    LocationComponentActivationOptions.builder(context, loadedMapStyle)
//                        .locationComponentOptions(customLocationComponentOptions)
//                        .locationEngine(locationEngine)
//                        .build()
//                )
//
//                if (ActivityCompat.checkSelfPermission(
//                        context,
//                        Manifest.permission.ACCESS_FINE_LOCATION
//                    ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
//                        context,
//                        Manifest.permission.ACCESS_COARSE_LOCATION
//                    ) == PackageManager.PERMISSION_GRANTED
//                ) {
//                    locationComponent.isLocationComponentEnabled = true
//                }
//
//                locationComponent.setCameraMode(
//                    CameraMode.TRACKING_GPS_NORTH,
//                    750L,
//                    FlutterMapViewFactoryv2.zoom,
//                    FlutterMapViewFactoryv2.bearing,
//                    FlutterMapViewFactoryv2.tilt,
//                    null
//                )
//                locationComponent.isLocationComponentEnabled = true
//                locationComponent.zoomWhileTracking(18.0)
//                locationComponent.renderMode = RenderMode.GPS
//                locationComponent.locationEngine = locationEngine
//
//            }
//
//        }
//    }
//
//    private fun Context.addDestinationIconSymbolLayer(loadedMapStyle: Style) {
////        loadedMapStyle.addImage("destination-icon-id",
////            BitmapFactory.decodeResource(this.resources, R.drawable.mapbox_marker_icon_default))
////        val geoJsonSource = GeoJsonSource("destination-source-id")
////        loadedMapStyle.addSource(geoJsonSource)
////        val destinationSymbolLayer = SymbolLayer("destination-symbol-layer-id", "destination-source-id")
////        destinationSymbolLayer.withProperties(
////            PropertyFactory.iconImage("destination-icon-id"),
////            PropertyFactory.iconAllowOverlap(true),
////            PropertyFactory.iconIgnorePlacement(true)
////        )
////        loadedMapStyle.addLayer(destinationSymbolLayer)
//    }
//    override fun onProgressChange(location: Location, routeProgress: RouteProgress) {
//
//        if (!isNavigationCanceled) {
//            try {
//
//                FlutterMapViewFactoryv2.distanceRemaining = routeProgress.distanceRemaining()
//                FlutterMapViewFactoryv2.durationRemaining = routeProgress.durationRemaining()
//
//                val progressEvent = VietMapRouteProgressEvent(routeProgress)
//                PluginUtilities.sendEvent(progressEvent)
////                addCustomMarker(LatLng(location.latitude, location.longitude), R.drawable.maplibre_marker_icon_default)
//
//                moveCamera(LatLng(location.latitude, location.longitude))
//
//                if (FlutterMapViewFactoryv2.simulateRoute && !isDisposed && !isBuildingRoute)
//                    mapBoxMap?.locationComponent?.forceLocationUpdate(location)
//
//                if (!isRefreshing) {
//                    isRefreshing = true
//                }
//            } catch (e: java.lang.Exception) {
//
//            }
//        }
//    }
//
//    override fun userOffRoute(p0: Location?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun onMilestoneEvent(p0: RouteProgress?, p1: String?, p2: Milestone?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun onRunning(p0: Boolean) {
//        TODO("Not yet implemented")
//    }
//
//    override fun onCancelNavigation() {
//        TODO("Not yet implemented")
//    }
//
//    override fun onNavigationFinished() {
//        TODO("Not yet implemented")
//    }
//
//    override fun onNavigationRunning() {
//        TODO("Not yet implemented")
//    }
//
//    override fun fasterRouteFound(p0: DirectionsRoute?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun willVoice(p0: SpeechAnnouncement?): SpeechAnnouncement {
//        TODO("Not yet implemented")
//    }
//
//    override fun willDisplay(p0: BannerInstructions?): BannerInstructions {
//        TODO("Not yet implemented")
//    }
//
//    override fun allowRerouteFrom(p0: Point?): Boolean {
//        TODO("Not yet implemented")
//    }
//
//    override fun onOffRoute(p0: Point?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun onRerouteAlong(p0: DirectionsRoute?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun onFailedReroute(p0: String?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun onArrival() {
//        TODO("Not yet implemented")
//    }
//
//    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun onCancel(arguments: Any?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun onMapLongClick(p0: LatLng): Boolean {
//        TODO("Not yet implemented")
//    }
//
//    override fun onMoveBegin(p0: MoveGestureDetector) {
//        TODO("Not yet implemented")
//    }
//
//    override fun onMove(p0: MoveGestureDetector) {
//        TODO("Not yet implemented")
//    }
//
//    override fun onMoveEnd(p0: MoveGestureDetector) {
//        TODO("Not yet implemented")
//    }
//
//}