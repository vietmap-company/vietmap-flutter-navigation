#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint demo_plugin.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'vietmap_flutter_navigation'
  s.version          = '0.0.3'
  s.summary          = 'Vietmap flutter navigation.'
  s.description      = <<-DESC
Vietmap flutter navigation.
                       DESC
  s.homepage         = 'http://vietmap.vn'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Vietmap company' => 'support@vietmap.vn' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.dependency 'VietMapNavigation', '3.3.0'
  s.dependency 'VietMapCoreNavigation', '3.1.0'
  s.platform = :ios, '12.0'
  # Flutter.framework does not contain a i386 slice.
  # s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386', 'FRAMEWORK_SEARCH_PATHS' => '$(PROJECT_DIR)/Frameworks' }
  s.swift_version = '5.0'

  # s.vendored_frameworks = 'Frameworks/*.xcframework'
  s.resource_bundles = {'vietmap_flutter_navigation_privacy' => ['vietmap_flutter_navigation/Sources/Resources/PrivacyInfo.xcprivacy']}
end
