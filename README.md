# Vietmap Flutter Navigation

[<img src="https://bizweb.dktcdn.net/100/415/690/themes/804206/assets/logo.png?1689561872933" height="40"/> </p>](https://vietmap.vn/maps-api)

Contact [vietmap.vn](https://bit.ly/vietmap-api) to register a valid key.

## Getting started

Add library to pubspec.yaml file
```yaml
  vietmap_flutter_navigation: latest_version
```

Check the latest version at [https://pub.dev/packages/vietmap_flutter_navigation](https://pub.dev/packages/vietmap_flutter_navigation)
 
or run this command in the terminal to add the library to the project:
```bash
  flutter pub add vietmap_flutter_navigation
```
## Android config


Add the below code to the build.gradle (project) file at path **android/build.gradle**

```gradle
 maven { url "https://jitpack.io" }
```


at the repositories block


```gradle

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
    }
}
```


## iOS config
Add the below codes to the Info.plist file. Replace your API key to **YOUR_API_KEY_HERE** 
```
	<key>VietMapAPIBaseURL</key>
	<string>https://maps.vietmap.vn/api/navigations/route/</string>
	<key>VietMapAccessToken</key>
	<string>YOUR_API_KEY_HERE</string>
	<key>VietMapURL</key>
	<string>https://run.mocky.io/v3/64ad9ec6-2715-4d56-a335-dedbfe5bc46d</string>
	<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
	<string>This app requires location permission to working normally</string>
	<key>NSLocationAlwaysUsageDescription</key>
	<string>This app requires location permission to working normally</string>
	<key>NSLocationWhenInUseUsageDescription</key>
	<string>This app requires location permission to working normally</string>
```


## Main characteristics

```dart
  late MapOptions _navigationOption;
  final _vietmapNavigationPlugin = VietMapNavigationPlugin();

  List<WayPoint> wayPoints = [
    WayPoint(name: "origin point", latitude: 10.759091, longitude: 106.675817),
    WayPoint(
        name: "destination point", latitude: 10.762528, longitude: 106.653099)
  ];
  /// Display the guide instruction image to the next turn
  Widget instructionImage = const SizedBox.shrink();

  Widget recenterButton = const SizedBox.shrink();
  RouteProgressEvent? routeProgressEvent;

  @override
  void initState() {
    super.initState();
    initialize();
  }

  Future<void> initialize() async {
    if (!mounted) return;
    _navigationOption = _vietmapNavigationPlugin.getDefaultOptions();
    _navigationOption.simulateRoute = false;

    _navigationOption.apiKey =
        'YOUR_API_KEY_HERE';
    _navigationOption.mapStyle =
        "https://run.mocky.io/v3/64ad9ec6-2715-4d56-a335-dedbfe5bc46d";

    _vietmapNavigationPlugin.setDefaultOptions(_navigationOption);
  }
```

Display the Navigation view, include map view, route and navigation
```dart
          NavigationView(
            mapOptions: _navigationOption,
            onMapCreated: (controller) {
              _controller = controller;
            },
            onRouteProgressChange: (RouteProgressEvent routeProgressEvent) {
              setState(() {
                this.routeProgressEvent = routeProgressEvent;
              });
              _setInstructionImage(routeProgressEvent.currentModifier,
                  routeProgressEvent.currentModifierType);
            },
          ),
```

Add banner instruction to display icon, route name,...
```dart
            BannerInstructionView(
              routeProgressEvent: routeProgressEvent,
              instructionIcon: instructionImage,
            )
```
Set instruction icon from routeProgress data.
```dart
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
```
Instruction icon [here](./example/assets/navigation_symbol), copy and paste to your project.

Figma design [here](https://www.figma.com/file/rWyQ5TNtt6E5l8tPEE9Tkl/VietMap-navigation-symbol?type=design&node-id=1%3A457&mode=design&t=yszRZCTouxAdYXXJ-1)Add the Bottom view, which contains the overview route, recenter and stop navigation button.
```dart
            BottomActionView(
              recenterButton: recenterButton,
              controller: _controller,
              onOverviewCallback: _showRecenterButton,
              onStopNavigationCallback: _onStopNavigation,
              routeProgressEvent: routeProgressEvent
            )
```

Add the dispose function for the navigation controller
```dart
  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }
```
Useful function
```dart
          /// Find a new route between two locations, 
          /// waypoint1 is origin, waypoint2 is destination.
            _controller?.buildRoute(wayPoints: <Waypoint>[waypoint1,waypoint2]);

          /// Start navigation, call after the buildRoute have a response.
            _controller?.startNavigation();


          /// Find route and start when the api response at least 1 route
            _controller?.buildAndStartNavigation(
                wayPoints: wayPoints: <Waypoint>[waypoint1,waypoint2],
                profile: DrivingProfile.drivingTraffic);
          
          /// recenter to the navigation
          _controller?.recenter();

          /// Overview the route
          _controller?.overview();

          /// Turn on/off the navigation voice guide
          _controller?.mute();

          /// Stop the navigation
          _controller?.finishNavigation();
```

## Troubleshooting
- We strongly recommend you call the **_controller?.buildRouteAndStartNavigation()** in a button or onMapRendered callback, which is called when the map is rendered successfully. 
```dart
    onMapRendered: () {
      _controller?.buildAndStartNavigation(
      wayPoints: wayPoints: <Waypoint>[waypoint1,waypoint2],
      profile: DrivingProfile.drivingTraffic);  
    }
```

Demo code [here](./example/lib/main.dart)
# Note: Replace apikey which is provided by VietMap to all YOUR_API_KEY_HERE tag to the application work normally



[<img src="https://bizweb.dktcdn.net/100/415/690/themes/804206/assets/logo.png?1689561872933" height="40"/> </p>](https://vietmap.vn/maps-api)
Email us: [maps-api.support@vietmap.vn](mailto:maps-api.support@vietmap.vn)


Contact for [support](https://vietmap.vn/lien-he)

Vietmap API document [here](https://maps.vietmap.vn/docs/map-api/overview/)

Have a bug to report? [Open an issue](https://github.com/vietmap-company/flutter-map-sdk/issues). If possible, include a full log and information which shows the issue.


Have a feature request? [Open an issue](https://github.com/vietmap-company/flutter-map-sdk/issues). Tell us what the feature should do and why you want the feature.

[Tài liệu tiếng Việt](./README.vi.md)