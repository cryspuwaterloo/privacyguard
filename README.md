
## PrivacyGuard 

PrivacyGuard is an Android app that alerts you when one of your apps leaks sensitive information to a remote server. PrivacyGuard does not require a rooted phone. It detects leaks by having the network traffic of all apps go through a VPN. Note that the VPN runs entirely on your phone and your apps' traffic does not get sent to a remote VPN server. PrivacyGuard is described in more detail in this technical paper:

Y. Song and U. Hengartner. [PrivacyGuard: A VPN-based Platform to Detect Information Leakage on Android Devices.](https://dl.acm.org/citation.cfm?id=2808120&CFID=801061730&CFTOKEN=35254378) Proc. of 5th Annual CCS Workshop on Security and Privacy in Smartphones and Mobile Devices (SPSM 2015), Denver, CO, October 2015, pp. 15-26. 

## Android build process:

1. Download Android Studio
2. To be installed (choose latest unless specified):
	- SDK 19 and above
	- Android SDK Build Tools
	- Android SDK Tools
	- Android SDK Platform-Tolls
	- Google Play service
	- Android Support Repository
	- Android Support Library
	- Intel x85 Emulator Accelerator
3. install intelhaxm-android to be able to run emulator
4. Change the memory limit of AVD emulator to that of HAXM (1 GB) for emulator to work:	[Stackoverflow](http://stackoverflow.com/questions/21031903/how-to-fix-hax-is-not-working-and	-emulator-runs-in-emulation-mode)
5. Imported project from local repo

## Limitations

1. TLS interception: no support for certificate pinning and SNI.

## Misc
The Google Maps API key is stored in privacyguard/app/src/debug/res/values/google_maps_api.xml

## FAQ

### 1. Failed to find target with hash string 'android-19'


	Error:Cause: failed to find target with hash string 'android-19' in: /Users/justinhu/Library/Android/sdk <a href="openAndroidSdkManager">Open Android SDK Manager</a>

- Install packages via SDK Manager

### 2. Dependency error
	 Error:(30, 30) error: package android.support.v4.app does not exist
- In Android Studio:

	1. Right click on your projects "app" folder and click on -> module settings
	2. Click on the "dependencies" tab
	3. Click on the + sign to add a new dependency and select "Library Dependency"
	4. Look for the library you need and add it


### 3.  Google Play Service Version
	/Users/justinhu/Documents/PrivacyGuard/app/build/intermediates/manifests/full/debug/AndroidManifest.xml
	Error:(41, 28) No resource found that matches the given name (at 'value' with value '@integer/google_play_services_version').
	
- In Android Studio, [add Google Play Services to Your Project](https://developers.google.com/android/guides/setup)

### 4. Gradle refresh failed

	Gradle 'PrivacyGuard' project refresh failed
	Error:Timeout waiting to lock cp_proj class cache for build file '/Users/justinhu/Documents/PrivacyGuard/app/build.gradle' (/Users/justinhu/.gradle/caches/2.8/scripts/build_7isvx4d7k6k1q7rukf8v9wizj/cp_proj). It is currently in use by another Gradle instance.

- Delete everything under "/Users/<your user name>/.gradle/caches/"

### 5. To execute "adb devices":
- find platform-tools in explorer
- C:\Users\<user>\AppData\Local\Android\sdk
- open command prompt and execute command

### 6. Device does not automatically show up when running app since drivers don't get installed:
http://stackoverflow.com/questions/16596877/android-studio-doesnt-see-device

- Steps to resolve:
- If you using Windows, the device won't show up because of driver issue.
- Go to device manager (just search it using Start) and look for any devices showing an error. Many androids will show as an unknown USB device and comes with exclamation mark. Select that device and try to update the drivers for it.
- But before that, you have to update your sdk manager and make sure Google USB Driver package is installed.
- When done, the driver files are downloaded into the \extras\google\usb_driver\ directory. Hints: Search "android_winusb.inf" under Windows Start and Open File Location to get the directory mentioned.
- Open up your device manager, navigate to your android device, right click on it and select Update Driver Software then select Browse driver software. Follow the file location path previously to install Google USB Driver.
- Restart Android Studio and Developer Options in your android device and reconnect USB.

### 7. Checking SQLite Databases:
-	Go to Tools -> DDMS or click the Device Monitor icon next to SDK Manager in Tool bar.
Device Monitor window will open.     

-	In File Explorer tab click data -> data -> your project name. After that your databases file will open . click "pull a file from device" icon. Save the file using .db extension.

-	Open FireFox, Press Alt , Tools->SQLiteManager.
Follow Database -> connect to Database -> browse your database file and click ok. Your SQLite file will opened now.

### 8. Google Maps set up Android 19: http://www.androidhive.info/2013/08/android-working-with-google-maps-v2/
	C:\Program Files\Java\jre1.8.0_60\bin>
	keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

### 9. Execution failed for task ':app:transformClassesWithDexForDebug'
	Execution failed for task ':app:transformClassesWithDexForDebug'
	> com.android.build.transform.api.TransformException: com.android.ide.common.process.ProcessException: org.gradle.process.internal.ExecException: Process 'command '/Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home/bin/java'' finished with non-zero exit value 2
	
- This is because the number of declared methods is too high. Android generally only allow app to have under 65K methods. 
- Simple and easy (but improper) way is to add this to build.gradle (app module)

		android {
   			...
   			defaultConfig {
      		...
      			multiDexEnabled true
   			}
		} 
- The better solution is to look at your library dependencies and recude unnecessary libraries
- For details, see this [stackoverflow](http://stackoverflow.com/questions/32798816/unexpected-top-level-exception-com-android-dex-dexexception-multiple-dex-files)







