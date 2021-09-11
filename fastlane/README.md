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

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
