# Vietmap Flutter navigation

Vietmap Flutter navigation

## pub.dev 


[https://pub.dev/packages/vietmap_flutter_navigation](https://pub.dev/packages/vietmap_flutter_navigation)
## Getting Started

Thêm thư viện vào file pubspec.yaml
```yaml
  vietmap_flutter_navigation:
    git:
      url: https://github.com/vietmap-company/vietmap-flutter-navigation
      ref: beta-2
```

### Android config


Thêm đoạn code sau vào build.gradle (project) tại path android/build.gradle

```gradle
 maven { url "https://jitpack.io" }
```


như sau


```gradle

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
    }
}
```


### iOS config
Thêm đoạn code sau vào file Info.plist
```
	<key>VietMapAPIBaseURL</key>
	<string>https://maps.vietmap.vn/api/navigations/route/</string>
	<key>VietMapAccessToken</key>
	<string>89cb1c3c260c27ea71a115ece3c8d7cec462e7a4c14f0944</string>
	<key>VietMapURL</key>
	<string>https://run.mocky.io/v3/ff325d44-9fdd-480f-9f0f-a9155bf362fa</string>
```
### Demo code

```dart

class VietmapNavigationScreen extends StatefulWidget {
  const VietmapNavigationScreen({super.key, this.to, this.from});

  final LatLng? to;
  final LatLng? from;
  @override
  State<VietmapNavigationScreen> createState() => _VietmapNavigationScreenState();
}

class _VietmapNavigationScreenState extends State<VietmapNavigationScreen> {
  MapNavigationViewController? _controller;
  late MapOptions _navigationOption;
  final _demoPlugin = DemoPlugin();

  List<WayPoint> wayPoints = [
    WayPoint(name: "You are here", latitude: 10.759091, longitude: 106.675817),
    WayPoint(name: "You are here", latitude: 10.762528, longitude: 106.653099)
  ];
  Widget instructionImage = const SizedBox.shrink();
  String guideDirection = "";
  Widget recenterButton = const SizedBox.shrink();
  RouteProgressEvent? routeProgressEvent;
  bool _isRouteBuilt = false;
  bool _isRunning = false;
  FocusNode focusNode = FocusNode();
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

    _navigationOption.apiKey =
        '89cb1c3c260c27ea71a115ece3c8d7cec462e7a4c14f0944';
    _navigationOption.mapStyle =
        "https://run.mocky.io/v3/ff325d44-9fdd-480f-9f0f-a9155bf362fa";
    // 'https://api.maptiler.com/maps/basic-v2/style.json?key=erfJ8OKYfrgKdU6J1SXm';

    _demoPlugin.setDefaultOptions(_navigationOption);
  }

  MapOptions? options;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        top: false,
        child: Stack(
          children: [
            NavigationView(
              mapOptions: _navigationOption,
              onMapCreated: (p0) {
                _controller = p0;

                WidgetsBinding.instance.addPostFrameCallback((timeStamp) async {
                  await Future.delayed(const Duration(milliseconds: 1000));
                  var listWaypoint = <WayPoint>[];
                  listWaypoint.add(WayPoint(
                      name: '',
                      latitude: widget.from?.latitude,
                      longitude: widget.from?.longitude));

                  listWaypoint.add(WayPoint(
                      name: '',
                      latitude: widget.to?.latitude,
                      longitude: widget.to?.longitude));
                  _controller?.buildAndStartNavigation(
                      wayPoints: listWaypoint,
                      profile: DrivingProfile.drivingTraffic);
                });
              },
              onMapMove: () => _showRecenterButton(),
              onRouteBuilt: (p0) {
                setState(() {
                  EasyLoading.dismiss();
                  _isRouteBuilt = true;
                });
              },
              onRouteProgressChange: (RouteProgressEvent routeProgressEvent) {
                setState(() {
                  this.routeProgressEvent = routeProgressEvent;
                });
                _setInstructionImage(routeProgressEvent.currentModifier,
                    routeProgressEvent.currentModifierType);
              },
              onArrival: () {
                _isRunning = false;
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                    content: Container(
                        height: 100,
                        color: Colors.red,
                        child: const Text('Bạn đã tới đích'))));
              },
            ),
            Positioned(
                top: MediaQuery.of(context).viewPadding.top,
                left: 0,
                child: BannerInstructionView(
                  routeProgressEvent: routeProgressEvent,
                  instructionIcon: instructionImage,
                )),
            Positioned(
                bottom: 0,
                child: BottomActionView(
                  recenterButton: recenterButton,
                  controller: _controller,
                  onOverviewCallback: _showRecenterButton,
                  onStopNavigationCallback: _onStopNavigation,
                  routeProgressEvent: routeProgressEvent,
                )),
          ],
        ),
      ),
    );
  }

  _showRecenterButton() {
    recenterButton = TextButton(
        onPressed: () {
          _controller?.recenter();
          recenterButton = const SizedBox.shrink();
        },
        child: Container(
            height: 50,
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
            decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(50),
                color: Colors.white,
                border: Border.all(color: Colors.black45, width: 1)),
            child: Row(
              children: const [
                Icon(
                  Icons.keyboard_double_arrow_up_sharp,
                  color: Colors.lightBlue,
                  size: 35,
                ),
                Text(
                  'Về giữa',
                  style: TextStyle(fontSize: 18, color: Colors.lightBlue),
                )
              ],
            )));
    setState(() {});
  }

  _setInstructionImage(String? modifier, String? type) {
    if (modifier != null && type != null) {
      List<String> data = [
        type.replaceAll(' ', '_'),
        modifier.replaceAll(' ', '_')
      ];
      String path = 'assets/navigation_symbol/${data.join('_')}.svg';
      setState(() {
        instructionImage = SvgPicture.asset(path, color: Colors.white);
      });
    }
  }

  _onStopNavigation() {
    Navigator.pop(context);
    setState(() {
      routeProgressEvent = null;
      _isRunning = false;
    });
  }
}
```