import 'package:demo_plugin/demo_plugin.dart';
import 'package:demo_plugin/embedded/controller.dart';
import 'package:demo_plugin/embedded/view.dart';
import 'package:demo_plugin/models/events.dart';
import 'package:demo_plugin/models/options.dart';
import 'package:demo_plugin/models/route_progress_event.dart';
import 'package:demo_plugin/models/way_point.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class DemoAndroidScreen extends StatefulWidget {
  const DemoAndroidScreen({super.key});

  @override
  State<DemoAndroidScreen> createState() => _DemoAndroidScreenState();
}

class _DemoAndroidScreenState extends State<DemoAndroidScreen> {
  bool isCustomizeUI = false;
  bool _isMultipleStop = false;
  double? _distanceRemaining, _durationRemaining;
  MapNavigationViewController? _controller;
  bool _routeBuilt = false;
  bool _isNavigating = false;
  bool _inFreeDrive = false;
  String _platformVersion = 'Unknown';
  late MapOptions _navigationOption;
  final _demoPlugin = DemoPlugin();
  String? _instruction;
  List<WayPoint> wayPoints = [
    WayPoint(name: "You are here", latitude: 10.759091, longitude: 106.675817),
    WayPoint(name: "You are here", latitude: 10.762528, longitude: 106.653099)
  ];
  String guideDirection = "";
  @override
  void initState() {
    super.initState();

    initialize();
  }

  Future<void> initialize() async {
    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    _navigationOption = _demoPlugin.getDefaultOptions();
    _navigationOption.simulateRoute = false;
    _navigationOption.isCustomizeUI = true;
    //_navigationOption.initialLatitude = 36.1175275;
    //_navigationOption.initialLongitude = -115.1839524;
    _demoPlugin.registerRouteEventListener(_onEmbeddedRouteEvent);
    _demoPlugin.setDefaultOptions(_navigationOption);

    String? platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await _demoPlugin.getPlatformVersion();
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    setState(() {
      _platformVersion = platformVersion ?? '';
    });
  }

  MapOptions? options;
  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      platformVersion =
          await _demoPlugin.getPlatformVersion() ?? 'Unknown platform version';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Stack(
          children: [
            MapNavigationView(
                options: _navigationOption,
                onRouteEvent: _onEmbeddedRouteEvent,
                onCreated: (MapNavigationViewController controller) async {
                  _controller = controller;
                  controller.initialize();
                }),
            Positioned(
                top: 0,
                left: 0,
                child: Container(
                  decoration: const BoxDecoration(color: Colors.white),
                  width: MediaQuery.of(context).size.width,
                  height: 100,
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        _instruction ?? "",
                        style: const TextStyle(
                            color: Colors.black,
                            fontSize: 19,
                            fontWeight: FontWeight.bold),
                      ),
                    ],
                  ),
                )),
            Positioned(
                bottom: 0,
                child: Container(
                    width: MediaQuery.of(context).size.width,
                    height: 100,
                    decoration: const BoxDecoration(color: Colors.white),
                    child: Wrap(
                      children: [
                        ElevatedButton(
                            onPressed: () {
                              _controller?.recenter();
                            },
                            child: const Text("Recenter")),
                        ElevatedButton(
                            onPressed: () {
                              _controller?.overview();
                            },
                            child: const Text("Overview")),
                        ElevatedButton(
                            onPressed: () {
                              _controller?.buildRoute(
                                  wayPoints: wayPoints,
                                  options: _navigationOption);
                            },
                            child: const Text("BuildRoute")),
                        ElevatedButton(
                            onPressed: () {
                              _controller?.startNavigation();
                            },
                            child: const Text("Start navigation")),
                        ElevatedButton(
                          onPressed: _isNavigating
                              ? () {
                                  setState(() {
                                    _isNavigating = false;
                                  });
                                  _controller?.finishNavigation();
                                }
                              : null,
                          child: const Text('Cancel '),
                        )
                      ],
                    )))
          ],
        ),
      ),
    );
  }

  Future<void> _onEmbeddedRouteEvent(e) async {
    _distanceRemaining = await _demoPlugin.getDistanceRemaining();
    _durationRemaining = await _demoPlugin.getDurationRemaining();

    switch (e.eventType) {
      case MapEvent.progressChange:
        var progressEvent = e.data as RouteProgressEvent;
        if (progressEvent.currentStepInstruction != null) {
          setState(() {
            _instruction = progressEvent.currentStepInstruction;
          });
        }
        break;
      case MapEvent.routeBuilding:
      case MapEvent.routeBuilt:
        setState(() {
          _routeBuilt = true;
        });
        break;
      case MapEvent.routeBuildFailed:
        setState(() {
          _routeBuilt = false;
        });
        break;
      case MapEvent.navigationRunning:
        setState(() {
          _isNavigating = true;
        });
        break;
      case MapEvent.onArrival:
        if (!_isMultipleStop) {
          await Future.delayed(const Duration(seconds: 3));
          await _controller?.finishNavigation();
        } else {}
        break;
      case MapEvent.navigationFinished:
      case MapEvent.navigationCancelled:
        setState(() {
          _routeBuilt = false;
          _isNavigating = false;
        });
        break;
      case MapEvent.milestoneEvent:
        break;
      default:
        break;
    }
    setState(() {});
  }
}
