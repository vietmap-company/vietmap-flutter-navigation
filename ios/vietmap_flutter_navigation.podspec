#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint demo_plugin.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'vietmap_flutter_navigation'
  s.version          = '0.0.1'
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
  s.dependency 'VietMapNavigation', '2.1.5'
  s.dependency 'VietMapCoreNavigation', '2.1.5'
  s.platform = :ios, '12.0'
  # Flutter.framework does not contain a i386 slice.
  # s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386', 'FRAMEWORK_SEARCH_PATHS' => '$(PROJECT_DIR)/Frameworks' }
  s.swift_version = '5.0'

  # s.vendored_frameworks = 'Frameworks/*.xcframework'

end
