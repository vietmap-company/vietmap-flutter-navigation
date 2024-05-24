package vn.vietmap.factory
import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import vn.vietmap.navigation_plugin.LifecycleProvider
import vn.vietmap.vietmapsdk.Vietmap

class MapViewFactory(private val messenger: BinaryMessenger,   private val lifecycle: LifecycleProvider, private val activity: Activity?) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context?, viewId: Int, args: Any?, ): PlatformView {
        val controller = FlutterMapViewFactory(context!!, messenger, viewId,  args, lifecycle, activity)
        Log.d("Lifecycle","onInit")
        Vietmap.getInstance(context)

        controller.init()
        return controller
    }
}