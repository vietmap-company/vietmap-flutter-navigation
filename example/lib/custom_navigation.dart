import 'dart:async';

import 'package:demo_plugin/demo_plugin.dart';
import 'package:demo_plugin/embedded/controller.dart';
import 'package:demo_plugin/embedded/view.dart';
import 'package:demo_plugin/models/direction_route.dart';
import 'package:demo_plugin/models/options.dart';
import 'package:demo_plugin/models/way_point.dart';
import 'package:demo_plugin/views/navigation_view.dart';
import 'package:flutter/material.dart';

class CustomNavigation extends StatefulWidget {
  const CustomNavigation({super.key});

  @override
  State<CustomNavigation> createState() => _CustomNavigationState();
}

class _CustomNavigationState extends State<CustomNavigation> {
  MapNavigationViewController? _controller;
  late MapOptions _navigationOption;
  late double value = 0.0;
  final _iconSize = 30;
  late Timer _timer;
  List<WayPoint> wayPoints = [
    WayPoint(name: "You are here", latitude: 10.759091, longitude: 106.675817),
    WayPoint(name: "Are you arrive", latitude: 10.762528, longitude: 106.653099)
  ];

  @override
  void initState() {
    super.initState();
    initialize();
    startTimer();
  }

  @override
  void dispose() {
    super.dispose();
    _timer.cancel();
  }

  void buildRoute() {
    _controller?.buildRoute(wayPoints: wayPoints);
    // _controller?.startNavigation(options: _navigationOption);
  }

  Future<void> initialize() async {
    if (!mounted) return;
    _navigationOption = DemoPlugin.instance.getDefaultOptions();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        top: false,
        child: Center(
          child: Stack(
            children: [
              _showMap(),
              _bodyNavigate(),
            ],
          ),
          // child: _test(),
        ),
      ),
    );
  }

  Widget _bodyNavigate() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        SizedBox(height: MediaQuery.of(context).viewPadding.top),
        _bannerTopGuide(),
        Expanded(child: _bannerMidGuide()),
        _bannerBottomGuide(),
      ],
    );
  }

  Widget _showMap() {
    return NavigationView(
      mapOptions: _navigationOption,
      onMapCreated: (p0) {
        _controller = p0;
        buildRoute();
      },
    );
  }

  Widget _bannerTopGuide() {
    return Container(
      height: MediaQuery.of(context).size.height * 0.1,
      width: MediaQuery.of(context).size.width * 0.97,
      margin: const EdgeInsets.all(5.0),
      padding: const EdgeInsets.only(left: 10.0, right: 10.0),
      decoration: BoxDecoration(
          color: Colors.lightBlue, borderRadius: BorderRadius.circular(7)),
      child: Row(
        children: [
          const Icon(
            Icons.straight,
            size: 50,
            color: Colors.white,
          ),
          Expanded(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  "Đường Trần Phú",
                  style: TextStyle(color: Colors.white, fontSize: 22),
                  overflow: TextOverflow.ellipsis,
                  maxLines: 1,
                ),
                Row(
                  children: const [
                    Text(
                      "khoảng ",
                      style: TextStyle(color: Colors.white),
                    ),
                    Text(
                      "200m ",
                      style: TextStyle(color: Colors.white, fontSize: 20),
                      softWrap: true,
                    ),
                    Text("sau đó", style: TextStyle(color: Colors.white)),
                    Icon(Icons.turn_right, size: 30, color: Colors.white),
                  ],
                )
              ],
            ),
          ),
          IconButton(
              onPressed: () {},
              icon: const Icon(
                Icons.volume_up_outlined,
                size: 30,
                color: Colors.white,
              ))
        ],
      ),
    );
  }

  Widget _bannerMidGuide() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        _bannerMidLeft(),
        _bannerMidRight(),
      ],
    );
  }

  Widget _bannerMidLeft() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        _currentSpeed(),
        _recenter(),
      ],
    );
  }

  Widget _bannerMidRight() {
    return Column(
      children: [
        Expanded(
          child: _inProgressRoute(),
        )
      ],
    );
  }

  Widget _recenter() {
    return Container(
      margin: const EdgeInsets.only(bottom: 5.0, left: 5.0),
      height: 50,
      width: 50,
      decoration:
          const BoxDecoration(shape: BoxShape.circle, color: Colors.lightBlue),
      child: IconButton(onPressed: () {}, icon: const Icon(Icons.my_location)),
    );
  }

  Widget _currentSpeed() {
    return Container(
      margin: const EdgeInsets.only(left: 5.0, top: 5.0),
      height: 50,
      width: 50,
      decoration:
          const BoxDecoration(shape: BoxShape.circle, color: Colors.black26),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: const [
          Text(
            "43",
            style: TextStyle(color: Colors.white, fontSize: 20),
          ),
          Text(
            "km/h",
            style: TextStyle(color: Colors.white, fontSize: 13),
          ),
        ],
      ),
    );
  }

  Widget _inProgressRoute() {
    return Container(
      margin: const EdgeInsets.only(right: 5.0, bottom: 5.0),
      child: Stack(fit: StackFit.loose, children: [
        Padding(
          padding: const EdgeInsets.only(left: 13),
          child: RotatedBox(
            quarterTurns: 3,
            child: LinearProgressIndicator(
              value: value,
              valueColor: const AlwaysStoppedAnimation<Color>(Colors.lightBlue),
              backgroundColor: Colors.black45,
            ),
          ),
        ),
        LayoutBuilder(builder: (context, constrains) {
          var padding = constrains.maxHeight * (1 - value) - (_iconSize / 2);
          var topPadding = padding < 0 ? 0.0 : padding;
          return Padding(
            padding: EdgeInsets.only(top: topPadding),
            child: Icon(
              Icons.navigation_sharp,
              size: _iconSize.toDouble(),
              color: Colors.black54,
            ),
          );
        })
      ]),
    );
  }

  void startTimer() {
    const oneSec = Duration(seconds: 1);
    _timer = Timer.periodic(
      oneSec,
      (Timer timer) {
        if (value >= 1) {
          setState(() {
            value = 0;
          });
        } else {
          setState(() {
            value += 0.01;
          });
        }
      },
    );
  }

  Widget _bannerBottomGuide() {
    return Container(
      height: MediaQuery.of(context).size.height * 0.1,
      width: MediaQuery.of(context).size.width,
      decoration: const BoxDecoration(
          borderRadius: BorderRadius.only(
              topLeft: Radius.circular(10), topRight: Radius.circular(10)),
          color: Colors.black26),
      child: Row(
        children: [
          IconButton(
              onPressed: () {},
              icon: const Icon(
                Icons.fork_left,
                size: 30,
                color: Colors.white,
              )),
          Expanded(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                const Text(
                  "1,7 km",
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white, fontSize: 22),
                ),
                RichText(
                  text: const TextSpan(
                    text: "3 phút",
                    style: TextStyle(color: Colors.white, fontSize: 17),
                    children: <TextSpan>[
                      TextSpan(
                          text: " - ",
                          style: TextStyle(color: Colors.white, fontSize: 17)),
                      TextSpan(
                          text: "17:32",
                          style: TextStyle(color: Colors.white, fontSize: 17)),
                    ],
                  ),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),
          IconButton(
              onPressed: () {
                Navigator.pop(context);
              },
              icon: const Icon(
                Icons.cancel_sharp,
                size: 30,
                color: Colors.white,
              ))
        ],
      ),
    );
  }
}
