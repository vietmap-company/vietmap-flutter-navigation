import 'package:demo_plugin/embedded/controller.dart';
import 'package:demo_plugin/models/events.dart';
import 'package:demo_plugin/models/navmode.dart';
import 'package:demo_plugin/models/options.dart';
import 'package:demo_plugin/models/route_progress_event.dart';
import 'package:demo_plugin/models/voice_units.dart';
import 'package:demo_plugin/models/way_point.dart';
import 'package:demo_plugin_example/navigation_screen.dart';
import 'package:flutter/material.dart';
import 'dart:async';
import 'package:demo_plugin/embedded/view.dart';
import 'package:flutter/services.dart';
import 'package:demo_plugin/demo_plugin.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  final _demoPlugin = DemoPlugin();
  final _toLatLngController = TextEditingController();
  final _fromLatLngController = TextEditingController();
  bool isCustomizeUI = false;
  bool _isMultipleStop = false;
  double? _distanceRemaining, _durationRemaining;
  MapNavigationViewController? _controller;
  bool _routeBuilt = false;
  bool _isNavigating = false;
  bool _inFreeDrive = false;
  late MapOptions _navigationOption;
  String? _instruction;
  @override
  void initState() {
    super.initState();
    initPlatformState();
    initialize();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initialize() async {
    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    _navigationOption = DemoPlugin.instance.getDefaultOptions();
    _navigationOption.simulateRoute = true;
    //_navigationOption.initialLatitude = 36.1175275;
    //_navigationOption.initialLongitude = -115.1839524;
    DemoPlugin.instance.registerRouteEventListener(_onEmbeddedRouteEvent);
    DemoPlugin.instance.setDefaultOptions(_navigationOption);

    String? platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await DemoPlugin.instance.getPlatformVersion();
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

  final ButtonStyle raisedButtonStyle = ElevatedButton.styleFrom(
    foregroundColor: Colors.black87,
    backgroundColor: Colors.grey[300],
    minimumSize: const Size(88, 36),
    padding: const EdgeInsets.symmetric(horizontal: 16),
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.all(Radius.circular(2)),
    ),
  );

  void _startNavigation() {
    double? fromLat, fromLong, toLat, toLong;
    if (_fromLatLngController.text.isNotEmpty) {
      var data = _fromLatLngController.text.trim().split(', ');
      fromLat = double.tryParse(data.first);
      fromLong = double.tryParse(data.last);
    }
    if (_toLatLngController.text.isNotEmpty) {
      var data = _toLatLngController.text.trim().split(', ');
      toLat = double.tryParse(data.first);
      toLong = double.tryParse(data.last);
    }
    List<WayPoint> wayPoints = [
      WayPoint(
          name: "You are here",
          latitude: fromLat ?? 10.792145,
          longitude: fromLong ?? 106.690157),
      // 10.762528, 106.653099

      WayPoint(name: "You are here", latitude: 10.762528, longitude: 106.653099)
    ];
    options = MapOptions(
      isCustomizeUI: isCustomizeUI,
      zoom: 19,
      tilt: 10000,
      bearing: 10000,
      enableRefresh: false,
      alternatives: false,
      voiceInstructionsEnabled: false,
      bannerInstructionsEnabled: false,
      allowsUTurnAtWayPoints: false,
      mode: MapNavigationMode.driving,
      units: VoiceUnits.imperial,
      simulateRoute: false,
      animateBuildRoute: true,
      longPressDestinationEnabled: false,
      language: 'vi',
    );
    var result = DemoPlugin.instance.startNavigation(wayPoints, options!);
    print(result);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Builder(builder: (context) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('Plugin example app'),
          ),
          body: Center(
            child: Column(
              children: [
                Text('Running on: $_platformVersion\n'),
                const SizedBox(height: 50),
                CheckboxListTile(
                  value: isCustomizeUI,
                  onChanged: (value) {
                    setState(() {
                      isCustomizeUI = value ?? !isCustomizeUI;
                    });
                  },
                  title: Text('Tuỳ chỉnh giao diện'),
                ),
                const Text('Copy lat long từ google'),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _fromLatLngController,
                        decoration: const InputDecoration(
                            hintText: 'Nhập điểm bắt đầu'),
                      ),
                    ),
                    ElevatedButton(
                        onPressed: () async {
                          ClipboardData? data =
                              await Clipboard.getData(Clipboard.kTextPlain);
                          if (data != null) {
                            _fromLatLngController.text = data.text ?? '';
                          }
                        },
                        child: Text('Paste'))
                  ],
                ),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _toLatLngController,
                        decoration: const InputDecoration(
                            hintText: 'Nhập điểm kết thúc'),
                      ),
                    ),
                    ElevatedButton(
                        onPressed: () async {
                          ClipboardData? data =
                              await Clipboard.getData(Clipboard.kTextPlain);
                          if (data != null) {
                            _toLatLngController.text = data.text ?? '';
                          }
                        },
                        child: Text('Paste'))
                  ],
                ),
                Text(_distanceRemaining.toString()),
                Text(_durationRemaining.toString()),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    ElevatedButton(
                      style: raisedButtonStyle,
                      onPressed: () {
                        _startNavigation();
                      },
                      child: const Text('Start Navigation'),
                    ),
                    ElevatedButton(
                      style: raisedButtonStyle,
                      onPressed: () {
                        Navigator.push(
                            context,
                            MaterialPageRoute(
                                builder: (_) => const NavigationScreen()));
                      },
                      child: const Text('Push to navigation screen'),
                    ),
                  ],
                ),
                // SizedBox(
                //   height: 300,
                //   child: Container(
                //     color: Colors.red,
                //     child: MapNavigationView(
                //         options: _navigationOption,
                //         onRouteEvent: _onEmbeddedRouteEvent,
                //         onCreated:
                //             (MapNavigationViewController controller) async {
                //           _controller = controller;
                //           controller.initialize();
                //         }),
                //   ),
                // )
              ],
            ),
          ),
        );
      }),
    );
  }

  Future<void> _onEmbeddedRouteEvent(e) async {
    _distanceRemaining = await DemoPlugin.instance.getDistanceRemaining();
    _durationRemaining = await DemoPlugin.instance.getDurationRemaining();

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
