import 'package:demo_plugin/demo_plugin.dart';
import 'package:demo_plugin/embedded/controller.dart';
import 'package:demo_plugin/embedded/view.dart';
import 'package:demo_plugin/models/options.dart';
import 'package:flutter/material.dart';

class CustomNavigation extends StatefulWidget {
  const CustomNavigation({super.key});

  @override
  State<CustomNavigation> createState() => _CustomNavigationState();
}

class _CustomNavigationState extends State<CustomNavigation> {
  MapNavigationViewController? _controller;
  late MapOptions _navigationOption;

  @override
  void initState() {
    super.initState();
    initialize();
  }

  Future<void> initialize() async {
    if (!mounted) return;
    _navigationOption = DemoPlugin.instance.getDefaultOptions();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        top: true,
        child: Center(
          child: Stack(
            children: [
              _showMap(),
              Positioned(
                  top: MediaQuery.of(context).viewPadding.top,
                  child: _bannerTopGuide()),
              Positioned(
                  bottom: MediaQuery.of(context).viewPadding.bottom,
                  child: _bannerBottomGuide()),
              // _bodyNavigate()
            ],
          ),
        ),
      ),
    );
  }

  Widget _bodyNavigate() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        _bannerTopGuide(),
        Expanded(
            child: Container(
          margin: const EdgeInsets.only(right: 10.0),
          child: const RotatedBox(
            quarterTurns: 3,
            child:
                LinearProgressIndicator(), // Is supposed to extend as far as possible
          ),
        )),
        _bannerBottomGuide(),
      ],
    );
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

  Widget _bannerTopGuide() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
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
        ),
        _currentSpeed()
      ],
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
      width: 25,
      height: 500,
      color: Colors.lightBlue,
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
              onPressed: () {},
              icon: const Icon(
                Icons.cancel_sharp,
                size: 30,
                color: Colors.white,
              ))
        ],
      ),
    );
  }

  // Controller

  Future<void> _onEmbeddedRouteEvent(e) async {
    print("listen data change");
    print(e.eventType);
    print(e.data);
  }
}
