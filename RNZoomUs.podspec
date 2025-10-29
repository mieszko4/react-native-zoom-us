require "json"
package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "RNZoomUs"
  s.version      = "6.4.5.24566"
  s.summary      = "RNZoomUs"
  s.description  = <<-DESC
                  React Native integration for Zoom SDK
                   DESC
  s.homepage     = "https://github.com/mieszko4/react-native-zoom-us"
  s.license      = "MIT"
  s.author       = { "author" => "author@domain.cn" }
  s.platform     = :ios, "12.0"

  s.source       = { :git => "https://github.com/mieszko4/react-native-zoom-us" }
  s.source_files = "ios/*.{h,m}"
  s.requires_arc = true

  s.static_framework = true
  s.dependency "React"
  s.dependency "RNZoomSDK", '6.4.5.24566'
  #  s.vendored_frameworks = ["ios/lib/MobileRTC.xcframework", "ios/lib/MobileRTCScreenShare.xcframework", "ios/lib/zoomcml.xcframework"]
  # s.resource = 'ios/lib/MobileRTCResources.bundle'
  # s.libraries = "sqlite3", "z.1.2.5", "c++"
  # s.weak_framework = 'VideoToolbox', 'CoreMedia', 'CoreVideo', 'CoreGraphics', 'ReplayKit'
  # s.requires_arc = true

end

