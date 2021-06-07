# Exif Thumbnail Adder

This application for android devices searches for pictures (JPEG) on your device and __adds a thumbnail__ if they don't have one yet. Thumbnails are added to the EXIF metadata structure.

It is designed to work from android Oreo (android 8, SDK 26) and was tested on real device running Android 10 and virtual device running android 8 and 11.

Please report issues here: [https://github.com/tenzap/exif-thumbnail-adder/issues](https://github.com/tenzap/exif-thumbnail-adder/issues).

For more information, some know facts and how you may contribute, refer to the [project homepage](https://github.com/tenzap/exif-thumbnail-adder).


## Rationale
On my phone (Xiaomi Redmi Note 9S), when wanting to import my pictures to Windows (or any device/operating system supporting MTP or PTP protocols), I noticed the pictures don't display a thumbnail in the import wizard (whether through the Photos app, or through the Windows Explorer import feature).
This is because my phone didn't add the thumbnail to the pictures I took with the camera.


## Features
- Add thumbnail to pictures (JPEG) that don't yet have one
- Lanczos algorithm to downsample picture thanks to [FFmpeg's swscale library](https://ffmpeg.org/libswscale.html) for best results.
- Select one or more folders to scan from any storage (internal, SDCard...). For example DCIM, Pictures...
- Exclude one subdirectory from selected directories
- Preserve timestamps of the pictures
- Processing log
- Synchronize deleted files in the source directory to the backup and working directory (so that you don't keep in the backup folder pictures you don't have anymore in the source folder)
- Conservative default options (backup pictures, skip corrupt files)
- Install app on internal storage or external storage
- Default EXIF library: [Exiv2](https://www.exiv2.org).
- Alternative libraries: Android-Exif-Extended (built-in), libexif (built-in), pixymeta-android (needs manual compilation from sources). See known facts on project page to learn more on benefits and drawbacks of each library.
- Settings:
    - Rotate the thumbnail
    - Replace existing thumbnail
    - Backup of original pictures (backup is never overwritten by the app once created if you choose to add the thumbnail to the input file in it initial location)
    - Skip pictures having malformed metadata (this can be disabled to process also files having corrupt tags)
    - Replace picture inplace or write new picture to another directory


## Requested permissions
- `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE`
    - to keep the timestamp of the pictures
- `MANAGE_EXTERNAL_STORAGE`
    - requested only with the `standard` flavor that is shipped on F-Droid for devices running Android 11 and above to keep the timestamp of the pictures
- get more details about them on the project homepage


## Installation
- Prerequisites: minimum android Oreo (android 8, SDK 26). App was tested up to Android 11.
- Download it through F-Droid app [here](https://f-droid.org/packages/com.exifthumbnailadder.app)
- Download the APK from the [release page](https://github.com/tenzap/exif-thumbnail-adder/releases)


## License
GPL-3.0 (see "COPYING" file on project homepage)


## Contribute
- You are very welcome to contribute to the project either by testing, reporting bugs, developing, creating pull requests with fixes and features.
- Suggestions for contribution
    - If you have a google developer account, you may contact me to see how you could publish the app to the play store
    - Transform the batch processing into a "Service" so that it doesn't stop when the user leaves the main "Activity"
    - Translation
        - through [crowdin project page](https://crowdin.com/project/exif-thumbnail-adder). If you want a language that is not listed on crowdin, please ask for it so that I make it available.
        - or translate the following files and submit a pull request/issue with the translated files
            - [app/src/main/res/values/arrays.xml](../../raw/master/app/src/main/res/values/arrays.xml)
            - [app/src/main/res/values/strings.xml](../../raw/master/app/src/main/res/values/strings.xml)
            - [fastlane/metadata/android/en-US/full_description.txt](../../raw/master/fastlane/metadata/android/en-US/full_description.txt)
            - [fastlane/metadata/android/en-US/short_description.txt](../../raw/master/fastlane/metadata/android/en-US/short_description.txt)
    - Improve theme/layout
    - Implement other backends and/or fix [pixymeta bug report](https://github.com/dragon66/pixymeta-android/issues/10)


## Known facts
- Performance may be slower on SDCards, that may be related to the speed of your SDCard.

### Android-Exif-Extended
- all the existing EXIF structure is kept and a new APP1 structure containing the thumbnail is added to the existing APP1.
- this means that all EXIF tags will be duplicate if checked by exiftool
- Any other tags (XMP for example) are kept

### Exiv2
- on some Canon pictures some tags of [Canon] group might be stripped. See [Exiv2 bugreport](https://github.com/Exiv2/exiv2/issues/1589)
- [XMP*] is kept
- If Exiv2 detects some problems (errors) in your files, the file are skipped (reported error is displayed in the app). This setting can be changed in the app configuration.

### libexif
- All [XMP*] metadata groups and tags get deleted.
- Some or all tags of [Olympus] [Canon] group get deleted.
- The tags supported by libexif and exif structure are rewritten.
- It is almost like running "exif --create-exif --remove-thumbnail --insert-thumbnail tb.jpg" from the exif command line.
- If libexif detects some problems (errors) in your files, the file are skipped (reported error is displayed in the app). This setting can be changed in the app configuration.

### pixymeta-android
- please note that at the time of writing this, pixymeta-android is licensed under EPL-1.0 which is not compatible with GPL. You may compile the app yourself to use pixymeta-android. See below for more info.
- **usage is discouraged** until pixymeta bug is fixed
- the existing EXIF tags are copied and things a rewritten from scratch. 
- [XMP*] tags are kept
- [InteropIFD] directory is not correctly rewritten leading to problems such as "Bad InteropIFD directory" or "IFD1 pointer references previous InteropIFD directory" or "GPS pointer references previous InteropIFD directory". See [pixymeta bug report](https://github.com/dragon66/pixymeta-android/issues/10).


## Concerning `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE`
- permit to read and write/update the picture from the storage of your device
- requested with the `google_play` flavor that is shipped on google play and with the `standard` flavor on Android 10 and before
- these permissions are required to keep the timestamp of the pictures


## Concerning `MANAGE_EXTERNAL_STORAGE` (for flavor `standard`)
Since flavor `standard` uses targetSdk >= 30 (ie Android 11+), I needed to use the `MANAGE_EXTERNAL_STORAGE` permission. This might be problematic to publish on the play store. See [1](https://developer.android.com/training/data-storage/manage-all-files#all-files-access-google-play) [2](https://support.google.com/googleplay/android-developer/answer/10467955).

Some explanations:

The app uses the Storage Access Framework to process the files. However, with Storage Access Framework, on copying files or modifying them, timestamps get updated. But when adding thumbnails we don't want them to change and thus I set them back to the original value. To set the values of timestamps back I use the BasicFileAttributesView class [3](https://developer.android.com/reference/java/nio/file/attribute/BasicFileAttributeView#setTimes(java.nio.file.attribute.FileTime,%20java.nio.file.attribute.FileTime,%20java.nio.file.attribute.FileTime). This works fine until targetSdk 28 (=android 9). There is a workaround for targetSdk 29 (android 10) but from targetSdk 30 (Android 11) onwards, the method returns an "AccessDeniedException". So I ended up using `MANAGE_EXTERNAL_STORAGE` with targetSdk 30, see [4](https://stackoverflow.com/a/66681306/15401262).

So in the App, when one is on Android 11+ with targetSdk 30 (which is the case of flavor `standard`), one is invited to give the "all files access" permissions through the settings. This is not mandatory. In case permission is not given, the user is informed that timestamps can't be kept during processing.

With flavor `google_play` targetSdk is set to 29. Hence it is still possible with Android 11 to not use `MANAGE_EXTERNAL_STORAGE`. For versions above 11 it is uncertain whether it will still work. To be sure to have all functionalities of the app if you run Android 11+ get the `standard` flavor from F-Droid.org

Please note that this is about the timestamps of the files (not the ones in the EXIF tags)

## Development / Building from source
This project has been developed in "Android Studio", you may use that to build the app yourself.

In addition to Android Studio you need these components (android studio can install them for you thanks to the SDK Manager):

* SDK
* NDK
* CMake


### Flavors
The app can be compiled in any of the following flavors:

* *standard* (version shipped on F-Droid)
* *google_play* (version shipped on Google Play). It is the same as "standard" except it has targetSdk 29 and doesn't request `MANAGE_EXTERNAL_STORAGE` permission.
* *pixymeta* is not shipped on any store. For licensing reasons you have to compile it yourself for personal use only (pixymeta-android is licensed under EPL-1.0 which is not compatible with GPL-3.0). Compilation guidance for this flavor is detailed below.


### To compile with pixymeta-android library
1. create file `./enable-pixymeta-android.gradle` and add this line: `include ':library:pixymeta-android'`
```Shell
echo "include ':library:pixymeta-android'" > ./enable-pixymeta-android.gradle
```
1. create file `./app/enable-pixymeta-android.gradle` and add this line: `dependencies { pixymetaImplementation project(path: ':library:pixymeta-android') }`
```Shell
echo "dependencies { pixymetaImplementation project(path: ':library:pixymeta-android') }" > ./app/enable-pixymeta-android.gradle
```
1. place the `src` directory of pixymeta-android that you can get from the [pixymeta-android project page](https://github.com/dragon66/pixymeta-android) into `library/pixymeta-android`. It must be dated >= 2021-03-27 and >= commit 15a6f25d891593e5c6f85e542a55150b2947e7f5
1. place files `build.gradle` and `AndroidManifest.xml` from pixymeta-android's root dir into `library/pixymeta-android`
1. select the build variant having flavor `pixymeta`


### To create screenshots
From within the root directory of the project run:
```Shell
ANDROID_SDK_ROOT=~/Android/Sdk/ fastlane screenshots
```
