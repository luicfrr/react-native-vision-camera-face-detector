require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "VisionCameraFaceDetector"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "15.5" } # 15.5 is the minimum version for GoogleMLKit/FaceDetection 7.0.0
  s.source       = { :git => "https://github.com/luicfrr/react-native-vision-camera-face-detector.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift}"

  s.dependency "React-Core"
  s.dependency "GoogleMLKit/FaceDetection" , "8.0.0"
  s.dependency "VisionCamera"
end
