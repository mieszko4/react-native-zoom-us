
Pod::Spec.new do |s|
  s.name         = "RNZoomUs"
  s.version      = "1.0.0"
  s.summary      = "RNZoomUs"
  s.description  = <<-DESC
                  RNZoomUs
                   DESC
  s.homepage     = "https://zoom.us/"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/author/RNZoomUs.git", :tag => "master" }
  s.source_files  = "RNZoomUs/**/*.{h,m}"
  s.requires_arc = true


  s.dependency "React"
  #s.dependency "others"

end

  