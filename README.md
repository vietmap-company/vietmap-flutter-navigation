# Vietmap Flutter navigation

Vietmap Flutter navigation

## Getting Started

Thêm thư viện vào file pubspec.yaml
```yaml

  vietmap_flutter_navigation: 
    git: https://github.com/vietmap-company/vietmap-flutter-navigation
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
```plist

	<key>VietMapAPIBaseURL</key>
	<string>https://maps.vietmap.vn/api/navigations/route/</string>
	<key>VietMapAccessToken</key>
	<string>89cb1c3c260c27ea71a115ece3c8d7cec462e7a4c14f0944</string>
	<key>VietMapURL</key>
	<string>https://run.mocky.io/v3/ff325d44-9fdd-480f-9f0f-a9155bf362fa</string>
```
### Demo code

```dart

class DemoAndroidScreen extends StatefulWidget {
  const DemoAndroidScreen({super.key});

  @override
  State<DemoAndroidScreen> createState() => _DemoAndroidScreenState();
}

class _DemoAndroidScreenState extends State<DemoAndroidScreen> {
  MapNavigationViewController? _controller;
  late MapOptions _navigationOption;
  final _demoPlugin = VietMapNavigationPlugin();

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
    // This is demo key, to get a valid key, contact https://vietmap.vn/maps-api
    _navigationOption.apiKey =
        '89cb1c3c260c27ea71a115ece3c8d7cec462e7a4c14f0944';
    _navigationOption.mapStyle =
        "https://run.mocky.io/v3/ff325d44-9fdd-480f-9f0f-a9155bf362fa";

    _demoPlugin.setDefaultOptions(_navigationOption);
  }

  MapOptions? options;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Stack(
          children: [
            NavigationView(
              mapOptions: _navigationOption,
              onMapCreated: (p0) {
                _controller = p0;
              },
              onMapMove: () => _showRecenterButton(),
              onRouteBuilt: (p0) {
                setState(() {
                  EasyLoading.dismiss();
                  _isRouteBuilt = true;
                });
              },
              onMapLongClick: (WayPoint? point) async {
                EasyLoading.show();
                var data =
                    await GetLocationFromLatLngUseCase(VietmapApiRepositories())
                        .call(LocationPoint(
                            lat: point?.latitude ?? 0,
                            long: point?.longitude ?? 0));
                EasyLoading.dismiss();
                data.fold((l) {
                  ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Có lỗi xảy ra')));
                }, (r) => _showBottomSheetInfo(r));
              },
              onMapClick: (WayPoint? point) async {
                if (focusNode.hasFocus) {
                  FocusScope.of(context).requestFocus(FocusNode());
                  return;
                }
                EasyLoading.show();
                var data =
                    await GetLocationFromLatLngUseCase(VietmapApiRepositories())
                        .call(LocationPoint(
                            lat: point?.latitude ?? 0,
                            long: point?.longitude ?? 0));
                EasyLoading.dismiss();
                data.fold((l) {
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
                      content:
                          Text('Không tìm thấy địa điểm gần vị trí bạn chọn')));
                }, (r) => _showBottomSheetInfo(r));
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
                top: 0,
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
            _isRunning
                ? const SizedBox.shrink()
                : Positioned(
                    top: 30,
                    child: FloatingSearchBar(
                      focusNode: focusNode,
                      onSearchItemClick: (p0) async {
                        EasyLoading.show();
                        VietmapPlaceModel? data;
                        var res = await GetPlaceDetailUseCase(
                                VietmapApiRepositories())
                            .call(p0.refId ?? '');
                        res.fold((l) {
                          EasyLoading.dismiss();
                          return;
                        }, (r) {
                          data = r;
                        });
                        wayPoints.clear();
                        var location = await Geolocator.getCurrentPosition();
                        wayPoints.add(WayPoint(
                            name: 'destination',
                            latitude: location.latitude,
                            longitude: location.longitude));
                        if (data?.lat != null) {
                          wayPoints.add(WayPoint(
                              name: '',
                              latitude: data?.lat,
                              longitude: data?.lng));
                        }
                        _controller?.buildRoute(wayPoints: wayPoints);
                      },
                    )),
            _isRouteBuilt && !_isRunning
                ? Positioned(
                    bottom: 20,
                    left: MediaQuery.of(context).size.width / 2 - 25,
                    child: ElevatedButton(
                        style: ButtonStyle(
                            shape: MaterialStateProperty.all<
                                    RoundedRectangleBorder>(
                                RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(18.0),
                                    side:
                                        const BorderSide(color: Colors.blue)))),
                        onPressed: () {
                          _isRunning = true;
                          _controller?.startNavigation();
                        },
                        child: const Text('Bắt đầu')),
                  )
                : const SizedBox.shrink()
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
    setState(() {
      routeProgressEvent = null;
      _isRunning = false;
    });
  }

  _showBottomSheetInfo(VietmapReverseModel data) {
    showModalBottomSheet(
        isScrollControlled: true,
        context: context,
        builder: (_) => AddressInfo(
              data: data,
              buildRoute: () async {
                EasyLoading.show();
                wayPoints.clear();
                var location = await Geolocator.getCurrentPosition();

                wayPoints.add(WayPoint(
                    name: 'destination',
                    latitude: location.latitude,
                    longitude: location.longitude));
                if (data.lat != null) {
                  wayPoints.add(WayPoint(
                      name: '', latitude: data.lat, longitude: data.lng));
                }
                _controller?.buildRoute(wayPoints: wayPoints);
                if (!mounted) return;
                Navigator.pop(context);
              },
              buildAndStartRoute: () async {
                EasyLoading.show();
                wayPoints.clear();
                var location = await Geolocator.getCurrentPosition();
                wayPoints.add(WayPoint(
                    name: 'destination',
                    latitude: location.latitude,
                    longitude: location.longitude));
                if (data.lat != null) {
                  wayPoints.add(WayPoint(
                      name: '', latitude: data.lat, longitude: data.lng));
                }
                _controller?.buildAndStartNavigation(
                    wayPoints: wayPoints,
                    profile: DrivingProfile.drivingTraffic);
                setState(() {
                  _isRunning = true;
                });
                if (!mounted) return;
                Navigator.pop(context);
              },
            ));
  }
}

```