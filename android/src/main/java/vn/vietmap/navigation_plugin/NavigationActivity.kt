//package vn.vietmap.navigation_plugin
//
//
//import android.content.*
//import android.location.Location
//import android.os.Bundle
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import vn.vietmap.navigation_plugin.VietmapNavigationLauncher.KEY_STOP_NAVIGATION
//import vn.vietmap.models.*
//import vn.vietmap.utilities.PluginUtilities.Companion.sendEvent
//import com.mapbox.android.gestures.MoveGestureDetector
//import com.mapbox.api.directions.v5.models.BannerInstructions
//import com.mapbox.api.directions.v5.models.DirectionsResponse
//import com.mapbox.api.directions.v5.models.DirectionsRoute
//import com.mapbox.geojson.Point
//import com.mapbox.mapboxsdk.Mapbox
//import com.mapbox.mapboxsdk.camera.CameraPosition
//import com.mapbox.mapboxsdk.geometry.LatLng
//import com.mapbox.mapboxsdk.location.modes.RenderMode
//import com.mapbox.mapboxsdk.maps.MapboxMap.OnMoveListener
//import com.mapbox.mapboxsdk.maps.Style
//import com.mapbox.services.android.navigation.ui.v5.NavigationView
//import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
//import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback
//import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener
//import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
//import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener
//import com.mapbox.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener
//import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap
//import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement
//import com.mapbox.services.android.navigation.v5.milestone.Milestone
//import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
//import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
//import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener
//import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
//import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
//import com.mapbox.services.android.navigation.v5.route.FasterRouteListener
//import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
//import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//
//class NavigationActivity : AppCompatActivity(),
//    OnNavigationReadyCallback,
//    ProgressChangeListener,
//    OffRouteListener,
//    MilestoneEventListener,
//    NavigationEventListener,
//    NavigationListener,
//    FasterRouteListener,
//    SpeechAnnouncementListener,
//    Callback<DirectionsResponse>,
//    BannerInstructionsListener,
//    RouteListener, OnMoveListener {
//
//    var receiver: BroadcastReceiver? = null
//
//    private var navigationView: NavigationView? = null
//    private lateinit var navigationMapboxMap: NavigationMapboxMap
//    private lateinit var mapboxNavigation: MapboxNavigation
//    private var dropoffDialogShown = false
//    private var lastKnownLocation: Location? = null
//
//    private val route by lazy { intent.getSerializableExtra("route") as? DirectionsRoute }
//    private var points: MutableList<Point> = mutableListOf()
//
//    private var currentDestination: Point? = null;
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//
//        receiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                finish()
//                DemoNavigationLauncher.cleanUpPreferences(applicationContext)
//            }
//        }
//        registerReceiver(receiver, IntentFilter(KEY_STOP_NAVIGATION))
//
//        super.onCreate(savedInstanceState)
//
//        setTheme(R.style.Theme_AppCompat_NoActionBar)
//
//        Mapbox.getInstance(this.applicationContext)
//
//        setContentView(R.layout.activity_navigation)
//
//        var p = intent.getSerializableExtra("waypoints") as? MutableList<Point>
//        if(p != null)
//        {
//            points = p
//        }
//
//        navigationView = findViewById(R.id.navigationViewPluginWidget)
//
////        navigationView= activity.findViewById(R.id.navigationViewPluginWidget)
//        navigationView?.onCreate(savedInstanceState,null)
//        navigationView?.initialize(
//            this,
//            getInitialCameraPosition()
//        )
//    }
//
//    override fun onLowMemory() {
//        super.onLowMemory()
//        navigationView?.onLowMemory()
//    }
//
//    override fun onStart() {
//        super.onStart()
//        navigationView?.onStart()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        navigationView?.onResume()
//    }
//
//    override fun onStop() {
//        super.onStop()
//        navigationView?.onStop()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        navigationView?.onPause()
//    }
//
//    override fun onDestroy() {
//        navigationView?.onDestroy()
//        unregisterReceiver(receiver)
//        super.onDestroy()
//    }
//
//    override fun onBackPressed() {
//        // If the navigation view didn't need to do anything, call super
//        if (!navigationView?.onBackPressed()!!) {
//            super.onBackPressed()
//        }
//    }
//
//    override fun onSaveInstanceState(outState: Bundle) {
//        navigationView?.onSaveInstanceState(outState)
//        super.onSaveInstanceState(outState)
//    }
//
//    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
//        super.onRestoreInstanceState(savedInstanceState)
//        navigationView?.onRestoreInstanceState(savedInstanceState)
//    }
//
//    override fun onNavigationReady(isRunning: Boolean) {
//
//        if (isRunning && ::navigationMapboxMap.isInitialized) {
//            return
//        }
//
//        if(points.count() > 0)
//        {
//            val point1:Waypoint = points.removeAt(0) as Waypoint
//            val point2:Waypoint = points.removeAt(0) as Waypoint
//
//            fetchRoute(point1.point, point2.point)
//        }
//
//    }
//
//    override fun onProgressChange(location: Location, routeProgress: RouteProgress) {
//        lastKnownLocation = location
//        val progressEvent = VietMapRouteProgressEvent(routeProgress)
//        VietMapNavigationPlugin.distanceRemaining = routeProgress.distanceRemaining().toFloat()
//        VietMapNavigationPlugin.durationRemaining = routeProgress.durationRemaining()
//        sendEvent(progressEvent)
//    }
//
//    override fun userOffRoute(location: Location) {
//
//        sendEvent(
//            VietMapEvents.USER_OFF_ROUTE,
//            VietMapLocation(
//                latitude = location.latitude,
//                longitude = location.longitude
//            ).toString())
//    }
//
//    override fun onMilestoneEvent(routeProgress: RouteProgress, instruction: String, milestone: Milestone) {
//
//        sendEvent(VietMapEvents.MILESTONE_EVENT,
//            VietMapMileStone(
//                identifier = milestone.identifier,
//                distanceTraveled = routeProgress.distanceTraveled(),
//                legIndex = routeProgress.legIndex(),
//                stepIndex = routeProgress.currentLegProgress().stepIndex()
//            ).toString())
//    }
//
//    override fun onRunning(running: Boolean) {
//
//        sendEvent(VietMapEvents.NAVIGATION_RUNNING)
//    }
//
//    override fun onCancelNavigation() {
//        sendEvent(VietMapEvents.NAVIGATION_CANCELLED)
//        navigationView?.stopNavigation()
//        VietMapNavigationPlugin.eventSink = null
//        DemoNavigationLauncher.stopNavigation(this)
//
//    }
//
//    override fun onNavigationFinished() {
//        sendEvent(VietMapEvents.NAVIGATION_FINISHED)
//    }
//
//    override fun onNavigationRunning() {
//        sendEvent(VietMapEvents.NAVIGATION_RUNNING)
//    }
//
//    override fun fasterRouteFound(directionsRoute: DirectionsRoute) {
//        sendEvent(VietMapEvents.FASTER_ROUTE_FOUND, directionsRoute.toJson())
//    }
//
//    override fun willVoice(announcement: SpeechAnnouncement?): SpeechAnnouncement? {
//        sendEvent(VietMapEvents.SPEECH_ANNOUNCEMENT,
//            "${announcement?.announcement()}")
//        return announcement
//    }
//
//    override fun willDisplay(instructions: BannerInstructions?): BannerInstructions? {
//        sendEvent(VietMapEvents.BANNER_INSTRUCTION,
//            "${instructions?.primary()?.text()}")
//        return instructions
//    }
//
//    override fun onArrival() {
//        sendEvent(VietMapEvents.ON_ARRIVAL)
//        if (points.isNotEmpty()) {
//            fetchRoute(getLastKnownLocation(), points.removeAt(0))
//            dropoffDialogShown = true // Accounts for multiple arrival events
//            //Toast.makeText(this, "You have arrived!", Toast.LENGTH_SHORT).show()
//        }
//        else
//        {
//            VietMapNavigationPlugin.eventSink = null
//        }
//    }
//
//    override fun onFailedReroute(errorMessage: String?) {
//        sendEvent(VietMapEvents.FAILED_TO_REROUTE,"${errorMessage}")
//    }
//
//    override fun onOffRoute(offRoutePoint: Point?) {
//        sendEvent(VietMapEvents.USER_OFF_ROUTE,
//            VietMapLocation(
//                latitude = offRoutePoint?.latitude(),
//                longitude = offRoutePoint?.longitude()
//            ).toString())
//        if(offRoutePoint != null)
//            fetchRoute(offRoutePoint, getCurrentDestination());
//        else
//            fetchRoute(getLastKnownLocation(), getCurrentDestination());
//    }
//
//    override fun onRerouteAlong(directionsRoute: DirectionsRoute?) {
//        sendEvent(VietMapEvents.REROUTE_ALONG, "${directionsRoute?.toJson()}")
//    }
//
//    private fun buildAndStartNavigation(directionsRoute: DirectionsRoute) {
//
//        sendEvent(VietMapEvents.ROUTE_BUILT, "${directionsRoute?.toJson()}")
//        dropoffDialogShown = false
//
//        navigationView?.retrieveNavigationMapboxMap()?.let {navigationMap ->
//
//            if(VietMapNavigationPlugin.mapStyleUrlDay != null)
//                navigationMap.retrieveMap().setStyle(Style.Builder().fromUri(VietMapNavigationPlugin.mapStyleUrlDay as String))
//
//            if(VietMapNavigationPlugin.mapStyleUrlNight != null)
//                navigationMap.retrieveMap().setStyle(Style.Builder().fromUri(VietMapNavigationPlugin.mapStyleUrlNight as String))
//
//
//            this.navigationMapboxMap = navigationMap
//            this.navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.NORMAL)
//            navigationView?.retrieveMapboxNavigation()?.let {
//                this.mapboxNavigation = it
//
//                mapboxNavigation.addOffRouteListener(this)
//                mapboxNavigation.addFasterRouteListener(this)
//                mapboxNavigation.addNavigationEventListener(this)
//            }
//
//            // Custom map style has been loaded and map is now ready
//            val options =
//                NavigationViewOptions.builder()
//                    .progressChangeListener(this)
//                    .milestoneEventListener(this)
//                    .navigationListener(this)
//                    .speechAnnouncementListener(this)
//                    .bannerInstructionsListener(this)
//                    .routeListener(this)
//                    .directionsRoute(directionsRoute)
//                    .shouldSimulateRoute(VietMapNavigationPlugin.simulateRoute)
//                    .onMoveListener(this)
//                    .build()
//
//            navigationView?.initViewConfig(VietMapNavigationPlugin.isCustomizeUI)
//            navigationView?.startNavigation(options)
//
//        }
//        //navigationView!!.startNavigation(navigationViewOptions)
//    }
//
//    private fun showDropoffDialog() {
//        val alertDialog = AlertDialog.Builder(this).create()
//        alertDialog.setMessage(getString(R.string.dropoff_dialog_text))
//        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dropoff_dialog_positive_text)
//        ) { dialogInterface: DialogInterface?, `in`: Int -> fetchRoute(getLastKnownLocation(), points.removeAt(0)) }
//        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dropoff_dialog_negative_text)
//        ) { dialogInterface: DialogInterface?, `in`: Int -> }
//        alertDialog.show()
//    }
//
//    private fun fetchRoute(origin: Point, destination: Point) {
//        val builder = NavigationRoute.builder(this)
//            .apikey("YOUR_API_KEY_HERE")
//            .origin(origin)
//            .destination(destination)
//            .alternatives(true)
//            .build()
//        builder.getRoute(this)
//    }
//
//    private fun getLastKnownLocation(): Point {
//        return Point.fromLngLat(lastKnownLocation?.longitude!!, lastKnownLocation?.latitude!!)
//    }
//    private fun getCurrentDestination(): Point {
//        return Point.fromLngLat(currentDestination?.longitude()!!, currentDestination?.latitude()!!)
//    }
//
//    override fun allowRerouteFrom(offRoutePoint: Point?): Boolean {
//        return true
//    }
//
//    private fun getInitialCameraPosition(): CameraPosition {
//        if(route == null)
//            return CameraPosition.DEFAULT;
//
//        val originCoordinate = route?.routeOptions()?.coordinates()?.get(0)
//        return CameraPosition.Builder()
//            .target(LatLng(originCoordinate!!.latitude(), originCoordinate.longitude()))
//            .zoom(VietMapNavigationPlugin.zoom)
//            .bearing(VietMapNavigationPlugin.bearing)
//            .tilt(VietMapNavigationPlugin.tilt)
//            .build()
//    }
//
//    override fun onResponse(
//        call: Call<DirectionsResponse>,
//        response: Response<DirectionsResponse>
//    ) {val directionsResponse = response.body()
//                    if (directionsResponse != null) {
//                        if (!directionsResponse.routes().isEmpty()) buildAndStartNavigation(directionsResponse.routes()[0]) else {
//                            val message = directionsResponse.message()
//                            sendEvent(VietMapEvents.ROUTE_BUILD_FAILED, message!!.replace("\"","'"))
//                            finish()
//                        }
//                    }
//    }
//
//    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
//        sendEvent(VietMapEvents.ROUTE_BUILD_FAILED, t.localizedMessage)
//                    finish()
//    }
//
//    override fun onMoveBegin(p0: MoveGestureDetector) {
//        println("On map move begin")
//    }
//
//    override fun onMove(p0: MoveGestureDetector) {
//        println("On map move")
//    }
//
//    override fun onMoveEnd(p0: MoveGestureDetector) {
//        println("On map move end")
//    }
//}