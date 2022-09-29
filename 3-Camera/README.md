
# Creating a Camera Application
## DJI LAB 3 TUTORIAL KOTLIN

***`WARNING: THIS TUTORIAL ASSUMES YOU'VE COMPLETED THE PREVIOUS TUTORIALS`***

This tutorial is designed for you to gain a basic understanding of the DJI Mobile SDK. It will implement the FPV view and two basic camera functionalities: Take Photo and Record video.

You can download the tutorial's final sample project from this [Github Page](https://github.com/godfreynolan/DJITutorialsKotlin/tree/main/1-Registration).

---
### Application Activation and Aircraft Binding in China

For DJI SDK mobile application used in China, it's required to activate the application and bind the aircraft to the user's DJI account.

If an application is not activated, the aircraft not bound (if required), or a legacy version of the SDK (< 4.1) is being used, all **camera live streams** will be disabled, and flight will be limited to a zone of 100m diameter and 30m height to ensure the aircraft stays within line of sight.

To learn how to implement this feature, please check this tutorial [Application Activation and Aircraft Binding](https://developer.dji.com/mobile-sdk/documentation/android-tutorials/ActivationAndBinding.html).

---
### Preparation
Throughout this tutorial we will be using Android Studio Bumblebee | 2021.1.1. You can download the latest version of Android Studio from here: http://developer.android.com/sdk/index.html.

> Note: In this tutorial, we will use Mavic Mini for testing. However, most other DJI drone models should be capable of working with this code. It is recommended to use the latest version of Android Studio for using this application.

---
### Setting up the Application

#### 1. Create the project

*   Open Android Studio and on the start-up screen select **File -> New Project**

*   In the **New Project** screen:
    *   Set the device to **"Phone and Tablet"**.
    *   Set the template to **"Empty Activity"** and then press **"Next"**.

*   On the next screen:
    * Set the **Application name** to your desired app name. In this example we will use `DJIFPV-Kotlin`.
    * The **Package name** is conventionally set to something like "com.companyName.applicationName". We will use `com.riis.fpv`.
    * Set **Language** to Kotlin
    * Set **Minimum SDK** to `API 21: Android 5.0 (Lollipop)`
    * Do **NOT** check the option to "Use legacy android.support.libraries"
    * Click **Finish** to create the project.

#### 2. Import Maven Dependency
In our previous tutorial, [Importing and Activating DJI SDK](https://github.com/godfreynolan/DJITutorialsKotlin/tree/main/1-Registration) in Android Studio Project, you have learned how to import the Android SDK Maven Dependency and activate your application. If you haven't read that previously, please take a look at it and implement the related features. Once you've done that, continue to implement the next features.

---
### Building the Layouts of Activity

#### 1. Creating the MApplication Class

In the project file navigator, go to **app -> java -> com -> riis -> fpv**, and right-click on the fpv directory. Select **New -> Kotlin Class** to create a new kotlin class and name it as `MApplication.kt`.

Then, open the `MApplication.kt` file and replace the content with the following:

```kotlin
package com.riis.kotlin_mediamanagerdemo

import android.app.Application
import android.content.Context
import com.secneo.sdk.Helper

class MApplication: Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Helper.install(this)
    }
}
```
Here we override the `attachBaseContext()` method to invoke the `install()` method of `Helper` class to load the SDK classes before using any SDK functionality. Failing to do so will result in unexpected crashes.

#### 2.  Implementing the MainActivity Class

The MainActivity.kt file is created by Android Studio by default. Let's replace its code with the following:

```kotlin
package com.riis.fpv

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dji.common.camera.SettingsDefinitions.CameraMode
import dji.common.camera.SettingsDefinitions.ShootPhotoMode
import dji.common.product.Model
import dji.sdk.base.BaseProduct
import dji.sdk.camera.Camera
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import dji.sdk.products.Aircraft
import dji.sdk.products.HandHeld
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.coroutines.launch

/*
This activity provides an interface to access a connected DJI Product's camera and use
it to take photos and record videos
*/
class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener, View.OnClickListener {

    //Class Variables
    private val TAG = MainActivity::class.java.name

    //listener that is used to receive video data coming from the connected DJI product
    private var receivedVideoDataListener: VideoFeeder.VideoDataListener? = null
    private var codecManager: DJICodecManager? = null //handles the encoding and decoding of video data

    private lateinit var videoSurface: TextureView //Used to display the DJI product's camera video stream
    private lateinit var captureBtn: Button
    private lateinit var shootPhotoModeBtn: Button
    private lateinit var recordVideoModeBtn: Button
    private lateinit var recordBtn: ToggleButton
    private lateinit var recordingTime: TextView


    //Creating the Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main) //inflating the activity_main.xml layout as the activity's view
        initUi() //initializing the UI

        /*
        The receivedVideoDataListener receives the raw video data and the size of the data from the DJI product.
        It then sends this data to the codec manager for decoding.
        */
        receivedVideoDataListener = VideoFeeder.VideoDataListener { videoBuffer, size ->
            codecManager?.sendDataToDecoder(videoBuffer, size)
        }

        /*
        NOTES:
        - getCameraInstance() is used to get an instance of the camera running on the DJI product as
          a Camera class object.

         - SystemState is a subclass of Camera that provides general information and current status of the camera.

         - Whenever a change in the camera's SystemState occurs, SystemState.Callback is an interface that
           asynchronously updates the camera's SystemState.

         - setSystemStateCallback is a method of the Camera class which allows us to define what else happens during
           the systemState callback. In this case, we update the UI on the UI thread whenever the SystemState shows
           that the camera is video recording.
        */

        getCameraInstance()?.let { camera ->
            camera.setSystemStateCallback {
                it.let { systemState ->
                    //Getting elapsed video recording time in minutes and seconds, then converting into a time string
                    val recordTime = systemState.currentVideoRecordingTimeInSeconds
                    val minutes = (recordTime % 3600) / 60
                    val seconds = recordTime % 60
                    val timeString = String.format("%02d:%02d", minutes, seconds)

                    //Accessing the UI thread to update the activity's UI
                    runOnUiThread {
                        //If the camera is video recording, display the time string on the recordingTime TextView
                        recordingTime.text = timeString
                        if (systemState.isRecording) {
                            recordingTime.visibility = View.VISIBLE

                        } else {
                            recordingTime.visibility = View.INVISIBLE
                        }
                    }
                }
            }
        }
    }

    //Function to initialize the activity's UI elements
    private fun initUi() {
        //referencing the layout views using their resource ids
        videoSurface = findViewById(R.id.video_previewer_surface)
        recordingTime = findViewById(R.id.timer)
        captureBtn = findViewById(R.id.btn_capture)
        recordBtn = findViewById(R.id.btn_record)
        shootPhotoModeBtn = findViewById(R.id.btn_shoot_photo_mode)
        recordVideoModeBtn = findViewById(R.id.btn_record_video_mode)

        /*
        Giving videoSurface a listener that checks for when a surface texture is available.
        The videoSurface will then display the surface texture, which in this case is a camera video stream.
        */
        videoSurface.surfaceTextureListener = this

        //Giving the non-toggle button elements a click listener
        captureBtn.setOnClickListener(this)
        shootPhotoModeBtn.setOnClickListener(this)
        recordVideoModeBtn.setOnClickListener(this)

        recordingTime.visibility = View.INVISIBLE

        /*
        recordBtn is a ToggleButton that when checked, the DJI product's camera starts video recording.
        When unchecked, the camera stops video recording.
        */
        recordBtn.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                startRecord()
            } else {
                stopRecord()
            }
        }
    }

    //Function to make the DJI drone's camera start video recording
    private fun startRecord() {
        val camera = getCameraInstance() ?:return //get camera instance or null if it doesn't exist

        /*
        starts the camera video recording and receives a callback. If the callback returns an error that
        is null, the operation is successful.
        */
        camera.startRecordVideo {
            if (it == null) {
                showToast("Record Video: Success")
            } else {
                showToast("Record Video Error: ${it.description}")
            }
        }
    }

    //Function to make the DJI product's camera stop video recording
    private fun stopRecord() {
        val camera = getCameraInstance() ?: return //get camera instance or null if it doesn't exist

        /*
        stops the camera video recording and receives a callback. If the callback returns an error that
        is null, the operation is successful.
        */
        camera.stopRecordVideo {
            if (it == null) {
                showToast("Stop Recording: Success")
            } else {
                showToast("Stop Recording: Error ${it.description}")
            }
        }
    }

    //Function that initializes the display for the videoSurface TextureView
    private fun initPreviewer() {

        //gets an instance of the connected DJI product (null if nonexistent)
        val product: BaseProduct = getProductInstance() ?: return

        //if DJI product is disconnected, alert the user
        if (!product.isConnected) {
            showToast(getString(R.string.disconnected))
        } else {
            /*
            if the DJI product is connected and the aircraft model is not unknown, add the
            receivedVideoDataListener to the primary video feed.
            */
            videoSurface.surfaceTextureListener = this
            if (product.model != Model.UNKNOWN_AIRCRAFT) {
                receivedVideoDataListener?.let {
                    VideoFeeder.getInstance().primaryVideoFeed.addVideoDataListener(
                        it
                    )
                }
            }
        }
    }

    //Function that uninitializes the display for the videoSurface TextureView
    private fun uninitPreviewer() {
        val camera: Camera = getCameraInstance() ?: return
    }

    //Function that displays toast messages to the user
    private fun showToast(msg: String?) {
        runOnUiThread { Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show() }
    }

    //When the MainActivity is created or resumed, initialize the video feed display
    override fun onResume() {
        super.onResume()
        initPreviewer()
    }

    //When the MainActivity is paused, clear the video feed display
    override fun onPause() {
        uninitPreviewer()
        super.onPause()
    }

    //When the MainActivity is destroyed, clear the video feed display
    override fun onDestroy() {
        uninitPreviewer()
        super.onDestroy()
    }

    //When a TextureView's SurfaceTexture is ready for use, use it to initialize the codecManager
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (codecManager == null) {
            codecManager = DJICodecManager(this, surface, width, height)
        }
    }

    //when a SurfaceTexture's size changes...
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    //when a SurfaceTexture is about to be destroyed, uninitialize the codedManager
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        codecManager?.cleanSurface()
        codecManager = null
        return false
    }

    //When a SurfaceTexture is updated...
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    //Handling what happens when certain layout views are clicked
    override fun onClick(v: View?) {
        when(v?.id) {
            //If the capture button is pressed, take a photo with the DJI product's camera
            R.id.btn_capture -> {
                captureAction()
            }
            //If the shoot photo mode button is pressed, set camera to only take photos
            R.id.btn_shoot_photo_mode -> {
                switchCameraMode(CameraMode.SHOOT_PHOTO)
            }
            //If the record video mode button is pressed, set camera to only record videos
            R.id.btn_record_video_mode -> {
                switchCameraMode(CameraMode.RECORD_VIDEO)
            }
            else -> {}
        }
    }

    //Function for taking a a single photo using the DJI Product's camera
    private fun captureAction() {
        val camera: Camera = getCameraInstance() ?: return

        /*
        Setting the camera capture mode to SINGLE, and then taking a photo using the camera.
        If the resulting callback for each operation returns an error that is null, then the
        two operations are successful.
        */
        val photoMode = ShootPhotoMode.SINGLE
        camera.setShootPhotoMode(photoMode) { djiError ->
            if (djiError == null) {
                lifecycleScope.launch {
                    camera.startShootPhoto { djiErrorSecond ->
                        if (djiErrorSecond == null) {
                            showToast("take photo: success")
                        } else {
                            showToast("Take Photo Failure: ${djiError?.description}")
                        }
                    }
                }
            }
        }
    }

    /*
    Function for setting the camera mode. If the resulting callback returns an error that
    is null, then the operation was successful.
    */
    private fun switchCameraMode(cameraMode: CameraMode) {
        val camera: Camera = getCameraInstance() ?: return

        camera.setMode(cameraMode) { error ->
            if (error == null) {
                showToast("Switch Camera Mode Succeeded")
            } else {
                showToast("Switch Camera Error: ${error.description}")
            }
        }

    }
    
    /*
    Note: 
    Depending on the DJI product, the mobile device is either connected directly to the drone,
    or it is connected to a remote controller (RC) which is then used to control the drone.
    */

    //Function used to get the DJI product that is directly connected to the mobile device
    private fun getProductInstance(): BaseProduct? {
        return DJISDKManager.getInstance().product
    }

    /*
    Function used to get an instance of the camera in use from the DJI product
    */
    private fun getCameraInstance(): Camera? {
        if (getProductInstance() == null) return null

        return when {
            getProductInstance() is Aircraft -> {
                (getProductInstance() as Aircraft).camera
            }
            getProductInstance() is HandHeld -> {
                (getProductInstance() as HandHeld).camera
            }
            else -> null
        }
    }

    //Function that returns True if a DJI aircraft is connected
    private fun isAircraftConnected(): Boolean {
        return getProductInstance() != null && getProductInstance() is Aircraft
    }
    
    //Function that returns True if a DJI product is connected
    private fun isProductModuleAvailable(): Boolean {
        return (getProductInstance() != null)
    }
    
    //Function that returns True if a DJI product's camera is available
    private fun isCameraModuleAvailable(): Boolean {
        return isProductModuleAvailable() && (getProductInstance()?.camera != null)
    }

    //Function that returns True if a DJI camera's playback feature is available
    private fun isPlaybackAvailable(): Boolean {
        return isCameraModuleAvailable() && (getProductInstance()?.camera?.playbackManager != null)
    }
}
```

In the code shown above, we implement the following features:

   1. Create the layout UI elements variables, including a TextureView `videoSurface`, three Buttons `captureBtn`, `shootPhotoModeBtn`, `recordVideoModeBtn`, one Toggle Button `recordBtn`, and a TextView `recordingTime`.
   
   2. Create a VideoDataListener variable called `receivedVideoDataListener` and a DJICodecManager variable called `codecManager`. The VideoDataListener will be used to recieve video data from a connected DJI product and the DJICodecManager will be used to decode the recieved video data.

   3. In the `onCreate()` function, invoke the `initUI()` method to initialize UI the variables. Then initialize `receivedVideoDataListener` and assign `codecManager` to decode the video data the listener recieves from a connected DJI product. Then get an instance of the camera from the DJI product and by accessing its `setSystemStateCallback`, have it display the elapsed recording time on the `recordingTime` TextView (if the camera is video recording).
   
   4. In the `initUi()` function, implement the `setOnClickListener()` method for all the Buttons. Also implement the `setOnCheckedChangeListener()` method for the `recordBtn` Toggle Button and set its action to start video recording (via the `startRecord()` function) when toggled and stop recording (via the `stopRecord()` function) when untoggled. Furthermore, initialize the surfaceTextureListener for `videoSurface` to allow it to display the video stream from the DJI product's camera.
   
   6. In the activity's `onResume()` function, invoke the `initPreviewer()` function which adds `receivedVideoDataListener` to the primary video feed of the connected DJI product's VideoFeeder. In the activity's `onPause()` and `onDestroy` functions, invoke `uninitPreviewer()` function which will reset the camera and stop the video feed.
   
   8. In the `onSurfaceTextureAvailable()` function, initialize `codecManager`. In the `onSurfaceTextureDestroyed()` function, clear the surface data of `codecManager` and uninitialize it.

   9.  Override the `onClick()` method to implement the click actions of the buttons:
       *   `captureBtn`: invokes `captureAction()` which tells the DJI product's camera to take a single photo
       *   `shootPhotoModeBtn`: invokes `switchCameraMode(CameraMode.SHOOT_PHOTO)` which tells the DJI product's camera to set itself to **PHOTO** mode
       *   `recordVideoModeBtn`: invokes `switchCameraMode(CameraMode.RECORD_VIDEO)` which which tells the DJI product's camera to set itself to **VIDEO** mode

#### 4.  Implementing the MainActivity Layout

Open the `activity_main.xml` layout file and replace the code with the following:

```xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"  
  xmlns:tools="http://schemas.android.com/tools"  
  android:layout_width="match_parent"  
  android:layout_height="match_parent"  
  android:orientation="vertical">  
  
 <TextureView  android:id="@+id/video_previewer_surface"  
  android:layout_width="match_parent"  
  android:layout_height="match_parent"  
  android:layout_above="@+id/linearLayout"  
  android:layout_gravity="center"  
  android:layout_marginBottom="-2dp" />  
  
 <LinearLayout  android:layout_width="match_parent"  
  android:layout_height="wrap_content"  
  android:orientation="horizontal"  
  android:layout_alignParentBottom="true"  
  android:id="@+id/linearLayout">  
 <Button  android:id="@+id/btn_capture"  
  android:layout_width="0dp"  
  android:layout_weight="1"  
  android:layout_gravity="center_vertical"  
  android:layout_height="wrap_content"  
  android:text="Capture"  
  android:textSize="12sp"/>  
  
 <ToggleButton  android:id="@+id/btn_record"  
  android:layout_width="0dp"  
  android:layout_height="wrap_content"  
  android:text="Start Record"  
  android:textOff="Start Record"  
  android:textOn="Stop Record"  
  android:layout_weight="1"  
  android:layout_gravity="center_vertical"  
  android:textSize="12dp"  
  android:checked="false" />  
  
 <Button  android:id="@+id/btn_shoot_photo_mode"  
  android:layout_width="0dp"  
  android:layout_weight="1"  
  android:layout_height="wrap_content"  
  android:layout_gravity="center_vertical"  
  android:text="Shoot Photo Mode"  
  android:textSize="12sp"/>  
  
 <Button  android:id="@+id/btn_record_video_mode"  
  android:layout_width="0dp"  
  android:layout_height="wrap_content"  
  android:text="Record Video Mode"  
  android:layout_weight="1"  
  android:layout_gravity="center_vertical" />  
  
 </LinearLayout>  
 <TextView  android:id="@+id/timer"  
  android:layout_width="150dp"  
  android:layout_weight="1"  
  android:layout_height="wrap_content"  
  android:layout_gravity="center_vertical"  
  android:layout_marginTop="23dp"  
  android:gravity="center"  
  android:textColor="#ffffff"  
  android:layout_alignTop="@+id/video_previewer_surface"  
  android:layout_centerHorizontal="true" />  
  
</RelativeLayout>
```

In the xml file, we create a `TextureView`(id: video_previewer_surface) element to show the live video stream from the camera. Moreover, we implement a LinearLayout element to create the "Capture" `Button`(id: btn_capture), "Record" `ToggleButton`(id: btn_record), "Shoot Photo Mode" `Button`(id: btn_shoot_photo_mode) and "Record Video Mode" `Button`(id: btn_record_video_mode).

Lastly, we create a `TextView`(id: timer) element to show the recorded video time.

#### 5.  Implementing the ConnectionActivity Class

To improve the user experience, we had better create an activity to show the connection status between the DJI Product and the SDK, once it's connected, the user can press the **OPEN** button to enter the **MainActivity**.

In the project navigator, go to **app -> java -> com -> riis -> fpv**, and right-click on the fpv directory. Select **New -> Kotlin Class/File** to create a new kotlin class and name it as `ConnectionActivity.kt`.

Next, replace the code of the `ConnectionActivity.kt` file with the following:

```kotlin
package com.riis.fpv

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import dji.sdk.sdkmanager.DJISDKManager

/*
This activity manages SDK registration and establishing a connection between the
DJI product and the user's mobile phone.
 */
class ConnectionActivity : AppCompatActivity() {

    //Class Variables
    private lateinit var mTextConnectionStatus: TextView
    private lateinit var mTextProduct: TextView
    private lateinit var mTextModelAvailable: TextView
    private lateinit var mBtnOpen: Button
    private lateinit var mVersionTv: TextView

    private val model: ConnectionViewModel by viewModels() //linking the activity to a viewModel

    companion object {
        const val TAG = "ConnectionActivity"
    }

    //Creating the Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //inflating the activity_connection.xml layout as the activity's view
        setContentView(R.layout.activity_connection)

        /*
        Request the following permissions defined in the AndroidManifest.
        1 is the integer constant we chose to use when requesting app permissions
        */
        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.VIBRATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.SYSTEM_ALERT_WINDOW,
                Manifest.permission.READ_PHONE_STATE
            ), 1)

        //Initialize the UI, register the app with DJI's mobile SDK, and set up the observers
        initUI()
        model.registerApp()
        observers()
    }

    //Function to initialize the activity's UI
    private fun initUI() {

        //referencing the layout views using their resource ids
        mTextConnectionStatus = findViewById(R.id.text_connection_status)
        mTextModelAvailable = findViewById(R.id.text_model_available)
        mTextProduct = findViewById(R.id.text_product_info)
        mBtnOpen = findViewById(R.id.btn_open)
        mVersionTv = findViewById(R.id.textView2)

        //Getting the DJI SDK version and displaying it on mVersionTv TextView
        mVersionTv.text = resources.getString(R.string.sdk_version, DJISDKManager.getInstance().sdkVersion)

        mBtnOpen.isEnabled = false //mBtnOpen Button is initially disabled

        //If mBtnOpen Button is clicked on, start MainActivity (only works when button is enabled)
        mBtnOpen.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    //Function to setup observers
    private fun observers() {
        //observer listens to changes to the connectionStatus variable stored in the viewModel
        model.connectionStatus.observe(this, Observer<Boolean> { isConnected ->
            //If boolean is True, enable mBtnOpen button. If false, disable the button.
            if (isConnected) {
                mTextConnectionStatus.text = "Status: Connected"
                mBtnOpen.isEnabled = true
            }
            else {
                mTextConnectionStatus.text = "Status: Disconnected"
                mBtnOpen.isEnabled = false
            }
        })

        /*
        Observer listens to changes to the product variable stored in the viewModel.
        product is a BaseProduct object and represents the DJI product connected to the mobile device
        */
        model.product.observe(this, Observer { baseProduct ->
            //if baseProduct is connected to the mobile device, display its firmware version and model name.
            if (baseProduct != null && baseProduct.isConnected) {
                mTextModelAvailable.text = baseProduct.firmwarePackageVersion

                //name of the aircraft attached to the remote controller
                mTextProduct.text = baseProduct.model.displayName
            }
        })
    }
}
```
In the code shown above, we implement the following:

1. Create the layout UI elements variables, including four TextViews `mTextConnectionStatus`, `mTextProduct`, `mTextModelAvailable`, `mVersionTv` and one Button `mBtnOpen`.

2. Link the activity to a ViewModel that stores the connection state and DJI SDK functions

4. In the onCreate() method, we request all the neccessary permissions for this application to work using the `ActivityCompat.requestPermissions()` method. We then invoke the `initUI()` method to initialize the four TextViews and the Button. We also setup the observers for this activity using the `observers()` method.

5. In the `initUI()` method, The `mBtnOpen` button is initially diabled. We invoke the `setOnClickListener()` method of `mBtnOpen` and set the Button's click action to start the MainActivity (only works when button is enabled). The `mVersionTv` TextView is set to display the DJI SDK version.

6. In the `observers()` method, we are observing changes (from the ViewModel) to the connection state between app and the DJI product as well as any changes to the product itself. Based on this, the `mTextConnectionStatus` will display the connection status, `mTextProduct` will the display the product's name, and `mTextModelAvailable` will display the DJI product's firmware version. If a DJI product is connected, the `mBtnOpen` Button becomes enabled.
 
#### 6. Implementing the ConnectionActivity Layout

Open the `activity_connection.xml` layout file and replace the code with the following:

```xml
<?xml version="1.0" encoding="utf-8"?>  
<RelativeLayout  
  xmlns:android="http://schemas.android.com/apk/res/android"  
  xmlns:tools="http://schemas.android.com/tools"  
  xmlns:app="http://schemas.android.com/apk/res-auto"  
  android:layout_width="match_parent"  
  android:layout_height="match_parent"  
  tools:context=".ConnectionActivity">  
  
 <TextView  android:id="@+id/text_connection_status"  
  android:layout_width="wrap_content"  
  android:layout_height="wrap_content"  
  android:layout_alignBottom="@+id/text_product_info"  
  android:layout_centerHorizontal="true"  
  android:layout_marginBottom="89dp"  
  android:gravity="center"  
  android:text="Status: No Product Connected"  
  android:textColor="@android:color/black"  
  android:textSize="20dp"  
  android:textStyle="bold" />  
  
 <TextView  android:id="@+id/text_product_info"  
  android:layout_width="wrap_content"  
  android:layout_height="wrap_content"  
  android:layout_centerHorizontal="true"  
  android:layout_marginTop="270dp"  
  android:text="@string/product_information"  
  android:textColor="@android:color/black"  
  android:textSize="20dp"  
  android:gravity="center"  
  android:textStyle="bold"  
  />  
  
 <TextView  android:id="@+id/text_model_available"  
  android:layout_width="match_parent"  
  android:layout_height="wrap_content"  
  android:layout_centerHorizontal="true"  
  android:gravity="center"  
  android:layout_marginTop="300dp"  
  android:text="@string/model_not_available"  
  android:textSize="15dp"/>  
  
 <Button  android:id="@+id/btn_open"  
  android:layout_width="150dp"  
  android:layout_height="55dp"  
  android:layout_centerHorizontal="true"  
  android:layout_marginTop="350dp"  
  android:background="@drawable/round_btn"  
  android:text="Open"  
  android:textColor="@color/colorWhite"  
  android:textSize="20dp"  
  />  
  
 <TextView  android:layout_width="wrap_content"  
  android:layout_height="wrap_content"  
  android:layout_centerHorizontal="true"  
  android:layout_marginTop="430dp"  
  android:text="@string/sdk_version"  
  android:textSize="15dp"  
  android:id="@+id/textView2" />  
  
 <TextView  android:id="@+id/textView"  
  android:layout_width="wrap_content"  
  android:layout_height="wrap_content"  
  android:layout_marginTop="58dp"  
  android:text="@string/app_name"  
  android:textAppearance="?android:attr/textAppearanceSmall"  
  android:textColor="@color/black_overlay"  
  android:textSize="20dp"  
  android:textStyle="bold"  
  android:layout_alignParentTop="true"  
  android:layout_centerHorizontal="true" />  
  
</RelativeLayout>
```
In the xml file, we create four TextViews and one Button within a RelativeLayout. We use the `TextView(id: text_connection_status)` to show the product connection status and use the `TextView(id:text_product_info)` to show the connected product name. The `Button(id: btn_open)` is used to open the **MainActivity**.

#### 7. Implementing the ConnectionViewModel Class

To store important variables and functions needed for mobile SDK registration and connection to the DJI product, an AndroidViewModel class is needed. This allows the app to maintain its connection state across rotation death.

In the project navigator, go to **app -> java -> com -> riis -> fpv**, and right-click on the fpv directory. Select **New -> Kotlin Class/File** to create a new kotlin class and name it as `ConnectionViewModel.kt`.

Next, replace the code of the `ConnectionViewModel.kt` file with the following:
```kotlin
package com.riis.fpv

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager

/*
This ViewModel stores important variables and functions needed for mobile SDK registration
and connection to the DJI product. This allows the app to maintain its connection state
across rotation death.
 */
class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    //product is a BaseProduct object which stores an instance of the currently connected DJI product
    val product: MutableLiveData<BaseProduct?> by lazy {
        MutableLiveData<BaseProduct?>()
    }

    //connectionStatus boolean describes whether or not a DJI product is connected
    val connectionStatus: MutableLiveData<Boolean> = MutableLiveData(false)

    //DJI SDK app registration
    fun registerApp() {
        /*
        Getting an instance of the DJISDKManager and using it to register the app
        (requires API key in AndroidManifest). After installation, the app connects to the DJI server via
        internet and verifies the API key. Subsequent app starts will use locally cached verification
        information to register the app when the cached information is still valid.
        */
        DJISDKManager.getInstance().registerApp(getApplication(), object: DJISDKManager.SDKManagerCallback {
            //Logging the success or failure of the registration
            override fun onRegister(error: DJIError?) {
                if (error == DJISDKError.REGISTRATION_SUCCESS) {
                    Log.i(ConnectionActivity.TAG, "onRegister: Registration Successful")
                } else {
                    Log.i(ConnectionActivity.TAG, "onRegister: Registration Failed - ${error?.description}")
                }
            }
            //called when the remote controller disconnects from the user's mobile device
            override fun onProductDisconnect() {
                Log.i(ConnectionActivity.TAG, "onProductDisconnect: Product Disconnected")
                connectionStatus.postValue(false) //setting connectionStatus to false
            }
            //called when the remote controller connects to the user's mobile device
            override fun onProductConnect(baseProduct: BaseProduct?) {
                Log.i(ConnectionActivity.TAG, "onProductConnect: Product Connected")
                product.postValue(baseProduct)
                connectionStatus.postValue(true) //setting connectionStatus to true
            }
            //called when the DJI aircraft changes
            override fun onProductChanged(baseProduct: BaseProduct?) {
                Log.i(ConnectionActivity.TAG, "onProductChanged: Product Changed - $baseProduct")
                product.postValue(baseProduct)

            }
            //Called when a component object changes. This method is not called if the component is already disconnected
            override fun onComponentChange(componentKey: BaseProduct.ComponentKey?, oldComponent: BaseComponent?, newComponent: BaseComponent?) {
                //Alert the user which component has changed, and mention what new component replaced the old component (can be null)
                Log.i(ConnectionActivity.TAG, "onComponentChange key: $componentKey, oldComponent: $oldComponent, newComponent: $newComponent")

                //Listens to connectivity changes in each new component
                newComponent?.let { component ->
                    component.setComponentListener { connected ->
                        Log.i(ConnectionActivity.TAG, "onComponentConnectivityChange: $connected")
                    }
                }
            }
            //called when loading SDK resources
            override fun onInitProcess(p0: DJISDKInitEvent?, p1: Int) {}

            //Called when Fly Safe database download progress is updated
            override fun onDatabaseDownloadProgress(p0: Long, p1: Long) {}
        })
    }
}
```
Here, we implement several features:

* variable **product** is used to store an instance of the currently connected DJI product
* variable **connectionStatus** describes whether or not a DJI product is connected
* The app is registered with the DJI SDK and an instance of `SDKManagerCallback` is initialized to provide feedback     from the SDK.
* Four interface methods of `SDKManagerCallback` are used. The `onRegister()` method is used to check the Application registration status and show text message here. When the product is connected or disconnected, the `onProductConnect()` and `onProductDisconnect()` methods will be invoked. Moreover, we use the `onComponentChange()` method to check the component changes.

>Note: Permissions must be requested by the application and granted by the user in order to register the DJI SDK correctly. This is taken care of in **ConnectionActivity** before it calls on the ViewModel's registerApp() method. Furthermore, the camera and USB hardwares must be declared in the **AndroidManifest** for DJI SDK to work.

#### 8. Configuring the Resource XMLs

Once you finish the above steps, let's copy all the images (xml files) from this Github project's drawable folder (**app -> res -> drawable**) to the same folder in your project.

<p align="center">
   <img src="../images/lab3_drawables_android_studio_screen_shot.PNG" width="30%" height="30%">
</p>

Moreover, open the `colors.xml` file and update the content as shown below:
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
    <color name="black_overlay">#000000</color>
    <color name="colorWhite">#FFFFFF</color>
    <color name="background_blue">#242d34</color>
    <color name="transparent">#00000000</color>
    <color name="dark_gray">#80000000</color>
</resources>
```
Furthermore, open the `strings.xml` file and replace the content with the following:
```xml
<resources>  
 <string name="app_name">DJIFPV-Kotlin</string>  
 <string name="action_settings">Settings</string>  
 <string name="disconnected">Disconnected</string>  
 <string name="product_information">Product Information</string>  
 <string name="connection_loose">Status: No Product Connected</string>  
 <string name="model_not_available">Model Not Available</string>  
 <string name="push_info">Push Info</string>  
 <string name="sdk_version">DJI SDK Version: %1$s</string>  
</resources>
```
Lastly, open the `styles.xml` file and replace the content with the following:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="status_text">
        <item name="android:shadowColor">@color/black_overlay</item>
        <item name="android:shadowDx">2</item>
        <item name="android:shadowDy">1</item>
        <item name="android:shadowRadius">6</item>
        <item name="android:textSize">17sp</item>
        <item name="android:textColor">@color/white</item>
    </style>
</resources>
```
---
### Registering the Application
After you finish the above steps, let's register our application with the App Key you obtain from the DJI Developer Website. If you are not familiar with the App Key, please check the [Get Started](https://developer.dji.com/mobile-sdk/documentation/quick-start/index.html).
1. Let's open the `AndroidManifest.xml` file and specify the permissions that your application needs by adding `<uses-permission>` elements into the `<manifest>` element of the `AndroidManifest.xml` file. We also need to declare the camera and USB hardwares using `<uses-feature>` child elements since they will be used by the application.
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="a`ndroid.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"
    tools:ignore="ProtectedPermissions" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    tools:ignore="ScopedStorage" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-feature android:name="android.hardware.camera" />  
<uses-feature android:name="android.hardware.camera.autofocus" />  
<uses-feature  
  android:name="android.hardware.usb.host"  
  android:required="false" />  
<uses-feature  
  android:name="android.hardware.usb.accessory"  
  android:required="true" />
```
Next, add `android:name=".MApplication"` inside of the `<application>` element in the `AndroidManifest.xml` file:
```xml
<application  
  android:name="com.riis.fpv.MApplication"  
  android:allowBackup="true"  
  android:icon="@mipmap/ic_launcher"  
  android:label="@string/app_name"  
  android:roundIcon="@mipmap/ic_launcher_round"  
  android:supportsRtl="true"  
  android:theme="@style/Theme.DJIFPVKotlin">
  ```
Moreover, let's add the following elements as childs of the `<application>` element, right on top of the "ConnectionActivity" `<activity>` element as shown below:
```xml
<!-- DJI SDK -->
<uses-library android:name="com.android.future.usb.accessory" />
<uses-library
   android:name="org.apache.http.legacy"
   android:required="false" />
<meta-data
   android:name="com.dji.sdk.API_KEY"
   android:value="Please enter your APP Key here." />
<!-- DJI SDK -->
```
In the code above, you should substitute your **App Key** of the application for "Please enter your App Key here." in the value attribute under the `android:name="com.dji.sdk.API_KEY` attribute.
Lastly, update the "MainActivity" and "ConnectionActivity" `<activity>` elements as shown below:
```xml
<activity android:name="com.riis.fpv.ConnectionActivity"
            android:screenOrientation="portrait"
            android:launchMode="singleTop"
            android:configChanges="orientation">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.hardware.usb.action.USB_ACCESSORY_ATTACHED" />
            </intent-filter>
        </activity>
        <activity android:name="com.riis.fpv.MainActivity"
            android:screenOrientation="userLandscape"/>
```
In the code above, we add the attributes of `android:screenOrientation` to set "ConnectionActivity" as **portrait** and set "MainActivity" as **landscape**.
Congratulations! Your Aerial FPV android app is complete, you can now use this app to control the camera of your DJI Product now.

#### Connection Activity 
<img src="../Videos/demo.gif" alt="drawing" width="30%" height="30%"/>

#### Main Activity (FPV Screen)
<img src="../Videos/demo2.gif" alt="drawing" width="60%" height="60%"/>

---
### Connecting to the Aircraft or Handheld Device

Please check this [Connect Mobile Device and Run Application](https://developer.dji.com/mobile-sdk/documentation/application-development-workflow/workflow-run.html#connect-mobile-device-and-run-application) guide to run the application and view the live video stream from your DJI product's camera.

---
### App Screenshots

#### Connection Activity 
*    Disconnected:
<img src="https://user-images.githubusercontent.com/33791954/154558715-200dc0b1-f8bb-483b-a07a-f40fae1d96a5.jpg" alt="drawing" width="30%" height="30%"/>

*    Connected:
<img src="https://user-images.githubusercontent.com/33791954/154558814-60241803-2ef1-4dd0-b03e-3581bab67bb4.jpg" alt="drawing" width="30%" height="30%"/>

#### Main Activity (FPV Screen)
*    Normal:
<img src="../images/screenshot_20220301-230002_djifpv-kotlin.jpg" alt="drawing" width="60%" height="60%"/>
     
*    Switching Camera Mode:
<img src="../images/screenshot_20220301-230011_djifpv-kotlin.jpg" alt="drawing" width="60%" height="60%"/>
     
*    Capturing Photo:
<img src="../images/screenshot_20220301-230021_djifpv-kotlin.jpg" alt="drawing" width="60%" height="60%"/>
     
*    Video Recording Started:
<img src="../images/screenshot_20220301-230040_djifpv-kotlin.jpg" alt="drawing" width="60%" height="60%"/>
     
*    Video Recording in Progress:
<img src="../images/screenshot_20220301-230046_djifpv-kotlin.jpg" alt="drawing" width="60%" height="60%"/>
     
*    Video Recording Stopped:
<img src="../images/screenshot_20220301-230118_djifpv-kotlin.jpg" alt="drawing" width="60%" height="60%"/>

---
### Summary

In this tutorial, you have learned how to use **MediaManager** to preview photos, play videos, download or delete files, you also learn how to get and show the video playback status info. By using the **MediaManager**, the users can get the metadata for all the multimedia files, and has access to each individual multimedia file. Hope you enjoy it!

---
### License

MIT
