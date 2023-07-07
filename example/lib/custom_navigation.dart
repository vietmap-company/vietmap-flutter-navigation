import 'dart:async';

import 'package:vietmap_flutter_navigation/demo_plugin.dart';
import 'package:vietmap_flutter_navigation/embedded/controller.dart';
import 'package:vietmap_flutter_navigation/embedded/view.dart';
import 'package:vietmap_flutter_navigation/models/direction_route.dart';
import 'package:vietmap_flutter_navigation/models/options.dart';
import 'package:vietmap_flutter_navigation/models/route_progress_event.dart';
import 'package:vietmap_flutter_navigation/models/way_point.dart';
import 'package:vietmap_flutter_navigation/views/navigation_view.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

class CustomNavigation extends StatefulWidget {
  const CustomNavigation({super.key});

  @override
  State<CustomNavigation> createState() => _CustomNavigationState();
}

class _CustomNavigationState extends State<CustomNavigation> {
  Widget instructionImage = const SizedBox.shrink();
  MapNavigationViewController? _controller;
  DirectionRoute? directionRoute;
  RouteProgressEvent? routeProgress;
  late MapOptions _navigationOption;
  double value = 0.0;
  final _iconSize = 30;
  List<WayPoint> wayPoints = [
    WayPoint(name: "You are here", latitude: 10.759091, longitude: 106.675817),
    WayPoint(name: "Are you arrive", latitude: 10.762528, longitude: 106.653099)
  ];

  @override
  void initState() {
    super.initState();
    initialize();
  }

  void buildRoute() {
    _controller?.buildRoute(wayPoints: wayPoints);
    // _controller?.startNavigation(options: _navigationOption);
  }

  Future<void> initialize() async {
    if (!mounted) return;
    _navigationOption = DemoPlugin.instance.getDefaultOptions();
    _navigationOption?.apiKey =
        "89cb1c3c260c27ea71a115ece3c8d7cec462e7a4c14f0944";
    _navigationOption?.mapStyle =
        "https://run.mocky.io/v3/ff325d44-9fdd-480f-9f0f-a9155bf362fa";
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        top: false,
        child: Center(child: _buildSearchRoute()),
      ),
    );
  }

  Widget _buildSearchRoute() {
    return Stack(
      children: [
        _showMap(),
      ],
    );
  }

  Widget _buildNavigation() {
    return Stack(
      children: [
        _showMap(),
        _bodyNavigate(),
      ],
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
      onRouteBuilt: (DirectionRoute route) {
        directionRoute = route;
      },
      onRouteProgressChange: (RouteProgressEvent routeProgress) {
        this.routeProgress = routeProgress;
        _setInstructionImage(
            routeProgress.currentModifier, routeProgress.currentModifierType);
        value = (1 -
            ((routeProgress.distanceRemaining?.toDouble() ?? 0.0)) /
                (directionRoute?.distance?.toDouble() ?? 1));
        setState(() {});
      },
    );
  }

  _setInstructionImage(String? modifier, String? type) {
    if (modifier != null && type != null && modifier != '' && type != '') {
      List<String> data = [
        type.replaceAll(' ', '_'),
        modifier.replaceAll(' ', '_')
      ];
      String path = 'assets/navigation_symbol/${data.join('_')}.svg';
      instructionImage = SvgPicture.asset(path, color: Colors.white);
    }
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
          instructionImage,
          Expanded(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  routeProgress?.currentStepInstruction ?? '',
                  style: const TextStyle(color: Colors.white, fontSize: 22),
                  overflow: TextOverflow.ellipsis,
                  maxLines: 1,
                ),
                Row(
                  children: [
                    const Text(
                      "khoảng ",
                      style: TextStyle(color: Colors.white),
                    ),
                    Text(
                      _calculateTotalDistance(
                          routeProgress?.distanceToNextTurn),
                      style: const TextStyle(color: Colors.white, fontSize: 20),
                      softWrap: true,
                    ),
                    const Text("sau đó", style: TextStyle(color: Colors.white)),
                    const Icon(Icons.turn_right, size: 30, color: Colors.white),
                  ],
                )
              ],
            ),
          ),
          IconButton(
              onPressed: () {
                _controller?.startNavigation(options: _navigationOption);
              },
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
      child: IconButton(
          onPressed: () {
            _controller?.recenter();
          },
          icon: const Icon(Icons.my_location)),
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
              onPressed: () {
                _controller?.overview();
              },
              icon: const Icon(
                Icons.route,
                size: 30,
                color: Colors.white,
              )),
          Expanded(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Text(
                  _calculateTotalDistance(routeProgress?.distanceRemaining),
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: Colors.white, fontSize: 22),
                ),
                RichText(
                  text: TextSpan(
                    text: _calculateEstimatedArrivalTime(),
                    style: const TextStyle(color: Colors.white, fontSize: 17),
                    children: <TextSpan>[
                      const TextSpan(
                          text: " - ",
                          style: TextStyle(color: Colors.white, fontSize: 17)),
                      TextSpan(
                          text: _getTimeArriveRemaining(),
                          style: const TextStyle(
                              color: Colors.white, fontSize: 17)),
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

  _calculateEstimatedArrivalTime() {
    var data = routeProgress?.durationRemaining ?? 0;
    DateTime dateTime = DateTime.now();
    DateTime estimateArriveTime = dateTime.add(Duration(seconds: data.round()));
    return '${estimateArriveTime.hour}:${estimateArriveTime.minute}';
  }

  _getTimeArriveRemaining() {
    var data = routeProgress?.durationRemaining ?? 0;
    if (data < 60) return '${data.round()} giây';
    if (data < 3600) return '${(data / 60).round()} phút';
    var hour = (data / 3600).round();
    var minute = ((data - hour * 3600) / 60).round();
    return '$hour giờ, $minute phút';
  }

  _calculateTotalDistance(double? distance) {
    var data = distance ?? 0;
    if (data < 1000) return '${data.round()} mét ';
    return '${(data / 1000).toStringAsFixed(2)} km';
  }
}
