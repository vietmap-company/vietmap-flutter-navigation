# Vietmap Flutter Navigation

[<img src="https://bizweb.dktcdn.net/100/415/690/themes/804206/assets/logo.png?1689561872933" height="40"/> </p>](https://vietmap.vn/maps-api)

Liên hệ [vietmap.vn](https://bit.ly/vietmap-api) để đăng kí key hợp lệ.

## Getting started

Thêm thư viện vào file pubspec.yaml
```yaml
  vietmap_flutter_navigation: latest_version
```

Kiểm tra phiên bản của thư viện tại [https://pub.dev/packages/vietmap_flutter_navigation](https://pub.dev/packages/vietmap_flutter_navigation)
 
hoặc chạy lệnh sau để thêm thư viện vào project:
```bash
  flutter pub add vietmap_flutter_navigation
```
## Cấu hình cho Android


Thêm đoạn code sau vào build.gradle (project) tại đường dẫn **android/build.gradle**

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


## Cấu hình cho iOS
Thêm đoạn code sau vào file Info.plist
```ruby
  <key>VietMapURL</key>
  <string>https://maps.vietmap.vn/api/maps/light/styles.json?apikey=YOUR_API_KEY_HERE</string>
  <key>VietMapAPIBaseURL</key>
  <string>https://maps.vietmap.vn/api/navigations/route/</string>
  <key>VietMapAccessToken</key>
  <string>YOUR_API_KEY_HERE</string>
    <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
  <string>This app requires location permission to working normally</string>
  <key>NSLocationAlwaysUsageDescription</key>
  <string>This app requires location permission to working normally</string>
  <key>NSLocationWhenInUseUsageDescription</key>
  <string>This app requires location permission to working normally</string>
```


Nâng min ios version lên 12.0 tại file `Podfile` (iOS), đường dẫn **ios/Podfile** (Bỏ comment dòng bên dưới)

```ruby
  platform :ios, '12.0' 
```

Tại terminal, cd vào thư mục ios và chạy lệnh sau để cài đặt pod file `(bỏ qua bước này nếu chỉ build cho Android hoặc chạy app trên Windows/Linux PC)`
```bash
  cd ios && pod install
```

- Nếu project hiển thị lỗi khi nâng version mới khi chạy lệnh `pod install`, vui lòng xóa các thư mục `ios/.symlinks`, `ios/Pods` và file `Podfile.lock`, sau đó chạy lệnh `pod install --repo-update` để cập nhật lại pod file.

## Các tính năng chính


### Import thư viện
```dart
  import 'package:vietmap_flutter_navigation/vietmap_flutter_navigation.dart';
```
### Khai báo các biến cần thiết và hàm cấu hình khởi tạo
```dart
  late MapOptions _navigationOption;
  final _vietmapNavigationPlugin = VietMapNavigationPlugin();

  List<WayPoint> wayPoints = [
    WayPoint(name: "origin point", latitude: 10.759091, longitude: 106.675817),
    WayPoint(
        name: "destination point", latitude: 10.762528, longitude: 106.653099)
  ];
  /// Hiển thị hình ảnh dẫn đường vào ngã rẽ tiếp theo
  Widget instructionImage = const SizedBox.shrink();

  Widget recenterButton = const SizedBox.shrink();
  RouteProgressEvent? routeProgressEvent;
 /// The controller to control the navigation, such as start, stop, recenter, overview,... 
  MapNavigationViewController? _navigationController;
```
Thêm hàm `initialize` vào `initState` để khởi tạo các tùy chọn cho bản đồ
```dart
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
        "YOUR_STYLE_HERE";

    _vietmapNavigationPlugin.setDefaultOptions(_navigationOption);
  }
``` 
- Thay `YOUR_API_KEY_HERE` bằng apikey được cung cấp bởi VietMap 

### Hiển thị Navigation view, bao gồm bản đồ và đường đi, điều hướng dẫn đường
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

### Hàm set hình ảnh dẫn đường
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
Chúng tôi sử dụng [flutter_svg](https://pub.dev/packages/flutter_svg) để hiển thị hình ảnh SVG.

Icon điều hướng dẫn đường được lưu trong thư mục [này](https://vietmapcorp-my.sharepoint.com/:u:/g/personal/thanhdt_vietmap_vn/EU0Heb0gMh1KtgCaoy5oih8BrOL6YKPWJUO-vXeGBp99hA?e=woyAvH), tải về, giải nén và thêm vào thư mục assets.

File thiết kế figma có thể tham khảo [tại đây](https://www.figma.com/file/rWyQ5TNtt6E5l8tPEE9Tkl/VietMap-navigation-symbol?type=design&node-id=1%3A457&mode=design&t=yszRZCTouxAdYXXJ-1)
 

### Thêm banner widget chỉ dẫn điều hướng 
```dart
  BannerInstructionView(
    routeProgressEvent: routeProgressEvent,
    instructionIcon: instructionImage,
  )
```
![Banner instruction view](https://github.com/vietmap-company/vietmap-flutter-navigation/raw/main/images/banner_instruction.png)

### Thêm các nút như xem tổng quan đường đi, về giữa để điều hướng dẫn đường
```dart
  BottomActionView(
    recenterButton: recenterButton,
    controller: _navigationController,
    routeProgressEvent: routeProgressEvent
  )
```

![Bottom action](https://github.com/vietmap-company/vietmap-flutter-navigation/raw/main/images/bottom_action.png)
Bạn có thể tuỳ chỉnh các widget này theo ý muốn.
Toàn bộ data được trả về từ hàm `onRouteProgressChange` được lưu trong biến `routeProgressEvent`.
### Thêm hàm dispose cho controller
```dart
  @override
  void dispose() {
    _navigationController?.dispose();
    super.dispose();
  }
```
Các hàm thường sử dụng
```dart
  /// Tìm đường mới từ 2 điểm, waypoint1 là điểm bắt đầu, 
  /// waypoint2 là điểm kết thúc.
  _navigationController?.buildRoute(wayPoints: <Waypoint>[waypoint1,waypoint2]);

  /// Bắt đầu dẫn đường, gọi sau khi đã gọi hàm buildRoute phía trên
  _navigationController?.startNavigation();

  /// Hàm tìm đường và bắt đầu dẫn đường khi tìm được đường đi
  _navigationController?.buildAndStartNavigation(
      wayPoints: wayPoints: <Waypoint>[waypoint1,waypoint2],
      profile: DrivingProfile.drivingTraffic);
  
  /// Hàm về giữa sau khi nhấn xem tông quan đường đi 
  /// hoặc người dùng di chuyển bản đồ tới vị trí khác
  _navigationController?.recenter();

  /// Hàm xem tổng quát đường đi
  _navigationController?.overview();

  /// Hàm tắt/bật tiếng khi dẫn đường
  _navigationController?.mute();

  /// Hàm kết thúc dẫn đường
  _navigationController?.finishNavigation();
```

## Thêm marker lên bản đồ
Chúng tôi cung cấp hàm `addImageMarkers` để thêm marker từ hình ảnh từ thư mục asset lên bản đồ.

### Marker
```dart
  /// Add a marker to the map
  List<Marker>? markers = await _navigationController?.addImageMarkers([
    Marker(
        imagePath: 'assets/50.png',
      latLng: const LatLng(10.762528, 106.653099)),
    Marker(
        imagePath: 'assets/40.png',
        latLng: const LatLng(10.762528, 106.753099)),
  ]);
``` 
## Lưu ý khi sử dụng
- Hàm **_navigationController?.buildRouteAndStartNavigation()** không nên để trong initState mà nên để trong hàm onPressed của button hoặc hàm onMapRendered của NavigationView để tránh crash app khi bản đồ chưa được khởi tạo hoàn toàn.
```dart
  onMapRendered: () {
    _navigationController?.buildAndStartNavigation(
    wayPoints: wayPoints: <Waypoint>[waypoint1,waypoint2],
    profile: DrivingProfile.drivingTraffic);  
  }
``` 

- Hãy chắc chắn rằng quyền truy cập vị trí đã được cấp trước khi điều hướng. Chúng tôi khuyến khích bạn sử dụng thư viện [geolocator](https://pub.dev/packages/geolocator) để xử lý quyền truy cập vị trí và lấy vị trí hiện tại của thiết bị.

Code mẫu màn hình dẫn đường [tại đây](./example/lib/main.dart)

Chúng tôi có một ứng dụng demo với [flutter_bloc](https://pub.dev/packages/flutter_bloc) và clean architecture pattern [tại đây](https://github.com/vietmap-company/flutter-navigation-example).
Vui lòng clone và chạy app để xem cách nó hoạt động. 

Bạn cũng có thể [tải ứng dụng demo](https://vmnavigation.page.link/navigation_demo) để xem cách nó hoạt động. 

## Lưu ý: Thay apikey được VietMap cung cấp vào toàn bộ tag _YOUR_API_KEY_HERE_ để ứng dụng hoạt động bình thường

Nếu có bất kỳ thắc mắc và hỗ trợ, vui lòng liên hệ:

[<img src="https://bizweb.dktcdn.net/100/415/690/themes/804206/assets/logo.png?1689561872933" height="40"/> </p>](https://vietmap.vn/maps-api)
Gửi email: [maps-api.support@vietmap.vn](mailto:maps-api.support@vietmap.vn)


Liên hệ [hỗ trợ](https://vietmap.vn/lien-he)

Tài liệu api [tại đây](https://maps.vietmap.vn/docs/map-api/overview/)


Có bug xảy ra? [Open an issue](https://github.com/vietmap-company/flutter-map-sdk/issues). Nếu có thể hãy đính kèm cả log lỗi.


Yêu cầu tính năng mới [Open an issue](https://github.com/vietmap-company/flutter-map-sdk/issues). Cho chúng tôi biết bạn muốn chúng tôi cải thiện điều gì.
 