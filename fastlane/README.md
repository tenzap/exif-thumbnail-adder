fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android test

```sh
[bundle exec] fastlane android test
```

Runs all the tests

### android build_test

```sh
[bundle exec] fastlane android build_test
```

Build all the tests without running them

### android build_androidTest

```sh
[bundle exec] fastlane android build_androidTest
```

Build all the instrumented tests without running them

### android build_release

```sh
[bundle exec] fastlane android build_release
```

Build Release APK & AAB

### android beta

```sh
[bundle exec] fastlane android beta
```

Submit a new Beta Build to Crashlytics Beta

### android deploy

```sh
[bundle exec] fastlane android deploy
```

Deploy a new version to the Google Play

### android get_version_code

```sh
[bundle exec] fastlane android get_version_code
```

get version_code

### android get_version_name

```sh
[bundle exec] fastlane android get_version_name
```

get version_name

### android prepare_metadata_for_googleplay

```sh
[bundle exec] fastlane android prepare_metadata_for_googleplay
```

Prepare matadata so that it is not rejected by google play

### android postpare_metadata_for_googleplay

```sh
[bundle exec] fastlane android postpare_metadata_for_googleplay
```

Postpare matadata - This restores to the state before the 'prepare'

### android build_for_screengrab

```sh
[bundle exec] fastlane android build_for_screengrab
```

Build debug and test APK for screenshots

### android copy_test_pics_for_screengrab

```sh
[bundle exec] fastlane android copy_test_pics_for_screengrab
```

Copy sample pictures to the device

### android screenshots

```sh
[bundle exec] fastlane android screenshots
```

Do screenshots

### android disable_animation

```sh
[bundle exec] fastlane android disable_animation
```

disable animations on device

### android prepare_device_for_tests

```sh
[bundle exec] fastlane android prepare_device_for_tests
```

Prepare device for tests

### android get_output_of_tests

```sh
[bundle exec] fastlane android get_output_of_tests
```

Get test output from device

### android connectedCheck

```sh
[bundle exec] fastlane android connectedCheck
```

Run all the instrumented tests except screenshots

### android connectedCheck_with_screenrecord

```sh
[bundle exec] fastlane android connectedCheck_with_screenrecord
```

Run all the instrumented tests except screenshots and save screenrecords

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
