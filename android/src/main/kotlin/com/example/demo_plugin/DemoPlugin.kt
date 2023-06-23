package com.example.demo_plugin

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.NonNull
import com.example.factory.MapViewFactory
import com.example.models.VietMapNavigationOptions
import com.example.models.Waypoint
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.services.android.navigation.ui.v5.NavigationView
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.*
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.platform.PlatformViewRegistry

/** DemoPlugin */
class DemoPlugin : FlutterPlugin, MethodCallHandler , ActivityAware,EventChannel.StreamHandler{
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    private lateinit var progressEventChannel: EventChannel
    private var currentActivity: Activity? = null
    private lateinit var currentContext: Context
    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val messenger = flutterPluginBinding.binaryMessenger
        channel = MethodChannel(messenger, "demo_plugin")
        channel.setMethodCallHandler(this)

        progressEventChannel = EventChannel(messenger, "demo_plugin/events")
        progressEventChannel.setStreamHandler(this)

        platformViewRegistry = flutterPluginBinding.platformViewRegistry
        binaryMessenger = messenger
    }

    companion object {

        var eventSink: EventChannel.EventSink? = null

        var PERMISSION_REQUEST_CODE: Int = 367

        lateinit var routes: List<DirectionsRoute>
        private var currentRoute: DirectionsRoute? = null
        val wayPoints: MutableList<Waypoint> = mutableListOf()
        var showAlternateRoutes: Boolean = true
        val allowsClickToSetDestination: Boolean = false
        var allowsUTurnsAtWayPoints: Boolean = false
        var navigationMode = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
        var simulateRoute = false
        var enableFreeDriveMode = false
        var mapStyleUrlDay: String? = null
        var mapStyleUrlNight: String? = null
        var navigationLanguage = "en"
        var navigationVoiceUnits = DirectionsCriteria.IMPERIAL
        var isCustomizeUI:Boolean=false
        var zoom = 15.0
        var bearing = 0.0
        var tilt = 0.0
        var distanceRemaining: Float? = null
        var durationRemaining: Double? = null
        var platformViewRegistry: PlatformViewRegistry? = null
        var binaryMessenger: BinaryMessenger? = null

        var viewId = "DemoPluginView"
        @JvmStatic
        var view_name = "DemoPluginView"

        @JvmStatic
        fun registerWith(registrar: PluginRegistry.Registrar) {
            val messenger = registrar.messenger()
            val instance = DemoPlugin()

            val channel = MethodChannel(messenger, "demo_plugin")
            channel.setMethodCallHandler(instance)

            val progressEventChannel = EventChannel(messenger, "demo_plugin/events")
            progressEventChannel.setStreamHandler(instance)

            platformViewRegistry = registrar.platformViewRegistry()
            binaryMessenger = messenger;

        }
    }


    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        currentActivity = null
        channel.setMethodCallHandler(null)
        progressEventChannel.setStreamHandler(null)
    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${Build.VERSION.RELEASE}")
            }
            "getDistanceRemaining" -> {
                result.success(distanceRemaining)
            }
            "getDurationRemaining" -> {
                result.success(durationRemaining)
            }
            "startFreeDrive" -> {
                enableFreeDriveMode = true
                checkPermissionAndBeginNavigation(call)
            }
            "startNavigation" -> {
                enableFreeDriveMode = false
                checkPermissionAndBeginNavigation(call)
            }
            "addWayPoints" -> {
                addWayPointsToNavigation(call, result)
            }
            "finishNavigation" -> {
                VietmapNavigationLauncher.stopNavigation(currentActivity)
            }
            "enableOfflineRouting" -> {
//              downloadRegionForOfflineRouting(call, result)
            }
            else -> result.notImplemented()
        }
    }

    override fun onListen(args: Any?, events: EventChannel.EventSink?) {
        eventSink = events
    }

    override fun onCancel(args: Any?) {
        eventSink = null
    }

    private fun checkPermissionAndBeginNavigation(
        call: MethodCall
    ) {
        val arguments = call.arguments as? Map<String, Any>

        isCustomizeUI = (arguments?.get("isCustomizeUI") ?: false) as Boolean
        VietMapNavigationOptions.instance.isCustomizeUI= isCustomizeUI
        val navMode = arguments?.get("mode") as? String
        if (navMode != null) {
            when (navMode) {
                "walking" -> navigationMode = DirectionsCriteria.PROFILE_WALKING
                "cycling" -> navigationMode = DirectionsCriteria.PROFILE_CYCLING
                "driving" -> navigationMode = DirectionsCriteria.PROFILE_DRIVING
            }
        }

        val alternateRoutes = arguments?.get("alternatives") as? Boolean
        if (alternateRoutes != null) {
            showAlternateRoutes = alternateRoutes
        }

        val simulated = arguments?.get("simulateRoute") as? Boolean
        if (simulated != null) {
            simulateRoute = simulated
        }

        val allowsUTurns = arguments?.get("allowsUTurnsAtWayPoints") as? Boolean
        if (allowsUTurns != null) {
            allowsUTurnsAtWayPoints = allowsUTurns
        }

        val language = arguments?.get("language") as? String
        if (language != null) {
            navigationLanguage = language
        }

        val units = arguments?.get("units") as? String

        if (units != null) {
            if (units == "imperial") {
                navigationVoiceUnits = DirectionsCriteria.IMPERIAL
            } else if (units == "metric") {
                navigationVoiceUnits = DirectionsCriteria.METRIC
            }
        }

        mapStyleUrlDay = arguments?.get("mapStyleUrlDay") as? String
        mapStyleUrlNight = arguments?.get("mapStyleUrlNight") as? String

        wayPoints.clear()

        if (enableFreeDriveMode) {
            checkPermissionAndBeginNavigation(wayPoints)
            return
        }

        val points = arguments?.get("wayPoints") as HashMap<Int, Any>
        for (item in points) {
            val point = item.value as HashMap<*, *>
            val name = point["Name"] as String
            val latitude = point["Latitude"] as Double
            val longitude = point["Longitude"] as Double

            var isSilent: Boolean? = null
            if (point["IsSilent"] != null) {

                isSilent = point["IsSilent"] as Boolean?
            }
            wayPoints.add(Waypoint(name, longitude, latitude, isSilent ?: false))
        }
        checkPermissionAndBeginNavigation(wayPoints)
    }

    private fun checkPermissionAndBeginNavigation(wayPoints: List<Waypoint>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasPermission =
                currentActivity?.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                currentActivity?.requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_CODE
                )
                beginNavigation(wayPoints)
            } else
                beginNavigation(wayPoints)
        } else
            beginNavigation(wayPoints)
    }

    private fun beginNavigation(wayPoints: List<Waypoint>) {
        VietmapNavigationLauncher.startNavigation(currentActivity, wayPoints)

    }


    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        currentActivity = binding.activity
    }

    override fun onDetachedFromActivity() {
        currentActivity!!.finish()
        currentActivity = null
    }
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        currentActivity = binding.activity

        currentContext = binding.activity.applicationContext
        Mapbox.getInstance(currentContext)
        if (platformViewRegistry != null && binaryMessenger != null && currentActivity != null) {
            platformViewRegistry?.registerViewFactory(
                viewId,
                MapViewFactory(binaryMessenger!!, currentActivity!!)
            )

        }
    }

    override fun onDetachedFromActivityForConfigChanges() {

        println("--------onDetachedFromActivityForConfigChanges----------------")
    }

    private fun addWayPointsToNavigation(
        call: MethodCall,
        result: Result
    ) {
        val arguments = call.arguments as? Map<String, Any>
        val points = arguments?.get("wayPoints") as HashMap<Int, Any>

        for (item in points) {
            val point = item.value as HashMap<*, *>
            val name = point["Name"] as String
            val latitude = point["Latitude"] as Double
            val longitude = point["Longitude"] as Double
            val isSilent = point["IsSilent"] as Boolean
            wayPoints.add(Waypoint(name, latitude, longitude, isSilent))
        }
        VietmapNavigationLauncher.addWayPoints(currentActivity, wayPoints)
    }

}
