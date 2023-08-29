package vn.vietmap.factory
import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
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
import vn.vietmap.android.gestures.MoveGestureDetector
import vn.vietmap.models.CurrentCenterPoint
import vn.vietmap.models.VietMapEvents
import vn.vietmap.models.VietMapLocation
import vn.vietmap.models.VietMapRouteProgressEvent
import vn.vietmap.navigation_plugin.VietMapNavigationPlugin
import vn.vietmap.services.android.navigation.ui.v5.camera.CameraOverviewCancelableCallback
import vn.vietmap.services.android.navigation.ui.v5.listeners.BannerInstructionsListener
import vn.vietmap.services.android.navigation.ui.v5.listeners.NavigationListener
import vn.vietmap.services.android.navigation.ui.v5.listeners.RouteListener
import vn.vietmap.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener
import vn.vietmap.services.android.navigation.ui.v5.route.NavigationMapRoute
import vn.vietmap.services.android.navigation.ui.v5.route.OnRouteSelectionChangeListener
import vn.vietmap.services.android.navigation.ui.v5.voice.NavigationSpeechPlayer
import vn.vietmap.services.android.navigation.ui.v5.voice.SpeechAnnouncement
import vn.vietmap.services.android.navigation.ui.v5.voice.SpeechPlayer
import vn.vietmap.services.android.navigation.ui.v5.voice.SpeechPlayerProvider
import vn.vietmap.services.android.navigation.v5.location.engine.LocationEngineProvider
import vn.vietmap.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
import vn.vietmap.services.android.navigation.v5.milestone.Milestone
import vn.vietmap.services.android.navigation.v5.milestone.MilestoneEventListener
import vn.vietmap.services.android.navigation.v5.milestone.VoiceInstructionMilestone
import vn.vietmap.services.android.navigation.v5.navigation.*
import vn.vietmap.services.android.navigation.v5.navigation.camera.RouteInformation
import vn.vietmap.services.android.navigation.v5.offroute.OffRouteListener
import vn.vietmap.services.android.navigation.v5.route.FasterRouteListener
import vn.vietmap.services.android.navigation.v5.routeprogress.ProgressChangeListener
import vn.vietmap.services.android.navigation.v5.routeprogress.RouteProgress
import vn.vietmap.services.android.navigation.v5.snap.SnapToRoute
import vn.vietmap.services.android.navigation.v5.utils.RouteUtils
import vn.vietmap.utilities.PluginUtilities
import vn.vietmap.vietmapsdk.Vietmap
import vn.vietmap.vietmapsdk.camera.CameraPosition
import vn.vietmap.vietmapsdk.camera.CameraUpdate
import vn.vietmap.vietmapsdk.camera.CameraUpdateFactory
import vn.vietmap.vietmapsdk.camera.CameraUpdateFactory.newLatLngBounds
import vn.vietmap.vietmapsdk.geometry.LatLng
import vn.vietmap.vietmapsdk.geometry.LatLngBounds
import vn.vietmap.vietmapsdk.location.LocationComponentActivationOptions
import vn.vietmap.vietmapsdk.location.LocationComponentOptions
import vn.vietmap.vietmapsdk.location.LocationLayerController
import vn.vietmap.vietmapsdk.location.LocationLayerRenderer
import vn.vietmap.vietmapsdk.location.engine.LocationEngine
import vn.vietmap.vietmapsdk.location.modes.CameraMode
import vn.vietmap.vietmapsdk.location.modes.RenderMode
import vn.vietmap.vietmapsdk.location.permissions.PermissionsManager
import vn.vietmap.vietmapsdk.maps.*
import vn.vietmap.vietmapsdk.maps.MapView.OnDidFinishRenderingMapListener
import vn.vietmap.vietmapsdk.maps.VietMapGL.OnMoveListener
import vn.vietmap.vietmapsdk.style.layers.LineLayer
import vn.vietmap.vietmapsdk.style.layers.Property.LINE_CAP_ROUND
import vn.vietmap.vietmapsdk.style.layers.Property.LINE_JOIN_ROUND
import vn.vietmap.vietmapsdk.style.layers.PropertyFactory.*
import vn.vietmap.vietmapsdk.style.layers.SymbolLayer
import vn.vietmap.vietmapsdk.style.sources.GeoJsonSource
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
    RouteListener, EventChannel.StreamHandler, VietMapGL.OnMapLongClickListener,
    VietMapGL.OnMapClickListener,OnDidFinishRenderingMapListener {

    private val activity: Activity
    private val context: Context
    private val methodChannel: MethodChannel
    private val eventChannel: EventChannel
    private val options: VietMapGLOptions
    private var mapView: MapView
    private var vietmapGL: VietMapGL? = null
    private var currentRoute: DirectionsRoute? = null
    private var routeClicked: Boolean = false
    private var locationEngine: LocationEngine? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private val navigationOptions = VietmapNavigationOptions
        .builder().maxTurnCompletionOffset(30.0)
        .maneuverZoneRadius(40.0).maximumDistanceOffRoute(50.0)
        .deadReckoningTimeInterval(5.0).maxManipulatedCourseAngle(25.0)
        .userLocationSnapDistance(20.0).secondsBeforeReroute(3)
        .enableOffRouteDetection(true).enableFasterRouteDetection(true).snapToRoute(true)
        .manuallyEndNavigationUponCompletion(false).defaultMilestonesEnabled(true)
        .minimumDistanceBeforeRerouting(10.0).metersRemainingTillArrival(20.0)
        .isFromNavigationUi(false).isDebugLoggingEnabled(false)
        .roundingIncrement(NavigationConstants.ROUNDING_INCREMENT_FIFTY)
        .timeFormatType(NavigationTimeFormat.NONE_SPECIFIED)
        .locationAcceptableAccuracyInMetersThreshold(100)
        .build()
//    private val navigationOptions = VietmapNavigationOptions
//        .builder().maxTurnCompletionOffset(30.0).maneuverZoneRadius(40.0)
//        .maximumDistanceOffRoute(20.0).deadReckoningTimeInterval(1.0)
//        .maxManipulatedCourseAngle(25.0).userLocationSnapDistance(10.0).secondsBeforeReroute(3)
//        .enableOffRouteDetection(true).enableFasterRouteDetection(false).snapToRoute(true)
//        .manuallyEndNavigationUponCompletion(false).defaultMilestonesEnabled(true)
//        .minimumDistanceBeforeRerouting(100.0).metersRemainingTillArrival(40.0)
//        .isFromNavigationUi(false).isDebugLoggingEnabled(false).roundingIncrement(50)
//        .timeFormatType(NavigationTimeFormat.NONE_SPECIFIED).locationAcceptableAccuracyInMetersThreshold(100)

    /*
    VietmapNavigationOptions
            .builder().maxTurnCompletionOffset(10.0)
            .maneuverZoneRadius(40.0).maximumDistanceOffRoute(30.0)
            .deadReckoningTimeInterval(1.0).maxManipulatedCourseAngle(25.0)
            .userLocationSnapDistance(20.0).secondsBeforeReroute(3)
            .enableOffRouteDetection(true).enableFasterRouteDetection(true).snapToRoute(true)
            .manuallyEndNavigationUponCompletion(false).defaultMilestonesEnabled(true)
            .minimumDistanceBeforeRerouting(10.0).metersRemainingTillArrival(20.0)
            .isFromNavigationUi(false).isDebugLoggingEnabled(false)
            .roundingIncrement(NavigationConstants.ROUNDING_INCREMENT_FIFTY)
            .timeFormatType(NavigationTimeFormat.NONE_SPECIFIED)
            .locationAcceptableAccuracyInMetersThreshold(100)
            .build()
     */
    private var navigation: VietmapNavigation
    private var mapReady = false
    private var isDisposed = false
    private var isRefreshing = false
    private var isBuildingRoute = false
    private var isNavigationInProgress = false
    private var isNavigationCanceled = false
    private var isOverviewing = false
    private var currentCenterPoint: CurrentCenterPoint? = null
    private val routeUtils = RouteUtils()
    private val snapEngine = SnapToRoute()
    private var apikey: String? = null
    private var speechPlayer: SpeechPlayer? = null
    private var routeProgress: RouteProgress? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var bitmapDrawable: BitmapDrawable? =null
    constructor(
        cxt: Context,
        messenger: BinaryMessenger,
        viewId: Int,
        act: Activity,
        args: Any?,
        binding: ActivityPluginBinding
    ) {

        Vietmap.getInstance(act.applicationContext)
            activity = act
            activity.application.registerActivityLifecycleCallbacks(this)
            context = cxt
            val arguments = args as? Map<*, *>
            if (arguments != null)
                setOptions(arguments)


        methodChannel = MethodChannel(messenger, "navigation_plugin/${viewId}")
        eventChannel = EventChannel(messenger, "navigation_plugin/${viewId}/events")
        eventChannel.setStreamHandler(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        options = VietMapGLOptions.createFromAttributes(context)
            .compassEnabled(false)
            .logoEnabled(true)
        mapView = MapView(context, options)
        locationEngine = if(simulateRoute) {
            ReplayRouteLocationEngine()
        }else{
            LocationEngineProvider.getBestLocationEngine(context)
        }
        navigation = VietmapNavigation(
            context,
            navigationOptions,
            locationEngine!!
        )
        methodChannel.setMethodCallHandler(this)
        mapView.getMapAsync(this)
        mapView.addOnDidFinishRenderingMapListener {
            PluginUtilities.sendEvent(
                VietMapEvents.ON_MAP_RENDERED
            )
        }
        configSpeechPlayer()
    }

    companion object {

        //Config
        var initialLatitude: Double? = null
        var initialLongitude: Double? = null
        var profile: String = "driving-traffic"
        val wayPoints: MutableList<Point> = mutableListOf()
        var navigationMode = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
        var simulateRoute = false
        var mapStyleURL: String? = null
        var navigationLanguage = Locale("en")
        var navigationVoiceUnits = DirectionsCriteria.IMPERIAL
        var zoom = 20.0
        var bearing = 10000.0
        var tilt = 10000.0
        var distanceRemaining: Double? = null
        var durationRemaining: Double? = null
        var alternatives = true
        var voiceInstructionsEnabled = true
        var bannerInstructionsEnabled = true
        var longPressDestinationEnabled = true
        var animateBuildRoute = true
        var isOptimized = false
        var originPoint: Point? = null
        var destinationPoint: Point? = null
        var isRunning:Boolean = false
    }


    private fun playVoiceAnnouncement(milestone: Milestone?) {
        if (milestone is VoiceInstructionMilestone) {
            var announcement = SpeechAnnouncement.builder()
                .voiceInstructionMilestone(milestone as VoiceInstructionMilestone?).build()
            speechPlayer!!.play(announcement)
        }
    }

    private fun configSpeechPlayer() {

        var speechPlayerProvider = SpeechPlayerProvider(context, "vi", true);
        this.speechPlayer = NavigationSpeechPlayer(speechPlayerProvider)
    }

    override fun getView(): View {
        return mapView
    }
    override fun onMethodCall(methodCall: MethodCall, result: MethodChannel.Result) {
        when (methodCall.method) {

            "buildRoute" -> {
                buildRoute(methodCall, result)
            }

            "buildAndStartNavigation" -> {
                buildRouteAndStartNavigation(methodCall, result)
            }
            "clearRoute" -> {
                clearRoute(methodCall, result)
            }
            "startNavigation" -> {
                startNavigation(methodCall, result)
            }
            "setCenterIcon" ->{

                val arguments = methodCall.arguments as? Map<*, *>
                val byteArray = arguments?.get("customLocationCenterIcon") as? ByteArray
                if(byteArray!=null){
                    this.bitmapDrawable = loadImageFromBinary(byteArray)
                    setCenterIcon();
                }
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
                recenter()
            }
            "overview" -> {
                overViewRoute()
            }
            "mute" -> {
                voiceInstructionsEnabled =methodCall.argument<Boolean>("isMute") ?: !voiceInstructionsEnabled
                speechPlayer?.let {
                    speechPlayer!!.isMuted = methodCall.argument<Boolean>("isMute") ?: false
                    result.success(speechPlayer!!.isMuted)
                }
            }
            "onDispose" -> {
                try {
                    isDisposed = true
                    mapReady = false
                    mapView.onStop()
                    navigation.onDestroy()
                    mapView.onDestroy()
                    result.success(true)
                }catch (e:Exception){
                    e.printStackTrace()
                    result.success(false)
                }
            }
            else -> result.notImplemented()
        }
    }

    private fun recenter() {
        isOverviewing = false
        if (currentCenterPoint != null) {
            moveCamera(
                LatLng(currentCenterPoint!!.latitude, currentCenterPoint!!.longitude),
                currentCenterPoint!!.bearing
            )
        }
    }

    private fun overViewRoute() {
        isOverviewing = true
        val padding:IntArray = intArrayOf(30,30,30,30)
        routeProgress?.let { showRouteOverview(padding, it) }
    }


    private fun buildRoute(methodCall: MethodCall, result: MethodChannel.Result) {
        isNavigationCanceled = false
        isNavigationInProgress = false

        val arguments = methodCall.arguments as? Map<*, *>
        if (arguments != null)
            setOptions(arguments)

        if (mapReady) {
            wayPoints.clear()
            var points = arguments?.get("wayPoints") as HashMap<*, *>
            for (item in points) {
                val point = item.value as HashMap<*, *>
                val latitude = point["Latitude"] as Double
                val longitude = point["Longitude"] as Double
                wayPoints.add(Point.fromLngLat(longitude, latitude))
            }
            var profile: String = arguments?.get("profile") as? String ?: "driving-traffic"
            originPoint = Point.fromLngLat(wayPoints[0].longitude(), wayPoints[0].latitude())
            destinationPoint = Point.fromLngLat(wayPoints[1].longitude(), wayPoints[1].latitude())

            fetchRouteWithBearing(false, profile)
            result.success(true)
        } else {
            result.success(false)
        }
    }

    private fun buildRouteAndStartNavigation(
        methodCall: MethodCall,
        result: MethodChannel.Result
    ) {
        isNavigationCanceled = false
        isNavigationInProgress = false

        val arguments = methodCall.arguments as? Map<*, *>
        if (arguments != null)
            setOptions(arguments)

        if (mapReady) {
            wayPoints.clear()
            var points = arguments?.get("wayPoints") as HashMap<*, *>
            for (item in points) {
                val point = item.value as HashMap<*, *>
                val latitude = point["Latitude"] as Double
                val longitude = point["Longitude"] as Double
                wayPoints.add(Point.fromLngLat(longitude, latitude))
            }

            var profile: String = arguments?.get("profile") as? String? ?: "driving-traffic"
            originPoint = Point.fromLngLat(wayPoints[0].longitude(), wayPoints[0].latitude())
            destinationPoint = Point.fromLngLat(wayPoints[1].longitude(), wayPoints[1].latitude())

            fetchRouteWithBearing(true, profile)
            result.success(true)
        } else {
            result.success(false)
        }
    }

    private fun clearRoute(methodCall: MethodCall, result: MethodChannel.Result) {
        if (navigationMapRoute != null) {
            navigationMapRoute?.removeRoute()
        }
        currentRoute = null
        result.success(true)
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
        tilt = 10000.0
        zoom = 19.0
        isOverviewing = false
        isNavigationCanceled = false
        vietmapGL?.locationComponent?.cameraMode = CameraMode.TRACKING_GPS_NORTH

        if (currentRoute != null) {
            if (simulateRoute) {
                val mockLocationEngine = ReplayRouteLocationEngine()
                mockLocationEngine.assign(currentRoute)
                navigation.locationEngine = mockLocationEngine
            } else {
                locationEngine?.let {
                    navigation.locationEngine = it
                }
            }
            isRunning= true
            vietmapGL?.locationComponent?.locationEngine = null
            navigation.addNavigationEventListener(this)
            navigation.addFasterRouteListener(this)
            navigation.addMilestoneEventListener(this)
            navigation.addOffRouteListener(this)
            navigation.addProgressChangeListener(this)
            navigation.snapEngine = snapEngine
            currentRoute?.let {
                isNavigationInProgress = true
                navigation.startNavigation(currentRoute!!)
                PluginUtilities.sendEvent(VietMapEvents.NAVIGATION_RUNNING)
                recenter()
            }
        }
    }

    private fun finishNavigation(isOffRouted: Boolean = false) {

        zoom = 15.0
        bearing = 0.0
        tilt = 0.0
        isNavigationCanceled = true

//        destinationPoint?.let {
//            moveCamera(LatLng(it.latitude(), it.longitude()),null)
//        }
        if (!isOffRouted) {
            isNavigationInProgress = false
            moveCameraToOriginOfRoute()
        }

        if (currentRoute != null) {
            isRunning = false
            navigation.stopNavigation()
            navigation.removeFasterRouteListener(this)
            navigation.removeMilestoneEventListener(this)
            navigation.removeNavigationEventListener(this)
            navigation.removeOffRouteListener(this)
            navigation.removeProgressChangeListener(this)
        }

    }
    private fun loadImageFromBinary(binaryData: ByteArray): BitmapDrawable {
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(binaryData, 0, binaryData.size)
        return BitmapDrawable(context.resources, bitmap)
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
            simulateRoute = simulated
        }

        val language = arguments["language"] as? String
        if (language != null) navigationLanguage = Locale(language)

        val units = arguments["units"] as? String

        if (units != null) {
            if (units == "imperial") navigationVoiceUnits = DirectionsCriteria.IMPERIAL
            else if (units == "metric") navigationVoiceUnits = DirectionsCriteria.METRIC
        }
        val styleUrl = arguments["mapStyle"] as? String

        if (styleUrl != null&&styleUrl!="") {
            mapStyleURL = styleUrl
        }
        val apik = arguments["apikey"] as? String
        if (apik != null&&apik!="") {
            apikey = apik
        }

        val byteArray = arguments["customLocationCenterIcon"] as? ByteArray
        if(byteArray!=null){
            this.bitmapDrawable = loadImageFromBinary(byteArray)
        }

        initialLatitude = arguments["initialLatitude"] as? Double
        initialLongitude = arguments["initialLongitude"] as? Double

        val zm = arguments["zoom"] as? Double
        if (zm != null) zoom = zm

        val br = arguments["bearing"] as? Double
        if (br != null) bearing = br

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
        if(voiceEnabled != null) {
            voiceInstructionsEnabled = voiceEnabled
            speechPlayer?.let {

                speechPlayer!!.isMuted = voiceEnabled
            }
        }

        val bannerEnabled = arguments["bannerInstructionsEnabled"] as? Boolean
        if(bannerEnabled != null)
            bannerInstructionsEnabled = bannerEnabled

        var longPress = arguments["longPressDestinationEnabled"] as? Boolean
        if(longPress != null)
            longPressDestinationEnabled = longPress
    }

    override fun onMapReady(map: VietMapGL) {
        this.mapReady = true
        this.vietmapGL = map
        if (simulateRoute) {
            locationEngine = ReplayRouteLocationEngine()
        }
        vietmapGL?.setStyle(mapStyleURL) { style ->
            context.addDestinationIconSymbolLayer(style)
            val routeLineLayer = LineLayer("line-layer-id", "source-id")
            routeLineLayer.setProperties(
                lineWidth(9f),
                lineColor(Color.RED),
                lineCap(LINE_CAP_ROUND),
                lineJoin(LINE_JOIN_ROUND)
            )
            style.addLayer(routeLineLayer)
            vietmapGL?.addOnMoveListener(object : OnMoveListener {
                override fun onMoveBegin(moveGestureDetector: MoveGestureDetector) {
                    isOverviewing = true
                    PluginUtilities.sendEvent(VietMapEvents.ON_MAP_MOVE)
                }

                override fun onMove(moveGestureDetector: MoveGestureDetector) {}
                override fun onMoveEnd(moveGestureDetector: MoveGestureDetector) {
                    PluginUtilities.sendEvent(VietMapEvents.ON_MAP_MOVE_END)
                }
            })


            enableLocationComponent(style)
            initMapRoute()
        }

        if (longPressDestinationEnabled)
            vietmapGL?.addOnMapLongClickListener(this)


//        markerViewManager = MarkerViewManager(mapView, vietmapGL)

        if (initialLatitude != null && initialLongitude != null)
            moveCamera(LatLng(initialLatitude!!, initialLongitude!!), null)

        PluginUtilities.sendEvent(VietMapEvents.MAP_READY)
    }

    private fun initMapRoute() {
        if (vietmapGL != null) {
            navigationMapRoute = NavigationMapRoute(mapView, vietmapGL!!)
        }

        navigationMapRoute?.setOnRouteSelectionChangeListener {
            routeClicked = true

            currentRoute = it
            if(isRunning){
                finishNavigation(isOffRouted = true)
                startNavigation()
            }
            PluginUtilities.sendEvent(
                VietMapEvents.ON_NEW_ROUTE_SELECTED,
                it.toJson()
            )
        }

        vietmapGL?.addOnMapClickListener(this)
    }
    override fun onMapLongClick(point: LatLng): Boolean {
        if (wayPoints.size === 2) {
            wayPoints.clear()
        }
        PluginUtilities.sendEvent(
            VietMapEvents.ON_MAP_LONG_CLICK,
            "{\"latitude\":${point.latitude},\"longitude\":${point.longitude}}"
        )
        return false
    }

    fun Context.addDestinationIconSymbolLayer(loadedMapStyle: Style) {
        val geoJsonSource = GeoJsonSource("destination-source-id")
        loadedMapStyle.addSource(geoJsonSource)
        val destinationSymbolLayer = SymbolLayer("destination-symbol-layer-id", "destination-source-id")
        destinationSymbolLayer.withProperties(
            iconImage("destination-icon-id"),
            iconAllowOverlap(true),
            iconIgnorePlacement(true)
        )
        loadedMapStyle.addLayer(destinationSymbolLayer)
    }

    private fun moveCamera(location: LatLng, bearing: Float?) {

        val cameraPosition = CameraPosition.Builder()
            .target(location)
            .zoom(zoom)
            .tilt(tilt)

        if(bearing!=null){
            cameraPosition.bearing(bearing.toDouble())
        }

        var duration = 3000
        if (!animateBuildRoute)
            duration = 1
        vietmapGL?.animateCamera(
            CameraUpdateFactory
                .newCameraPosition(cameraPosition.build()), duration
        )
    }

    private fun moveCamera(location: LatLng, bearing: Float?, tilt: Double, zoom: Double) {

        val cameraPosition = CameraPosition.Builder()
            .target(location)
            .zoom(zoom)
            .tilt(tilt)

        if (bearing != null) {
            cameraPosition.bearing(bearing.toDouble())
        }

        var duration = 3000
        if (!animateBuildRoute)
            duration = 1
        vietmapGL?.animateCamera(
            CameraUpdateFactory
                .newCameraPosition(cameraPosition.build()), duration
        )
    }

    private fun addCustomMarker(
        location: LatLng,
        @DrawableRes markerIcon: Int,
        rotationFrom: Double? = null,
        rotationTo: Double? = null
    ) {
        val markerView = ImageView(context)
        markerView.setImageResource(markerIcon)
        markerView.layoutParams = ViewGroup.LayoutParams(100, 100)

        rotationTo?.let {
            markerView.rotation = rotationTo.toFloat()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            markerView.elevation = 16.0f
        }
    }

    private fun addLocationMarker(location: LatLng, @DrawableRes markerIcon: Int) {

        val markerView = ImageView(context)
        markerView.setImageResource(markerIcon)
        markerView.layoutParams = ViewGroup.LayoutParams(100, 100)


    }

        private fun buildRoute(point: LatLng) {
            val lastLocation = vietmapGL?.locationComponent?.lastKnownLocation
            if (lastLocation?.longitude != null) {
                wayPoints.add(
                    Point.fromLngLat(lastLocation.longitude, lastLocation.latitude)

                )

                originPoint = Point.fromLngLat(lastLocation.longitude, lastLocation.latitude)
            }
            wayPoints.add(Point.fromLngLat(point.longitude, point.latitude))

            destinationPoint = Point.fromLngLat(point.longitude, point.latitude)
            fetchRouteWithBearing(false, profile)
        }

    private fun getRoute(
        context: Context,
        isStartNavigation: Boolean,
        bearing: Float?,
        profile: String
    ) {

        if (!PluginUtilities.isNetworkAvailable(context)) {
            PluginUtilities.sendEvent(
                VietMapEvents.ROUTE_BUILD_FAILED,
                "No Internet Connection"
            )
            return
        }

        PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILDING)
        val br = bearing ?: 0.0
        val builder = NavigationRoute.builder(activity).apikey(apikey ?: "")
            .origin(originPoint!!, 60.0, br.toDouble())
            .destination(destinationPoint!!)
            .alternatives(true)
            ///driving-traffic
            ///cycling
            ///walking
            ///motorcycle
            .profile(profile)
            .build()
        builder.getRoute(object : Callback<DirectionsResponse> {
            override fun onResponse(
                call: Call<DirectionsResponse?>,
                response: Response<DirectionsResponse?>
            ) {
                if (response.body() == null || response.body()!!.routes().size < 1) {
                    PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILD_FAILED, "No routes found")
                    return
                }

                currentRoute = response.body()!!.routes()[0]
                val data = currentRoute?.toJson()
                PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILT, "${currentRoute?.toJson()}")
//                moveCameraToOriginOfRoute()

                // Draw the route on the map
                if (navigationMapRoute != null) {
                    navigationMapRoute?.removeRoute()
                } else {
                    navigationMapRoute = NavigationMapRoute(mapView, vietmapGL!!)
                }
                //show multiple route to map
                if (response.body()!!.routes().size > 1) {
                    navigationMapRoute?.addRoutes(response.body()!!.routes())
                } else {
                    navigationMapRoute?.addRoute(currentRoute)
                }


                isBuildingRoute = false
                overViewRoute()
                //Start Navigation again from new Point, if it was already in Progress
                if (isNavigationInProgress||isStartNavigation) {
                    startNavigation()
                }
            }

            override fun onFailure(call: Call<DirectionsResponse?>, throwable: Throwable) {
                isBuildingRoute = false
                PluginUtilities.sendEvent(
                    VietMapEvents.ROUTE_BUILD_FAILED,
                    "${throwable.message?.replace("\"", "'")}"
                )
            }
        })
    }
    private fun moveCameraToOriginOfRoute() {
        currentRoute?.let {
            try {
                val originCoordinate = it.routeOptions()?.coordinates()?.get(0)
                originCoordinate?.let {
                    val location = LatLng(originCoordinate.latitude(), originCoordinate.longitude())
                    moveCamera(location,null)
                }
            }catch (e:java.lang.Exception){
                   Timber.i(String.format("moveCameraToOriginOfRoute, %s", "Error: ${e.message}"))
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)

    }

    override fun onActivityStarted(activity: Activity) {

        try {
            mapView.onStart()
        } catch (e: java.lang.Exception) {
            Timber.i(String.format("onActivityStarted, %s", "Error: ${e.message}"))
        }
    }

    override fun onActivityResumed(activity: Activity) {
        mapView.onResume()
    }

    override fun onActivityPaused(activity: Activity) {
        mapView.onPause()
    }

    override fun onActivityStopped(activity: Activity) {
        mapView.onStop()
    }

    override fun onActivitySaveInstanceState(@NonNull p0: Activity, @NonNull outState: Bundle) {
        mapView.onSaveInstanceState(outState)
    }

    override fun onActivityDestroyed(activity: Activity) {
        mapView.onDestroy()

        speechPlayer!!.onDestroy()
    }


    override fun onProgressChange(location: Location, routeProgress: RouteProgress) {

        if (!isNavigationCanceled) {
            try {
                distanceRemaining = routeProgress.distanceRemaining()
                durationRemaining = routeProgress.durationRemaining()

                val progressEvent = VietMapRouteProgressEvent(routeProgress)
                PluginUtilities.sendEvent(progressEvent)
//                addCustomMarker(
//                    LatLng(location.latitude, location.longitude),
//                    R.drawable.vietmap_marker_icon_default
//                )

                currentCenterPoint =
                    CurrentCenterPoint(location.latitude, location.longitude, location.bearing)

                if (!isOverviewing) {
                    this.routeProgress = routeProgress
                    moveCamera(LatLng(location.latitude, location.longitude), location.bearing)
                }

                if(!isDisposed && !isBuildingRoute) {
                    val snappedLocation: Location =
                        snapEngine.getSnappedLocation(location, routeProgress)
                    vietmapGL?.locationComponent?.forceLocationUpdate(snappedLocation)
                }

                if (simulateRoute && !isDisposed && !isBuildingRoute) {
                    vietmapGL?.locationComponent?.forceLocationUpdate(location)
                }

                if (!isRefreshing) {
                    isRefreshing = true
                }
            } catch (e: java.lang.Exception) {
            }
        }
    }

    override fun userOffRoute(location: Location) {
        speechPlayer!!.onOffRoute()
        PluginUtilities.sendEvent(
            VietMapEvents.USER_OFF_ROUTE,
            "{\"latitude\":${location.latitude},\"longitude\":${location.longitude}}"
        )
        doOnNewRoute(Point.fromLngLat(location.longitude, location.latitude))
    }

    override fun onMilestoneEvent(routeProgress: RouteProgress, instruction: String, milestone: Milestone) {
        if(voiceInstructionsEnabled){
            playVoiceAnnouncement(milestone)
        }
        if (routeUtils.isArrivalEvent(routeProgress, milestone) && isNavigationInProgress) {
            vietmapGL?.locationComponent?.locationEngine = locationEngine
            PluginUtilities.sendEvent(VietMapEvents.ON_ARRIVAL)

            vietmapGL?.locationComponent?.locationEngine = locationEngine
            finishNavigation()
        }
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
        navigation.stopNavigation()
        isRunning= false
    }

    override fun onNavigationFinished() {
        vietmapGL?.locationComponent?.locationEngine = locationEngine
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
        vietmapGL?.locationComponent?.locationEngine = locationEngine
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

                moveCamera(LatLng(it.latitude(), it.longitude()), null)

                PluginUtilities.sendEvent(
                    VietMapEvents.USER_OFF_ROUTE,
                    VietMapLocation(
                        latitude = it.latitude(),
                        longitude = it.longitude()
                    ).toString()
                )

            }

            PluginUtilities.sendEvent(
                VietMapEvents.USER_OFF_ROUTE,
                VietMapLocation(
                    latitude = offRoutePoint?.latitude(),
                    longitude = offRoutePoint?.longitude()
                ).toString()
            )

            originPoint = offRoutePoint
            isNavigationInProgress = true
            fetchRouteWithBearing(false, profile)
        }
    }

    private fun fetchRouteWithBearing(isStartNavigation: Boolean, profile: String) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient?.lastLocation?.addOnSuccessListener(
                activity
            ) { location: Location? ->
                if (location != null) {
                    getRoute(context, isStartNavigation, location.bearing, profile)
                }
            }
        } else {
            getRoute(context, isStartNavigation, null, profile)
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
//                .backgroundDrawable()


                .build()
            vietmapGL?.locationComponent?.let { locationComponent ->
                locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, loadedMapStyle)
                        .locationComponentOptions(customLocationComponentOptions)
                        .locationEngine(locationEngine)
                        .build()
                )

                locationComponent.setCameraMode(
                    CameraMode.TRACKING_GPS_NORTH,
                    750L,
                    zoom,
                    locationComponent.lastKnownLocation?.bearing?.toDouble() ?: 0.0,
                    tilt,
                    null
                )
                locationComponent.zoomWhileTracking(18.0)
                locationComponent.renderMode = RenderMode.GPS
                locationComponent.locationEngine = locationEngine

                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }

                locationComponent.isLocationComponentEnabled = true
            }

        }
    }

    override fun dispose() {
        isDisposed = true
        mapReady = false
        if(voiceInstructionsEnabled) {
            try {
                speechPlayer?.onDestroy()
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
        if (isNavigationInProgress) {
            finishNavigation()
        }
        navigation.onDestroy()

        mapView.onStop()
        mapView.onDestroy()
    }

        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            VietMapNavigationPlugin.eventSink = events
        }

        override fun onCancel(arguments: Any?) {
            VietMapNavigationPlugin.eventSink = null
        }

        override fun onMapClick(point: LatLng): Boolean {
            if(routeClicked){
                routeClicked = false
                return true
            }

            PluginUtilities.sendEvent(
                VietMapEvents.ON_MAP_CLICK,
                "{\"latitude\":${point.latitude},\"longitude\":${point.longitude}}"
            )
            return true
        }

    override fun onDidFinishRenderingMap(p0: Boolean) {
        TODO("Not yet implemented")
    }

    private fun buildRouteInformationFromProgress(routeProgress: RouteProgress?): RouteInformation {
        return if (routeProgress == null) {
            RouteInformation.create(null, null, null)
        } else RouteInformation.create(routeProgress.directionsRoute(), null, null)
    }
    private fun showRouteOverview(padding: IntArray?, currentRouteProgress: RouteProgress){

        val routeInformation: RouteInformation =
            buildRouteInformationFromProgress(currentRouteProgress)
        animateCameraForRouteOverview(routeInformation, padding!!)
    }
    private fun animateCameraForRouteOverview(
        routeInformation: RouteInformation,
        padding: IntArray
    ) {
        val cameraEngine = navigation.cameraEngine
        val routePoints = cameraEngine.overview(routeInformation)
        if (!routePoints.isEmpty()) {
            animateVietmapGLForRouteOverview(padding, routePoints)
        }
    }

    private fun animateVietmapGLForRouteOverview(padding: IntArray, routePoints: List<Point>) {
        if (routePoints.size <= 1) {
            return
        }
        val resetUpdate: CameraUpdate = buildResetCameraUpdate()
        val overviewUpdate: CameraUpdate = buildOverviewCameraUpdate(padding, routePoints)
        vietmapGL?.animateCamera(
            resetUpdate, 150,
            CameraOverviewCancelableCallback(overviewUpdate, vietmapGL)
        )
    }

    private fun buildResetCameraUpdate(): CameraUpdate {
        val resetPosition: CameraPosition = CameraPosition.Builder().tilt(0.0).bearing(0.0).build()
        return CameraUpdateFactory.newCameraPosition(resetPosition)
    }

    private fun buildOverviewCameraUpdate(
        padding: IntArray,
        routePoints: List<Point>
    ): CameraUpdate {
        val routeBounds = convertRoutePointsToLatLngBounds(routePoints)
        return newLatLngBounds(
            routeBounds, padding[0], padding[1], padding[2], padding[3]
        )
    }

    private fun convertRoutePointsToLatLngBounds(routePoints: List<Point>): LatLngBounds {
        val latLngs: MutableList<LatLng> = ArrayList()
        for (routePoint in routePoints) {
            latLngs.add(LatLng(routePoint.latitude(), routePoint.longitude()))
        }
        return LatLngBounds.Builder()
            .includes(latLngs)
            .build()
    }
private  fun setCenterIcon(){
//    var locationComponentOptions = vietmapGL?.locationComponent?.locationComponentOptions
////    locationComponentOptions?.toBuilder()?.backgroundDrawable(bitmapDrawable)
//    val locationLayerController : LocationLayerController?  = vietmapGL?.locationComponent?.locationLayerController;
//    val locationLayerRenderer:LocationLayerRenderer? = locationLayerController?.getLocationLayerRenderer()
}
}