package vn.vietmap.factory

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMisc
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
import vn.vietmap.navigation_plugin.LifecycleProvider
import vn.vietmap.navigation_plugin.VietMapNavigationPlugin
import vn.vietmap.services.android.navigation.ui.v5.ThemeSwitcher
import vn.vietmap.services.android.navigation.ui.v5.camera.CameraOverviewCancelableCallback
import vn.vietmap.services.android.navigation.ui.v5.listeners.BannerInstructionsListener
import vn.vietmap.services.android.navigation.ui.v5.listeners.NavigationListener
import vn.vietmap.services.android.navigation.ui.v5.listeners.RouteListener
import vn.vietmap.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener
import vn.vietmap.services.android.navigation.ui.v5.route.NavigationMapRoute
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
import vn.vietmap.vietmapsdk.annotations.IconFactory
import vn.vietmap.vietmapsdk.annotations.Marker
import vn.vietmap.vietmapsdk.annotations.MarkerOptions
import vn.vietmap.vietmapsdk.camera.CameraPosition
import vn.vietmap.vietmapsdk.camera.CameraUpdate
import vn.vietmap.vietmapsdk.camera.CameraUpdateFactory
import vn.vietmap.vietmapsdk.camera.CameraUpdateFactory.newLatLngBounds
import vn.vietmap.vietmapsdk.geometry.LatLng
import vn.vietmap.vietmapsdk.geometry.LatLngBounds
import vn.vietmap.vietmapsdk.location.LocationComponentActivationOptions
import vn.vietmap.vietmapsdk.location.LocationComponentOptions
import vn.vietmap.vietmapsdk.location.engine.LocationEngine
import vn.vietmap.vietmapsdk.location.modes.CameraMode
import vn.vietmap.vietmapsdk.location.modes.RenderMode
import vn.vietmap.vietmapsdk.maps.*
import vn.vietmap.vietmapsdk.maps.VietMapGL.OnMoveListener
import vn.vietmap.vietmapsdk.style.expressions.Expression
import vn.vietmap.vietmapsdk.style.layers.LineLayer
import vn.vietmap.vietmapsdk.style.layers.Property.LINE_CAP_ROUND
import vn.vietmap.vietmapsdk.style.layers.Property.LINE_JOIN_ROUND
import vn.vietmap.vietmapsdk.style.layers.PropertyFactory.*
import vn.vietmap.vietmapsdk.style.layers.SymbolLayer
import vn.vietmap.vietmapsdk.style.sources.GeoJsonSource
import java.util.*
import kotlin.math.*


class FlutterMapViewFactory : PlatformView, MethodCallHandler, OnMapReadyCallback,
    ProgressChangeListener,
    OffRouteListener, MilestoneEventListener, NavigationEventListener, NavigationListener,
    FasterRouteListener, SpeechAnnouncementListener, BannerInstructionsListener, RouteListener,
    EventChannel.StreamHandler, VietMapGL.OnMapLongClickListener, VietMapGL.OnMapClickListener,
    DefaultLifecycleObserver {

    private var activity: Activity? = null
    private val context: Context
    private val methodChannel: MethodChannel
    private val eventChannel: EventChannel
    private val options: VietMapGLOptions
    private var mapView: MapView? = null
    private var vietmapGL: VietMapGL? = null
    private var currentRoute: DirectionsRoute? = null
    private var routeClicked: Boolean = false
    private var locationEngine: LocationEngine? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var directionsRoutes: List<DirectionsRoute>? = null
    private var distanceToOffRoute = 30 //distance in meter
    private val navigationOptions =
        VietmapNavigationOptions.builder().maxTurnCompletionOffset(30.0).maneuverZoneRadius(40.0)
            .maximumDistanceOffRoute(50.0).deadReckoningTimeInterval(5.0)
            .maxManipulatedCourseAngle(25.0).userLocationSnapDistance(20.0).secondsBeforeReroute(3)
            .enableOffRouteDetection(true).enableFasterRouteDetection(false).snapToRoute(false)
            .manuallyEndNavigationUponCompletion(false).defaultMilestonesEnabled(true)
            .minimumDistanceBeforeRerouting(10.0).metersRemainingTillArrival(20.0)
            .isFromNavigationUi(false).isDebugLoggingEnabled(false)
            .roundingIncrement(NavigationConstants.ROUNDING_INCREMENT_FIFTY)
            .timeFormatType(NavigationTimeFormat.NONE_SPECIFIED)
            .locationAcceptableAccuracyInMetersThreshold(100).build()
    private var navigation: VietmapNavigation? = null
    private var mapReady = false
    private var isDisposed = false
    private var isRefreshing = false
    private var isBuildingRoute = false
    private var isNavigationInProgress = false
    private var isNavigationCanceled = false
    private var isOverviewing = false
    private var isNextTurnHandling = false
    private var currentCenterPoint: CurrentCenterPoint? = null
    private var routeUtils = RouteUtils()
    private val snapEngine = SnapToRoute()
    private var apikey: String? = null
    private var speechPlayer: SpeechPlayer? = null
    private var routeProgress: RouteProgress? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var bitmapDrawable: BitmapDrawable? = null
    private var maxDifference: Double = 60.0
    private var primaryRouteIndex = 0
    private var mapReadyResult: MethodChannel.Result? = null
    private val listMarkers: ArrayList<Marker> = ArrayList()
    private var lifecycleProvider: LifecycleProvider? = null

    constructor(
        cxt: Context,
        messenger: BinaryMessenger,
        viewId: Int,
        args: Any?,
        lifecycle: LifecycleProvider,
        activity: Activity?,
    ) {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            this.activity = VietMapNavigationPlugin.currentActivity!!
        } else {
            if (activity != null) {
                this.activity = activity
            }
        }
        context = cxt
        val arguments = args as? Map<*, *>
        if (arguments != null) setOptions(arguments)

        lifecycleProvider = lifecycle
        methodChannel = MethodChannel(messenger, "navigation_plugin/${viewId}")
        eventChannel = EventChannel(messenger, "navigation_plugin/${viewId}/events")
        eventChannel.setStreamHandler(this)

        fusedLocationClient = this.activity?.let { LocationServices.getFusedLocationProviderClient(it) }
        options =
            VietMapGLOptions.createFromAttributes(context).compassEnabled(false).logoEnabled(true)

    }

    companion object {

        private var disposed = false

        //Config
        var isMapViewStarted: Boolean = false
        var initialLatitude: Double? = null
        var initialLongitude: Double? = null
        var profile: String = "driving-traffic"
        val wayPoints: MutableList<Point> = mutableListOf()
        var navigationMode = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
        var simulateRoute = false
        var mapStyleURL: String? = null
        var navigationLanguage = Locale("vi")
        var navigationVoiceUnits = DirectionsCriteria.IMPERIAL
        var zoom = 20.0
        var bearing = 0.0
        var tilt = 0.0
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
        var isRunning: Boolean = false

        var padding: IntArray = intArrayOf(150, 500, 150, 500)

    }


    private fun playVoiceAnnouncement(milestone: Milestone?) {
        if (milestone is VoiceInstructionMilestone) {
            var announcement = SpeechAnnouncement.builder()
                .voiceInstructionMilestone(milestone as VoiceInstructionMilestone?).build()
            speechPlayer!!.play(announcement)
        }
    }

    private fun configSpeechPlayer() {
        var speechPlayerProvider = SpeechPlayerProvider(context, "vi", true)
        this.speechPlayer = NavigationSpeechPlayer(speechPlayerProvider)
    }

    override fun getView(): View {
        return mapView!!
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
                clearRoute(result)
            }

            "startNavigation" -> {
                startNavigation(methodCall, result)
            }

            "setCenterIcon" -> {

                val arguments = methodCall.arguments as? Map<*, *>
                val byteArray = arguments?.get("customLocationCenterIcon") as? ByteArray
                if (byteArray != null) {
                    this.bitmapDrawable = loadImageFromBinary(byteArray)
                }
            }

            "finishNavigation" -> {
                finishNavigation(result)
            }

            "getDistanceRemaining" -> {
                result.success(distanceRemaining)
            }

            "addMarkerGroup" -> {
                addMarkerGroup(methodCall, result)
            }

            "addMarkers" -> {
                addMarker(methodCall, result)
            }

            "removeMarkers" -> {
                removeMarker(methodCall, result)
            }

            "removeAllMarkers" -> {
                removeAllMarkers(methodCall, result)
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
                voiceInstructionsEnabled =
                    methodCall.argument<Boolean>("isMute") ?: !voiceInstructionsEnabled
                speechPlayer?.let {
                    speechPlayer!!.isMuted = methodCall.argument<Boolean>("isMute") ?: false
                    result.success(speechPlayer!!.isMuted)
                }
            }

            "queryRenderedFeatures" -> {

                val reply: MutableMap<String, Any> = HashMap()
                val features: List<Feature>
                val layerIds = (methodCall.argument<List<String>>("layerIds")
                    ?: listOf<String>()).toTypedArray<String>()
                var jsonElement: JsonElement? = null
                if (methodCall.argument<List<Any>>("filter") != null) {
                    val filter: List<Any> = methodCall.argument<List<Any>>("filter")!!
                    jsonElement = if (filter == null) null else Gson().toJsonTree(filter)
                }
                var jsonArray: JsonArray? = null
                if (jsonElement != null && jsonElement.isJsonArray) {
                    jsonArray = jsonElement.asJsonArray
                }
                val filterExpression =
                    if (jsonArray == null) null else Expression.Converter.convert(jsonArray)
                features = if (methodCall.hasArgument("x")) {
                    val x: Double = methodCall.argument("x")!!
                    val y: Double = methodCall.argument("y")!!
                    val pixel = PointF(x.toFloat(), y.toFloat())
                    vietmapGL!!.queryRenderedFeatures(pixel, filterExpression, *layerIds)
                } else {
                    val left: Double = methodCall.argument("left")!!
                    val top: Double = methodCall.argument("top")!!
                    val right: Double = methodCall.argument("right")!!
                    val bottom: Double = methodCall.argument("bottom")!!
                    val rectF = RectF(
                        left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat()
                    )
                    vietmapGL!!.queryRenderedFeatures(rectF, filterExpression, *layerIds)
                }
                val featuresJson: MutableList<String> = ArrayList()
                for (feature in features) {
                    featuresJson.add(feature.toJson())
                }
                reply["features"] = featuresJson
                result.success(reply)
            }
            "animateCamera" -> {
                val location = LatLng(
                    methodCall.argument("latitude")!! ,
                    methodCall.argument("longitude")!!
                )

                PluginUtilities.sendEvent(VietMapEvents.ON_MAP_MOVE)
                isOverviewing = true
                 animateCamera(location, methodCall.argument("bearing") as? Float?, methodCall.argument("duration")?:1000,methodCall.argument("zoom") as? Double?,methodCall.argument("tilt") as? Double?)
            }
            "moveCamera" -> {
                val location = LatLng(
                    methodCall.argument("latitude")!!,
                    methodCall.argument("longitude")!!
                )

                PluginUtilities.sendEvent(VietMapEvents.ON_MAP_MOVE)
                isOverviewing = true
                moveCameraWithoutAnimation(location, methodCall.argument("bearing") as? Float?, methodCall.argument("zoom") as? Double?, methodCall.argument("tilt") as? Double?)
            } 
            "onDispose" -> {
                try {
                    isDisposed = true
                    mapReady = false
                    mapView?.onStop()
                    navigation?.onDestroy()
                    mapView?.onDestroy()
                    result.success(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    result.success(false)
                }
            }

            "map#toScreenLocationBatch" -> {
                val param: DoubleArray? = methodCall.argument("coordinates") as DoubleArray?
                val reply = DoubleArray(param?.size ?: 0)
                if (param == null || reply == null) return
                var i = 0
                while (i < param.size) {
                    val latLng: LatLng = LatLng(param!![i], param!![i + 1])
                    val pointf = vietmapGL!!.projection.toScreenLocation(latLng)
                    reply[i] = pointf.x.toDouble()
                    reply[i + 1] = pointf.y.toDouble()
                    i += 2
                }
                // println(reply)
                result.success(reply)
            }

            "map#toScreenLocation" -> {

                val reply: MutableMap<String, Any> = HashMap()
                val pointf = vietmapGL!!.projection.toScreenLocation(
                    LatLng(
                        methodCall.argument("latitude")!!, methodCall.argument("longitude")!!
                    )
                )
                reply["x"] = pointf.x
                reply["y"] = pointf.y
                result.success(reply)
            }

            "map#toLatLng" -> {

                val reply: MutableMap<String, Any> = HashMap()
                val x: Double = methodCall.argument("x")!!
                val y: Double = methodCall.argument("y")!!
                val latlng = vietmapGL!!.projection.fromScreenLocation(
                    PointF(
                        x.toFloat(), y.toFloat()
                    )
                )
                reply["latitude"] = latlng.latitude
                reply["longitude"] = latlng.longitude
                result.success(reply)
            }

            "map#waitForMap" -> {

                if (vietmapGL != null) {
                    result.success(null)
                    return
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
        routeProgress?.let { showRouteOverview(padding, it) }
    }


    private fun buildRoute(methodCall: MethodCall, result: MethodChannel.Result) {
        isNavigationCanceled = false
        isNavigationInProgress = false

        val arguments = methodCall.arguments as? Map<*, *>
        if (arguments != null) setOptions(arguments)

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
        methodCall: MethodCall, result: MethodChannel.Result,
    ) {
        isNavigationCanceled = false
        isNavigationInProgress = false

        val arguments = methodCall.arguments as? Map<*, *>
        if (arguments != null) setOptions(arguments)

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

    private fun clearRoute(result: MethodChannel.Result) {
        if (navigationMapRoute != null) {
            navigationMapRoute?.removeRoute()
        }
        currentRoute = null
        result.success(true)
        PluginUtilities.sendEvent(VietMapEvents.NAVIGATION_CANCELLED)
    }

    private fun startNavigation(methodCall: MethodCall, result: MethodChannel.Result) {

        val arguments = methodCall.arguments as? Map<*, *>
        if (arguments != null) setOptions(arguments)

        startNavigation()

        if (currentRoute != null) {
            result.success(true)
        } else {
            result.success(false)
        }
    }

    private fun finishNavigation(result: MethodChannel.Result) {
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
                navigation?.locationEngine = mockLocationEngine
            } else {
                locationEngine?.let {
                    navigation?.locationEngine = it
                }
            }
            isRunning = true
            vietmapGL?.locationComponent?.locationEngine = null
            navigation?.addNavigationEventListener(this)
            navigation?.addFasterRouteListener(this)
            navigation?.addMilestoneEventListener(this)
            navigation?.addOffRouteListener(this)
            navigation?.addProgressChangeListener(this)
            navigation?.snapEngine = snapEngine

            navigationMapRoute!!.updateRouteArrowVisibilityTo(true)
            navigationMapRoute!!.showAlternativeRoutes(true)
            navigationMapRoute!!.updateRouteVisibilityTo(true)
            navigationMapRoute!!.showAlternativeRoutes(true)
            currentRoute?.let {
                isNavigationInProgress = true
                navigation?.startNavigation(currentRoute!!)
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

        if (!isOffRouted) {
            isNavigationInProgress = false
            moveCameraToOriginOfRoute()
        }

        if (currentRoute != null) {
            isRunning = false
            navigation?.stopNavigation()
            navigation?.removeFasterRouteListener(this)
            navigation?.removeMilestoneEventListener(this)
            navigation?.removeNavigationEventListener(this)
            navigation?.removeOffRouteListener(this)
            navigation?.removeProgressChangeListener(this)
        }

    }

    private fun loadImageFromBinary(binaryData: ByteArray): BitmapDrawable {
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(binaryData, 0, binaryData.size)
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun setOptions(arguments: Map<*, *>) {
        val navMode = arguments["mode"] as? String
        if (navMode != null) {
            profile = navMode
            when (navMode) {
                "walking" -> {
                    navigationMode = DirectionsCriteria.PROFILE_WALKING
                }

                "cycling" -> {
                    navigationMode = DirectionsCriteria.PROFILE_CYCLING
                }

                "driving" -> {
                    navigationMode = DirectionsCriteria.PROFILE_DRIVING
                }

            }
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

        if (styleUrl != null && styleUrl != "") {
            mapStyleURL = styleUrl
        }
        val apik = arguments["apikey"] as? String
        if (apik != null && apik != "") {
            apikey = apik
        }


        val byteArray = arguments["customLocationCenterIcon"] as? ByteArray
        if (byteArray != null) {
            this.bitmapDrawable = loadImageFromBinary(byteArray)
        }

        initialLatitude = arguments["initialLatitude"] as? Double
        initialLongitude = arguments["initialLongitude"] as? Double

        val zm = arguments["zoom"] as? Double
        if (zm != null) zoom = zm

        val br = arguments["bearing"] as? Double
        if (br != null) bearing = br

        val tt = arguments["tilt"] as? Double
        if (tt != null) tilt = tt

        val optim = arguments["isOptimized"] as? Boolean
        if (optim != null) isOptimized = optim

        val anim = arguments["animateBuildRoute"] as? Boolean
        if (anim != null) animateBuildRoute = anim

        val altRoute = arguments["alternatives"] as? Boolean
        if (altRoute != null) alternatives = altRoute

        val voiceEnabled = arguments["voiceInstructionsEnabled"] as? Boolean
        if (voiceEnabled != null) {
            voiceInstructionsEnabled = voiceEnabled
            speechPlayer?.let {

                speechPlayer!!.isMuted = voiceEnabled
            }
        }

        val bannerEnabled = arguments["bannerInstructionsEnabled"] as? Boolean
        if (bannerEnabled != null) bannerInstructionsEnabled = bannerEnabled

        var longPress = arguments["longPressDestinationEnabled"] as? Boolean
        if (longPress != null) longPressDestinationEnabled = longPress
    }

    override fun onMapReady(map: VietMapGL) {
        if (mapReadyResult != null) {
            mapReadyResult!!.success(null)
            mapReadyResult = null
        }
        this.mapReady = true
        this.vietmapGL = map
        if (simulateRoute) {
            locationEngine = ReplayRouteLocationEngine()
        }
        vietmapGL?.setStyle(mapStyleURL?.let { Style.Builder().fromUri(it) }) { style ->
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


            val routeLineLayer = LineLayer("line-layer-id", "source-id")
            routeLineLayer.setProperties(
                lineWidth(9f),
                lineColor(Color.RED),
                lineCap(LINE_CAP_ROUND),
                lineJoin(LINE_JOIN_ROUND)
            )
            style.addLayer(routeLineLayer)
            enableLocationComponent(style)
            initMapRoute()

            context.addDestinationIconSymbolLayer(style)
        }

        if (longPressDestinationEnabled) vietmapGL?.addOnMapLongClickListener(this)



        if (initialLatitude != null && initialLongitude != null) {
            // println("MoveCamera5")

            moveCamera(

                LatLng(
                    initialLatitude!!,
                    initialLongitude!!
                ), null
            )
        }
        vietmapGL!!.setOnMarkerClickListener { marker ->
            PluginUtilities.sendEvent(VietMapEvents.MARKER_CLICKED, "{\'markerId\':${marker.id}}")
            return@setOnMarkerClickListener true
        }
        PluginUtilities.sendEvent(VietMapEvents.MAP_READY)
    }

    private fun initMapRoute() {
        if (vietmapGL != null) {
            val routeStyleRes = ThemeSwitcher.retrieveNavigationViewStyle(
                        mapView!!.context,
                vn.vietmap.services.android.navigation.R.attr.navigationViewRouteStyle
                    )
            navigationMapRoute =
                        NavigationMapRoute(
                            navigation,
                            mapView!!,
                            vietmapGL!!,
                            routeStyleRes,
                            "vmadmin_province"
                        )
        }

        navigationMapRoute?.setOnRouteSelectionChangeListener {
            routeClicked = true

            currentRoute = it

            val routePoints: List<Point> =
                currentRoute?.routeOptions()?.coordinates() as List<Point>
            animateVietmapGLForRouteOverview(padding, routePoints)
            primaryRouteIndex = try {
                it.routeIndex()?.toInt() ?: 0
            } catch (e: Exception) {
                0
            }
            if (isRunning) {
                finishNavigation(isOffRouted = true)
                startNavigation()
            }
            PluginUtilities.sendEvent(
                VietMapEvents.ON_NEW_ROUTE_SELECTED, it.toJson()
            )
        }

        vietmapGL?.addOnMapClickListener(this)
    }

    override fun onMapLongClick(point: LatLng): Boolean {
        val pointf = vietmapGL!!.projection.toScreenLocation(point)
        if (wayPoints.size === 2) {
            wayPoints.clear()
        }
        PluginUtilities.sendEvent(
            VietMapEvents.ON_MAP_LONG_CLICK,

            "{\"latitude\":${point.latitude},\"longitude\":${point.longitude},\"x\":${pointf.x},\"y\":${pointf.y}}"
        )
        return false
    }

    private fun Context.addDestinationIconSymbolLayer(loadedMapStyle: Style) {
        val geoJsonSource = GeoJsonSource("destination-source-id")
        loadedMapStyle.addSource(geoJsonSource)

        val destinationSymbolLayer =
            SymbolLayer("destination-symbol-layer-id", "destination-source-id")
        destinationSymbolLayer.withProperties(
            iconImage("destination-icon-id"), iconAllowOverlap(true), iconIgnorePlacement(true)
        )
        loadedMapStyle.addLayer(destinationSymbolLayer)
    }

    private fun moveCamera(location: LatLng, bearing: Float?) {
        // println("Camera is moving")
        val cameraPosition = CameraPosition.Builder().target(location).zoom(zoom).tilt(tilt)

        if (bearing != null) {
            cameraPosition.bearing(bearing.toDouble())
        }

        var duration = 1000
        if (!animateBuildRoute) duration = 1
        vietmapGL?.animateCamera(
            CameraUpdateFactory.newCameraPosition(cameraPosition.build()), duration
        )
    }
    private  fun animateCamera(location: LatLng, bearing: Float?, duration: Int = 1000, zoom: Double? ,tilt: Double?) {
        // println("Camera is moving")
        val cameraPosition = CameraPosition.Builder().target(location)
        zoom?.let {
            cameraPosition.zoom(it)
        }
        tilt?.let {
            cameraPosition.tilt(it)
        }

        if (bearing != null) {
            cameraPosition.bearing(bearing.toDouble())
        }
        vietmapGL?.animateCamera(
            CameraUpdateFactory.newCameraPosition(cameraPosition.build()), duration
        )
    }
    private fun moveCameraWithoutAnimation(location: LatLng, bearing: Float?, zoom: Double? ,tilt: Double?) {
        // println("Camera is moving")

        val cameraPosition = CameraPosition.Builder().target(location)
        zoom?.let {
            cameraPosition.zoom(it)
        }
        tilt?.let {
            cameraPosition.tilt(it)
        }
        if (bearing != null) {
            cameraPosition.bearing(bearing.toDouble())
        }

        vietmapGL?.moveCamera(
            CameraUpdateFactory.newCameraPosition(cameraPosition.build())
        )
    }

    private fun getRoute(
        context: Context, isStartNavigation: Boolean, bearing: Float?, profile: String,
    ) {

        if (!PluginUtilities.isNetworkAvailable(context)) {
            PluginUtilities.sendEvent(
                VietMapEvents.ROUTE_BUILD_FAILED, "No Internet Connection"
            )
            return
        }

        PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILDING)
        val br = bearing ?: 0.0
        val builder = NavigationRoute.builder(activity).apikey(apikey ?: "")
            .origin(originPoint!!, 60.0, br.toDouble()).destination(destinationPoint!!)
            .alternatives(true)
            ///driving-traffic
            ///cycling
            ///walking
            ///motorcycle
            .profile(profile).build()

        builder.getRoute(object : Callback<DirectionsResponse> {
            override fun onResponse(
                call: Call<DirectionsResponse>, response: Response<DirectionsResponse>,
            ) {
                if (response.body() == null || response.body()!!.routes().size < 1) {
                    PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILD_FAILED, "No routes found")
                    return
                }
                directionsRoutes = response.body()!!.routes()
                currentRoute = if (directionsRoutes!!.size <= primaryRouteIndex) {
                    directionsRoutes!![0]
                } else {
                    directionsRoutes!![primaryRouteIndex]
                }

                PluginUtilities.sendEvent(VietMapEvents.ROUTE_BUILT, "${currentRoute?.toJson()}")

                // Draw the route on the map
                if (navigationMapRoute != null) {
                    navigationMapRoute?.removeRoute()
                } else {
                    val routeStyleRes = ThemeSwitcher.retrieveNavigationViewStyle(
                        mapView!!.context,
                        vn.vietmap.services.android.navigation.R.attr.navigationViewRouteStyle
                    )
                    navigationMapRoute =
                        NavigationMapRoute(
                            navigation,
                            mapView!!,
                            vietmapGL!!,
                            routeStyleRes,
                            "vmadmin_province"
                        )
                }

                //show multiple route to map
                if (response.body()!!.routes().size > 1) {
                    navigationMapRoute?.addRoutes(directionsRoutes!!)
                } else {
                    navigationMapRoute?.addRoute(currentRoute)
                }


                isBuildingRoute = false
                // get route point from current route
                val routePoints: List<Point> =
                    currentRoute?.routeOptions()?.coordinates() as List<Point>
                animateVietmapGLForRouteOverview(padding, routePoints)
                //Start Navigation again from new Point, if it was already in Progress
                if (isNavigationInProgress || isStartNavigation) {
                    startNavigation()
                }
            }

            override fun onFailure(call: Call<DirectionsResponse?>, throwable: Throwable) {
                isBuildingRoute = false
                PluginUtilities.sendEvent(
                    VietMapEvents.ROUTE_BUILD_FAILED, "${throwable.message?.replace("\"", "'")}"
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
                    // println("MoveCamera1")
                    moveCamera(location, null)
                }
            } catch (e: java.lang.Exception) {
                Timber.i(String.format("moveCameraToOriginOfRoute, %s", "Error: ${e.message}"))
            }
        }
    }

//    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
//        mapView?.onCreate(savedInstanceState)
//
//    }


    override fun onCreate(owner: LifecycleOwner) {

        if (disposed) {
            return
        }
        mapView?.onCreate(null)

    }

    override fun onStart(owner: LifecycleOwner) {
        if (disposed) {
            return
        }

//        if (!isMapViewStarted) {
        mapView?.onStart()
        isMapViewStarted = true
//        }
    }

    override fun onResume(owner: LifecycleOwner) {

        if (disposed) {
            return
        }
        mapView?.onResume()

    }

    override fun onPause(owner: LifecycleOwner) {

        if (disposed) {
            return
        }
        mapView?.onPause()
    }

    override fun onStop(owner: LifecycleOwner) {

        if (disposed) {
            return
        }
        mapView?.onStop()
    }


    private fun destroyMapViewIfNecessary() {

        if (mapView == null) {
            return
        }
        mapView?.onStop()
        mapView?.onDestroy()

        mapView = null
    }

    override fun onDestroy(owner: LifecycleOwner) {

        owner.lifecycle.removeObserver(this)
        if (disposed) {
            return
        }
        destroyMapViewIfNecessary()
    }

    fun init() {

        mapView = MapView(context, options)

        lifecycleProvider?.getVietMapLifecycle()?.addObserver(this)

        locationEngine = if (simulateRoute) {
            ReplayRouteLocationEngine()
        } else {
            LocationEngineProvider.getBestLocationEngine(context)
        }
        navigation = VietmapNavigation(
            context, navigationOptions, locationEngine!!
        )
        methodChannel.setMethodCallHandler(this)
        mapView?.getMapAsync(this)
        mapView?.addOnDidFinishRenderingMapListener {
            PluginUtilities.sendEvent(
                VietMapEvents.ON_MAP_RENDERED
            )
        }
        configSpeechPlayer()

    }


    override fun onProgressChange(location: Location, routeProgress: RouteProgress) {

        var currentSpeed = location.speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentSpeed = location.speedAccuracyMetersPerSecond
        }
        if (!isNavigationCanceled) {
            try {
                val noRoutes: Boolean = directionsRoutes?.isEmpty() ?: true

                val newCurrentRoute: Boolean = !routeProgress.directionsRoute()
                    .equals(directionsRoutes?.get(primaryRouteIndex))
                val isANewRoute: Boolean = noRoutes || newCurrentRoute
                if (isANewRoute) {
                } else {

                    distanceRemaining = routeProgress.distanceRemaining()
                    durationRemaining = routeProgress.durationRemaining()


                    if (!isDisposed && !isBuildingRoute) {
                        val snappedLocation: Location =
                            snapEngine.getSnappedLocation(location, routeProgress)

                        val progressEvent =
                            VietMapRouteProgressEvent(routeProgress, location, snappedLocation)
                        PluginUtilities.sendEvent(progressEvent)
                        currentCenterPoint =
                            CurrentCenterPoint(
                                snappedLocation.latitude,
                                snappedLocation.longitude,
                                snappedLocation.bearing
                            )
                        if (!isOverviewing) {
                            this.routeProgress = routeProgress
                            if (currentSpeed > 0) {
                                moveCamera(
                                    LatLng(snappedLocation.latitude, snappedLocation.longitude),
                                    snappedLocation.bearing
                                )
                            }
                        }

                        vietmapGL?.locationComponent?.forceLocationUpdate(snappedLocation)
                    }

//                    if (simulateRoute && !isDisposed && !isBuildingRoute) {
//                        vietmapGL?.locationComponent?.forceLocationUpdate(location)
//                    }

                    if (!isRefreshing) {
                        isRefreshing = true
                    }
                }
                handleProgressChange(routeProgress, location)
            } catch (e: java.lang.Exception) {
            }
        }
    }

    fun snapLocationToClosestLatLng(targetLocation: LatLng, latLngList: List<LatLng>): LatLng? {
        var closestLatLng: LatLng? = null
        var closestDistance = Double.MAX_VALUE

        for (latLng in latLngList) {
            val distance = calculateHaversineDistance(targetLocation, latLng)
            if (distance < closestDistance) {
                closestDistance = distance
                closestLatLng = latLng
            }
        }

        return closestLatLng
    }

    private fun calculateHaversineDistance(point1: LatLng, point2: LatLng): Double {
        val lat1 = Math.toRadians(point1.latitude)
        val lon1 = Math.toRadians(point1.longitude)
        val lat2 = Math.toRadians(point2.latitude)
        val lon2 = Math.toRadians(point2.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        // Radius of the Earth in kilometers (mean value)
        val earthRadius = 6371.0

        return earthRadius * c
    }

    override fun userOffRoute(location: Location) {
        if (checkIfUserOffRoute(location)) {
            speechPlayer!!.onOffRoute()
            PluginUtilities.sendEvent(
                VietMapEvents.USER_OFF_ROUTE,
                "{\"latitude\":${location.latitude},\"longitude\":${location.longitude}}"
            )
            doOnNewRoute(Point.fromLngLat(location.longitude, location.latitude))
        }
    }

    private fun checkIfUserOffRoute(location: Location): Boolean {
        if (routeProgress?.currentStepPoints() != null) {
            val snapLocation: Location = snapEngine.getSnappedLocation(location, routeProgress)
            val distance: Double = calculateDistanceBetween2Point(location, snapLocation)
            return distance > this.distanceToOffRoute && checkIfUserIsDrivingToOtherRoute(location)
//                && areBearingsClose(
//            location.bearing.toDouble(), snapLocation.bearing.toDouble()
//        )
        }
        return false
    }

    private fun checkIfUserIsDrivingToOtherRoute(location: Location): Boolean {
        directionsRoutes?.forEach {
            //get list point
            snapLocationLatLng(
                location,
                it.routeOptions()?.coordinates() as List<Point>
            )?.let { snapLocation ->
                val distance: Double = calculateDistanceBetween2Point(location, snapLocation)
                if (distance < 30) {
                    if (it != currentRoute) {
                        currentRoute = it
                        currentRoute?.toJson()?.let { it1 ->
                            PluginUtilities.sendEvent(
                                VietMapEvents.ON_NEW_ROUTE_SELECTED,
                                it1
                            )
                            return false
                        }
                    }

                }
            }
        }
        return true
    }

    private fun snapLocationLatLng(location: Location, stepCoordinates: List<Point>): Location? {
        val snappedLocation = Location(location)
        val locationToPoint = Point.fromLngLat(location.longitude, location.latitude)
        if (stepCoordinates.size > 1) {
            val feature = TurfMisc.nearestPointOnLine(locationToPoint, stepCoordinates)
            val point = feature.geometry() as Point?
            snappedLocation.longitude = point!!.longitude()
            snappedLocation.latitude = point.latitude()
        }
        return snappedLocation
    }

    private fun calculateDistanceBetween2Point(location1: Location, location2: Location): Double {
        val radius = 6371000.0 // meters

        val dLat = (location2.latitude - location1.latitude) * PI / 180.0
        val dLon = (location2.longitude - location1.longitude) * PI / 180.0

        val a =
            sin(dLat / 2.0) * sin(dLat / 2.0) + cos(location1.latitude * PI / 180.0) * cos(location2.latitude * PI / 180.0) * sin(
                dLon / 2.0
            ) * sin(dLon / 2.0)
        val c = 2.0 * kotlin.math.atan2(sqrt(a), sqrt(1.0 - a))

        return radius * c
    }

    override fun onMilestoneEvent(
        routeProgress: RouteProgress, instruction: String, milestone: Milestone,
    ) {

        if (voiceInstructionsEnabled) {
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
        navigation?.stopNavigation()
        isRunning = false
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
            PluginUtilities.sendEvent(
                VietMapEvents.SPEECH_ANNOUNCEMENT,
                "${announcement?.announcement()}"
            )
            announcement
        } else {
            null
        }
    }

    override fun willDisplay(instructions: BannerInstructions?): BannerInstructions? {
        return if (bannerInstructionsEnabled) {
            PluginUtilities.sendEvent(
                VietMapEvents.BANNER_INSTRUCTION,
                "${instructions?.primary()?.text()}"
            )

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
                // println("MoveCamera3")

                moveCamera(LatLng(it.latitude(), it.longitude()), null)

                PluginUtilities.sendEvent(
                    VietMapEvents.USER_OFF_ROUTE, VietMapLocation(
                        latitude = it.latitude(), longitude = it.longitude()
                    ).toString()
                )

            }

            PluginUtilities.sendEvent(
                VietMapEvents.USER_OFF_ROUTE, VietMapLocation(
                    latitude = offRoutePoint?.latitude(), longitude = offRoutePoint?.longitude()
                ).toString()
            )

            originPoint = offRoutePoint
            isNavigationInProgress = true
            fetchRouteWithBearing(false, profile)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchRouteWithBearing(isStartNavigation: Boolean, profile: String) {
        activity?.let {
            fusedLocationClient?.lastLocation?.addOnSuccessListener(
                it
            ) { location: Location? ->
                if (location != null) {
                    getRoute(context, isStartNavigation, location.bearing, profile)
                } else {

                    getRoute(context, isStartNavigation, null, profile)
                }
            }
        }

    }

    override fun onRerouteAlong(directionsRoute: DirectionsRoute?) {
        PluginUtilities.sendEvent(VietMapEvents.REROUTE_ALONG, "${directionsRoute?.toJson()}")
        refreshNavigation(directionsRoute)
    }

    override fun allowRerouteFrom(offRoutePoint: Point?): Boolean {

        return true
    }


    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        val customLocationComponentOptions =
            LocationComponentOptions.builder(context).pulseEnabled(true)
//                .backgroundDrawable()
                .build()
        vietmapGL?.locationComponent?.let { locationComponent ->
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    .locationEngine(locationEngine).build()
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

            locationComponent.isLocationComponentEnabled = true
        }

    }

    override fun dispose() {
        isDisposed = true
        mapReady = false

        if (disposed) {
            return
        }
        disposed = true
        methodChannel.setMethodCallHandler(null)
        if (voiceInstructionsEnabled) {
            try {
                speechPlayer?.onDestroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (isNavigationInProgress) {
            finishNavigation()
        }
        navigation?.onDestroy()

        mapView?.onStop()
        mapView?.onDestroy()

        lifecycleProvider?.getVietMapLifecycle()?.removeObserver(this)
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        VietMapNavigationPlugin.eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        VietMapNavigationPlugin.eventSink = null
    }

    override fun onMapClick(point: LatLng): Boolean {
        if (routeClicked) {
            routeClicked = false
            return true
        }

        val pointf = vietmapGL!!.projection.toScreenLocation(point)
        PluginUtilities.sendEvent(
            VietMapEvents.ON_MAP_CLICK,
            "{\"latitude\":${point.latitude},\"longitude\":${point.longitude},\"x\":${pointf.x},\"y\":${pointf.y}}"
        )
        return true
    }


    private fun buildRouteInformationFromProgress(routeProgress: RouteProgress?): RouteInformation {
        return if (routeProgress == null) {
            RouteInformation.create(null, null, null)
        } else RouteInformation.create(routeProgress.directionsRoute(), null, null)
    }

    private fun showRouteOverview(padding: IntArray?, currentRouteProgress: RouteProgress) {

        val routeInformation: RouteInformation =
            buildRouteInformationFromProgress(currentRouteProgress)
        animateCameraForRouteOverview(routeInformation, padding!!)
    }

    private fun animateCameraForRouteOverview(
        routeInformation: RouteInformation, padding: IntArray,
    ) {
        val cameraEngine = navigation?.cameraEngine
        val routePoints = cameraEngine?.overview(routeInformation)
        if (routePoints?.isNotEmpty() == true) {
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
            resetUpdate, 150, CameraOverviewCancelableCallback(overviewUpdate, vietmapGL)
        )
    }

    private fun buildResetCameraUpdate(): CameraUpdate {
        val resetPosition: CameraPosition = CameraPosition.Builder().tilt(0.0).bearing(0.0).build()
        return CameraUpdateFactory.newCameraPosition(resetPosition)
    }

    private fun buildOverviewCameraUpdate(
        padding: IntArray, routePoints: List<Point>,
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
        return LatLngBounds.Builder().includes(latLngs).build()
    }

    private fun addMarkerGroup(call: MethodCall, result: MethodChannel.Result) {
        val markerGroupId = call.argument<String>("markerGroupId") ?: ""
        val byteArray: ByteArray? = call.argument<ByteArray>("imageBytes")
        val iconBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)


        vietmapGL!!.setOnMarkerClickListener { marker ->
            if (marker != null) {
                val markerId = marker.id
                // println(markerId)
            }
            false
        }
        vietmapGL!!.style!!.addImage(markerGroupId, iconBitmap)
    }

    private fun handleProgressChange(routeProgress: RouteProgress, location: Location) {
        // println("handleProgressChange")
        if (location.speed < 1) return
        // println("start handleProgressChange")

        val distanceRemainingToNextTurn =
            routeProgress.currentLegProgress()?.currentStepProgress()?.distanceRemaining()
        if (distanceRemainingToNextTurn != null && distanceRemainingToNextTurn < 30) {
            isNextTurnHandling = true
            val resetPosition: CameraPosition =
                CameraPosition.Builder().tilt(0.0).zoom(17.0).build()
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(resetPosition)
            vietmapGL?.animateCamera(
                cameraUpdate, 1000
            )
        } else {
            if (routeProgress.currentLegProgress().currentStepProgress()
                    .distanceTraveled() > 30 && !isOverviewing
            ) {
                isNextTurnHandling = false
                recenter()
            }
        }
    }

    fun calculateInSampleSize(
        options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int,
    ): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            // Calculate ratios of height and width to requested height and width
            val heightRatio: Int = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio: Int = Math.round(width.toFloat() / reqWidth.toFloat())

            // Choose the smallest ratio as inSampleSize value to ensure the final image
            // is larger than the requested width and height
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }

        return inSampleSize
    }
    private fun addMarker(call: MethodCall, result: MethodChannel.Result) {
        val data = call.arguments as Map<String, Any>
        val listMarkerId = ArrayList<Long>()
        try {


            data.entries.forEach() {
                val d = it.value as Map<String, Any>
                val position = LatLng(d["latitude"] as Double, d["longitude"] as Double)
                val byteArray: ByteArray? = d["imageBytes"] as ByteArray?
                val width :Int? = d["width"] as? Int?
                val height :Int? = d["height"] as? Int?
                var iconBitmap: Bitmap?= BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
                if(width != null&&height !=null){
                   iconBitmap =
                       iconBitmap?.let { it1 -> Bitmap.createScaledBitmap(it1, width, height, false) }
                }
                val icon = iconBitmap?.let { it1 -> IconFactory.getInstance(context).fromBitmap(it1) }

                val markerOption = MarkerOptions().icon(icon).title((d["title"] ?: "") as String)
                    .snippet((d["snippet"] ?: "") as String).position(position)

                val marker: Marker = vietmapGL!!.addMarker(markerOption)
                listMarkerId.add(marker.id)
                listMarkers.add(marker)
            }


            result.success(listMarkerId)
        } catch (e: Exception) {
            e.printStackTrace()
            result.success(listMarkerId)
        }
    }

    private fun removeMarker(call: MethodCall, result: MethodChannel.Result) {
        val data = (call.arguments as Map<String, Any>)["markerIds"] as ArrayList<Int>
        try {
            data.forEach() { markerId ->
                val temp = ArrayList<Marker>()
                listMarkers.forEach() {
                    if (it.id == markerId.toLong()) {
                        it.remove()
                        temp.add(it)
                    }
                }
                listMarkers.removeAll(temp.toSet())
            }
            result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            result.success(false)
        }

    }

    private fun removeAllMarkers(call: MethodCall, result: MethodChannel.Result) {
        try {
            listMarkers.forEach() {
                it.remove()
            }
            result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            result.success(false)
        }
    }
}