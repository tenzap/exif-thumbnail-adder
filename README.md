# Exif Thumbnail Adder

This application for android devices searches for pictures (JPEG) on your device and __adds a thumbnail__ if they don't have one yet. Thumbnails are added to the EXIF metadata structure.

It is designed to work from android Oreo (android 8, SDK 26).

Please report issues here: [https://github.com/tenzap/exif-thumbnail-adder/issues](https://github.com/tenzap/exif-thumbnail-adder/issues).

For more information, some known facts and how you may contribute, refer to the [project homepage](https://github.com/tenzap/exif-thumbnail-adder).


## Rationale
On some smartphones, when wanting to import pictures to Windows (or any device/operating system supporting MTP or PTP protocols), I noticed the pictures may not display a thumbnail in the import wizard (whether through the Photos app, or through the Windows Explorer import feature).

There are two possible reasons for that behaviour. First, the thumbnail is not present in the picture, usually because the app that created the picture didn't add a thumbnail. Second, there is a thumbnail but it is ignored because some EXIF tags are missing.


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
- Alternative libraries: Android-Exif-Extended, libexif, pixymeta-android. See known facts on project page to learn more on benefits and drawbacks of each library.
- Settings:
    - Rotate the thumbnail
    - Replace existing thumbnail
    - Backup of original pictures (backup is never overwritten by the app once created if you choose to add the thumbnail to the input file in its initial location)
    - Skip pictures having malformed metadata (this can be disabled to process also files having corrupt tags)
    - Replace picture in place or write new picture to another directory


## Requested permissions
- By Android version:
  - Android 8-9: `WRITE_EXTERNAL_STORAGE`
  - Android 10: `WRITE_EXTERNAL_STORAGE`, `ACCESS_MEDIA_LOCATION`
  - Android 11-12L: `READ_EXTERNAL_STORAGE`, `ACCESS_MEDIA_LOCATION`, (optionally `MANAGE_EXTERNAL_STORAGE`)
  - Android 13: `READ_MEDIA_IMAGES`, `ACCESS_MEDIA_LOCATION`, `POST_NOTIFICATIONS`, (optionally `MANAGE_EXTERNAL_STORAGE`)

- `WRITE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES`
  - to keep the timestamp of the pictures and for file processing
- `MANAGE_EXTERNAL_STORAGE`
  - to keep the timestamp of the pictures (on devices running Android 11 and above)
- `ACCESS_MEDIA_LOCATION`
  - to keep location (GPS) metadata when processing files
- `POST_NOTIFICATIONS`
  - to show progress of processing in notification drawer


## Privacy notice
- This application doesn't collect and doesn't send any information on you or your usage.


## Installation
- Prerequisites: minimum android Oreo (android 8, SDK 26). App was tested up to Android 13.
- Download it through F-Droid app [here](https://f-droid.org/packages/com.exifthumbnailadder.app)
- Download it through Google Play [here](https://play.google.com/store/apps/details?id=com.exifthumbnailadder.app)
- Download it through Amazon Appstore [here](https://www.amazon.com/dp/B09CZ7684J/)
- Download the APK from the [release page](https://github.com/tenzap/exif-thumbnail-adder/releases)


## License
GPL-3.0 (see "COPYING" file on project homepage)


## Contribute
- You are very welcome to contribute to the project either by translating, testing, reporting bugs, developing, creating pull requests with fixes and features.
- Suggestions for contribution
    - Translation
        - through [crowdin project page](https://crowdin.com/project/exif-thumbnail-adder). If you want a language that is not listed on crowdin, please ask for it so that I make it available.
        - or translate the following files and submit a pull request/issue with the translated files
            - [app/src/main/res/values/arrays.xml](../../raw/master/app/src/main/res/values/arrays.xml)
            - [app/src/main/res/values/strings.xml](../../raw/master/app/src/main/res/values/strings.xml)
            - [fastlane/metadata/android/en-US/full_description.txt](../../raw/master/fastlane/metadata/android/en-US/full_description.txt)
            - [fastlane/metadata/android/en-US/short_description.txt](../../raw/master/fastlane/metadata/android/en-US/short_description.txt)
    - Improve theme/layout
    - Implement other backends
    - keep [XMP*] metadata when using libexif. See these posts [a](https://stackoverflow.com/q/67264563/15401262), [b](https://sourceforge.net/p/libexif/bugs/121/), [c](https://stackoverflow.com/a/22504601/15401262).
    - fix pixymeta bug report [1](https://github.com/dragon66/pixymeta-android/issues/18).


## Known facts
- Performance may be slower on SDCards, that may be related to the speed of your SDCard.

### Android-Exif-Extended
- all the existing EXIF structure is kept and a new APP1 structure containing the thumbnail is added to the existing APP1.
- this means that **all EXIF tags will be duplicate** if checked by exiftool
- Any other metadata (XMP for example) are kept

### Exiv2
- If Exiv2 detects some problems (errors) in a file, it is skipped (reported error is displayed in the app). This setting can be changed in the app configuration.

### libexif
- **To avoid loosing XMP metadata, Exif Thumbnail Adder skips the pictures that have XMP metadata.** This is because libexif doesn't keep [XMP\*] metadata, and all XMP tags would get deleted.
- **Some or all tags of [Olympus] [Canon] group might be deleted.**
- The tags supported by libexif and exif structure are rewritten from what libexif could read.
- It is almost like running "exif --create-exif --remove-thumbnail --insert-thumbnail tb.jpg" from the exif command line.
- If libexif detects some problems (errors) in a file, it is skipped (reported error is displayed in the app). This setting can be changed in the app configuration.

### pixymeta-android
- **To avoid loosing makernotes metadata, Exif Thumbnail Adder skips the pictures that have makernotes metadata.** This is because pixymeta-android doesn't support makernotes well. See bug report [1](https://github.com/dragon66/pixymeta-android/issues/18): for example ETA didn't skip these pictures, makernotes of Canon, Nikon, Olympus, Sanyo... might get corrupted.
- the existing EXIF tags are read and metadata is rewritten from scratch using what was read


## Concerning some permissions

### `WRITE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES`
- these permissions are required to keep the timestamp of the pictures
- they are also required with most exif libraries during processing of files


### `MANAGE_EXTERNAL_STORAGE`
Since the app uses targetSdk >= 30 (ie Android 11+), I needed to use the `MANAGE_EXTERNAL_STORAGE` permission.

Some explanations:

The app uses the Storage Access Framework to process the files. However, with Storage Access Framework, on copying files or modifying them, timestamps get updated. But when adding thumbnails we don't want them to change and thus I set them back to the original value. To set the values of timestamps back I use the BasicFileAttributesView class [1](https://developer.android.com/reference/java/nio/file/attribute/BasicFileAttributeView#setTimes(java.nio.file.attribute.FileTime,%20java.nio.file.attribute.FileTime,%20java.nio.file.attribute.FileTime)). This works fine until targetSdk 28 (=android 9). There is a workaround for targetSdk 29 (android 10) but from targetSdk 30 (Android 11) onwards, the method returns an "AccessDeniedException". So I ended up using `MANAGE_EXTERNAL_STORAGE` with targetSdk >= 30, see [2](https://stackoverflow.com/a/66681306/15401262).

So in the App, when one is on Android 11+ with targetSdk >= 30, one is invited to give the "all files access" permissions through the settings. This is not mandatory. In case permission is not given, the user is informed that timestamps can't be kept during processing.

Please note that this is about the timestamps of the files (not the ones in the EXIF tags)


### `ACCESS_MEDIA_LOCATION`
- to keep location (GPS) metadata when processing files. Otherwise, GPS metadata will be lost because Android will hide it to the app.


### `POST_NOTIFICATIONS`
- to show progress of processing in notification drawer. Useful when letting the app work in the background.


## Development / Building from source
This project has been developed in "Android Studio", you may use that to build the app yourself.

Version 0.9.18 requires Android Studio Dolphin (2021.3) or above.

In addition to Android Studio you need these SDK components which will be installed by Android Studio's SDK Manager:

* SDK
* NDK
* CMake


### Flavors
The app can be compiled in any of the following flavors:

* *standard* (version shipped on F-Droid, Google Play, Amazon Appstore)


### To create screenshots
From within the root directory of the project run:

  ```Shell
  ANDROID_SDK_ROOT=~/Android/Sdk/ bundle exec fastlane screenshots
  ```

### To run the tests
1. Get the test pictures:

    ```Shell
    cd tests/data/exif-samples
    git clone https://github.com/ianare/exif-samples
    ```

1. run the test suite

    ```Shell
    ANDROID_SDK_ROOT=~/Android/Sdk/ bundle exec fastlane connectedCheck_with_screenrecord
    ```

1. run the test scripts

    ```Shell
    cd tests
    ./run_tests.sh
    ```
