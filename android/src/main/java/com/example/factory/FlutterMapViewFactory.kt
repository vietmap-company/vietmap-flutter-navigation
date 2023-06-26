package com.example.factory

//import com.mapbox.services.android.navigation.ui.v5.LocationEngineConductor
import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import com.example.demo_plugin.DemoPlugin
import com.example.demo_plugin.R
import com.example.models.VietMapEvents
import com.example.models.VietMapLocation
import com.example.models.VietMapRouteProgressEvent
import com.example.utilities.PluginUtilities
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.engine.LocationEngine
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.location.permissions.PermissionsManager
import com.mapbox.mapboxsdk.maps.*
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND
import com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.NavigationPresenter
import com.mapbox.services.android.navigation.ui.v5.NavigationView
import com.mapbox.services.android.navigation.ui.v5.NavigationViewModel
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera
import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener
import com.mapbox.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement
import com.mapbox.services.android.navigation.v5.location.engine.LocationEngineProvider
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.platform.PlatformView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.*

class FlutterMapViewFactory  :
    PlatformView,
    MethodCallHandler,
    Application.ActivityLifecycleCallbacks,
    OnMapReadyCallback,
    ProgressChangeListener,
    OffRouteListener,
    MilestoneEventListener,
    NavigationEventListener,
    NavigationListener,
    FasterRouteListener,
    SpeechAnnouncementListener,
    BannerInstructionsListener,
    RouteListener, EventChannel.StreamHandler, MapboxMap.OnMapLongClickListener,
    MapboxMap.OnMoveListener{

    private val activity: Activity
    private val context: Context

    private val methodChannel: MethodChannel
    private val eventChannel: EventChannel

    private val options: MapboxMapOptions

    private var mapView: MapView
    private var mapBoxMap: MapboxMap? = null
    private var currentRoute: DirectionsRoute? = null
    private var navigationView: NavigationView? = null
    private var locationEngine: LocationEngine? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private val navigationOptions = MapboxNavigationOptions.builder()
        .build()
    private var navigation: MapboxNavigation
    private var mapReady = false
    private var isDisposed = false
    private var isRefreshing = false
    private var isBuildingRoute = false
    private var isNavigationInProgress = false
    private var isNavigationCanceled = false
    private var navigationViewModel: NavigationViewModel? = null

    constructor(cxt: Context, messenger: BinaryMessenger, viewId: Int, act: Activity, args: Any?) {

        Mapbox.getInstance(act.applicationContext)
        activity = act
        activity.application.registerActivityLifecycleCallbacks(this)
        context = cxt
        val arguments = args as? Map<*, *>
        if (arguments != null)
            setOptions(arguments)

        methodChannel = MethodChannel(messenger, "demo_plugin/${viewId}")
        eventChannel = EventChannel(messenger, "demo_plugin/${viewId}/events")
        eventChannel.setStreamHandler(this)

        options = MapboxMapOptions.createFromAttributes(context)
            .compassEnabled(false)
            .logoEnabled(true)
        mapView = MapView(context, options)
        navigationView = NavigationView(context)
        locationEngine = LocationEngineProvider.getBestLocationEngine(context)
        navigation = MapboxNavigation(
            context,
            navigationOptions,
            locationEngine!!
        )

        navigationViewModel = NavigationViewModel(activity.application)
        navigationViewModel?.initializeLocationEngine()
        navigationViewModel?.initializeNavigation(context, navigationOptions, locationEngine)
        navigationViewModel?.initializeRouter()
//            navigationViewModel?.initializeLocationEngineFrom(options)
        navigationView?.onCreate(null, navigationViewModel)
        methodChannel.setMethodCallHandler(this)
        mapView.getMapAsync(this)

//        navigationMapRoute = NavigationMapRoute(mapView, mapBoxMap!!)

    }

    companion object {

        //Config
        var initialLatitude: Double? = null
        var initialLongitude: Double? = null

        val wayPoints: MutableList<Point> = mutableListOf()
        var navigationMode = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
        var simulateRoute = false
        var mapStyleURL: String? = null
        var navigationLanguage = Locale("en")
        var navigationVoiceUnits = DirectionsCriteria.IMPERIAL
        var zoom = 18.0
        var bearing = 10000.0
        var tilt = 10000.0
        var distanceRemaining: Double? = null
        var durationRemaining: Double? = null

        var alternatives = true

        var allowsUTurnAtWayPoints = false
        var enableRefresh = false
        var voiceInstructionsEnabled = true
        var bannerInstructionsEnabled = true
        var longPressDestinationEnabled = true
        var animateBuildRoute = true
        var isOptimized = false

        var originPoint: Point? = null
        var destinationPoint: Point? = null
    }

    override fun getView(): View {
        return if (isNavigationInProgress) {
            println("getting navigation view")
            navigationView!!
        } else {
            println("getting map view")
            mapView
        }
    }
    override fun onMethodCall(methodCall: MethodCall, result: MethodChannel.Result) {
        when (methodCall.method) {

            "buildRoute" -> {
                buildRoute(methodCall, result)
            }
            "clearRoute" -> {
                clearRoute(methodCall, result)
            }
            "startNavigation" -> {
                startNavigation(methodCall, result)
                /*
                val navigationIntent = Intent(activity, NavigationFragmentActivity::class.java)
                navigationIntent.putExtra("waypoints", wayPoints as Serializable)
                activity.startActivity(navigationIntent)

                 */
            }
            "finishNavigation" -> {
                finishNavigation(methodCall, result)
            }
            "getDistanceRemaining" -> {
                result.success(distanceRemaining)
            }
            "getDurationRemaining" -> {
                result.success(durationRemaining)
            }
            "recenter" -> {

                val navigationPresenter: NavigationPresenter? =
                    navigationView?.getNavigationPresenter()
                navigationPresenter?.onRecenterClick()

            }
            "overview" -> {
                val navigationPresenter: NavigationPresenter? =
                    navigationView?.getNavigationPresenter()
                navigationPresenter?.onRouteOverviewClick()
            }
            else -> result.notImplemented()
        }
    }

    private fun buildRoute(methodCall: MethodCall, result: MethodChannel.Result) {
        isNavigationCanceled = false
        isNavigationInProgress = false

        val arguments = methodCall.arguments as? Map<*, *>
        if(arguments != null)
            setOptions(arguments)

        if (mapReady) {
            wayPoints.clear()
            var points = arguments?.get("wayPoints") as HashMap<*, *>
            for (item in points)
            {
                val point = item.value as HashMap<*, *>
                val latitude = point["Latitude"] as Double
                val longitude = point["Longitude"] as Double
                wayPoints.add(Point.fromLngLat(longitude, latitude))
            }
            getRoute(context)
            result.success(true)
        } else {
            result.success(false)
        }
    }

    private fun clearRoute(methodCall: MethodCall, result: MethodChannel.Result) {
        if (navigationMapRoute != null)
            navigationMapRoute?.updateRouteArrowVisibilityTo(false)

        PluginUtilities.sendEvent(VietMapEvents.NAVIGATION_CANCELLED)
    }

    private fun startNavigation(methodCall: MethodCall, result: MethodChannel.Result) {

        val arguments = methodCall.arguments as? Map<*, *>
        if(arguments != null)
            setOptions(arguments)

        startNavigation()

        if (currentRoute != null) {
            result.success(true)
        } else {
            result.success(false)
        }
    }

    private fun finishNavigation(methodCall: MethodCall, result: MethodChannel.Result) {

        finishNavigation()

        if (currentRoute != null) {
            result.success(true)
        } else {
            result.success(false)
        }
    }

    private fun startNavigation() {
        isNavigationCanceled = false
        if (currentRoute != null) {
            if (simulateRoute) {
                (locationEngine as ReplayRouteLocationEngine).assign(currentRoute)
                navigation.locationEngine = locationEngine as ReplayRouteLocationEngine
            }
            val options =
                NavigationViewOptions.builder()
                    .progressChangeListener(this)
                    .milestoneEventListener(this)
                    .navigationListener(this)
                    .speechAnnouncementListener(this)
                    .bannerInstructionsListener(this)
                    .routeListener(this)
                    .locationEngine(locationEngine)
                    .directionsRoute(currentRoute)
                    .shouldSimulateRoute(simulateRoute)
                    .onMoveListener(this)
                    .navigationOptions(navigationOptions)
                    .build()

            currentRoute?.let {

                isNavigationInProgress = true
                navigationView?.initViewConfig(false)
                navigationView?.startNavigation(options)
                navigationView?.startCamera(currentRoute)
                navigationView?.updateCameraRouteOverview()
//                navigation.startNavigation(currentRoute!!)
                PluginUtilities.sendEvent(VietMapEvents.NAVIGATION_RUNNING)
            }
        }
    }

    private fun finishNavigation(isOffRouted: Boolean = false) {

        zoom = 15.0
        bearing = 0.0
        tilt = 0.0
        isNavigationCanceled = true

        if (!isOffRouted) {
            isNavigationInProgress = false
            moveCameraToOriginOfRoute()
        }

        if (currentRoute != null) {
            navigation.stopNavigation()
            navigation.removeFasterRouteListener(this)
            navigation.removeMilestoneEventListener(this)
            navigation.removeNavigationEventListener(this)
            navigation.removeOffRouteListener(this)
            navigation.removeProgressChangeListener(this)
        }

    }

    private fun setOptions(arguments: Map<*, *>)
    {
        val navMode = arguments["mode"] as? String
        if(navMode != null)
        {
            if(navMode == "walking")
                navigationMode = DirectionsCriteria.PROFILE_WALKING;
            else if(navMode == "cycling")
                navigationMode = DirectionsCriteria.PROFILE_CYCLING;
            else if(navMode == "driving")
                navigationMode = DirectionsCriteria.PROFILE_DRIVING;
        }

        val simulated = arguments["simulateRoute"] as? Boolean
        if (simulated != null) {
            println(simulated)
            println("----------------------------------------")
            simulateRoute = simulated
        }

        val language = arguments["language"] as? String
        if(language != null)
            navigationLanguage = Locale(language)

        val units = arguments["units"] as? String

        if(units != null)
        {
            if(units == "imperial")
                navigationVoiceUnits = DirectionsCriteria.IMPERIAL
            else if(units == "metric")
                navigationVoiceUnits = DirectionsCriteria.METRIC
        }

        mapStyleURL = arguments["mapStyleURL"] as? String

        initialLatitude = arguments["initialLatitude"] as? Double
        initialLongitude = arguments["initialLongitude"] as? Double

        val zm = arguments["zoom"] as? Double
        if(zm != null)
            zoom = zm

        val br = arguments["bearing"] as? Double
        if(br != null)
            bearing = br

        val tt = arguments["tilt"] as? Double
        if(tt != null)
            tilt = tt

        val optim = arguments["isOptimized"] as? Boolean
        if(optim != null)
            isOptimized = optim

        val anim = arguments["animateBuildRoute"] as? Boolean
        if(anim != null)
            animateBuildRoute = anim

        val altRoute = arguments["alternatives"] as? Boolean
        if(altRoute != null)
            alternatives = altRoute

        val voiceEnabled = arguments["voiceInstructionsEnabled"] as? Boolean
        if(voiceEnabled != null)
            voiceInstructionsEnabled = voiceEnabled

        val bannerEnabled = arguments["bannerInstructionsEnabled"] as? Boolean
        if(bannerEnabled != null)
            bannerInstructionsEnabled = bannerEnabled

        var longPress = arguments["longPressDestinationEnabled"] as? Boolean
        if(longPress != null)
            longPressDestinationEnabled = longPress
    }

    override fun onMapReady(map: MapboxMap) {
        this.mapReady = true
        this.mapBoxMap = map
        if (simulateRoute) {
            locationEngine = ReplayRouteLocationEngine()
        }
        if (mapStyleURL == null)
            mapStyleURL = "https://run.mocky.io/v3/961aaa3a-f380-46be-9159-09cc985d9326"

        mapBoxMap?.setStyle(mapStyleURL) { style ->
            context.addDestinationIconSymbolLayer(style)
            enableLocationComponent(style)

            val routeLineLayer = LineLayer("line-layer-id", "source-id")
            routeLineLayer.setProperties(
                lineWidth(9f),
                lineColor(Color.RED),
                lineCap(LINE_CAP_ROUND),
                lineJoin(LINE_JOIN_ROUND)
            )
            style.addLayer(routeLineLayer)
        }

        if(longPressDestinationEnabled)
            mapBoxMap?.addOnMapLongClickListener(this);

//        markerViewManager = MarkerViewManager(mapView, mapBoxMap)

        if(initialLatitude != null && initialLongitude != null)
            moveCamera(LatLng(initialLatitude!!, initialLongitude!!))

        PluginUtilities.sendEvent(VietMapEvents.MAP_READY)
    }

    override fun onMapLongClick(point: LatLng): Boolean {
        if (wayPoints.size === 2) {
            wayPoints.clear()
        }

        val lastLocation = mapBoxMap?.locationComponent?.lastKnownLocation
        if(lastLocation?.longitude != null)
            wayPoints.add(Point.fromLngLat(lastLocation.longitude, lastLocation.latitude))
        wayPoints.add(Point.fromLngLat(point.longitude, point.latitude))
        getRoute(context)
        var navCam = NavigationCamera(mapBoxMap!!, navigation, mapBoxMap!!.locationComponent)
        navCam.showRouteOverview(intArrayOf(20, 20, 20, 20))
        return false
    }

    fun Context.addDestinationIconSymbolLayer(loadedMapStyle: Style) {
//        loadedMapStyle.addImage("destination-icon-id",
//            BitmapFactory.decodeResource(this.resources, R.drawable.mapbox_marker_icon_default))
        val geoJsonSource = GeoJsonSource("destination-source-id")
        loadedMapStyle.addSource(geoJsonSource)
        val destinationSymbolLayer = SymbolLayer("destination-symbol-layer-id", "destination-source-id")
        destinationSymbolLayer.withProperties(
            PropertyFactory.iconImage("destination-icon-id"),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true)
        )
        loadedMapStyle.addLayer(destinationSymbolLayer)
    }

    private fun moveCamera(location: LatLng) {
        val cameraPosition = CameraPosition.Builder()
            .target(location)
            .zoom(zoom)
            .bearing(bearing)
            .tilt(tilt)
            .build()
        var duration = 3000
        if(animateBuildRoute)
            duration = 1
        mapBoxMap?.animateCamera(CameraUpdateFactory
            .newCameraPosition(cameraPosition), duration)
    }

    private fun addCustomMarker(location: LatLng, @DrawableRes markerIcon: Int, rotationFrom: Double? = null, rotationTo: Double? = null) {
//        if (initialMarkerView != null) {
//            markerViewManager.removeMarker(initialMarkerView!!)
//        }

        val markerView = ImageView(context)
        markerView.setImageResource(markerIcon)
        markerView.layoutParams = ViewGroup.LayoutParams(100, 100)

        rotationTo?.let {
            markerView.rotation = rotationTo.toFloat()
            /*rotationFrom?.let {
                val rotate = RotateAnimation(rotationFrom.toFloat(), rotationTo.toFloat(), Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
                rotate.duration = 800
                rotate.interpolator = LinearInterpolator()
                rotate.isFillEnabled = true
                //rotate.fillAfter = true
                markerView.startAnimation(rotate)
            }*/
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            markerView.elevation = 16.0f
        }
//        initialMarkerView = MarkerView(location, markerView)
//        initialMarkerView?.let {
//            markerViewManager.addMarker(it)
//        }
    }

    private fun addLocationMarker(location: LatLng, @DrawableRes markerIcon: Int) {
//        if (locationMarkerView != null) {
//            markerViewManager.removeMarker(locationMarkerView!!)
//        }

        val markerView = ImageView(context)
        markerView.setImageResource(markerIcon)
        markerView.layoutParams = ViewGroup.LayoutParams(100, 100)

//        locationMarkerView = MarkerView(location, markerView)
//        locationMarkerView?.let {
//            markerViewManager.addMarker(it)
//        }
    }


    private fun getRoute(context: Context) {

        if (!PluginUtilities.isNetworkAvailable(context)) {
            PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILD_FAILED, "No Internet Connection")
            return
        }

        PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILDING)

        originPoint = Point.fromLngLat(wayPoints[0].longitude(), wayPoints[0].latitude())
        destinationPoint = Point.fromLngLat(wayPoints[1].longitude(), wayPoints[1].latitude())
        val builder = NavigationRoute.builder(activity)
            .apikey("95f852d9f8c38e08ceacfd456b59059d0618254a50d3854c")
            .origin(originPoint!!)
            .destination(destinationPoint!!)
            .alternatives(true)
            .build()
        builder.getRoute(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse?>, response: Response<DirectionsResponse?>) {

                if (response.body() == null || response.body()!!.routes().size < 1) {
                    PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILD_FAILED, "No routes found")
                    return
                }

                currentRoute = response.body()!!.routes()[0]
                PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILT,"${currentRoute?.toJson()}")
                moveCameraToOriginOfRoute()

                // Draw the route on the map
                if (navigationMapRoute != null) {
                    navigationMapRoute?.updateRouteArrowVisibilityTo(false)
                } else {
//                    navigationMapRoute = NavigationMapRoute(navigation, mapView, mapBoxMap!!, R.style.NavigationMapRoute)
                }
                navigationMapRoute?.addRoute(currentRoute)
                isBuildingRoute = false

                //Start Navigation again from new Point, if it was already in Progress
                if (isNavigationInProgress) {
                    startNavigation()
                }
            }

            override fun onFailure(call: Call<DirectionsResponse?>, throwable: Throwable) {
                isBuildingRoute = false
                PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILD_FAILED, "${throwable.message}")
            }
        })
    }

    private fun moveCameraToOriginOfRoute() {
        currentRoute?.let {
            val originCoordinate = it.routeOptions()?.coordinates()?.get(0)
            originCoordinate?.let {
                val location = LatLng(originCoordinate.latitude(), originCoordinate.longitude())
                moveCamera(location)
                //addCustomMarker(location)
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        println("-------------------------onActivityCreated")
        mapView.onCreate(savedInstanceState)
         navigationView?.onCreate(savedInstanceState,null)
//         navigationView?.initialize(
//            OnNavigationReadyCallback { return@OnNavigationReadyCallback },
//            getInitialCameraPosition()
//        )
    }

    override fun onActivityStarted(activity: Activity) {

        try {
             navigationView?.onStart()
            mapView.onStart()
        } catch (e: java.lang.Exception) {
            Timber.i(String.format("onActivityStarted, %s", "Error: ${e.message}"))
        }
    }

    override fun onActivityResumed(activity: Activity) {
         navigationView?.onResume()
        mapView.onResume()
    }

    override fun onActivityPaused(activity: Activity) {
         navigationView?.onPause()
        mapView.onPause()
    }

    override fun onActivityStopped(activity: Activity) {
        //mapView.onStop()
        // navigationView?.onStop()
    }

    override fun onActivitySaveInstanceState(@NonNull p0: Activity, @NonNull outState: Bundle) {
         navigationView?.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onActivityDestroyed(activity: Activity) {
        // navigationView?.onDestroy()
        //mapView.onDestroy()
    }


    override fun onProgressChange(location: Location, routeProgress: RouteProgress) {

        if (!isNavigationCanceled) {
            try {

                distanceRemaining = routeProgress.distanceRemaining()
                durationRemaining = routeProgress.durationRemaining()

                val progressEvent = VietMapRouteProgressEvent(routeProgress)
                PluginUtilities.sendEvent(progressEvent)
                addCustomMarker(LatLng(location.latitude, location.longitude), R.drawable.maplibre_marker_icon_default)

                moveCamera(LatLng(location.latitude, location.longitude))

                if (simulateRoute && !isDisposed && !isBuildingRoute)
                    mapBoxMap?.locationComponent?.forceLocationUpdate(location)

                if (!isRefreshing) {
                    isRefreshing = true
                }
            } catch (e: java.lang.Exception) {

            }
        }
    }

    override fun userOffRoute(location: Location) {

        doOnNewRoute(Point.fromLngLat(location.latitude, location.longitude))
    }

    override fun onMilestoneEvent(routeProgress: RouteProgress, instruction: String, milestone: Milestone) {

        if (!isNavigationCanceled) {
            PluginUtilities.sendEvent(VietMapEvents.MILESTONE_EVENT, instruction)
        }
    }

    override fun onRunning(running: Boolean) {

        if (!isNavigationCanceled) {
            PluginUtilities.sendEvent(VietMapEvents.NAVIGATION_RUNNING)

        }
    }

    override fun onCancelNavigation() {
        PluginUtilities.sendEvent(VietMapEvents.NAVIGATION_CANCELLED)
         navigationView?.stopNavigation()
        navigation.stopNavigation()

    }

    override fun onNavigationFinished() {
        PluginUtilities.sendEvent(VietMapEvents.NAVIGATION_FINISHED)
    }

    override fun onNavigationRunning() {
        if (!isNavigationCanceled) {
            PluginUtilities.sendEvent(VietMapEvents.NAVIGATION_RUNNING)

        }
    }

    override fun fasterRouteFound(directionsRoute: DirectionsRoute) {
        PluginUtilities.sendEvent(VietMapEvents.FASTER_ROUTE_FOUND, directionsRoute.toJson())

        refreshNavigation(directionsRoute)

    }

    private fun refreshNavigation(directionsRoute: DirectionsRoute?, shouldCancel: Boolean = true) {
        directionsRoute?.let {

            if (shouldCancel) {

                currentRoute = directionsRoute
                finishNavigation()
                startNavigation()
            }
        }
    }

    private fun getInitialCameraPosition(): CameraPosition {
        if(currentRoute == null)
            return CameraPosition.DEFAULT;

        val originCoordinate = currentRoute?.routeOptions()?.coordinates()?.get(0)
        return CameraPosition.Builder()
            .target(LatLng(originCoordinate!!.latitude(), originCoordinate.longitude()))
            .zoom(DemoPlugin.zoom)
            .bearing(DemoPlugin.bearing)
            .tilt(DemoPlugin.tilt)
            .build()
    }
    override fun willVoice(announcement: SpeechAnnouncement?): SpeechAnnouncement? {
        return if (voiceInstructionsEnabled) {
            PluginUtilities.sendEvent(VietMapEvents.SPEECH_ANNOUNCEMENT, "${announcement?.announcement()}")
            announcement
        } else {
            null
        }
    }

    override fun willDisplay(instructions: BannerInstructions?): BannerInstructions? {
        return if (bannerInstructionsEnabled) {
            PluginUtilities.sendEvent(VietMapEvents.BANNER_INSTRUCTION, "${instructions?.primary()?.text()}")

            return instructions
        } else {
            null
        }
    }

    override fun onArrival() {
        PluginUtilities.sendEvent(VietMapEvents.ON_ARRIVAL)
    }

    override fun onFailedReroute(errorMessage: String?) {
        PluginUtilities.sendEvent(VietMapEvents.FAILED_TO_REROUTE, "$errorMessage")

    }

    override fun onOffRoute(offRoutePoint: Point?) {
        doOnNewRoute(offRoutePoint)
    }

    private fun doOnNewRoute(offRoutePoint: Point?) {
        if (!isBuildingRoute) {
            isBuildingRoute = true

            offRoutePoint?.let {

                finishNavigation(isOffRouted = true)

                moveCamera(LatLng(it.latitude(), it.longitude()))

                PluginUtilities.sendEvent(VietMapEvents.USER_OFF_ROUTE,
                    VietMapLocation(
                        latitude = it.latitude(),
                        longitude = it.longitude()
                    ).toString())

            }

            PluginUtilities.sendEvent(VietMapEvents.USER_OFF_ROUTE,
                VietMapLocation(
                    latitude = offRoutePoint?.latitude(),
                    longitude = offRoutePoint?.longitude()
                ).toString())

            originPoint = offRoutePoint
            isNavigationInProgress = true
            getRoute(context)
        }
    }

    override fun onRerouteAlong(directionsRoute: DirectionsRoute?) {
        PluginUtilities.sendEvent(VietMapEvents.REROUTE_ALONG, "${directionsRoute?.toJson()}")
        refreshNavigation(directionsRoute)

    }

    override fun allowRerouteFrom(offRoutePoint: Point?): Boolean {

        return true
    }


    private fun enableLocationComponent(@NonNull loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(context)) {
            val customLocationComponentOptions = LocationComponentOptions.builder(context)
                .pulseEnabled(true)
                .build()
            mapBoxMap?.locationComponent?.let { locationComponent ->
//                try {
                locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, loadedMapStyle)
                        .locationComponentOptions(customLocationComponentOptions)
                        .locationEngine(locationEngine)
                        .build()
                )

                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationComponent.isLocationComponentEnabled = true
                }

                locationComponent.setCameraMode(
                    CameraMode.TRACKING_GPS_NORTH,
                    750L,
                    zoom,
                    bearing,
                    tilt,
                    null
                )
                locationComponent.isLocationComponentEnabled = true
                locationComponent.zoomWhileTracking(18.0)
                locationComponent.renderMode = RenderMode.GPS
                locationComponent.locationEngine = locationEngine

            }

            navigationView?.initializeNavigationMap(mapView, mapBoxMap)
        }
    }

    override fun dispose() {
        isDisposed = true
        mapReady = false
        mapView.onStop()
        mapView.onDestroy()
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        DemoPlugin.eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        DemoPlugin.eventSink = null
    }


    override fun onMoveBegin(p0: MoveGestureDetector) {
        println("On map move begin")
    }

    override fun onMove(p0: MoveGestureDetector) {
        println("On map move")
    }

    override fun onMoveEnd(p0: MoveGestureDetector) {
        println("On map move end")
    }

}