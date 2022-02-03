# pixymeta-android
Standalone Android version of PixyMeta - a pure Java image metadata manipulation tool.

Image metadata manipulation:
----------------------------------------
- JPEG and TIFF EXIF data manipulation
   * Insert EXIF data into JPEG.
   * Extract EXIF data from JPEG.
   * Remove EXIF data and other insignificant APPn segments from JPEG.
   * Insert EXIF data into TIFF.
   * Read EXIF data embedded in TIFF.
- JPEG and TIFF ICC Profile support
   * Insert ICC profile to JPEG and TIFF.
   * Extract ICC profile from JPEG and TIFF.
- JPEG and TIFF IPTC metadata support
   * Insert IPTC directly to TIFF via RichTiffIPTC tag.
   * Insert IPTC to JPEG via APP13 Photoshop IRB
   * Extract IPTC from both TIFF and JPEG
- JPEG and TIFF Photoshop IRB metadata support
   * Insert IRB into JPEG via APP13 segment
   * Insert IRB into TIFF via tag PHOTOSHOP.
   * Extract IRB data from both JPEG and TIFF.
- JPEG, GIF, PNG, TIFF XMP metadata support
   * Insert XMP metada into JPEG, GIF, PNG, and TIFF image
   * Extract XMP metadata from JPEG, GIF, PNG, and TIFF image
   * In case of JPEG, handle normal XMP and extendedXMP which cannot fit into one APP1 segment

Where can I get the latest release?
-----------------------------------
There is currently no stable release of PIXYMETA-ANDROID. However you can pull the latest SNAPSHOT from Sonatype SNAPSHOT repository by adding the snapshot repository to your pom.xml:
 
```xml
<repository>
  <id>oss.sonatype.org</id>
  <name>Sonatype Snapshot Repository</name>
  <url>https://oss.sonatype.org/content/repositories/snapshots</url>
  <releases>
    <enabled>false</enabled>
  </releases>
  <snapshots>
    <enabled>true</enabled>
  </snapshots>
</repository> 
```

Then you can use the SNAPSHOT version of ICAFE in your pom.xml:

```xml
<dependency>
  <groupId>com.github.dragon66</groupId>
  <artifactId>pixymeta-android</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
``` 

Go to the [wiki] page to see this library in action or grab the "pixymeta-android.jar" from the lib folder and try it yourself!

[wiki]:https://github.com/dragon66/pixymeta-android/wiki
[Open]:https://github.com/dragon66/pixymeta-android/issues/new
Tested on Android Nexus4 virtual device only!!!

[Project using pixymeta-android](https://github.com/CreativeCommons-Seneca/cc-xmp-tag)

Suggestions? custom requirements? [Open] an issue or send email to me directly: yuwen_66@yahoo.com
