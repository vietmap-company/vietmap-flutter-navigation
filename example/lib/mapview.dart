import 'package:vietmap_flutter_navigation/demo_plugin.dart';
import 'package:vietmap_flutter_navigation/embedded/controller.dart';
import 'package:vietmap_flutter_navigation/embedded/view.dart';
import 'package:vietmap_flutter_navigation/models/options.dart';
import 'package:flutter/material.dart';

class VietMapView extends StatefulWidget {
  const VietMapView({super.key});

  @override
  State<VietMapView> createState() => _MapViewState();
}

class _MapViewState extends State<VietMapView> {
  MapNavigationViewController? _controller;
  late MapOptions _navigationOption;

  final ButtonStyle raisedButtonStyle = ElevatedButton.styleFrom(
    foregroundColor: Colors.black87,
    backgroundColor: Colors.grey[300],
    minimumSize: const Size(88, 36),
    padding: const EdgeInsets.symmetric(horizontal: 16),
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.all(Radius.circular(2)),
    ),
  );

  @override
  void initState() {
    super.initState();
    initialize();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initialize() async {
    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;
    _navigationOption = DemoPlugin.instance.getDefaultOptions();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(title: const Text('MapView')),
        body: Center(
          child: Stack(children: [
            _showMap(),
            Positioned(
                bottom: 20,
                right: 20,
                child: IconButton(
                  icon: const Icon(
                    Icons.assistant_navigation,
                    size: 50,
                    color: Colors.blue,
                  ),
                  onPressed: () {
                    _startNavigation();
                  },
                )),
            Positioned(
                bottom: 20,
                left: 20,
                child: ElevatedButton(
                  style: raisedButtonStyle,
                  onPressed: () {
                    _controller?.clearRoute();
                  },
                  child: const Text('Clear route'),
                )),
          ]),
        ));
  }

  Widget _showMap() {
    return MapNavigationView(
      onRouteEvent: _onEmbeddedRouteEvent,
      onCreated: (MapNavigationViewController controller) async {
        _controller = controller;
        controller.initialize();
      },
      options: _navigationOption,
    );
  }

  Future<void> _onEmbeddedRouteEvent(e) async {}

  void _startNavigation() {
    _controller?.startNavigation(options: _navigationOption);
    // _controller?.recenterMap();
  }
}
