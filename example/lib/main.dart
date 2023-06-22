import 'package:demo_plugin/models/navmode.dart';
import 'package:demo_plugin/models/options.dart';
import 'package:demo_plugin/models/voice_units.dart';
import 'package:demo_plugin/models/way_point.dart';
import 'package:demo_plugin_example/mapview.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:demo_plugin/demo_plugin.dart';

void main() {
  runApp(const MaterialApp(
    title: 'Plugin example app',
    home: MyApp(),
  ));
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
  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

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
      WayPoint(name: "You are here", latitude: 10.747709, longitude: 106.649902)
    ];
    MapOptions options = MapOptions(
      isCustomizeUI: isCustomizeUI,
      zoom: 15,
      tilt: 0,
      bearing: 0,
      enableRefresh: false,
      alternatives: true,
      voiceInstructionsEnabled: true,
      bannerInstructionsEnabled: true,
      allowsUTurnAtWayPoints: true,
      mode: MapNavigationMode.drivingWithTraffic,
      units: VoiceUnits.imperial,
      simulateRoute: false,
      animateBuildRoute: true,
      longPressDestinationEnabled: true,
      language: 'vi',
    );
    var result = _demoPlugin.startNavigation(wayPoints, options);
    print(result);
  }

  @override
  Widget build(BuildContext context) {
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
              title: const Text('Tuỳ chỉnh giao diện'),
            ),
            const Text('Copy lat long từ google'),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _fromLatLngController,
                    decoration:
                        const InputDecoration(hintText: 'Nhập điểm bắt đầu'),
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
                    child: const Text('Paste'))
              ],
            ),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _toLatLngController,
                    decoration:
                        const InputDecoration(hintText: 'Nhập điểm kết thúc'),
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
                    child: const Text('Paste'))
              ],
            ),
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
                  MaterialPageRoute(builder: (context) => const VietMapView()),
                );
              },
              child: const Text('Open MapView'),
            )
          ],
        ),
      ),
    );
  }
}
