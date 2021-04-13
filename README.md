# Exif Thumbnail Adder

This is an application for android devices that will search for pictures (JPEG) on your device and __add a thumbnail__ in the EXIF tag if they don't have one yet.

It is supposed to work from android Oreo (android 8, SDK 26) and was tested on real device running Android 10 and virtual device running android 11.

Homepage: https://github.com/tenzap/exif-thumbnail-adder


## Rationale
On my phone (Xiaomi Redmi Note 9S), when wanting to import my pictures to Windows (or any device/operating system supporting MTP or PTP protocols), I noticed the pictures don't display a thumbnail in the import wizard (whether through the Photos app, or through the Windows Explorer import feature).
This is because my phone didn't add the thumbnail to the pictures I took with the camera.


## Features
- Add thumbnail to pictures (JPEG) that don't yet have one
- Preserve timestamps of the pictures
- Selection of the folders to scan. For example DCIM or Pictures.
- Processing log
- Works on both SDCards and device internal memory (named primary external storage & secondary external storage in the android technical world)
- Possibility to exclude one subdirectory from selected directories
- App can be installed either on internal storage or external storage
- Various options
    - Rotation of the thumbnail
    - Backup of original pictures
    - Replace picture inplace or write new picture to another directory
    - Choose the exif library for adding thumbnails (Android-Exif-Extended or pixymeta-android). Please note that at the time of writing this, pixymeta-android is licensed under EPL-1.0 which is not compatible with GPL. You may compile yourself a variant having pixymeta-android. See below for more info.


## Installation
- Prerequisites: minimum android Oreo (android 8, SDK 26). App was tested up to Android 11.
- Download through [F-Droid](https://f-droid.org) app [here](https://f-droid.org/packages/com.exifthumbnailadder.app/)
- Download the APK from the release page:  https://github.com/tenzap/exif-thumbnail-adder/releases


## Known facts
- Performance may be slower on SDCards, that may be related to the speed of your SDCard.
- When choosing Android-Exif-Extended library, all the existing EXIF structure is kept and a new APP1 structure containing the thumbnail is added to the existing one.
- When choosing pixymeta-android library, the existing EXIF tags are copied and things a rewritten from scratch. The resulting EXIF values were the same in my testing except the InterOp IFD which was not copied (see https://github.com/dragon66/pixymeta-android/issues/10)


## Contribute
- Please feel free to contribute to the project either by testing, reporting bugs, developing, creating pull requests with fixes and features.
- Suggestions for contribution
    - If you have a google developer account, you may contact me to see how you could publish the app to the play store
    - Transform the batch processing into a "Service" so that it doesn't stop when the user leaves the main "Activity"
    - Translation
    - Improve theme/layout
    - Implement another backend and/or fix https://github.com/dragon66/pixymeta-android/issues/10


## Licence
GPL-3.0 (see "COPYING" file)


## To create screenshots
`./gradlew assembleDebug assembleAndroidTest`  
`fastlane screengrab --use_timestamp_suffix false --clear_previous_screenshots -q "en-US,fr-FR" --reinstall_app`


## Development / Building from source
This project has been developed in "Android Studio", you may use that to build the app yourself.


### To compile with pixymeta-android library
1. create file './enable-pixymeta-android.gradle' and add this line  
`include ':library:pixymeta-android'`
1. create file './app/enable-pixymeta-android.gradle' and add this line  
`dependencies { pixymetaImplementation project(path: ':library:pixymeta-android') }`
1. place the src directory of pixymeta-android that you can find here https://github.com/dragon66/pixymeta-android into `library/pixymeta-android`. It must be dated >= 2021-03-27 and >= commit 15a6f25d891593e5c6f85e542a55150b2947e7f5
1. select the build variant having flavor 'pixymeta'
