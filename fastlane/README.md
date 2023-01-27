fastlane documentation
================
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew install fastlane`

# Available Actions
## Android
### android test
```
fastlane android test
```
Runs all the tests
### android build_test
```
fastlane android build_test
```
Build all the tests without running them
### android build_androidTest
```
fastlane android build_androidTest
```
Build all the instrumented tests without running them
### android beta
```
fastlane android beta
```
Submit a new Beta Build to Crashlytics Beta
### android deploy
```
fastlane android deploy
```
Deploy a new version to the Google Play
### android build_signed_apk_standard_release
```
fastlane android build_signed_apk_standard_release
```
Build signed APK (flavor: standard ; type: Release)
### android build_amazon_apk
```
fastlane android build_amazon_apk
```
Build amazon app store APK
### android build_for_screengrab
```
fastlane android build_for_screengrab
```
Build debug and test APK for screenshots
### android copy_test_pics_for_screengrab
```
fastlane android copy_test_pics_for_screengrab
```
Copy sample pictures to the device
### android screenshots
```
fastlane android screenshots
```
Do screenshots
### android disable_animation
```
fastlane android disable_animation
```
disable animations on device
### android prepare_device_for_tests
```
fastlane android prepare_device_for_tests
```
Prepare device for tests
### android get_output_of_tests
```
fastlane android get_output_of_tests
```
Get test output from device
### android connectedCheck
```
fastlane android connectedCheck
```
Run all the instrumented tests except screenshots
### android connectedCheck_with_screenrecord
```
fastlane android connectedCheck_with_screenrecord
```
Run all the instrumented tests except screenshots and save screenrecords

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
