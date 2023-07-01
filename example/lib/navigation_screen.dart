import 'package:demo_plugin/demo_plugin.dart';
import 'package:demo_plugin/embedded/controller.dart';
import 'package:demo_plugin/embedded/view.dart';
import 'package:demo_plugin/models/events.dart';
import 'package:demo_plugin/models/options.dart';
import 'package:demo_plugin/models/route_progress_event.dart';
import 'package:demo_plugin/models/way_point.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class NavigationScreen extends StatefulWidget {
  const NavigationScreen({super.key});

  @override
  State<NavigationScreen> createState() => _NavigationScreenState();
}

class _NavigationScreenState extends State<NavigationScreen> {
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
      appBar: AppBar(title: const Text("Demo navigation")),
      body: Column(
        children: [
          SizedBox(
            height: 300,
            child: Container(
              color: Colors.red,
              child: MapNavigationView(
                  options: _navigationOption,
                  onRouteEvent: _onEmbeddedRouteEvent,
                  onCreated: (MapNavigationViewController controller) async {
                    _controller = controller;
                    controller.initialize();
                  }),
            ),
          ),
          Row(
            children: [
              ElevatedButton(
                  onPressed: () {
                    _demoPlugin.startNavigation(wayPoints, _navigationOption);
                  },
                  child: const Text('startNavigation')),
              Expanded(
                child: CheckboxListTile(
                    value: _navigationOption.simulateRoute,
                    title: Text('SimulateRoute'),
                    onChanged: (value) {
                      setState(() {
                        _navigationOption.simulateRoute = value;
                      });
                    }),
              )
            ],
          ),
          Container(
            color: Colors.grey,
            width: double.infinity,
            child: const Padding(
              padding: EdgeInsets.all(10),
              child: (Text(
                "Embedded Navigation",
                style: TextStyle(color: Colors.white),
                textAlign: TextAlign.center,
              )),
            ),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ElevatedButton(
                onPressed: _isNavigating
                    ? null
                    : () {
                        if (_routeBuilt) {
                          _controller?.clearRoute();
                        } else {
                          // var wayPoints = <WayPoint>[];
                          // wayPoints.add(_home);
                          // wayPoints.add(_store);
                          _isMultipleStop = wayPoints.length > 2;
                          _controller?.buildRoute(
                              wayPoints: wayPoints, options: _navigationOption);
                        }
                      },
                child: Text(_routeBuilt && !_isNavigating
                    ? "Clear Route"
                    : "Build Route"),
              ),
              const SizedBox(
                width: 10,
              ),
              ElevatedButton(
                onPressed: _routeBuilt && !_isNavigating
                    ? () {
                        _controller?.startNavigation();
                      }
                    : null,
                child: const Text('Start '),
              ),
              const SizedBox(
                width: 10,
              ),
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
          ),
          ElevatedButton(
            onPressed: _inFreeDrive
                ? null
                : () async {
                    _inFreeDrive = await _controller?.startFreeDrive() ?? false;
                  },
            child: const Text("Free Drive "),
          ),
          const Center(
            child: Padding(
              padding: EdgeInsets.all(10),
              child: Text(
                "Long-Press Embedded Map to Set Destination",
                textAlign: TextAlign.center,
              ),
            ),
          ),
          Container(
            color: Colors.grey,
            width: double.infinity,
            child: Padding(
              padding: const EdgeInsets.all(10),
              child: (Text(
                _instruction == null
                    ? "Banner Instruction Here"
                    : _instruction!,
                style: const TextStyle(color: Colors.white),
                textAlign: TextAlign.center,
              )),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(
                left: 20.0, right: 20, top: 20, bottom: 10),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                Row(
                  children: <Widget>[
                    const Text("Duration Remaining: "),
                    Text(_durationRemaining != null
                        ? "${(_durationRemaining! / 60).toStringAsFixed(0)} minutes"
                        : "---")
                  ],
                ),
                Row(
                  children: <Widget>[
                    const Text("Distance Remaining: "),
                    Text(_distanceRemaining != null
                        ? "${(_distanceRemaining! * 0.000621371).toStringAsFixed(1)} miles"
                        : "---")
                  ],
                ),
                Row(
                  children: _isNavigating
                      ? [
                          ElevatedButton(
                              onPressed: () {
                                _controller?.recenter();
                              },
                              child: const Text('ReCenter')),
                          ElevatedButton(
                              onPressed: () {
                                _controller?.overview();
                              },
                              child: const Text('overView')),
                        ]
                      : [],
                )
              ],
            ),
          ),
          const Divider()
        ],
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
          _instruction = progressEvent.currentStepInstruction;
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
