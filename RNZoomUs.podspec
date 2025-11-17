require "json"
package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "RNZoomUs"
  s.version      = package["version"]
  s.summary      = "RNZoomUs"
  s.description  = <<-DESC
                  React Native integration for Zoom SDK
                   DESC
  s.homepage     = "https://github.com/mieszko4/react-native-zoom-us"
  s.license      = "MIT"
  s.author       = { "author" => "author@domain.cn" }
  s.platform     = :ios, "13.0"

  s.source       = { :git => "https://github.com/mieszko4/react-native-zoom-us" }
  s.source_files = "ios/*.{h,m}"
  s.requires_arc = true

  s.static_framework = true
  s.dependency "React"
  s.dependency "ZoomSDK", '6.5.10.27930'

end

