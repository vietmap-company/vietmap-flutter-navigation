# Vietmap Flutter Navigation

[<img src="https://bizweb.dktcdn.net/100/415/690/themes/804206/assets/logo.png?1689561872933" height="40"/> </p>](https://vietmap.vn/maps-api)

Contact [vietmap.vn](https://bit.ly/vietmap-api) to register a valid key.

<!-- [Tài liệu tiếng Việt](./README.vi.md) -->

## Getting started

Add library to `pubspec.yaml` file
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

Add below code to AndroidManifest (for android 14 and above)
```xml
<application>
...
  <!-- Add this code block -->
  <service
      android:name="vn.vietmap.services.android.navigation.v5.navigation.NavigationService"
      android:foregroundServiceType="location"
      android:exported="false">
  </service>
</application>
```
Add below permission to AndroidManifest.xml
```xml
 <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```
Upgrade the minSdkVersion to a minimum is 24 in the build.gradle (app) file, at path **android/app/build.gradle**
```gradle
  minSdkVersion 24
```

## iOS config
Add the below codes to the Info.plist file. Replace the **`YOUR_API_KEY_HERE`** with your API key.
```ruby
  <key>VietMapURL</key>
  <string>https://maps.vietmap.vn/api/maps/light/styles.json?apikey=YOUR_API_KEY_HERE</string>
  <key>VietMapAPIBaseURL</key>
  <string>https://maps.vietmap.vn/api/navigations/route/</string>
  <key>VietMapAccessToken</key>
  <string>YOUR_API_KEY_HERE</string>  
  <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
  <string>Your request location description</string>
  <key>NSLocationAlwaysUsageDescription</key>
  <string>Your request location description</string>
  <key>NSLocationWhenInUseUsageDescription</key>
  <string>Your request location description</string>
```

Upgrade min ios version to 12.0 in the Podfile (iOS) file, at path **ios/Podfile** (uncomment the line below)

```ruby
  platform :ios, '12.0' 
```

In your terminal, cd to the ios folder and run the command below to install the pod file `(you can skip this step if you only build for Android or run the app on the Windows/Linux PC)`
```bash
  cd ios && pod install
```

- If the project shows an issue when upgrading to the new version when running the `pod install` command, please remove the `ios/.symlinks`, `ios/Pods` folders, and `Podfile.lock` file, then run the `pod install --repo-update` command to update the pod file. 


## Main characteristics
### Import the library
```dart
  import 'package:vietmap_flutter_navigation/vietmap_flutter_navigation.dart';
```
### Define necessary variables
```dart
  // Define the map options
  late MapOptions _navigationOption;

  final _vietmapNavigationPlugin = VietMapNavigationPlugin();

  List<LatLng> waypoints = const [
    LatLng(10.759091, 106.675817),
    LatLng(10.762528, 106.653099)
  ];
  /// Display the guide instruction image to the next turn 
  Widget instructionImage = const SizedBox.shrink();

  Widget recenterButton = const SizedBox.shrink();
  
  /// RouteProgressEvent contains the route information, current location, next turn, distance, duration,...
  /// This variable is update real time when the navigation is started
  RouteProgressEvent? routeProgressEvent;
  
  /// The controller to control the navigation, such as start, stop, recenter, overview,... 
  MapNavigationViewController? _navigationController;
```
Add the `initialize` function to `initState` function to initialize the map options
```dart
  @override
  void initState() {
    super.initState();
    initialize();
  }

  Future<void> initialize() {
    if (!mounted) return;
    _navigationOption = _vietmapNavigationPlugin.getDefaultOptions();
    /// set the simulate route to true to test the navigation without the real location
    _navigationOption.simulateRoute = false;

    _navigationOption.apiKey =
        'YOUR_API_KEY_HERE';
    _navigationOption.mapStyle =
        "https://maps.vietmap.vn/api/maps/light/styles.json?apikey=YOUR_API_KEY_HERE";

    _vietmapNavigationPlugin.setDefaultOptions(_navigationOption);
  }
```
- Replace `your apikey` which is provided by VietMap with `YOUR_API_KEY_HERE` tag to the application works normally

### Display the Navigation view, including map view, route, and navigation
```dart
  NavigationView(
    mapOptions: _navigationOption,
    onMapCreated: (controller) {
      _navigationController = controller;
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
- In the `routeProgressEvent` response, we provide the `currentLocation` and `snappedLocation`, which are the raw location of the device and the snapped location on the route, respectively. You can use one of them to track the location of the device.

### Set the instruction icon from routeProgressEvent data.
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
We use [flutter_svg](https://pub.dev/packages/flutter_svg) to display the SVG image.

Instruction icon [here](https://vietmapcorp-my.sharepoint.com/:u:/g/personal/thanhdt_vietmap_vn/EU0Heb0gMh1KtgCaoy5oih8BrOL6YKPWJUO-vXeGBp99hA?e=woyAvH), download, extract, and add to the assets folder.

Figma design for the instruction [here](https://www.figma.com/file/rWyQ5TNtt6E5l8tPEE9Tkl/VietMap-navigation-symbol?type=design&node-id=1%3A457&mode=design&t=yszRZCTouxAdYXXJ-1), please copy and design your own icon.
### Build a route between two locations
- We provide the `buildRoute` function to build a route between two locations. You can add more than 2 locations to the `wayPoints` variable.
<div style="width:100%; text-align:center" >
  <img src="https://github.com/vietmap-company/vietmap-flutter-navigation/raw/main/images/navigation.gif"  alt="Vietmap navigation demo gif" width="400"/>
</div>

- We're adding the `onMapLongClick` callback to the `NavigationView` to build a route when the user long clicks on the map.

```dart
  NavigationView(

        ...
  
        onMapLongClick: (LatLng? latLng, Point? point) {
          if (latLng == null) return;
          _navigationController?.buildRoute(wayPoints: [
            /// Replace the latitude and longitude with your origin location
            LatLng(10.759173, 106.675879),
            latLng
          ], profile: DrivingProfile.cycling);
        },
      ),
```
### Start navigation when the route is built successfully
- We provide the `onRouteBuilt` callback which responds the route when it is built successfully. Only the first route of the response list will be returned.
- We provide the `onNewRouteSelected` callback which responds the route while the user selects another route from the map.
```dart
  NavigationView(

    ...

    onRouteBuilt: (DirectionRoute route) {
      
    },
    onNewRouteSelected: (DirectionRoute route) {
      
    },
  ),
```
-  You can start the navigation when the route is built successfully by calling the `_navigationController?.startNavigation` function.
```dart
  _navigationController?.startNavigation();
```
 
### Add banner instructions to display icon, route name, next turn guide,...
```dart
  BannerInstructionView(
    routeProgressEvent: routeProgressEvent,
    instructionIcon: instructionImage,
  )
```
![Banner instruction view](https://github.com/vietmap-company/vietmap-flutter-navigation/raw/main/images/banner_instruction_en.png)
### Add the bottom view, which contains the overview route, recenter, and the stop navigation button.
```dart
  BottomActionView(
    recenterButton: recenterButton,
    controller: _navigationController, 
    routeProgressEvent: routeProgressEvent
  )
```
![Bottom action](https://github.com/vietmap-company/vietmap-flutter-navigation/raw/main/images/bottom_action_en.png)
You can customize all of the widgets above to fit your design.
All data is provided by the `routeProgressEvent` variable.
### Add the dispose function for the navigation controller
```dart
  @override
  void dispose() {
    _navigationController?.onDispose();
    super.dispose();
  }
```
### Useful function
- Build a route and start navigation. The below functions are used to build a route to start navigation.
```dart
  /// Find a new route between two locations (you can add more than 2 locations)
  _navigationController?.buildRoute(wayPoints: <LatLng>[currentLocation, destinationLocation]);

  /// Start navigation, call after the buildRoute have a response.
  _navigationController?.startNavigation();

  /// Find route and start when the api response at least 1 route
  _navigationController?.buildAndStartNavigation(
      wayPoints: wayPoints: <LatLng>[currentLocation, destinationLocation],
      profile: DrivingProfile.drivingTraffic);
```

- Recenter to the current location, overview the route, turn on/off the navigation voice guide, and stop the navigation. The below functions are used to control the navigation and call after the navigation is started.
```dart
  /// recenter to the user location
  _navigationController?.recenter();

  /// Overview the route
  _navigationController?.overview();

  /// Turn on/off the navigation voice guide
  _navigationController?.mute();

  /// Stop the navigation
  _navigationController?.finishNavigation();
```
- Move the camera to the specific location
```dart
  /// Move the camera to the specific location
  _navigationController?.moveCamera(
      latLng: const LatLng(22.762528, 106.653099),
      zoom: 8,
      tilt: 0,
      bearing: 0);
```
- Animate the camera to the specific location
```dart
  /// Animate the camera to the specific location 
  _navigationController?.animateCamera(
    latLng: const LatLng(22.762528, 106.653099),
    zoom: 8,
    tilt: 0,
    bearing: 0);
```
## Add a marker to the map
We provide the `addImageMarkers` function to add multiple markers to the map
- Add a marker from the asset image  

### Marker from assets image
```dart
  /// Add a marker to the map
  List<NavigationMarker>? markers = await _navigationController?.addImageMarkers([
    NavigationMarker(
        imagePath: 'assets/50.png',
      latLng: const LatLng(10.762528, 106.653099)),
    NavigationMarker(
        imagePath: 'assets/40.png',
        latLng: const LatLng(10.762528, 106.753099),
        width: 80,
        height: 80),
  ]);
``` 
- NOTE: The width and height must be both null or both have a value. If one of them has a value, the other must have a value too.

## Troubleshooting
- We strongly recommend you call the **_navigationController?.buildRouteAndStartNavigation()** in a `button` or `onMapRendered` callback, which is called when the map is rendered successfully to ensure that the application does not crash while executing some function while our SDK is rendering the map. 
```dart
  onMapRendered: () {
    _navigationController?.buildAndStartNavigation(
    wayPoints: wayPoints: <Waypoint>[waypoint1,waypoint2],
    profile: DrivingProfile.drivingTraffic);  
  }
```
- Please ensure that the location permission has been granted before navigating. We recommend you use the [geolocator](https://pub.dev/packages/geolocator) package to handle the location permission and get the current location of the device.

Demo code [here](./example/lib/main.dart)

We have a demo app with [flutter_bloc](https://pub.dev/packages/flutter_bloc) and clean architecture pattern [here](https://github.com/vietmap-company/flutter-navigation-example).
Please clone and run the app to see how it works.

You can also [download the example app](https://vmnavigation.page.link/navigation_demo) to see how it works.


## Note: Replace apikey which is provided by VietMap to all `YOUR_API_KEY_HERE` tag to the application work normally



[<img src="https://bizweb.dktcdn.net/100/415/690/themes/804206/assets/logo.png?1689561872933" height="40"/> </p>](https://vietmap.vn/maps-api)
Email us: [maps-api.support@vietmap.vn](mailto:maps-api.support@vietmap.vn)

Vietmap API and price [here](https://vietmap.vn/maps-api)

Contact for [support](https://vietmap.vn/lien-he)

Vietmap API document [here](https://maps.vietmap.vn/docs/map-api/overview/)

Have a bug to report? [Open an issue](https://github.com/vietmap-company/flutter-map-sdk/issues). If possible, include a full log and information that shows the issue.


Have a feature request? [Open an issue](https://github.com/vietmap-company/flutter-map-sdk/issues). Tell us what the feature should do and why you want the feature.
 