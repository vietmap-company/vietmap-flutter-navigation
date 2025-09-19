## 4.0.0
* Upgrade `vietmap_gl_platform_interface` to `1.0.0`
* Migrate to new Flutter Gradle configuration 
## 3.3.2
* Bump `vietmap_gl_platform_interface` to `0.3.0`
## 3.3.1
* Upgrade to latest AGP
## 3.3.0
* Draw route arrow maneuver below road name on `iOS` and `Android`
## 3.2.0
* Draw route arrow maneuver on `iOS` and `Android`
## 3.1.1
* Improve navigation UX on iOS
* Add width and height for `NavigationMarker`.
* Fix required iOS version
* Snap user location to map
* Add `moveCamera` and `animateCamera` method for `MapNavigationViewController`
## 3.0.0
* Fix route not build on android 12 and above
* Fix navigation not working on android 12 and above
* Fix crash on android 14
* Migration to `Metal` render for `iOS`
## 2.2.0 
* Upgrade `vietmap_gl_platform_interface` to version `0.2.0`
## 2.1.2
* Fix error when fetch route on android 9 and below
* Update navigation instruction icon for example project
## 2.1.1
* Fix `NavigationView` display over another widget while hide the app and open the app again
* Optimize `NavigationView` performance
* Fix memory leaks while hide the app and open the app again with android 10 and above
## 2.1.0
* Add example project
## 2.0.0
* Release v2.0.0
* Fix navigation view not show on first load with android 10
## 2.0.0-beta-4
* Upgrade `targetSDK` and `compileSDK` to 34
## 2.0.0-beta-3
* Fix `buildAndStartNavigation` ios not working
## 2.0.0-beta-2
* Add `RECEIVER_EXPORTED` for android `BroadCast Receive`
## 2.0.0-beta-1
* Fix iOS routing - motorcycle profile

## 2.0.0-beta
* Created a new feature to ensure that the API key provided by Vietmap can only be used by the application(s) that register with our system.
* Add encrypted data for API key verification that cannot be used by 3rd parties without consent from Vietmap or your organization
## 1.5.4
* Fix android `LocationComponent` overload
## 1.5.3
* Upgrade `vietmap_gl_platform_interface` to version `0.1.5`
## 1.5.2 
* Fix `NavigationView` display over another widget, when the user pushes from the navigation view to another screen, then hide the app and open the app again
## 1.5.1
* Deprecate `WayPoint` class, use `LatLng` instead
* Upgrade `vietmap_gl_platform_interface` to `0.1.4`
## 1.5.0
* Upgrade `vietmap_gl_platform_interface` to `0.1.3`
## 1.4.8
* Provide `queryRenderedFeatures` method to get point data from map
* Provide user clicked point data to `onMapClick` and `onMapLongClick` callback
## 1.4.7
* Update `README.md` document
## 1.4.5
* Upgrade `vietmap_gl_platform_interface` to `0.1.2`
## 1.4.4
* Update `README.md` document
* Refactor `Marker` to `NavigationMarker`
## 1.4.1
* Fix screen not keep awake when navigation on `iOS`
* Update `README.md` document
* Update `License` to `BSD 3-Clause License`

## 1.4.0
* Update response data for `onProgressChange` callback
* Upgrade `vietmap_gl_platform_interface` to `0.1.0`

## 1.3.2
* Update draw route below road name for `iOS`
* Update document for `pod install` error when build `iOS`
## 1.3.1
* Update android mapview SDK
## 1.3.0
* Update iOS native SDK to `1.1.0`
* Update android native SDK to `2.1.0`
## 1.2.8
* Update draw route below road name for android
## 1.2.7
* Remove unnecessary library
* Update README.md document
## 1.2.6
* Update target development to `12` for iOS
## 1.2.5
* Update `xcode 15`
## 1.2.4
* Fix crash when build route
## 1.2.3
* Update `README.md `document
* Update map style url
## 1.2.2
- Optimize UX when user direction to next turn on `Android`
## 1.2.1
- Add `addMarkers` from image or flutter widget feature
## 1.2.0
- Update overview route padding
- Fix `onProgressChange` crash in android
- Remove unused code
- Update  [vietmap](https://vietmap.vn/maps-api) homepage
## 1.1.9
- Update auto overview route when the route built success
## 1.1.8
- Fix `onProgressChange` response data when start selected route ios
## 1.1.7
- Update route info when user selected new route while navigation is running
## 1.1.6
- Fix `VietMapEvents.ON_NEW_ROUTE_SELECTED` event
## 1.1.5
- Add `onNewRouteSelected` callback
## 1.1.4
- Update `README.md` document
## 1.1.3
- Update `README.md` document
## 1.1.2
- Fix warning on load drawable image
## 1.1.1
- Optimize navigation performance
## 1.1.0
- Optimize navigation performance
## 1.0.6
- Fix crash on android when click finish navigation
## 1.0.5
- Migrate to vietmap android navigation sdk version 2.0.0
- Fix android overview route
## 1.0.4
- Fix cancel navigation iOS
## 1.0.3
- Update `readme.md` document
## 1.0.2
- Remove dead code
- Update document 
- Optimize android navigation performance
## 1.0.1
- Fix simulate route on iOS
## 1.0.0
- Optimize iOS navigation performance
- Fix android crash when open navigation
## 0.0.9
- Add `onMapRendered` callback
## 0.0.8
- Remove unnecessary library
## 0.0.7
- Optimize Android navigation config 
- Fix android crash when open navigation
## 0.0.6
- Update Readme.md to support custom vector tile
## 0.0.5
- Support Vietmap custom vector tile
## 0.0.4
- Fix build route and follow location iOS
## 0.0.3
- Fix calculate arrival time
## 0.0.2
- Update code sample and guide to README.MD
## 0.0.1
- Release stable flutter version
