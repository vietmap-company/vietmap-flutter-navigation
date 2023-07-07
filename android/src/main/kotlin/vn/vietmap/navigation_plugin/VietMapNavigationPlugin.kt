package vn.vietmap.navigation_plugin

import android.app.Activity
import android.content.Context
import androidx.annotation.NonNull
import vn.vietmap.factory.MapViewFactory
import com.mapbox.mapboxsdk.Mapbox
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.*
import io.flutter.plugin.platform.PlatformViewRegistry

/** VietMapNavigationPlugin */
class VietMapNavigationPlugin : FlutterPlugin , ActivityAware,EventChannel.StreamHandler
    {
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
        channel = MethodChannel(messenger, "navigation_plugin")

        progressEventChannel = EventChannel(messenger, "navigation_plugin/events")
        progressEventChannel.setStreamHandler(this)

        platformViewRegistry = flutterPluginBinding.platformViewRegistry
        binaryMessenger = messenger
    }

    companion object {

        var eventSink: EventChannel.EventSink? = null

        var platformViewRegistry: PlatformViewRegistry? = null
        var binaryMessenger: BinaryMessenger? = null

        var viewId = "VietMapNavigationPluginView"
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        currentActivity = null
        channel.setMethodCallHandler(null)
        progressEventChannel.setStreamHandler(null)
    }

    override fun onListen(args: Any?, events: EventChannel.EventSink?) {
        eventSink = events
    }

    override fun onCancel(args: Any?) {
        eventSink = null
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

    override fun onDetachedFromActivityForConfigChanges() {}
}
