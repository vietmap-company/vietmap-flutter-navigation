package vn.vietmap.factory
import android.app.Activity
import android.content.Context
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import vn.vietmap.vietmapsdk.Vietmap


class MapViewFactory(private val messenger: BinaryMessenger, private val activity: Activity, private  val binding: ActivityPluginBinding) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context?, viewId: Int, args: Any?): PlatformView {
        if (context != null) {
            Vietmap.getInstance(context)
        }
        return FlutterMapViewFactory(context!!, messenger, viewId, activity, args,binding)
    }

}