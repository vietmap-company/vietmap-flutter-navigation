package com.example.factory
import android.app.Activity
import android.content.Context
import com.example.utilities.PluginUtilities

import com.mapbox.mapboxsdk.Mapbox
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory


class MapViewFactory(private val messenger: BinaryMessenger, private val activity: Activity) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context?, viewId: Int, args: Any?): PlatformView {
        if (context != null) {
            Mapbox.getInstance(context)
        }
        return FlutterMapViewFactory(context!!, messenger, viewId, activity, args)
    }
}