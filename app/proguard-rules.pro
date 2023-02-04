# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#pixymeta uses reflection to get "fromShort" method.
#So we have to disable minify for this method on the classes that have it
-keep public class pixy.image.tiff.FieldType { public static ** fromShort(short); }
-keep public class pixy.image.jpeg.Marker { public static ** fromShort(short); }
-keep public class pixy.image.tiff.TiffTag { public static ** fromShort(short); }
-keep public class pixy.meta.adobe.ImageResourceID { public static ** fromShort(short); }
-keep public class pixy.meta.exif.ExifTag { public static ** fromShort(short); }
-keep public class pixy.meta.exif.GPSTag { public static ** fromShort(short); }
-keep public class pixy.meta.exif.InteropTag { public static ** fromShort(short); }

# Fix: java.lang.ClassNotFoundException: org.apache.log4j.helpers.Loader
-keep class org.apache.log4j.helpers.Loader {*;}

# Fix Pending exception java.lang.NoSuchFieldError: no "Z" field "enableLog" in class "Lcom/exifthumbnailadder/app/MainApplication;" or its superclasses
-keep public class com.exifthumbnailadder.app.MainApplication { public static boolean enableLog; }
