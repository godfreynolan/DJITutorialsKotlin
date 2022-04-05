# DJI Video Decoder Tutorial

## DJI LAB 9 TUTORIAL KOTLIN

***`WARNING: THIS TUTORIAL ASSUMES YOU'VE COMPLETED THE PREVIOUS TUTORIALS`***

In this tutorial, you will learn how to use `FFmpeg` for video frame parsing and to use the `MediaCodec` for hardware decoding. It will help to parse video frames and decode the raw video stream data from DJI Camera and output the YUV data.

You can download the tutorial's final sample project from this [Github Page](https://github.com/riisinterns/VideoDecoder).

> Note: In this tutorial, we will use Mavic Pro for testing and use Android Studio 3.0 for developing the demo application.

---

### Introduction

In order to use DJI drones for computer vision projects, the first step is to parse the video stream obtained from the camera into image frames. DJI provides a Demo program on github that decodes the video into image frames. The official website documentation does not explain how to integrate this decoding Demo into your own project, but simply explains the main purpose of the DJIVideoStreamDecoder and NativeHelper classes.

---

### Preparation

#### 1. Setup Android Development Environment

Throughout this tutorial we will be using the latest version of android studio, which you can download from here: <http://developer.android.com/sdk/index.html>.

---

### Initial Gradle and Manifest Setup

This part is important as it will help us to setup the project with the latest libraries and dependencies. Some notable ones are ffmpeg and the libdjivideojni.so library which are referenced within the code.

#### 1. Create the project

Open Android Studio and select **File -> New -> New Project** to create a new project, named `"VideoDecoder"`. Enter the company domain and package name `(Here we use "com.riis.videodecoder")` you want and press Next. Set the mimimum SDK version as `API 22: Android 5.1 (Lollipop)` for "Phone and Tablet" and press Next. Then select "Empty Activity" and press Next. Lastly, leave the Activity Name as "MainActivity", and the Layout Name as "activity_main", Press "Finish" to create the project.

#### 2. Add Some String Resources

Please edit `strings.xml` and add the following resources below.

```xml
<resources>
    <string name="app_name_decoding_sample">Video Stream Decoding Sample</string>
    <string name="title_main_activity">Video Stream Decoding Sample</string>
    <string name="product_information">Product Information</string>
    <string name="connection_loose">Status: No Product Connected</string>
    <string name="model_not_available">Model Not Available</string>
    <string name="sdk_version">DJI SDK Version: %1$s</string>
</resources>
```

#### 3. Android Manifest Permissions

Specify the permissions of your application needs, by adding `<uses-permission>` elements as children of the `<manifest>` element in the `AndroidManifest.xml` file.

```xml
    <uses-permission android:name="android.permission.VIBRATE"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
    <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"
        tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        tools:ignore="ScopedStorage" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
    <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />

    <uses-feature android:name="android.hardware.camera"/>
    <uses-feature android:name="android.hardware.camera.autofocus"/>
    <uses-feature
        android:name="android.hardware.usb.host"
        android:required="false"/>
    <uses-feature
        android:name="android.hardware.usb.accessory"
        android:required="true"/>
```

In the code above, we specify the permissions of your application needs by adding `<uses-permission>` elements as children of the `<manifest>` element.

Moreover, because not all Android-powered devices are guaranteed to support the USB accessory and host APIs, include two elements that declare that your application uses the "android.hardware.usb.accessory" and "android.hardware.usb.host" feature.

Finally, we need to specify the requirement for OpenGL ES version 2.

For more details of description on the permissions, refer to https://developers.google.com/maps/documentation/android/config.

Furthermore, let's replace the `<application>` element with the followings:

```xml
    <application
        android:name=".VideoDecodingApplication"
        android:allowBackup="true"
        android:label="@string/app_name_decoding_sample"
        android:supportsRtl="true"
        android:theme="@style/Theme.VideoDecoder">

        <uses-library android:name="org.apache.http.legacy" android:required="false" />

        <meta-data
            android:name="com.dji.sdk.API_KEY"
            android:value="enter your app key here"/>

        <activity
            android:name=".ConnectionActivity"
            android:launchMode="singleTop"
            android:screenOrientation="portrait"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
                <action android:name="android.hardware.usb.action.USB_ACCESSORY_ATTACHED"/>
            </intent-filter>
            <meta-data
                android:name="android.hardware.usb.action.USB_ACCESSORY_ATTACHED"
                android:resource="@xml/accessory_filter"/>
        </activity>
        <activity
            android:name=".MainActivity"
            android:screenOrientation="landscape"
            android:configChanges="orientation|screenSize"
            android:theme="@android:style/Theme.NoTitleBar.Fullscreen" />
    </application>
```

Please enter the **App Key** of the application in the value part of `android:name="com.dji.sdk.API_KEY"` attribute. For more details of the `AndroidManifest.xml` file, please check this tutorial's Github source code of the demo project.

#### 4. Adding Multidex Support with Gradle

We need to add Multidex support to avoid the 64K limit with Gradle.

Modify the module-level `build.gradle` file configuration to include the support library and enable multidex output in both **defaultConfig** and **dependencies** parts, as shown in the following code snippet:

```gradle
android {
    compileSdkVersion 32

    defaultConfig {
        ...
        minSdkVersion 22
        targetSdkVersion 32
        ...
        
        // Enabling multidex support.
        multiDexEnabled true
    }
    ...
}

dependencies {
  ...
  implementation 'androidx.multidex:multidex:2.0.1'
}
```

In the code above, we declare the "compileSdkVersion", "buildToolsVersion", "minSdkVersion" and "targetSdkVersion".

Then select **Tools->Android->Sync Project** with Gradle Files to sync the gradle files.

For more details about configuring your App for Multidex with Gradle, please check this link: <http://developer.android.com/tools/building/multidex.html>.

---

### Importing the DJI Dependencies

Please follow [Lab Two: Import and Activate SDK into Application](https://github.com/riisinterns/drone-lab-two-import-and-activate-sdk-in-android-studio) tutorial to learn how to import the Android SDK Maven Dependency for DJI.

---

### Creating App Layouts and Classes

#### 1. Implementing ConnectionActivity and VideoDecodingApplication

To improve the user experience, we had better create an activity to show the connection status between the DJI Product and the SDK, once it's connected, the user can press the **OPEN** button to enter the **MainActivity**. You can also check the [Creating an Camera Application](https://github.com/riisinterns/drone-lab-three-camera-demo) tutorial to learn how to implement the `ConnectionActivity` Class and Layout in this project (along with its viewmodel). If you open the `activity_connection.xml` file, and click on the Design tab on the top right, you should see the preview screenshot of `ConnectionActivity` as shown below:

<img src="./images/connection_page.jpg" alt="drawing" width="200"/>

> Never mind the *MavicMini* name on the connection page, it was only used for testing the connection status.

In order to create the `VideoDecodingApplication.kt` class, please add the following code to that file inside the com.riis.videodecoder package:

```kotlin
package com.riis.videodecoder

import android.app.Application
import android.content.Context
import com.secneo.sdk.Helper
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKManager

class VideoDecodingApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        Helper.install(this@VideoDecodingApplication)
    }

    companion object {
        private var mProduct: BaseProduct? = null

        @get:Synchronized
        val productInstance: BaseProduct?
            get() {
                if (null == mProduct) {
                    mProduct = DJISDKManager.getInstance().product
                }
                return mProduct
            }

        @Synchronized
        fun updateProduct(product: BaseProduct?) {
            mProduct = product
        }
    }
}
```

This is an Application class to do DJI SDK Registration, product connection, product change and product connectivity change checking. Then it broadcasts the changes.

#### 2. Implementing MainActivity

Open the `activity_main.xml` layout file and replace the code with the following:

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.riis.videodecoder.MainActivity">

    <RelativeLayout
        android:id="@+id/main_title_rl"
        android:layout_width="fill_parent"
        android:layout_height="40dp"
        android:background="@color/purple_200">

        <TextView
            android:id="@+id/title_tv"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_centerInParent="true"
            android:textColor="@android:color/white"
            android:text="@string/title_main_activity"/>

    </RelativeLayout>

    <TextureView
        android:id="@+id/livestream_preview_ttv"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_centerInParent="true"
        android:layout_gravity="center"
        android:alpha="50"
        android:visibility="visible"
        android:layout_below="@id/main_title_rl"/>

    <SurfaceView
        android:id="@+id/livestream_preview_sf"
        android:layout_width="360dp"
        android:layout_height="180dp"
        android:layout_centerInParent="true"
        android:layout_gravity="center"
        android:visibility="gone"
        android:layout_below="@id/main_title_rl"/>

    <LinearLayout
        android:layout_width="150dp"
        android:layout_height="wrap_content"
        android:layout_alignParentLeft="true"
        android:layout_below="@id/main_title_rl"
        android:orientation="vertical">

        <Button
            android:id="@+id/activity_main_screen_texture"
            android:layout_width="150dp"
            android:layout_height="wrap_content"
            android:text="Demo TextureView"
            android:clickable="true"
            android:onClick="onClick"
            android:gravity="center"  />

        <Button
            android:id="@+id/activity_main_screen_surface_with_own_decoder"
            android:layout_width="150dp"
            android:layout_height="wrap_content"
            android:text="Demo custom decoder"
            android:clickable="true"
            android:onClick="onClick"
            android:gravity="center" />

        <Button
            android:id="@+id/activity_main_screen_surface"
            android:layout_width="150dp"
            android:layout_height="wrap_content"
            android:text="Demo SurfaceView"
            android:onClick="onClick"
            android:gravity="center" />


        <Button
            android:id="@+id/activity_main_screen_shot"
            android:layout_width="150dp"
            android:layout_height="wrap_content"
            android:text="YUV Screen Shot"
            android:clickable="true"
            android:onClick="onClick"
            android:gravity="center" />

    </LinearLayout>
    <TextView
        android:id="@+id/activity_main_save_path"
        android:layout_width="400dp"
        android:layout_height="match_parent"
        android:padding="5dp"
        android:background="@color/purple_200"
        android:layout_toEndOf="@id/activity_main_screen_shot"
        android:layout_alignParentEnd="true"
        android:layout_below="@id/main_title_rl"
        android:textColor="@color/white"
        android:visibility="invisible"
        android:scrollbars="vertical"
        android:gravity="bottom"
        tools:ignore="NotSibling" />

</RelativeLayout>
```

In the xml file, we implement the following UIs:

* Create a RelativeLayout to add a TextView to show the SDK connection status on the top.

* Then create a texture view and surface view to show the video.

* Then 4 buttons are created with each of the first 3 being used to show different video streams. The last button is used to take a screen shot of the video.

* Finally, a TextView is created to show the save path of the screen shot.

Next, open the `colors.xml` file in the "values" folder and add the following code below:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
```

Furthermore, visit the example project on the github page and open the drawable folder and copy its contents into the drawable folder of this project. Your drawable folder should have the following contents:

<img src="./images/drawable.png" alt="drawing" width="500"/>

Now, if you open the `activity_maps.xml` file, and click on the Design tab on the top right, you should see the preview screenshot of `MainActivity` as shown below:

<img src="./images/main_activity.png" alt="drawing" width="500"/>

---

### Implementing Decoder Features in MainActivity

> NOTE: This section is a more in depth version of the previous section. The main steps for converting the video stream data into images are implemented in `MainActivity.kt`. These images get transmitted to the android system in the YUV format and is stored as such. The total number of image frames is controlled by `DJIVIdeoStreamDecoder.getInstance()`.

#### 1. Update the Connection Status TextView

Let's open `MainActivity.kt` file and add the following variables which will be later used throughout the activity:

```kotlin
    private var surfaceCallback: SurfaceHolder.Callback? = null

    private enum class DemoType {
        USE_TEXTURE_VIEW, USE_SURFACE_VIEW, USE_SURFACE_VIEW_DEMO_DECODER
    }

    private var standardVideoFeeder: VideoFeeder.VideoFeed? = null
    private var mReceivedVideoDataListener: VideoFeeder.VideoDataListener? = null
    private var titleTv: TextView? = null
    private var mainHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_WHAT_SHOW_TOAST -> Toast.makeText(
                    applicationContext, msg.obj as String, Toast.LENGTH_SHORT
                ).show()
                MSG_WHAT_UPDATE_TITLE -> if (titleTv != null) {
                    titleTv!!.text = msg.obj as String
                }
                else -> {}
            }
        }
    }
    private var videostreamPreviewTtView: TextureView? = null
    private var videostreamPreviewSf: SurfaceView? = null
    private var videostreamPreviewSh: SurfaceHolder? = null
    private var mCamera: Camera? = null
    private var mCodecManager: DJICodecManager? = null
    private var savePath: TextView? = null
    private var screenShot: Button? = null
    private var stringBuilder: StringBuilder? = null
    private var videoViewWidth = 0
    private var videoViewHeight = 0
    private var count = 0


    private val isTranscodedVideoFeedNeeded: Boolean
        get() = if (VideoFeeder.getInstance() == null) {
            false
        } else VideoFeeder.getInstance().isFetchKeyFrameNeeded || VideoFeeder.getInstance()
            .isLensDistortionCalibrationNeeded

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val MSG_WHAT_SHOW_TOAST = 0
        private const val MSG_WHAT_UPDATE_TITLE = 1
        private var demoType: DemoType? = DemoType.USE_TEXTURE_VIEW
        val isM300Product: Boolean
            get() {
                if (DJISDKManager.getInstance().product == null) {
                    return false
                }
                val model: Model = DJISDKManager.getInstance().product.model
                return model === Model.MATRICE_300_RTK
            }
    }
```

#### 2. Working on Initializing the UI

Now, using the `onCreate` method, we will initialize the UI. If an M300 product is being used, we will change the physical video source to the right cam or the top cam. To do this, we use the `isM300Product` companion object variable to check if the product is an M300 product. The code snippet looks like the following:

```kotlin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initUi()
        if (isM300Product) {
            // If your MutltipleLensCamera is set at right or top, you need to change the PhysicalSource to RIGHT_CAM or TOP_CAM.
            VideoDecodingApplication.productInstance?.airLink?.ocuSyncLink?.assignSourceToPrimaryChannel(
                PhysicalSource.LEFT_CAM, PhysicalSource.FPV_CAM
            ) { error: DJIError? ->
                if (error == null) {
                    showToast("assignSourceToPrimaryChannel success.")
                } else {
                    showToast("assignSourceToPrimaryChannel fail, reason: " + error.description)
                }
            }
        }
    }

    private fun initUi() {
        savePath = findViewById<View>(R.id.activity_main_save_path) as TextView
        screenShot = findViewById<View>(R.id.activity_main_screen_shot) as Button
        screenShot!!.isSelected = false
        titleTv = findViewById<View>(R.id.title_tv) as TextView
        videostreamPreviewTtView = findViewById<View>(R.id.livestream_preview_ttv) as TextureView
        videostreamPreviewSf = findViewById<View>(R.id.livestream_preview_sf) as SurfaceView
        videostreamPreviewSf!!.isClickable = true
        videostreamPreviewSf!!.setOnClickListener {
            val rate: Float = VideoFeeder.getInstance().transcodingDataRate
            showToast("current rate:" + rate + "Mbps")
            if (rate < 10) {
                VideoFeeder.getInstance().transcodingDataRate = 10.0f
                showToast("set rate to 10Mbps")
            } else {
                VideoFeeder.getInstance().transcodingDataRate = 3.0f
                showToast("set rate to 3Mbps")
            }
        }
        updateUIVisibility()
    }

    private fun updateUIVisibility() {
        when (demoType) {
            DemoType.USE_SURFACE_VIEW -> {
                videostreamPreviewSf!!.visibility = View.VISIBLE
                videostreamPreviewTtView!!.visibility = View.GONE
            }
            DemoType.USE_SURFACE_VIEW_DEMO_DECODER -> {
                /**
                 * we need display two video stream at the same time, so we need let them to be visible.
                 */
                videostreamPreviewSf!!.visibility = View.VISIBLE
                videostreamPreviewTtView!!.visibility = View.VISIBLE
            }
            DemoType.USE_TEXTURE_VIEW -> {
                videostreamPreviewSf!!.visibility = View.GONE
                videostreamPreviewTtView!!.visibility = View.VISIBLE
            }
            else -> {}
        }
    }
```

In the code above, we invoke the `initUi()` method to initialize all the TextViews, Buttons, SurfaceViews, and TextureViews. The video stream preview surface view is set to be clickable by setting a click listener. When it is clicked, the transcoding data rate is received from the video feeder, and the rate is displayed on the screen as a toast. Next, if the rate is less than 10Mbps, we set the rate to 10Mbps. Otherwise, we set the rate to 3Mbps.

After the UI is initialized, we update the UI visibility according to the `demoType` variable using the `updateUIVisibility()` method. If the demo type is `USE_TEXTURE_VIEW`, the TextureView is visible. If the demo type is `USE_SURFACE_VIEW`, the SurfaceView is visible. If the demo type is `USE_SURFACE_VIEW_DEMO_DECODER`, both the SurfaceView and the TextureView are visible.

#### 3. Implementing the Video Feed on the UI

Next, let's implement the `override onResume()` method to initialize the surface or texture view or to notify status changes. The code snippet looks like the following:

```kotlin
    override fun onResume() {
        super.onResume()
        initSurfaceOrTextureView()
        notifyStatusChange()
    }

    private fun initSurfaceOrTextureView() {
        when (demoType) {
            DemoType.USE_SURFACE_VIEW -> initPreviewerSurfaceView()
            DemoType.USE_SURFACE_VIEW_DEMO_DECODER -> {
                /**
                 * we also need init the textureView because the pre-transcoded video steam will display in the textureView
                 */
                initPreviewerTextureView()
                /**
                 * we use standardVideoFeeder to pass the transcoded video data to DJIVideoStreamDecoder, and then display it
                 * on surfaceView
                 */
                initPreviewerSurfaceView()
            }
            DemoType.USE_TEXTURE_VIEW -> initPreviewerTextureView()
            else -> {}
        }
    }
```

In the code above, based on the `demoType` variable, we initialize the surface or texture view. If the demo type is `USE_SURFACE_VIEW`, we initialize the previewer surface view. If the demo type is `USE_SURFACE_VIEW_DEMO_DECODER`, we initialize both of the previewer texture and surface views. FInally, if the demo type is `USE_TEXTURE_VIEW`, we just initialize the previewer texture view.

Along with this, we also need to notify the status change. The code snippet looks like the following:

```kotlin
    private var lastupdate: Long = 0
    private fun notifyStatusChange() {
        val product: BaseProduct? = VideoDecodingApplication.productInstance
        Log.d(
            TAG,
            "notifyStatusChange: " + when {
                product == null -> "Disconnect"
                product.model == null -> "null model"
                else -> product.model.name
            }
        )
        if (product != null) {
            if (product.isConnected && product.model != null) {
                updateTitle(product.model.name + " Connected " + demoType?.name)
            } else {
                updateTitle("Disconnected")
            }
        }

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener =
            VideoFeeder.VideoDataListener { videoBuffer, size ->
                if (System.currentTimeMillis() - lastupdate > 1000) {
                    Log.d(
                        TAG,
                        "camera recv video data size: $size"
                    )
                    lastupdate = System.currentTimeMillis()
                }
                when (demoType) {
                    DemoType.USE_SURFACE_VIEW -> mCodecManager?.sendDataToDecoder(videoBuffer, size)
                    DemoType.USE_SURFACE_VIEW_DEMO_DECODER ->
                        /**
                         * we use standardVideoFeeder to pass the transcoded video data to DJIVideoStreamDecoder, and then display it
                         * on surfaceView
                         */
                        /**
                         * we use standardVideoFeeder to pass the transcoded video data to DJIVideoStreamDecoder, and then display it
                         * on surfaceView
                         */
                        DJIVideoStreamDecoder.instance?.parse(videoBuffer, size)
                    DemoType.USE_TEXTURE_VIEW -> mCodecManager?.sendDataToDecoder(videoBuffer, size)
                    else -> {}
                }
            }
        if (product != null) {
            if (!product.isConnected) {
                mCamera = null
                showToast("Disconnected")
            } else {
                if (!product.model.equals(Model.UNKNOWN_AIRCRAFT)) {
                    mCamera = product.camera
                    if (mCamera != null) {
                        if (mCamera!!.isFlatCameraModeSupported) {
                            mCamera!!.setFlatMode(
                                SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE
                            ) { djiError: DJIError? ->
                                if (djiError != null) {
                                    showToast("can't change flat mode of camera, error:" + djiError.description)
                                }
                            }
                        } else {
                            mCamera!!.setMode(
                                SettingsDefinitions.CameraMode.SHOOT_PHOTO
                            ) { djiError: DJIError? ->
                                if (djiError != null) {
                                    showToast("can't change mode of camera, error:" + djiError.description)
                                }
                            }
                        }
                    }

                    //When calibration is needed or the fetch key frame is required by SDK, should use the provideTranscodedVideoFeed
                    //to receive the transcoded video feed from main camera.
                    if (demoType == DemoType.USE_SURFACE_VIEW_DEMO_DECODER && isTranscodedVideoFeedNeeded) {
                        standardVideoFeeder = VideoFeeder.getInstance().provideTranscodedVideoFeed()
                        standardVideoFeeder!!.addVideoDataListener(mReceivedVideoDataListener!!)
                        return
                    }
                    VideoFeeder.getInstance().primaryVideoFeed
                        .addVideoDataListener(mReceivedVideoDataListener!!)
                }
            }
        }
    }
```

In the code above, we start off by updating the title of the activity. If the product is connected, we update the title with the product model name and the demo type. If the product is not connected, we update the title with the string `Disconnected`.

Next, we update a listener to receive the raw H264 video data for camera live view. Based on the `demoType` variable, we use the `sendDataToDecoder()` method to send the raw H264 video data to the decoder if the demo type is `USE_SURFACE_VIEW` or `USE_TEXTURE_VIEW`. Whereas, if the demo type is `USE_SURFACE_VIEW_DEMO_DECODER` we use `standardVideoFeeder` to pass the transcoded video data to `DJIVideoStreamDecoder.kt`, and then display it on surfaceView.

Then, we check if the product is connected and the product model is not `UNKNOWN_AIRCRAFT`. If the product is connected and the product model is not `UNKNOWN_AIRCRAFT`, we get the camera instance from the product. If the camera is not null, we check if the camera is flat camera mode supported and then set the flat mode to `PHOTO_SINGLE` otherwise, we set the mode of the camera to `SHOOT_PHOTO`. If there is any error during this process, we show the error messages on the screen with a toast.

FInally, we then check if the demo type is `USE_SURFACE_VIEW_DEMO_DECODER` and the `isTranscodedVideoFeedNeeded` is true. If so, we get the instance of the `standardVideoFeeder` and add the `mReceivedVideoDataListener` to the `standardVideoFeeder`. If the demo type is not `USE_SURFACE_VIEW_DEMO_DECODER` or the `isTranscodedVideoFeedNeeded` is false, we get the instance of the `primaryVideoFeed` and add the `mReceivedVideoDataListener` to the `primaryVideoFeed`.

In the code snippet below, we implement the `override onDestroy()` and `override onPause()` methods. In the `onDestroy()` method, we clean the surface of the codec manager and destroy it. In the `onPause()` method, we remove the video data listener from the `primaryVideoFeed` and the `standardVideoFeeder`.

```kotlin
    override fun onPause() {
        if (mCamera != null) {
            VideoFeeder.getInstance().primaryVideoFeed
                .removeVideoDataListener(mReceivedVideoDataListener)
            standardVideoFeeder?.removeVideoDataListener(mReceivedVideoDataListener)
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (mCodecManager != null) {
            mCodecManager!!.cleanSurface()
            mCodecManager!!.destroyCodec()
        }
        super.onDestroy()
    }
```

#### 4. Initializing the Surface and Texture Views

This section implements some of the methods described in the last section.

The code snippet below initializes a fake texture view to for the codec manager, so that the video raw data can be received by the camera:

```kotlin
    private fun initPreviewerTextureView() {
        videostreamPreviewTtView!!.surfaceTextureListener = object :
            TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Log.d(TAG, "real onSurfaceTextureAvailable")
                videoViewWidth = width
                videoViewHeight = height
                Log.d(
                    TAG,
                    "real onSurfaceTextureAvailable: width $videoViewWidth height $videoViewHeight"
                )
                if (mCodecManager == null) {
                    mCodecManager = DJICodecManager(applicationContext, surface, width, height)
                    //For M300RTK, you need to actively request an I frame.
                    mCodecManager!!.resetKeyFrame()
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                videoViewWidth = width
                videoViewHeight = height
                Log.d(
                    TAG,
                    "real onSurfaceTextureAvailable2: width $videoViewWidth height $videoViewHeight"
                )
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                mCodecManager?.cleanSurface()
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }
```

Similarly, the code snippet below initializes a surface view to pass the transcoded video data into the `DJIVideoStreamDecoder.kt` which then later displays it on the surface view:

```kotlin
    private fun initPreviewerSurfaceView() {
        videostreamPreviewSh = videostreamPreviewSf!!.holder
        surfaceCallback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "real onSurfaceTextureAvailable")
                videoViewWidth = videostreamPreviewSf!!.width
                videoViewHeight = videostreamPreviewSf!!.height
                Log.d(
                    TAG,
                    "real onSurfaceTextureAvailable3: width $videoViewWidth height $videoViewHeight"
                )
                when (demoType) {
                    DemoType.USE_SURFACE_VIEW -> if (mCodecManager == null) {
                        mCodecManager = DJICodecManager(
                            applicationContext, holder, videoViewWidth,
                            videoViewHeight
                        )
                    }
                    DemoType.USE_SURFACE_VIEW_DEMO_DECODER -> {
                        // This demo might not work well on P3C and OSMO.
                        NativeHelper.instance?.init()
                        DJIVideoStreamDecoder.instance?.init(applicationContext, holder.surface)
                        DJIVideoStreamDecoder.instance?.resume()
                    }
                    else -> {}
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                videoViewWidth = width
                videoViewHeight = height
                Log.d(
                    TAG,
                    "real onSurfaceTextureAvailable4: width $videoViewWidth height $videoViewHeight"
                )
                when (demoType) {
                    DemoType.USE_SURFACE_VIEW -> {}
                    DemoType.USE_SURFACE_VIEW_DEMO_DECODER -> DJIVideoStreamDecoder.instance
                        ?.changeSurface(holder.surface)
                    else -> {}
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                when (demoType) {
                    DemoType.USE_SURFACE_VIEW -> if (mCodecManager != null) {
                        mCodecManager!!.cleanSurface()
                        mCodecManager!!.destroyCodec()
                        mCodecManager = null
                    }
                    DemoType.USE_SURFACE_VIEW_DEMO_DECODER -> {
                        DJIVideoStreamDecoder.instance?.stop()
                        NativeHelper.instance?.release()
                    }
                    else -> {}
                }
            }
        }
        videostreamPreviewSh!!.addCallback(surfaceCallback)
    }
```

#### 5. Implementing the override methods of the DJICodecManager.YuvDataCallback and Saving YUV Images

In order to do this, the MainActivity class should implement `DJICodecManager.YuvDataCallback` and override the `override onYuvDataReceived()` method. This method looks like the following:

```kotlin
    override fun onYuvDataReceived(
        format: MediaFormat,
        yuvFrame: ByteBuffer?,
        dataSize: Int,
        width: Int,
        height: Int
    ) {
        //In this demo, we test the YUV data by saving it into JPG files.
        //DJILog.d(TAG, "onYuvDataReceived " + dataSize);
        if (count++ % 30 == 0 && yuvFrame != null) {
            val bytes = ByteArray(dataSize)
            yuvFrame[bytes]
            //DJILog.d(TAG, "onYuvDataReceived2 " + dataSize);
            AsyncTask.execute {
                // two samples here, it may has other color format.
                when (format.getInteger(MediaFormat.KEY_COLOR_FORMAT)) {
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible ->     //                        //NV12
                        //COLOR_FormatYUV420SemiPlanar
                        if (Build.VERSION.SDK_INT <= 23) {
                            oldSaveYuvDataToJPEG(bytes, width, height)
                        } else {
                            newSaveYuvDataToJPEG(bytes, width, height)
                        }
                    else -> {}
                }
            }
        }
    }
```

In the code above, the `yuvFrame` is a `ByteBuffer` that contains the YUV data. The `dataSize` is the size of the YUV data. The `width` and `height` are the width and height of the YUV data. Given the current YUV media format and the current sdk version, either the old or the new method of saving the YUV data to JPEG files will be used or the new one.

The old method is shown below:

```kotlin
    // For android API <= 23
    private fun oldSaveYuvDataToJPEG(yuvFrame: ByteArray, width: Int, height: Int) {
        if (yuvFrame.size < width * height) {
            //DJILog.d(TAG, "yuvFrame size is too small " + yuvFrame.length);
            return
        }
        val y = ByteArray(width * height)
        val u = ByteArray(width * height / 4)
        val v = ByteArray(width * height / 4)
        val nu = ByteArray(width * height / 4) //
        val nv = ByteArray(width * height / 4)
        System.arraycopy(yuvFrame, 0, y, 0, y.size)
        for (i in u.indices) {
            v[i] = yuvFrame[y.size + 2 * i]
            u[i] = yuvFrame[y.size + 2 * i + 1]
        }
        val uvWidth = width / 2
        val uvHeight = height / 2
        for (j in 0 until uvWidth / 2) {
            for (i in 0 until uvHeight / 2) {
                val uSample1 = u[i * uvWidth + j]
                val uSample2 = u[i * uvWidth + j + uvWidth / 2]
                val vSample1 = v[(i + uvHeight / 2) * uvWidth + j]
                val vSample2 = v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2]
                nu[2 * (i * uvWidth + j)] = uSample1
                nu[2 * (i * uvWidth + j) + 1] = uSample1
                nu[2 * (i * uvWidth + j) + uvWidth] = uSample2
                nu[2 * (i * uvWidth + j) + 1 + uvWidth] = uSample2
                nv[2 * (i * uvWidth + j)] = vSample1
                nv[2 * (i * uvWidth + j) + 1] = vSample1
                nv[2 * (i * uvWidth + j) + uvWidth] = vSample2
                nv[2 * (i * uvWidth + j) + 1 + uvWidth] = vSample2
            }
        }
        //nv21test
        val bytes = ByteArray(yuvFrame.size)
        System.arraycopy(y, 0, bytes, 0, y.size)
        for (i in u.indices) {
            bytes[y.size + i * 2] = nv[i]
            bytes[y.size + i * 2 + 1] = nu[i]
        }
        Log.d(
            TAG,
            ("onYuvDataReceived: frame index: "
                    + DJIVideoStreamDecoder.instance?.frameIndex
                    ) + ",array length: "
                    + bytes.size
        )
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            screenShot(
                bytes,
                applicationContext.getExternalFilesDir("DJI")!!.path + "/DJI_ScreenShot",
                width,
                height
            )
        } else {
            screenShot(
                bytes,
                Environment.getExternalStorageDirectory().toString() + "/DJI_ScreenShot",
                width,
                height
            )
        }
    }
```

The code below shows the new method of saving the YUV data to JPEG files:

```kotlin
    private fun newSaveYuvDataToJPEG(yuvFrame: ByteArray, width: Int, height: Int) {
        if (yuvFrame.size < width * height) {
            //DJILog.d(TAG, "yuvFrame size is too small " + yuvFrame.length);
            return
        }
        val length = width * height
        val u = ByteArray(width * height / 4)
        val v = ByteArray(width * height / 4)
        for (i in u.indices) {
            v[i] = yuvFrame[length + 2 * i]
            u[i] = yuvFrame[length + 2 * i + 1]
        }
        for (i in u.indices) {
            yuvFrame[length + 2 * i] = u[i]
            yuvFrame[length + 2 * i + 1] = v[i]
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            screenShot(
                yuvFrame,
                applicationContext.getExternalFilesDir("DJI")!!.path + "/DJI_ScreenShot",
                width,
                height
            )
        } else {
            screenShot(
                yuvFrame,
                Environment.getExternalStorageDirectory().toString() + "/DJI_ScreenShot",
                width,
                height
            )
        }
    }
```

They both essentially do the same thing and saves the YUV data to JPEG files. They also both use the `screenShot()` function to save the YUV frame to JPEG. Here is a snippet below:

```kotlin
    /**
     * Save the buffered data into a JPG image file
     */
    private fun screenShot(buf: ByteArray, shotDir: String, width: Int, height: Int) {
        val dir = File(shotDir)
        if (!dir.exists() || !dir.isDirectory) {
            dir.mkdirs()
        }
        val yuvImage = YuvImage(
            buf,
            ImageFormat.NV21,
            width,
            height,
            null
        )
        val outputFile: OutputStream
        val path = dir.toString() + "/ScreenShot_" + System.currentTimeMillis() + ".jpg"
        outputFile = try {
            FileOutputStream(File(path))
        } catch (e: FileNotFoundException) {
            Log.e(
                TAG,
                "test screenShot: new bitmap output file error: $e"
            )
            return
        }
        yuvImage.compressToJpeg(
            Rect(
                0,
                0,
                width,
                height
            ), 100, outputFile
        )
        try {
            outputFile.close()
        } catch (e: IOException) {
            Log.e(
                TAG,
                "test screenShot: compress yuv image error: $e"
            )
            e.printStackTrace()
        }
        runOnUiThread { displayPath(path) }
    }

    private fun displayPath(_path: String) {
        var path = _path
        if (stringBuilder == null) {
            stringBuilder = StringBuilder()
        }
        path = """
            $path
            
            """.trimIndent()
        stringBuilder!!.append(path)
        savePath!!.text = stringBuilder.toString()
    }

```

As you can see above, this code snippet is mostly self explanatory. It saves the YUV frame into a folder called `DJI_ScreenShot` and saves the image with its current time stamp. The `displayPath()` function is used to display the path of the saved image.

#### 6. Handling the onClick events

Consider the code snippets below:

```kotlin
    fun onClick(v: View) {
        if (v.id == R.id.activity_main_screen_shot) {
            handleYUVClick()
        } else {
            var newDemoType: DemoType? = null
            if (v.id == R.id.activity_main_screen_texture) {
                newDemoType = DemoType.USE_TEXTURE_VIEW
            } else if (v.id == R.id.activity_main_screen_surface) {
                newDemoType = DemoType.USE_SURFACE_VIEW
            } else if (v.id == R.id.activity_main_screen_surface_with_own_decoder) {
                newDemoType = DemoType.USE_SURFACE_VIEW_DEMO_DECODER
            }
            if (newDemoType != null && newDemoType != demoType) {
                // Although finish will trigger onDestroy() is called, but it is not called before OnCreate of new activity.
                if (mCodecManager != null) {
                    mCodecManager!!.cleanSurface()
                    mCodecManager!!.destroyCodec()
                    mCodecManager = null
                }
                demoType = newDemoType
                finish()
                overridePendingTransition(0, 0)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        }
    }

    private fun handleYUVClick() {
        if (screenShot!!.isSelected) {
            screenShot!!.text = "YUV Screen Shot"
            screenShot!!.isSelected = false
            when (demoType) {
                DemoType.USE_SURFACE_VIEW, DemoType.USE_TEXTURE_VIEW -> {
                    mCodecManager?.enabledYuvData(false)
                    mCodecManager?.yuvDataCallback = null
                }
                DemoType.USE_SURFACE_VIEW_DEMO_DECODER -> {
                    DJIVideoStreamDecoder.instance
                        ?.changeSurface(videostreamPreviewSh!!.surface)
                    DJIVideoStreamDecoder.instance?.setYuvDataListener(null)
                }
                else -> {}
            }
            savePath!!.text = ""
            savePath!!.visibility = View.INVISIBLE
            stringBuilder = null
        } else {
            screenShot!!.text = "Live Stream"
            screenShot!!.isSelected = true
            when (demoType) {
                DemoType.USE_TEXTURE_VIEW, DemoType.USE_SURFACE_VIEW -> {
                    mCodecManager?.enabledYuvData(true)
                    mCodecManager?.yuvDataCallback = this
                }
                DemoType.USE_SURFACE_VIEW_DEMO_DECODER -> {
                    DJIVideoStreamDecoder.instance?.changeSurface(null)
                    DJIVideoStreamDecoder.instance?.setYuvDataListener(this@MainActivity)
                }
                else -> {}
            }
            savePath!!.text = ""
            savePath!!.visibility = View.VISIBLE
        }
    }
```

As you can see above, the `handleYUVClick()` function is used to handle the click event of the `screenShot` button. Once clicked, the save directory of each screenshot will be shown in the `savePath` text view. Aside from that, the rest of the `onClick()` options for the buttons are used to switch to different demo type views.

---

### Setting up NativeHelper.kt

This singleton class is created in order to be able to invoke native methods. Start off by creating this class within the media package of the project. Firstly, start off by creating an interface within the class as shown below:

```kotlin
object NativeHelper {
    interface NativeDataListener {
        /**
         * Callback method for receiving the frame data from NativeHelper.
         * Note that this method will be invoke in framing thread, which means time consuming
         * processing should not in this thread, or the framing process will be blocked.
         * @param data
         * @param size
         * @param frameNum
         * @param isKeyFrame
         * @param width
         * @param height
         */
        fun onDataRecv(
            data: ByteArray?,
            size: Int,
            frameNum: Int,
            isKeyFrame: Boolean,
            width: Int,
            height: Int
        )
    }
    ...
}
```

Next, create a local variable and a setter for the NativeDataListener interface as shown below:

```kotlin
    private var dataListener: NativeDataListener? = null
    fun setDataListener(dataListener: NativeDataListener?) {
        this.dataListener = dataListener
    }
```

The functions below are external functions that can be invoked from the native libraries and as such must be defined. Each function has comments explaining the function's purpose.

```kotlin
    //JNI
    /**
     * Test the ffmpeg.
     * @return
     */
    external fun codecinfotest(): String?

    /**
     * Initialize the ffmpeg.
     * @return
     */
    external fun init(): Boolean

    /**
     * Framing the raw data from camera
     * @param buf
     * @param size
     * @return
     */
    external fun parse(buf: ByteArray?, size: Int): Boolean

    /**
     * Release the ffmpeg
     * @return
     */
    external fun release(): Boolean

    /**
     * Invoke by JNI
     * Callback the frame data.
     * @param buf
     * @param size
     * @param frameNum
     * @param isKeyFrame
     * @param width
     * @param height
     */
    fun onFrameDataRecv(
        buf: ByteArray?,
        size: Int,
        frameNum: Int,
        isKeyFrame: Boolean,
        width: Int,
        height: Int
    ) {
        if (dataListener != null) {
            dataListener!!.onDataRecv(buf, size, frameNum, isKeyFrame, width, height)
        }
    }
```

Finally, load the native libraries in `init` and create a getter for the NativeHelper instance as shown below:

```kotlin
    val instance: NativeHelper
    get() {
        return this
    }

    init {
        System.loadLibrary("ffmpeg")
        System.loadLibrary("djivideojni")
    }
```

---

### Programming the DJIVideoStreamDecoder Utility Class

> NOTE: THIS CLASS IS VERY LARGE SO NOT ALL OF IT IS SHOWN HERE. PLEASE REFER TO THE DJIVideoStreamDecoder.kt FILE FOR MORE INFORMATION.

The first thing to note is that the entire decoding process is implemented through `FFmpeg` and `MediaCodec`. According to the tutorial on the official website, `DJIVideoStreamDecoder.kt` and `NativeHelper.kt` are the key classes for decoding.

This class is a helper class for hardware decoding. Please follow the following steps to use it:

1. Initialize and set the instance as a listener of NativeDataListener to receive the frame data.

2. Send the raw data from camera to ffmpeg for frame parsing.

3. Get the parsed frame data from ffmpeg parsing frame callback and cache the parsed framed data into the frameQueue.

4. Initialize the MediaCodec as a decoder and then check whether there is any i-frame in the MediaCodec. If not, get the default i-frame from sdk resource and insert it at the head of frameQueue. Then dequeue the framed data from the frameQueue and feed it(which is Byte buffer) into the MediaCodec.

5. Get the output byte buffer from MediaCodec, if a surface(Video Previewing View) is configured in the MediaCodec, the output byte buffer is only need to be released. If not, the output yuv data should invoke the callback and pass it out to external listener, it should also be released too.

6. Release the ffmpeg and the MediaCodec, stop the decoding thread.

Begin by creating a new class `DJIVideoStreamDecoder.kt` within the `media` package. Additionally, implement a companion object and init blocks as shown below (most of its members are self-explanatory):

```kotlin
class DJIVideoStreamDecoder private constructor() : NativeDataListener {

    ...

    companion object {
        private val TAG = DJIVideoStreamDecoder::class.java.simpleName
        private const val BUF_QUEUE_SIZE = 30
        private const val MSG_INIT_CODEC = 0
        private const val MSG_FRAME_QUEUE_IN = 1
        private const val MSG_DECODE_FRAME = 2
        private const val MSG_YUV_DATA = 3
        const val VIDEO_ENCODING_FORMAT = "video/avc"

        @get:Synchronized
        var instance: DJIVideoStreamDecoder? = null
            get() {
                if (field == null) {
                    field = DJIVideoStreamDecoder()
                }
                return field
            }
            private set
    }

    init {
        createTime = System.currentTimeMillis()
        frameQueue = ArrayBlockingQueue(BUF_QUEUE_SIZE)
        startDataHandler()
        handlerThreadNew = HandlerThread("native parser thread")
        handlerThreadNew.start()
        handlerNew = Handler(handlerThreadNew.looper) { msg ->
            val buf = msg.obj as ByteArray
            NativeHelper.instance?.parse(buf, msg.arg1)
            false
        }
    }

    ...
}
```

Next, add the following class variables:

```kotlin
    private val handlerThreadNew: HandlerThread
    private val handlerNew: Handler
    private val DEBUG = false
    private val frameQueue: Queue<DJIFrame>?
    private var dataHandlerThread: HandlerThread? = null
    private var dataHandler: Handler? = null
    private var context: Context? = null
    private var codec: MediaCodec? = null
    private var surface: Surface? = null
    var frameIndex = -1
    private var currentTime: Long = 0
    var width = 0
    var height = 0
    private var hasIFrameInQueue = false
    var bufferInfo = MediaCodec.BufferInfo()
    var bufferChangedQueue = LinkedList<Long>()
    private val createTime: Long
    private var yuvDataListener: YuvDataCallback? = null
```

Now, let's go list out each function and inner classes and define its purpose as follows:

> NOTE: Please go through the sample project to understand and implement each function in more detail.

* `setYuvDataListener()`: Sets the yuv frame data receiving callback. The callback method will be invoked when the decoder output yuv frame data. What should be noted here is that the hardware decoder would not output any yuv data if a surface is configured into, which mean that if you want the yuv frames, you should set "null" surface when calling the "configure" method of MediaCodec.

* `private class DJIFrame` : A data structure for containing the frames.

* `logd()` : Prints a debug log message

* `loge()` : Prints an error log message

* `init()` : Initializes the decoder.

* `parse()` : Frames the raw data from the camera.

* `getIframeRawId()` : Gets the resource ID of the IDR frame.

* `getDefaultKeyFrame()` : Gets the default black IDR frame.

* `initCodec()` : Initializes and starts the hardware decoder.

* `startDataHandler()` : Starts the data handler thread and handles frame data sent to it.

* `stopDataHandler()` : Stops the data handler thread and safely quits.

* `changeSurface()` : Changes the displaying surface of the decoder. What should be noted here is that the hardware decoder would not output any yuv data if a surface is configured into, which mean that if you want the yuv frames, you should set "null" surface when calling the "configure" method of MediaCodec.

* `releaseCodec()` : Releases and closes the codec.

* `onFrameQueueIn()` : Queues in the input frame.

* `decodeFrame()` : Dequeue the frames from the queue and decode them using the hardware decoder.

* `stop()` : Stops the decoding process.

* `resume()` : Resumes the decoding process.

* `override onDataRecv()` : Creates new DJI Frames and add them to a queue which then sends them to the data handler.

#### Importing the h264 Files as a Raw Resource and adding the JNI Libraries

This is a necessary step for the decoder to work. The decoder needs to know the size of the h264 file and the file itself. The easiest way to do this is to reference the h264 file as a raw resource.

First start off by clicking the top left dropdown in the project explorer and be sure to have `Project` selected. Next, go to the bottom of the list and expand `External Libraries`.

<img src="./images/external_lib.png" width="50%">

Afterwards, open up the `res` folder within there.

<img src="./images/res_dir.png" width="50%">

Then, copy all the contents within the `raw` folder under `External Libraries` into your local `raw` folder within `res`.

<img src="./images/raw_contents.png" width="50%">

Finally, add the `libs` folder to your project.

---

### Final Testing and Demo

#### 1. Connect to the Drone

Start the app and connect to the drone:

<img src="./images/connection_page.jpg" width="50%">

#### 2. View the Video Stream Using the Built-in Decoder

<img src="./images/surface_view.jpg" width="50%">

<img src="./images/texture_view.jpg" width="50%">

#### 3. View the Video Stream in a Using the Custom Decoder

<img src="./images/custom_decoder.jpg" width="50%">

> NOTE: Within this image, I've also clicked the YUV Screenshot Button which saves frames from the stream into the Android Device. Clicking this button again toggles it back to the Live Stream.

---

### Conclusion

To conclude, we've learned to use `FFmpeg` for video frame parsing and to use the `MediaCodec` for hardware decoding. We parsed video frames and decoded the raw video stream data from DJI Camera and outputted the YUV data. Hope this tutorial can help you integrate the Decoder feature in your DJI SDK based Application. Good luck, and hope you enjoyed this tutorial!

---

## License

MIT
