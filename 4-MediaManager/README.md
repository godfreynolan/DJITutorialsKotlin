
# Creating a Media Manager Application
## DJI LAB 4 TUTORIAL KOTLIN

***`WARNING: THIS TUTORIAL ASSUMES YOU'VE COMPLETED THE PREVIOUS TUTORIALS`***

In this tutorial, you will learn how to use the MediaManager to interact with the file system on the SD card of the aircraft's camera. By the end of this tutorial, you will have an app that you can use to preview photos, play videos, download or delete files and so on.

In order for our app to manage photos and videos, however, it must first be able to take and record them. Fortunately, by using DJI Android UX SDK, you can implement shooting photos and recording videos functionalities easily with standard DJI Go UIs.

You can download the tutorial's final sample project from this [Github Page]([[https://github.com/riisinterns/drone-lab-four-media-manager](https://github.com/godfreynolan/DJITutorialsKotlin/tree/main/4-MediaManager)](https://github.com/godfreynolan/DJITutorialsKotlin/tree/main/4-MediaManager)).

---
### Application Activation and Aircraft Binding in China

For DJI SDK mobile application used in China, it's required to activate the application and bind the aircraft to the user's DJI account.

If an application is not activated, the aircraft not bound (if required), or a legacy version of the SDK (< 4.1) is being used, all **camera live streams** will be disabled, and flight will be limited to a zone of 100m diameter and 30m height to ensure the aircraft stays within line of sight.

To learn how to implement this feature, please check this tutorial [Application Activation and Aircraft Binding](https://developer.dji.com/mobile-sdk/documentation/android-tutorials/ActivationAndBinding.html).

---
### Preparation
Throughout this tutorial we will be using Android Studio Bumblebee | 2021.1.1. You can download the latest version of Android Studio from here: http://developer.android.com/sdk/index.html.

> Note: In this tutorial, we will use Mavic Mini for testing. However, most other DJI drone models should be capable of working with this code. It is recommended to use the latest version of Android Studio for using this application. Furthermore, a micro SD card must be inserted into the drone for this application to work.

---
### Setting up the Application

#### 1. Create the project

*   Open Android Studio and on the start-up screen select **File -> New Project**

*   In the **New Project** screen:
    *   Set the device to **"Phone and Tablet"**.
    *   Set the template to **"Empty Activity"** and then press **"Next"**.

*   On the next screen:
    * Set the **Application name** to your desired app name. In this example we will use `Kotlin-MediaManagerDemo`.
    * The **Package name** is conventionally set to something like "com.companyName.applicationName". We will use `com.riis.kotlin_mediamanagerdemo`.
    * Set **Language** to Kotlin
    * Set **Minimum SDK** to `API 21: Android 5.0 (Lollipop)`
    * Do **NOT** check the option to "Use legacy android.support.libraries"
    * Click **Finish** to create the project.

*   In the `build.gradle (Module)` file, set the **applicationID** to **com.riis.kotlin_mediamanagerdemo**. This bundle ID corresponds to the DJI key that we will use.

#### 2. Import Maven Dependency

Please check the [Importing and Activating DJI SDK](https://github.com/godfreynolan/DJITutorialsKotlin/tree/main/1-Registration) tutorial to learn how to import the DJI Android UX SDK Maven Dependency to your project. If you haven't read that previously, please take a look at it.

#### build.gradle (Project)
Please **replace everything** in the `build.gradle (Project)` with
```
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    ext.kotlin_version = '1.6.10'
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:7.0.4'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
```

#### build.gradle (Module)
Please **replace everything** in `build.gradle (Module)` with 
```
plugins {
    id 'com.android.application'
    id 'kotlin-android'
}

android {
    compileSdkVersion 31
    buildToolsVersion "30.0.3"

    defaultConfig {
        applicationId 'com.riis.kotlin_mediamanagerdemo'
        minSdkVersion 21
        targetSdkVersion 30
        versionCode 1
        multiDexEnabled true
        versionName "1.0"
        ndk {
            // On x86 devices that run Android API 23 or above, if the application is targeted with API 23 or
            // above, FFmpeg lib might lead to runtime crashes or warnings.
            abiFilters 'armeabi-v7a', 'x86', 'arm64-v8a'
        }

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
        debug {
            shrinkResources false
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = '1.8'
    }
    dexOptions {
        javaMaxHeapSize "4g"
    }

    packagingOptions {
        doNotStrip "*/*/libdjivideo.so"
        doNotStrip "*/*/libSDKRelativeJNI.so"
        doNotStrip "*/*/libFlyForbid.so"
        doNotStrip "*/*/libduml_vision_bokeh.so"
        doNotStrip "*/*/libyuv2.so"
        doNotStrip "*/*/libGroudStation.so"
        doNotStrip "*/*/libFRCorkscrew.so"
        doNotStrip "*/*/libUpgradeVerify.so"
        doNotStrip "*/*/libFR.so"
        doNotStrip "*/*/libDJIFlySafeCore.so"
        doNotStrip "*/*/libdjifs_jni.so"
        doNotStrip "*/*/libsfjni.so"
        doNotStrip "*/*/libDJICommonJNI.so"
        doNotStrip "*/*/libDJICSDKCommon.so"
        doNotStrip "*/*/libDJIUpgradeCore.so"
        doNotStrip "*/*/libDJIUpgradeJNI.so"
        exclude 'META-INF/rxjava.properties'
    }
}

dependencies {
    implementation 'androidx.documentfile:documentfile:1.0.1'
    //DJI Dependencies
    implementation 'androidx.multidex:multidex:2.0.0'
    implementation ('com.dji:dji-sdk:4.16', {
        exclude module: 'library-anti-distortion'
        exclude module: 'fly-safe-database'
    })
    implementation ('com.dji:dji-uxsdk:4.16', {
        exclude module: 'library-anti-distortion'
        exclude module: 'fly-safe-database'
    })
    compileOnly ('com.dji:dji-sdk-provided:4.16')

    // ViewModels and Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2'
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.2.4")


    //Default
    implementation fileTree(dir: "libs", include: ["*.jar"])
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    implementation 'androidx.lifecycle:lifecycle-extensions:2.0.0-rc01'
    implementation 'androidx.annotation:annotation:1.2.0'
    implementation 'androidx.appcompat:appcompat:1.2.0'
    implementation 'com.google.android.material:material:1.3.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.0.4'
    testImplementation 'junit:junit:4.+'
    androidTestImplementation 'androidx.test.ext:junit:1.1.2'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.3.0'


}

// Please uncomment the following code if you use your own sdk version.
//apply from : "https://terra-1-g.djicdn.com/71a7d383e71a4fb8887a310eb746b47f/msdk/Android-CommonConfig/config_sample_all.gradle"
```

#### Android Jetifier
Please **add** the following line to the `gradle.properties` file
```
android.enableJetifier=true
```

#### settings.gradle
Please **replace everything** in the `settings.gradle` with
```
rootProject.name = "Kotlin-MediaManagerDemo"
include ':app'
```

---
### Building the Layouts of Activity

#### 1. Creating the MApplication Class

In the project file navigator, go to **app -> java -> com -> riis -> kotlin_mediamanagerdemo**, and right-click on the kotlin_mediamanagerdemo directory. Select **New -> Kotlin Class** to create a new kotlin class and name it as `MApplication.kt`.

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
#### 2.  Implementing the ConnectionActivity Class

In the project navigator, go to **app -> java -> com -> riis -> kotlin_mediamanagerdemo**, and right-click on the kotlin_mediamanagerdemo directory. Select **New -> Kotlin Class/File** to create a new kotlin class and name it as `ConnectionActivity.kt`.

Next, replace the code of the `ConnectionActivity.kt` file with the following:

```kotlin
package com.riis.kotlin_mediamanagerdemo

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

class ConnectionActivity : AppCompatActivity() {
    private lateinit var mTextConnectionStatus: TextView
    private lateinit var mTextProduct: TextView
    private lateinit var mTextModelAvailable: TextView
    private lateinit var mBtnOpen: Button
    private lateinit var mVersionTv: TextView

    private val model: ConnectionViewModel by viewModels()

    companion object {
        const val TAG = "ConnectionActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

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

        initUI()
        model.registerApp()
        observers()

    }

    private fun initUI() {
        mTextConnectionStatus = findViewById(R.id.text_connection_status)
        mTextModelAvailable = findViewById(R.id.text_model_available)
        mTextProduct = findViewById(R.id.text_product_info)
        mBtnOpen = findViewById(R.id.btn_open)
        mVersionTv = findViewById(R.id.textView2)
        mVersionTv.text = resources.getString(R.string.sdk_version, DJISDKManager.getInstance().sdkVersion)
        mBtnOpen.isEnabled = false
        mBtnOpen.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observers() {
        model.connectionStatus.observe(this, Observer<Boolean> { isConnected ->
            if (isConnected) {
                mTextConnectionStatus.text = "Status: Connected"
                mBtnOpen.isEnabled = true
            }
            else {
                mTextConnectionStatus.text = "Status: Disconnected"
                mBtnOpen.isEnabled = false
            }
        })

        model.product.observe(this, Observer { baseProduct ->
            if (baseProduct != null && baseProduct.isConnected) {
                mTextModelAvailable.text = baseProduct.firmwarePackageVersion
                mTextProduct.text = baseProduct.model.displayName
            }

        })
    }
}
```
 
#### 3. Implementing the ConnectionActivity Layout

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

  <TextView
      android:id="@+id/text_connection_status"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:gravity="center"
      android:text="Status: No Product Connected"
      android:textColor="@android:color/black"
      android:textSize="20dp"
      android:textStyle="bold"
      android:layout_alignBottom="@+id/text_product_info"
      android:layout_centerHorizontal="true"
      android:layout_marginBottom="89dp" />

  <TextView
      android:id="@+id/text_product_info"
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

  <TextView
      android:id="@+id/text_model_available"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:layout_centerHorizontal="true"
      android:gravity="center"
      android:layout_marginTop="300dp"
      android:text="@string/model_not_available"
      android:textSize="15dp"/>

  <Button
      android:id="@+id/btn_open"
      android:layout_width="150dp"
      android:layout_height="55dp"
      android:layout_centerHorizontal="true"
      android:layout_marginTop="350dp"
      android:background="@drawable/round_btn"
      android:text="Open"
      android:textColor="@color/colorWhite"
      android:textSize="20dp"
      />

  <TextView
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:layout_centerHorizontal="true"
      android:layout_marginTop="430dp"
      android:text="@string/sdk_version"
      android:textSize="15dp"
      android:id="@+id/textView2" />

  <TextView
      android:id="@+id/textView"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:layout_marginTop="58dp"
      android:text="DJI MediaManagerDemo"
      android:textAppearance="?android:attr/textAppearanceSmall"
      android:textColor="@color/black_overlay"
      android:textSize="20dp"
      android:textStyle="bold"
      android:layout_alignParentTop="true"
      android:layout_centerHorizontal="true" />

  </RelativeLayout>
```

#### 4. Implementing the ConnectionViewModel Class

In the project navigator, go to **app -> java -> com -> riis -> kotlin_mediamanagerdemo**, and right-click on the kotlin_mediamanagerdemo directory. Select **New -> Kotlin Class/File** to create a new kotlin class and name it as `ConnectionViewModel.kt`. 

Next, replace the code of the `ConnectionViewModel.kt` file with the following:
```kotlin
package com.riis.kotlin_mediamanagerdemo

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

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    val product: MutableLiveData<BaseProduct?> by lazy {
        MutableLiveData<BaseProduct?>()
    }

    val connectionStatus: MutableLiveData<Boolean> = MutableLiveData(false)

    fun registerApp() {
        DJISDKManager.getInstance().registerApp(getApplication(), object: DJISDKManager.SDKManagerCallback {
            override fun onRegister(error: DJIError?) {
                if (error == DJISDKError.REGISTRATION_SUCCESS) {
                    Log.i(ConnectionActivity.TAG, "onRegister: Registration Successful")
                } else {
                    Log.i(ConnectionActivity.TAG, "onRegister: Registration Failed - ${error?.description}")
                }
            }

            override fun onProductDisconnect() {
                Log.i(ConnectionActivity.TAG, "onProductDisconnect: Product Disconnected")
                connectionStatus.postValue(false)
            }

            override fun onProductConnect(baseProduct: BaseProduct?) {
                Log.i(ConnectionActivity.TAG, "onProductConnect: Product Connected")
                product.postValue(baseProduct)
                connectionStatus.postValue(true)
            }

            override fun onProductChanged(baseProduct: BaseProduct?) {
                Log.i(ConnectionActivity.TAG, "onProductChanged: Product Changed - $baseProduct")
                product.postValue(baseProduct)

            }

            override fun onComponentChange(componentKey: BaseProduct.ComponentKey?, oldComponent: BaseComponent?, newComponent: BaseComponent?) {
                Log.i(ConnectionActivity.TAG, "onComponentChange key: $componentKey, oldComponent: $oldComponent, newComponent: $newComponent")
                newComponent?.let { component ->
                    component.setComponentListener { connected ->
                        Log.i(ConnectionActivity.TAG, "onComponentConnectivityChange: $connected")
                    }
                }
            }

            override fun onInitProcess(p0: DJISDKInitEvent?, p1: Int) {}

            override fun onDatabaseDownloadProgress(p0: Long, p1: Long) {}

        })
    }

}
```

>Note: Permissions must be requested by the application and granted by the user in order to register the DJI SDK correctly. This is taken care of in **ConnectionActivity** before it calls on the ViewModel's registerApp() method. Furthermore, the camera and USB hardwares must be declared in the **AndroidManifest** for DJI SDK to work.


#### 5. Implementing the MainActivity Class

The MainActivity.kt file is created by Android Studio by default. Let's replace its code with the following:

```kotlin
package com.riis.kotlin_mediamanagerdemo

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dji.common.camera.SettingsDefinitions
import dji.common.error.DJICameraError
import dji.common.error.DJIError
import dji.common.util.CommonCallbacks.CompletionCallbackWithTwoParam
import dji.log.DJILog
import dji.sdk.base.BaseProduct
import dji.sdk.camera.Camera
import dji.sdk.media.*
import dji.sdk.media.MediaManager.*
import dji.sdk.products.Aircraft
import dji.sdk.products.HandHeld
import dji.sdk.sdkmanager.DJISDKManager
import java.io.File
import java.util.*

/*
The main purpose of this activity is to demonstrate how to access media resources on a DJI aircraft's SD card.
If media manager is available on your DJI drone, then you can use it to preview photos, play videos,
download or delete files and so on.
*/
class MainActivity : AppCompatActivity(), View.OnClickListener {


    //Class Variables
    companion object {
        private val TAG = "MAIN_ACTIVITY"
    }

    private lateinit var mBackBtn: Button
    private lateinit var mDeleteBtn: Button
    private lateinit var mReloadBtn: Button
    private lateinit var mDownloadBtn: Button
    private lateinit var mStatusBtn: Button
    private lateinit var mPlayBtn: Button
    private lateinit var mResumeBtn: Button
    private lateinit var mPauseBtn: Button
    private lateinit var mStopBtn: Button
    private lateinit var mMoveToBtn: Button
    private lateinit var listView: RecyclerView
    private var mLoadingDialog: ProgressDialog? = null
    private var mDownloadDialog: ProgressDialog? = null
    private var mPushDrawerSd: SlidingDrawer? = null
    private lateinit var mDisplayImageView: ImageView
    private lateinit var mPushTv: TextView

    /*
    Notes:
    *   If the DJI product already has videos or pictures saved to its SD card, these media files can
        be accessed and interacted with using the MediaManager class.

    *   The MediaManager has a enum class called FileListState which stores the state of its file list.

    *   The file list is a list of files the MediaManager obtains from the DJI product's SD card.

    *   In this app, we use a recycler view to display previews of each media file in the MediaManager's file list.
        To do this, we have created the mediaFileList variable to locally store the data from the MediaManager's
        file list. The recycler view uses the mediaFileList as its adapter's data set.
    */

    private lateinit var mListAdapter: FileListAdapter //recycler view adapter
    private var mediaFileList: MutableList<MediaFile> = mutableListOf() //empty list of MediaFile objects
    private var mMediaManager: MediaManager? = null //uninitialized media manager

    //variable for the current state of the MediaManager's file list
    private var currentFileListState = FileListState.UNKNOWN

    /*
    The scheduler object can be used to queue and download small content types of media
    (previews, thumbnails, and custom data) sequentially from a series of files. The scheduler can
    also re-prioritize files during the download process.
    */
    private var scheduler: FetchMediaTaskScheduler? = null

    private var currentProgress = -1 //integer variable for the current download progress
    private var lastClickViewIndex = 0
    private var lastClickView: View? = null

    /*
    Creating a photo and video file directory on the user's mobile phone which will store the media
    files that get downloaded from the DJI product's SD card.
    */
    private lateinit var photoStorageDir:File
    private lateinit var videoStorageDir:File

    //Creating the Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) //inflating the activity_main layout as the activity's view
        initUI() //initializing the UI

        /*
        getExternalFilesDir() refers to a private directory on the user's mobile device which can only be
        accessed by this app.Here we have created a pictures and videos folder within this private directory.
        */
        photoStorageDir = File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.path)
        videoStorageDir = File(this.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!.path)

    }

    //When the activity is resumed, initialize the MediaManager
    override fun onResume() {
        super.onResume()
        initMediaManager()
    }

    override fun onDestroy() {
        lastClickView = null
        mMediaManager?.let {mediaManager ->
            mediaManager.stop(null)
            mediaManager.removeFileListStateCallback(updateFileListStateListener)
            mediaManager.removeMediaUpdatedVideoPlaybackStateListener(
                updatedVideoPlaybackStateListener
            )
            mediaManager.exitMediaDownloading()
            if (scheduler != null) {
                scheduler!!.removeAllTasks()
            }
        }
        getCameraInstance()?.let {camera ->
            camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO) { mError ->
                if (mError != null) {
                    Log.i(TAG, "Set Shoot Photo Mode Failed" + mError.description)
                }
            }
            mediaFileList.clear()
            super.onDestroy()
        }
        mLoadingDialog?.dismiss()
        mDownloadDialog?.dismiss()
    }

    //Function used to initialize the activity's Layout views
    private fun initUI() {

        //referencing the recycler view by its resource id and giving it a LinearLayoutManager
        listView = findViewById(R.id.filelistView)
        val layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
        listView.layoutManager = layoutManager

        //Instantiating a FileListAdapter and setting it as the recycler view's adapter
        mListAdapter = FileListAdapter()
        listView.adapter = mListAdapter

        //Creating a ProgressDialog and configuring its behavioural settings as a loading screen
        mLoadingDialog = ProgressDialog(this@MainActivity)
        mLoadingDialog?.let { progressDialog ->
            progressDialog.setMessage("Please wait")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.setCancelable(false)   
        }

        //Creating a ProgressDialog and configuring its behavioural settings as a download progress screen
        mDownloadDialog = ProgressDialog(this@MainActivity)
        mDownloadDialog?.let { progressDialog ->
            progressDialog.setTitle("Downloading file")
            progressDialog.setIcon(android.R.drawable.ic_dialog_info)
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.setCancelable(true)

            //If the ProgressDialog is cancelled, the MediaManager will stop the downloading process
            progressDialog.setOnCancelListener {
                mMediaManager?.exitMediaDownloading()
            }   
        }

        //referencing other layout views by their resource ids
        mPushDrawerSd = findViewById(R.id.pointing_drawer_sd)
        mPushTv = findViewById(R.id.pointing_push_tv)
        mBackBtn = findViewById(R.id.back_btn)
        mDeleteBtn = findViewById(R.id.delete_btn)
        mDownloadBtn = findViewById(R.id.download_btn)
        mReloadBtn = findViewById(R.id.reload_btn)
        mStatusBtn = findViewById(R.id.status_btn)
        mPlayBtn = findViewById(R.id.play_btn)
        mResumeBtn = findViewById(R.id.resume_btn)
        mPauseBtn = findViewById(R.id.pause_btn)
        mStopBtn = findViewById(R.id.stop_btn)
        mMoveToBtn = findViewById(R.id.moveTo_btn)
        mDisplayImageView = findViewById(R.id.imageView)
        mDisplayImageView.visibility = View.VISIBLE
        mBackBtn.setOnClickListener(this)
        mDeleteBtn.setOnClickListener(this)
        mDownloadBtn.setOnClickListener(this)
        mReloadBtn.setOnClickListener(this)
        mDownloadBtn.setOnClickListener(this)
        mStatusBtn.setOnClickListener(this)
        mPlayBtn.setOnClickListener(this)
        mResumeBtn.setOnClickListener(this)
        mPauseBtn.setOnClickListener(this)
        mStopBtn.setOnClickListener(this)
        mMoveToBtn.setOnClickListener(this)
    }

    //Function used to display the loading ProgressDialog
    private fun showProgressDialog() {
        runOnUiThread { mLoadingDialog?.show() }
    }
    //Function used to dismiss the loading ProgressDialog
    private fun hideProgressDialog() {
        runOnUiThread {
            mLoadingDialog?.let { progressDialog ->
                if (progressDialog.isShowing) {
                    progressDialog.dismiss()
                }
            }
        }
    }

    //Function used to display the download ProgressDialog
    private fun showDownloadProgressDialog() {
        runOnUiThread {
            mDownloadDialog?.let { progressDialog ->
                //progressDialog.incrementProgressBy(progressDialog.progress)
                progressDialog.show()
            }
        }
    }
    //Function used to dismiss the download ProgressDialog
    private fun hideDownloadProgressDialog() {
        runOnUiThread {
            mDownloadDialog?.let { progressDialog ->
                if (progressDialog.isShowing) {
                    progressDialog.dismiss()
                }
            }
        }
    }

    //Function that turns strings into toast messages displayed to the user
    private fun setResultToToast(result: String) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
        }
    }

    //Function used to display a string on the mPushTv TextView
    private fun setResultToText(string: String) {
        runOnUiThread {
            mPushTv.text = string
        }
    }

    //Function used to initialize the MediaManager
    private fun initMediaManager() {
        //If there is no DJI product connected to the mobile device...
        if (getProductInstance() == null) {
            //clear the mediaFileList and notify the recycler view's adapter is that its dataset has changed
            mediaFileList.clear()
            mListAdapter.notifyDataSetChanged()

            DJILog.e(TAG, "Product disconnected")
            return

        //If there is a DJI product connected to the mobile device...
        } else {
            //get an instance of the DJI product's camera
            getCameraInstance()?.let { camera ->
                //If the camera supports downloading media from it...
                if (camera.isMediaDownloadModeSupported) {
                    mMediaManager = camera.mediaManager //get the camera's MediaManager
                    mMediaManager?.let { mediaManager ->

                        /*
                         NOTE:
                         To know when a change in the MediaManager's file list state occurs, the MediaManager needs a
                         FileListStateListener. We have created a FileListStateListener (further down in the code)
                         named updateFileListStateListener, and gave this listener to the MediaManager.

                         Similarly, the MediaManager also needs a VideoPlaybackStateListener to monitor changes to
                         its video playback state. We have created updatedVideoPlaybackStateListener
                         (further down in the code) for this reason, and gave it to the MediaManager.
                        */
                        mediaManager.addUpdateFileListStateListener(updateFileListStateListener)
                        mediaManager.addMediaUpdatedVideoPlaybackStateListener(
                                updatedVideoPlaybackStateListener
                        )
                        //Setting the camera mode to media download mode and then receiving an error callback
                        camera.setMode(
                                SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD
                        ) { error ->
                            //If the error is null, the operation was successful
                            if (error == null) {
                                DJILog.e(TAG, "Set cameraMode success")
                                showProgressDialog() //show the loading screen ProgressDialog
                                getFileList() //update the mediaFileList using the DJI product' SD card
                            //If the error is not null, alert user
                            } else {
                                setResultToToast("Set cameraMode failed")
                            }
                        }
                        //If the MediaManager doesn't support video playback, let the user know
                        if (mediaManager.isVideoPlaybackSupported) {
                            DJILog.e(TAG, "Camera support video playback!")
                        } else {
                            setResultToToast("Camera does not support video playback!")
                        }
                        //Setting the scheduler to be the MediaManager's scheduler
                        scheduler = mediaManager.scheduler
                    }
                } else {
                    //If the camera doesn't support downloading media from it, alert the user
                    setResultToToast("Media Download Mode not Supported")
                }
            }
        }
        return
    }

    /*
    NOTE:
    * refreshFileListOfStorageLocation() is used to update the MediaManager's file list using the
      DJI product's SD card.

    * If the file list state is RESET, the MediaManager will fetch the complete list
      list of files from the SD card. If the file list state is INCOMPLETE, the MediaManager will only fetch
      the missing list of files from the SD card.

    * This function requires the storage location of the media files to be specified. In this case, the SD card.

    * Once the MediaManager's file list is refreshed, use it to update the recycler view's mediaFileList.
    */

    private fun getFileList() {
        getCameraInstance()?.let { camera -> //Get an instance of the connected DJI product's camera
            mMediaManager = camera.mediaManager //Get the camera's MediaManager
            mMediaManager?.let { mediaManager ->
                //If the MediaManager's file list state is syncing or deleting, the MediaManager is busy
                if (currentFileListState == FileListState.SYNCING || currentFileListState == FileListState.DELETING) {
                    DJILog.e(TAG, "Media Manager is busy.")
                } else {
                    setResultToToast(currentFileListState.toString()) //for debugging

                    //refreshing the MediaManager's file list using the connected DJI product's SD card
                    mediaManager.refreshFileListOfStorageLocation(
                            SettingsDefinitions.StorageLocation.SDCARD //file storage location
                    ) { djiError -> //checking the callback error

                        //If the error is null, dismiss the loading screen ProgressDialog
                        if (null == djiError) {
                            hideProgressDialog()

                            //Reset data if the file list state is not incomplete
                            if (currentFileListState != FileListState.INCOMPLETE) {
                                mediaFileList.clear()
                                lastClickViewIndex = -1
                                lastClickView = null
                            }
                            //updating the recycler view's mediaFileList using the now refreshed MediaManager's file list
                            mediaManager.sdCardFileListSnapshot?.let { listOfMedia ->
                                mediaFileList = listOfMedia
                            }

                            /*
                            Sort the files in the mediaFileList by descending order based on the time each media file was created.
                            Older files are now at the top of the mediaFileList, and newer ones are at the bottom. This results in
                            recent files showing up first in the recycler view.
                            */
                            mediaFileList.sortByDescending { it.timeCreated }

                            //Resume the scheduler. This will allow it to start executing any tasks in its download queue.
                            scheduler?.let { schedulerSafe ->
                                schedulerSafe.resume { error ->
                                    //if the callback error is null, the operation was successful.
                                    if (error == null) {
                                        getThumbnails() //
                                    }
                                }
                            }
                        /*
                        If there was an error with refreshing the MediaManager's file list, dismiss the loading progressDialog
                        and alert the user.
                        */
                        } else {
                            hideProgressDialog()
                            setResultToToast("Get Media File List Failed:" + djiError.description)
                        }
                    }
                }
            }

        }
    }

    /*
    NOTE:
    Because full resolution photos/videos take too long to load, we want the recycler view to only display
    thumbnails of each media file in the mediaFileList.
    */

    //Function used to get the thumbnail images of all the media files in the mediaFileList
    private fun getThumbnails() {
        //if the mediaFileList is empty, alert the user
        if (mediaFileList.size <= 0) {
            setResultToToast("No File info for downloading thumbnails")
            return
        }
        //if the mediaFileList is not empty, call getThumbnailByIndex() on each media file
        for (i in mediaFileList.indices) {
            getThumbnailByIndex(i)
        }
    }

    //creating a Callback which is called whenever media content is downloaded using FetchMediaTask()
    private val taskCallback =
        FetchMediaTask.Callback { _, option, error ->
            //if the callback error is null, the download operation was successful
            if (null == error) {
                //if a preview image or thumbnail was downloaded, notify the recycler view that its data set has changed.
                if (option == FetchMediaTaskContent.PREVIEW) {
                    runOnUiThread {mListAdapter.notifyDataSetChanged()}
                }
                if (option == FetchMediaTaskContent.THUMBNAIL) {
                    runOnUiThread {mListAdapter.notifyDataSetChanged()}
                }
            } else {
                DJILog.e(TAG, "Fetch Media Task Failed" + error.description)
            }
        }

    private fun getThumbnailByIndex(index: Int) {
        /*
        Creating a task to fetch the thumbnail of a media file in the mediaFileList.
        This function also calls taskCallback to check for and respond to errors.
        */
        val task =
            FetchMediaTask(mediaFileList[index], FetchMediaTaskContent.THUMBNAIL, taskCallback)

        /*
        Using the scheduler to move each task to the back of its download queue.
        The task will be executed after all other tasks are completed.
        */
        scheduler?.let {
            it.moveTaskToEnd(task)
        }
    }

    //Creating a ViewHolder to store the item views displayed in the RecyclerView
    private class ItemHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        //referencing child views from the item view's layout using their resource ids
        var thumbnail_img: ImageView = itemView.findViewById(R.id.filethumbnail) as ImageView
        var file_name: TextView = itemView.findViewById(R.id.filename) as TextView
        var file_type: TextView = itemView.findViewById(R.id.filetype) as TextView
        var file_size: TextView = itemView.findViewById(R.id.fileSize) as TextView
        var file_time: TextView = itemView.findViewById(R.id.filetime) as TextView

    }

    //Creating an adapter for the RecyclerView
    private inner class FileListAdapter : RecyclerView.Adapter<ItemHolder>() {

        //returns the number of items in the adapter's data set list
        override fun getItemCount(): Int {
            return mediaFileList.size
        }
        //inflates an item view and creates a ViewHolder to wrap it
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
            val view = LayoutInflater.from(parent.context)
                //item view layout defined in media_info_item.xml
                .inflate(R.layout.media_info_item, parent, false)
            return ItemHolder(view)
        }

        /*
        Binds a ViewHolder in the recyclerView to a MediaFile object in the mediaFileList.
        The UI of the ViewHolder is changed to display the MediaFile's data.
        */
        override fun onBindViewHolder(mItemHolder: ItemHolder, index: Int) {
            val mediaFile: MediaFile = mediaFileList[mItemHolder.adapterPosition]

            //If the media file is not a movie or mp4 file, then hide the itemView's file_time TextView
            if (mediaFile.mediaType != MediaFile.MediaType.MOV && mediaFile.mediaType != MediaFile.MediaType.MP4) {
                mItemHolder.file_time.visibility = View.GONE

            /*
            If the media file is a movie or mp4 file, show the video's duration time in seconds
            on the itemView's file_time TextView.
            */
            } else {
                mItemHolder.file_time.visibility = View.VISIBLE
                mItemHolder.file_time.text = mediaFile.durationInSeconds.toString() + " s"
            }
            //display the media file's name, type, size, and thumbnail in the itemView
            mItemHolder.file_name.text = mediaFile.fileName
            mItemHolder.file_type.text = mediaFile.mediaType.name
            mItemHolder.file_size.text = mediaFile.fileSize.toString() + " Bytes"
            mItemHolder.thumbnail_img.setImageBitmap(mediaFile.thumbnail)

            //making the thumbnail_img ImageView clickable
            mItemHolder.thumbnail_img.setOnClickListener(ImgOnClickListener)

            //setting the MediaFile object as the thumbnail_img ImageView's tag
            mItemHolder.thumbnail_img.tag = mediaFile

            //setting the current mediaFileList index as the itemView's tag
            mItemHolder.itemView.tag = mItemHolder.adapterPosition

            //if the itemView is clicked on...
            mItemHolder.itemView.setOnClickListener {
                lastClickViewIndex = mItemHolder.adapterPosition //save the current mediaFileList index

                //if lastClickView is not null and it is not the currently selected itemView...
                if (lastClickView != null && lastClickView != it) {
                    lastClickView!!.isSelected = false //set the last-clicked itemView's isSelected attribute to false
                }
                //set the currently selected itemView's isSelected attribute to true
                mItemHolder.itemView.isSelected = true
                lastClickView = it //set lastClickView to the currently selected itemView
                Log.i(TAG, "ClickListenerIndex: $lastClickViewIndex")
            }
        }
    }

    //if the thumbnail_img ImageView is clicked on...
    private val ImgOnClickListener =
        View.OnClickListener { v ->
            val selectedMedia = v.tag as MediaFile
            //if the MediaManager is not null, call addMediaTask() on the ImageView's MediaFile
            if (mMediaManager != null) {
                addMediaTask(selectedMedia)
            }
        }

    /*
    Function used to download the preview image of a provided MediaFile, and
    then display the preview on the mDisplayImageView ImageView.
    */
    private fun addMediaTask(mediaFile: MediaFile) {
        mMediaManager?.let {
            //creating a task to fetch the preview of the provided MediaFile
            val task = FetchMediaTask(
                    mediaFile,
                    FetchMediaTaskContent.PREVIEW
            ) { mediaFile, _, error ->
                //if the callback error is null, the download was successful
                if (null == error) {
                    //if the downloaded preview image is not null, make the mDisplayImageView
                    //... visible and use it to display the preview.
                    if (mediaFile.preview != null) {
                        runOnUiThread {
                            val previewBitmap = mediaFile.preview
                            mDisplayImageView.visibility = View.VISIBLE
                            mDisplayImageView.setImageBitmap(previewBitmap)
                        }
                    } else {
                        setResultToToast("null bitmap!")
                    }
                } else {
                    setResultToToast("fetch preview image failed: " + error.description)
                }
            }
            //resume the scheduler
            it.scheduler.resume { error ->
                /*
                If the callback error is null, push the task to the front of the scheduler's download queue.
                The task will be executed after any currently executing task is complete.
                */
                if (error == null) {
                    it.scheduler.moveTaskToNext(task)
                } else {
                    setResultToToast("resume scheduler failed: " + error.description)
                }
            }
        }
    }

    //Listeners
    private val updateFileListStateListener =
        //when the MediaManager's FileListState changes, save the state to currentFileListState
        FileListStateListener { state -> currentFileListState = state }

    private val updatedVideoPlaybackStateListener =
        //when the MediaManager's videoPlaybackState changes, pass the state into updateStatusTextView()
        VideoPlaybackStateListener { videoPlaybackState -> updateStatusTextView(videoPlaybackState) }

    //Function used to update the status text view (mPushTv)
    private fun updateStatusTextView(videoPlaybackState: VideoPlaybackState?) {
        val pushInfo = StringBuffer()
        addLineToSB(pushInfo, "Video Playback State", null)

        //if the video playback state is not null...
        if (videoPlaybackState != null) {
            if (videoPlaybackState.playingMediaFile != null) { //if there is a video media file playing...
                addLineToSB( //add the media file's index to the StringBuffer
                    pushInfo,
                    "media index",
                    videoPlaybackState.playingMediaFile.index
                )
                addLineToSB( //add the media file's size to the StringBuffer
                    pushInfo,
                    "media size",
                    videoPlaybackState.playingMediaFile.fileSize
                )
                addLineToSB( //add the media file's duration to the StringBuffer
                    pushInfo,
                    "media duration",
                    videoPlaybackState.playingMediaFile.durationInSeconds
                )
                addLineToSB( //add the media file's creation date to the StringBuffer
                    pushInfo,
                    "media created date",
                    videoPlaybackState.playingMediaFile.dateCreated
                )
                addLineToSB( //add the media file's orientation to the StringBuffer
                    pushInfo,
                    "media orientation",
                    videoPlaybackState.playingMediaFile.videoOrientation
                )
            } else { //if there is no video media file playing...
                addLineToSB(pushInfo, "media index", "None")
            }
            /*
            Add the media file's playingPosition, playbackStatus, cachedPercentage, cachedPosition,
            as different lines to the StringBuffer.
            */
            addLineToSB(pushInfo, "media current position", videoPlaybackState.playingPosition)
            addLineToSB(pushInfo, "media current status", videoPlaybackState.playbackStatus)
            addLineToSB(
                pushInfo,
                "media cached percentage",
                videoPlaybackState.cachedPercentage
            )
            addLineToSB(pushInfo, "media cached position", videoPlaybackState.cachedPosition)
            pushInfo.append("\n") //new line

            //display the StringBuffer's string on the mPushTv TextView
            setResultToText(pushInfo.toString())
        }
    }

    //Function used to add a new line to a StringBuffer
    private fun addLineToSB(
        sb: StringBuffer?,
        name: String?,
        value: Any?
    ) {
        if (sb == null) return
        sb.append(if (name == null || "" == name) "" else "$name: ")
            .append(if (value == null) "" else value.toString() + "").append("\n")
    }

    private val downloadFileListener = object: DownloadListener<String>{
        //if the download fails, dismiss the download progressDialog, alert the user,
        //...and reset currentProgress.
        override fun onFailure(error: DJIError) {
            hideDownloadProgressDialog()
            setResultToToast("Download File Failed" + error.description)
            currentProgress = -1
        }

        override fun onProgress(total: Long, current: Long) {}

        //called every 1 second to show the download rate
        override fun onRateUpdate(
            total: Long, //the total size
            current: Long, //the current download size
            persize: Long //the download size between two calls
        ) {
            //getting the current download progress as an integer between 1-100
            val tmpProgress = (1.0 * current / total * 100).toInt()

            if (tmpProgress != currentProgress) {
                mDownloadDialog?.let {
                    it.progress = tmpProgress //set tmpProgress as the progress of the download progressDialog
                    currentProgress = tmpProgress //save tmpProgress to currentProgress
                }
            }
        }

        //When the download starts, reset currentProgress and show the download ProgressDialog
        override fun onStart() {
            currentProgress = -1
            showDownloadProgressDialog()
        }
        //When the download successfully finishes, dismiss the download ProgressDialog, alert the user,
        //...and reset currentProgress.
        override fun onSuccess(filePath: String) {
            hideDownloadProgressDialog()
            setResultToToast("Download File Success:$filePath")
            currentProgress = -1
        }

        override fun onRealtimeDataUpdate(p0: ByteArray?, p1: Long, p2: Boolean) {
        }
    }

    //Function used to download full resolution photos/videos from the DJI product's SD card
    private fun downloadFileByIndex(index: Int) {
        val camera: Camera = getCameraInstance() ?: return

        camera.setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD) { error ->
            if (error == null) {
                Log.d(TAG, "Set Camera to Download Mode Succeeded")
            } else {
                Log.d(TAG,"Set Camera to Download Mode Succeeded Failed: ${error.description}")
            }
        }
        //If the media file's type is panorama or shallow_focus, don't download it
        if (mediaFileList[index].mediaType == MediaFile.MediaType.PANORAMA
            || mediaFileList[index].mediaType == MediaFile.MediaType.SHALLOW_FOCUS
        ) {
            return
        }
        //If the media file's type is MOV or MP4, download it to videoStorageDir
        if (mediaFileList[index].mediaType == MediaFile.MediaType.MOV
                || mediaFileList[index].mediaType == MediaFile.MediaType.MP4) {
            mediaFileList[index].fetchFileData(videoStorageDir, null, downloadFileListener)
        }
        //If the media file's type is JPEG or JSON, download it to photoStorageDir
        if (mediaFileList[index].mediaType == MediaFile.MediaType.JPEG
            || mediaFileList[index].mediaType == MediaFile.MediaType.JSON) {
            mediaFileList[index].fetchFileData(photoStorageDir, null, downloadFileListener)
        }
    }
    //Function used to delete a media file from the DJI product's SD card
    private fun deleteFileByIndex(index: Int) {
        val fileToDelete = ArrayList<MediaFile>()
        //if the size of mediaFileList is larger than the provided index...
        if (mediaFileList.size > index) {
            //delete the media file from the SD card
            fileToDelete.add(mediaFileList[index])
            mMediaManager?.let {  mediaManager ->
                mediaManager.deleteFiles(
                        fileToDelete,
                        object :
                                CompletionCallbackWithTwoParam<List<MediaFile?>?, DJICameraError?> {
                            //if the deletion from the SD card is successful...
                            override fun onSuccess(
                                    x: List<MediaFile?>?,
                                    y: DJICameraError?
                            ) {
                                DJILog.e(TAG, "Delete file success")
                                //remove the deleted file from the mediaFileList
                                runOnUiThread {
                                    mediaFileList.removeAt(index)

                                    //Reset select view
                                    lastClickViewIndex = -1
                                    lastClickView = null

                                    //Update recyclerView
                                    mListAdapter.notifyDataSetChanged()
                                }
                            }
                            //if the deletion from the SD card failed, alert the user
                            override fun onFailure(error: DJIError) {
                                setResultToToast("Delete file failed")
                            }
                        })
            }
        }
    }
    //Function used to play the last-clicked media file in the recyclerView if it is a type of video
    private fun playVideo() {
        mDisplayImageView.visibility = View.INVISIBLE
        if (lastClickViewIndex == -1) return

        val selectedMediaFile = mediaFileList[lastClickViewIndex]

        //if the selected media file is a video, play it using the MediaManager
        if (selectedMediaFile.mediaType == MediaFile.MediaType.MOV || selectedMediaFile.mediaType == MediaFile.MediaType.MP4) {
            mMediaManager?.let { mediaManager ->
                mediaManager.playVideoMediaFile(
                    selectedMediaFile
                ) { error ->
                    //if the callback error is null, the video played successfully
                    if (null != error) {
                        setResultToToast("Play Video Failed " + error.description)
                    } else { //alert the user of the error
                        DJILog.e(TAG, "Play Video Success")
                    }
                }
            }
        }
    }

    private fun moveToPosition() {
        val li = LayoutInflater.from(this)
        val promptsView = li.inflate(R.layout.prompt_input_position, null)
        val alertDialogBuilder =
            AlertDialog.Builder(this)
        alertDialogBuilder.setView(promptsView)
        val userInput = promptsView.findViewById<EditText>(R.id.editTextDialogUserInput)
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton("OK") { dialog, id ->
                val ms = userInput.text.toString()
                mMediaManager?.let { mediaManager ->
                    if(ms != ""){
                        mediaManager.moveToPosition(ms.toInt().toFloat()) { error ->
                            if (error != null) {
                                setResultToToast("Move to video position failed" + error.description)
                            } else {
                                DJILog.e(TAG, "Move to video position successfully.")
                            }
                        }
                    }

                }
            }
            .setNegativeButton(
                "Cancel"
            ) { dialog, id -> dialog.cancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.back_btn -> {
                finish()
            }
            R.id.delete_btn -> {
                deleteFileByIndex(lastClickViewIndex)
            }
            R.id.reload_btn -> {
                getFileList()
            }
            R.id.download_btn -> {
                Log.d(TAG, "$lastClickViewIndex")
                downloadFileByIndex(lastClickViewIndex)
            }
            R.id.status_btn -> {
                if (mPushDrawerSd!!.isOpened) {
                    mPushDrawerSd!!.animateClose()
                } else {
                    mPushDrawerSd!!.animateOpen()
                }
            }
            R.id.play_btn -> {
                playVideo()
            }
            R.id.resume_btn -> {
                mMediaManager?.let { mediaManager ->
                    mediaManager.resume { error ->
                        if (null != error) {
                            setResultToToast("Resume Video Failed" + error.description)
                        } else {
                            DJILog.e(TAG, "Resume Video Success")
                        }
                    }
                }
            }
            R.id.pause_btn -> {
                mMediaManager?.let { mediaManager ->
                    mediaManager.pause { error ->
                        if (null != error) {
                            setResultToToast("Pause Video Failed" + error.description)
                        } else {
                            DJILog.e(TAG, "Pause Video Success")
                        }
                    }
                }
            }
            R.id.stop_btn -> {
                mMediaManager?.let { mediaManager ->
                    mediaManager.stop { error ->
                        if (null != error) {
                            setResultToToast("Stop Video Failed" + error.description)
                        } else {
                            DJILog.e(TAG, "Stop Video Success")
                        }
                    }
                }
            }
            R.id.moveTo_btn -> {
                moveToPosition()
            }
            else -> {
            }
        }
    }

    private fun getProductInstance(): BaseProduct? {
        return DJISDKManager.getInstance().product
    }
    
    private fun getCameraInstance(): Camera? {
        if (getProductInstance() == null) return null
        
        if (getProductInstance() is Aircraft) {
            return (getProductInstance() as Aircraft).camera
        } else if (getProductInstance() is HandHeld) {
            return (getProductInstance() as HandHeld).camera
        } else
            return null
    }
    
    private fun isAircraftConnected(): Boolean {
        return getProductInstance() != null && getProductInstance() is Aircraft
    }
    
    private fun isProductModuleAvailable(): Boolean {
        return (getProductInstance() != null)
    }
    
    private fun isCameraModuleAvailable(): Boolean {
        return isProductModuleAvailable() && (getProductInstance()?.camera != null)
    }
    
    private fun isPlaybackAvailable(): Boolean {
        return isCameraModuleAvailable() && (getProductInstance()?.camera?.playbackManager != null)
    }
}
```
#### 6. Default Layout Activity
In the project navigator, go to **app -> java -> com -> riis -> kotlin_mediamanagerdemo**, and right-click on the kotlin_mediamanagerdemo directory. Select **New -> Kotlin Class/File** to create a new kotlin class and name it as `DefaultLayoutActivity.kt`. 

Next, replace the code of the `DefaultLayoutActivity.kt` file with the following:
```kotlin
package com.riis.kotlin_mediamanagerdemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.riis.kotlin_mediamanagerdemo.R

class DefaultLayoutActivity : AppCompatActivity() {

    private lateinit var mediaManagerBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_layout)

        mediaManagerBtn = findViewById(R.id.btn_mediaManager)
        mediaManagerBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
```

#### 7.  Implementing the MainActivity Layout

Open the `activity_main.xml` layout file and replace the code with the following:

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <Button
        android:id="@+id/back_btn"
        android:layout_width="wrap_content"
        android:layout_height="40dp"
        android:layout_alignParentStart="true"
        android:layout_alignParentTop="true"
        android:text="Back"
        android:textSize="11sp" />

    <Button
        android:id="@+id/delete_btn"
        android:layout_width="wrap_content"
        android:layout_height="40dp"
        android:layout_alignParentTop="true"
        android:layout_toEndOf="@+id/back_btn"
        android:text="Delete"
        android:textSize="11sp" />

    <Button
        android:id="@+id/reload_btn"
        android:layout_width="wrap_content"
        android:layout_height="40dp"
        android:layout_alignParentTop="true"
        android:layout_toEndOf="@+id/delete_btn"
        android:text="Reload"
        android:textSize="11sp" />

    <Button
        android:id="@+id/download_btn"
        android:layout_width="wrap_content"
        android:layout_height="40dp"
        android:layout_alignParentTop="true"
        android:layout_toEndOf="@+id/reload_btn"
        android:text="Download"
        android:textSize="11sp" />

    <Button
        android:id="@+id/status_btn"
        android:layout_width="wrap_content"
        android:layout_height="40dp"
        android:layout_alignParentTop="true"
        android:layout_toEndOf="@+id/download_btn"
        android:text="Status"
        android:textSize="11sp" />

    <Button
        android:id="@+id/play_btn"
        android:layout_width="wrap_content"
        android:layout_height="40dp"
        android:layout_alignParentStart="true"
        android:layout_below="@+id/back_btn"
        android:text="Play"
        android:textSize="11sp" />

    <Button
        android:id="@+id/resume_btn"
        android:layout_width="wrap_content"
        android:layout_height="40dp"
        android:layout_below="@+id/back_btn"
        android:layout_toEndOf="@+id/play_btn"
        android:text="Resume"
        android:textSize="11sp" />

    <Button
        android:id="@+id/pause_btn"
        android:layout_width="wrap_content"
        android:layout_height="40dp"
        android:layout_below="@+id/reload_btn"
        android:layout_toEndOf="@+id/resume_btn"
        android:text="Pause"
        android:textSize="11sp" />

    <Button
        android:id="@+id/stop_btn"
        android:layout_width="wrap_content"
        android:layout_height="40dp"
        android:layout_below="@+id/reload_btn"
        android:layout_toEndOf="@+id/pause_btn"
        android:text="Stop"
        android:textSize="11sp" />

    <Button
        android:id="@+id/moveTo_btn"
        android:layout_width="wrap_content"
        android:layout_height="40dp"
        android:layout_below="@+id/reload_btn"
        android:layout_toEndOf="@+id/stop_btn"
        android:text="MoveTo"
        android:textSize="11sp" />

    <!-- Widget to see first person view (FPV) -->
    <dji.ux.widget.FPVWidget
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_below="@+id/play_btn"
        android:layout_toStartOf="@+id/pointing_drawer_sd"
        android:id="@+id/FPVWidget" />

    <ImageView
        android:id="@+id/imageView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_below="@+id/play_btn"
        android:layout_marginTop="0dp"
        android:layout_toStartOf="@+id/pointing_drawer_sd"
        android:background="@color/black_overlay"
        android:visibility="invisible" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/filelistView"
        android:layout_width="250dp"
        android:layout_height="match_parent"
        android:layout_marginLeft="0dp"
        android:layout_toEndOf="@+id/FPVWidget" />

    <SlidingDrawer
        android:id="@+id/pointing_drawer_sd"
        android:layout_width="230dp"
        android:layout_height="match_parent"
        android:layout_alignParentRight="true"
        android:content="@+id/pointing_drawer_content"
        android:handle="@+id/pointing_handle"
        android:orientation="horizontal">

        <ImageView
            android:id="@id/pointing_handle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content" />

        <RelativeLayout
            android:id="@+id/pointing_drawer_content"
            android:layout_width="250dp"
            android:layout_height="match_parent"
            android:layout_centerVertical="true"
            android:layout_marginLeft="340dp"
            android:background="@color/black_overlay">

            <ScrollView
                android:layout_width="250dp"
                android:layout_height="fill_parent"
                android:layout_alignParentEnd="true"
                android:layout_alignParentRight="true"
                android:layout_marginEnd="-5dp"
                android:layout_marginRight="-5dp"
                android:clickable="false"
                android:scrollbars="vertical">

                <TextView
                    android:id="@+id/pointing_push_tv"
                    style="@style/status_text"
                    android:layout_width="200dp"
                    android:layout_height="wrap_content"
                    android:layout_marginLeft="30dp"
                    android:scrollbars="vertical"
                    android:text="@string/push_info" />
            </ScrollView>
        </RelativeLayout>

    </SlidingDrawer>

</RelativeLayout>
```
#### 8. Creating the activity_default_layout.xml layout

In the project file navigator, go to **app -> res -> layout** and right-click on the layout directory. Select **New -> Layout Resource File** to create a xml file and name it as `activity_default_layout.xml`. Copy the following code into it:
```xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    xmlns:custom="http://schemas.android.com/apk/res-auto"
    android:id="@+id/activity_main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background_blue"
    android:orientation="horizontal"
    tools:context=".MainActivity">

    <!-- Widget to see first person view (FPV) -->
    <dji.ux.widget.FPVWidget
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

    <dji.ux.widget.FPVOverlayWidget
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

    <!-- Widgets in top status bar -->
    <LinearLayout
        android:id="@+id/signal"
        android:layout_width="match_parent"
        android:layout_height="25dp"
        android:background="@color/dark_gray"
        android:orientation="horizontal">

        <dji.ux.widget.PreFlightStatusWidget
            android:id="@+id/status"
            android:layout_width="238dp"
            android:layout_height="25dp"/>

        <dji.ux.widget.FlightModeWidget
            android:layout_width="103dp"
            android:layout_height="22dp"/>

        <dji.ux.widget.GPSSignalWidget
            android:layout_width="44dp"
            android:layout_height="22dp"/>

        <dji.ux.widget.VisionWidget
            android:layout_width="22dp"
            android:layout_height="22dp"/>

        <dji.ux.widget.RemoteControlSignalWidget
            android:layout_width="38dp"
            android:layout_height="22dp"/>

        <dji.ux.widget.VideoSignalWidget
            android:layout_width="38dp"
            android:layout_height="22dp"/>

        <dji.ux.widget.WiFiSignalWidget
            android:layout_width="32dp"
            android:layout_height="25dp"/>

        <dji.ux.widget.BatteryWidget
            android:layout_width="96dp"
            android:layout_height="22dp"
            custom:excludeView="singleVoltage"/>

        <dji.ux.widget.ConnectionWidget
            android:layout_marginTop="3dp"
            android:layout_width="18dp"
            android:layout_height="18dp"/>
    </LinearLayout>

    <LinearLayout
        android:id="@+id/camera"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_below="@id/signal"
        android:layout_centerHorizontal="true"
        android:layout_margin="12dp"
        android:background="@color/dark_gray"
        android:orientation="horizontal">

        <dji.ux.widget.AutoExposureLockWidget
            android:layout_width="25dp"
            android:layout_height="25dp"/>

        <dji.ux.widget.FocusExposureSwitchWidget
            android:layout_width="25dp"
            android:layout_height="25dp"/>

        <dji.ux.widget.FocusModeWidget
            android:layout_width="25dp"
            android:layout_height="25dp"/>

        <dji.ux.widget.config.CameraConfigISOAndEIWidget
            android:layout_width="50dp"
            android:layout_height="25dp"/>

        <dji.ux.widget.config.CameraConfigShutterWidget
            android:layout_width="50dp"
            android:layout_height="25dp"/>

        <dji.ux.widget.config.CameraConfigApertureWidget
            android:layout_width="50dp"
            android:layout_height="25dp"/>

        <dji.ux.widget.config.CameraConfigEVWidget
            android:layout_width="50dp"
            android:layout_height="25dp"/>

        <dji.ux.widget.config.CameraConfigWBWidget
            android:layout_width="50dp"
            android:layout_height="25dp"/>

        <dji.ux.widget.config.CameraConfigStorageWidget
            android:layout_width="108dp"
            android:layout_height="25dp"/>

    </LinearLayout>
    <dji.ux.widget.RemainingFlightTimeWidget
        android:layout_alignParentTop="true"
        android:layout_marginTop="18dp"
        android:layout_width="match_parent"
        android:background="@color/transparent"
        android:layout_height="20dp"/>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        android:orientation="horizontal"
        android:padding="12dp">

        <dji.ux.widget.dashboard.DashboardWidget
            android:id="@+id/Compass"
            android:layout_width="405dp"
            android:layout_height="91dp"
            android:layout_marginRight="12dp"/>

    </LinearLayout>

    <!--Take off and return home buttons on left -->
    <LinearLayout
        android:layout_width="40dp"
        android:layout_height="wrap_content"
        android:layout_centerVertical="true"
        android:layout_marginStart="12dp"
        android:orientation="vertical">

        <dji.ux.widget.TakeOffWidget
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:layout_marginBottom="12dp"/>

        <dji.ux.widget.ReturnHomeWidget
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:layout_marginTop="12dp"/>
    </LinearLayout>


    <dji.ux.widget.controls.CameraControlsWidget
        android:id="@+id/CameraCapturePanel"
        android:layout_width="50dp"
        android:layout_height="213dp"
        android:layout_alignParentRight="true"
        android:layout_below="@id/camera" />


    <dji.ux.panel.CameraSettingExposurePanel
        android:id="@+id/CameraExposureMode"
        android:layout_width="180dp"
        android:layout_height="263dp"
        android:layout_below="@id/camera"
        android:layout_marginLeft="360dp"
        android:layout_toLeftOf="@+id/CameraCapturePanel"
        android:background="@color/transparent"
        android:gravity="center"
        android:visibility="invisible" />

    <dji.ux.panel.CameraSettingAdvancedPanel
        android:id="@+id/CameraAdvancedSetting"
        android:layout_width="180dp"
        android:layout_height="263dp"
        android:layout_below="@id/camera"
        android:layout_marginLeft="360dp"
        android:layout_toLeftOf="@+id/CameraCapturePanel"
        android:background="@color/transparent"
        android:gravity="center"
        android:visibility="invisible" />

    <Button
        android:id="@+id/btn_mediaManager"
        android:layout_width="38dp"
        android:layout_height="30dp"
        android:layout_alignStart="@+id/CameraCapturePanel"
        android:layout_below="@id/CameraCapturePanel"
        android:background="@drawable/playback"
        android:visibility="visible" />

    <!-- Pre-flight checklist panel -->
    <dji.ux.panel.PreFlightCheckListPanel
        android:id="@+id/PreflightCheckView"
        android:layout_width="400dp"
        android:layout_height="wrap_content"
        android:layout_below="@id/signal"
        android:visibility="gone"/>
</RelativeLayout>
```

#### 9. Creating the media_info_item.xml layout

In the project file navigator, go to **app -> res -> layout** and right-click on the layout directory. Select **New -> Layout Resource File** to create a xml file and name it as `media_info_item.xml`. Copy the following code into it:

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/parent"
    android:layout_width="250dp"
    android:layout_height="wrap_content"
    android:background="@drawable/background_selector"
    android:orientation="horizontal">

    <ImageView
        android:id="@+id/filethumbnail"
        android:layout_width="110dp"
        android:layout_height="80dp"
        android:layout_alignParentStart="true"
        android:layout_alignParentTop="true"
        android:minHeight="150dp"
        android:minWidth="250dp"
        android:scaleType="fitXY" />

    <TextView
        android:id="@+id/filename"
        android:layout_width="140dp"
        android:layout_height="20dp"
        android:layout_alignParentTop="true"
        android:layout_toEndOf="@+id/filethumbnail"
        android:clickable="false"
        android:text="FileName" />

    <TextView
        android:id="@+id/filetype"
        android:layout_width="140dp"
        android:layout_height="20dp"
        android:layout_below="@+id/filename"
        android:layout_toEndOf="@+id/filethumbnail"
        android:clickable="false"
        android:text="FileType" />

    <TextView
        android:id="@+id/fileSize"
        android:layout_width="140dp"
        android:layout_height="20dp"
        android:layout_below="@+id/filetype"
        android:layout_toEndOf="@+id/filethumbnail"
        android:clickable="false"
        android:text="FileSize" />

    <TextView
        android:id="@+id/filetime"
        android:layout_width="140dp"
        android:layout_height="20dp"
        android:layout_below="@+id/fileSize"
        android:layout_toEndOf="@+id/filethumbnail"
        android:clickable="false"
        android:text="FileTime" />


</RelativeLayout>
```
#### 10. Creating the prompt_input_position.xml layout

In the project file navigator, go to **app -> res -> layout** and right-click on the layout directory. Select **New -> Layout Resource File** to create a xml file and name it as `prompt_input_position.xml`. Copy the following code into it:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/layout_prompt"
    android:layout_width="fill_parent"
    android:layout_height="fill_parent"
    android:orientation="vertical"
    android:padding="10dp" >

    <TextView
        android:id="@+id/textView1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Input Position : "
        android:textAppearance="?android:attr/textAppearanceLarge" />

    <EditText
        android:id="@+id/editTextDialogUserInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="number">

        <requestFocus />

    </EditText>

</LinearLayout>
```
#### 11. Configuring the Resource XMLs

Once you finish the above steps, let's configure some xml files that our app is going to use.

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
    <string name="app_name">Kotlin-MediaManagerDemo</string>
    <string name="action_settings">Settings</string>
    <string name="disconnected">Disconnected</string>
    <string name="product_information">Product Information</string>
    <string name="connection_loose">Status: No Product Connected</string>
    <string name="model_not_available">Model Not Available</string>
    <string name="push_info">Push Info</string>
    <string name="sdk_version">DJI SDK Version: %1$s</string>
</resources>
```
Lastly, right click on `/app/res/values` folder and create new `Values Resource File`. Name it **styles.xml** and press **OK**. Place the the following inside the:
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

#### 12. Adding Drawable Resources
1. Create a new `Drawable Resource File` under `/app/res/drawable` and name it `round_btn.xml`. Place the following code inside.
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@drawable/round_btn_normal" android:state_focused="true"></item>
    <item android:drawable="@drawable/round_btn_pressed" android:state_selected="true"></item>
    <item android:drawable="@drawable/round_btn_pressed" android:state_pressed="true"></item>
    <item android:drawable="@drawable/round_btn_disable" android:state_enabled="false"></item>
    <item android:drawable="@drawable/round_btn_normal" ></item>
</selector>
```
2. Create another `Drawable Resource File` under `/app/res/drawable` and name it `round_btn_disable.xml`. Place the following code inside
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#AAAAAAAA" />
    <corners android:topLeftRadius="10dp"
        android:topRightRadius="10dp"
        android:bottomRightRadius="10dp"
        android:bottomLeftRadius="10dp"/>
</shape>
```
3. Create another `Drawable Resource File` under `/app/res/drawable` and name it `round_btn_normal.xml`. Place the following code inside
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#FF314268" />
    <corners android:topLeftRadius="10dp"
        android:topRightRadius="10dp"
        android:bottomRightRadius="10dp"
        android:bottomLeftRadius="10dp"/>
</shape>
```
4. Create another `Drawable Resource File` under `/app/res/drawable` and name it `round_btn_pressed.xml`. Place the following code inside
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#AA314268" />
    <corners android:topLeftRadius="10dp"
             android:topRightRadius="10dp"
             android:bottomRightRadius="10dp"
             android:bottomLeftRadius="10dp"/>
</shape>
```
5. Create another `Drawable Resource File` under `/app/res/drawable` and name it `background_selector.xml`. Place the following code inside
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@color/blue" android:state_pressed="false" android:state_selected="true" />
    <item android:drawable="@android:color/white" android:state_selected="false" />
</selector>
```
---
### Registering the Application
After you finish the above steps, let's register our application with the App Key you obtain from the DJI Developer Website. If you are not familiar with the App Key, please check the [Get Started](https://developer.dji.com/mobile-sdk/documentation/quick-start/index.html). A DJI App key has already been provided in the following code. **WARNING** The names of the activities in the manifest may have to be retyped in to work with your project. For example, if there is an error with `.ConnectionActivity`. Then, delete `.ConnectionActivity` and start typing `ConnectionActivity`. Once the autocompleted entry pops up, select it.

1. Let's open the `AndroidManifest.xml` file and replace its code with the following:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.riis.kotlin_mediamanagerdemo">
    <!-- Permissions and features -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />

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

    <application
        android:name=".MApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.KotlinMediaManagerDemo">
        <!-- DJI SDK -->
        <uses-library android:name="com.android.future.usb.accessory" />
        <uses-library
            android:name="org.apache.http.legacy"
            android:required="false" />

        <meta-data
            android:name="com.dji.sdk.API_KEY"
            android:value="2645608b67785db2b36512f5" />

        <activity
            android:name=".ConnectionActivity"
            android:configChanges="orientation"
            android:launchMode="singleTop"
            android:screenOrientation="portrait">
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
             <meta-data
                android:name="android.hardware.usb.action.USB_ACCESSORY_ATTACHED"
                android:resource="@xml/accessory_filter"/>
        </activity>
        <activity
            android:name=".DefaultLayoutActivity"
            android:configChanges="orientation"
            android:screenOrientation="landscape" />
        <activity
            android:name=".MainActivity"
            android:screenOrientation="landscape" />
    </application>

</manifest>
```

2. All that is left is to add the accessory filter file to the project. With this file, the app can determine what devices are being plugged into the Android phone. Create a new `Directory` under `app/res/` called **xml** if one has not already been made. Then, right click the newly created folder and create a new `XML Resource File` called **accessory_filter.xml**. Then press **OK**. Inside this resource file, replace all pre-existing code with the following code. The user will now be prompted to open the app when DJI controllers are plugged in.
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <usb-accessory model="T600" manufacturer="DJI"/>
    <usb-accessory model="AG410" manufacturer="DJI"/>
</resources>

```
Congratulations! Your DJI drone Media Manager android app is complete.

---
### Connecting to the Aircraft or Handheld Device

Please check this [Connect Mobile Device and Run Application](https://developer.dji.com/mobile-sdk/documentation/application-development-workflow/workflow-run.html#connect-mobile-device-and-run-application) guide to run the application and view the live video stream from your DJI product's camera.

---
### App Demo

#### Connection Activity 
<img src="https://github.com/godfreynolan/DJITutorialsKotlin/tree/main/4-MediaManager/Images/demo.gif" alt="drawing" width="30%" height="30%"/>

#### Main Activity 
*    Scrolling through image gallery:
<img src="https://github.com/godfreynolan/DJITutorialsKotlin/blob/main/4-MediaManager/Images/demo2.gif" alt="drawing" width="60%" height="60%"/>

*    Reloading the screen:
<img src="https://github.com/godfreynolan/DJITutorialsKotlin/blob/main/4-MediaManager/Images/demo3.gif" alt="drawing" width="60%" height="60%"/>

*    Pressing the back button:
<img src="https://github.com/godfreynolan/DJITutorialsKotlin/blob/main/4-MediaManager/Images/demo4.gif" alt="drawing" width="60%" height="60%"/>

*    Deleting files:
<img src="https://github.com/godfreynolan/DJITutorialsKotlin/blob/main/4-MediaManager/Images/demo5.gif" alt="drawing" width="60%" height="60%"/>

*    Downloading media:
<img src="https://github.com/godfreynolan/DJITutorialsKotlin/blob/main/4-MediaManager/Images/demo6.gif" alt="drawing" width="60%" height="60%"/>

*    Playing, pausing, and resuming a recorded video:
<img src="https://github.com/godfreynolan/DJITutorialsKotlin/blob/main/4-MediaManager/Images/demo7.gif" alt="drawing" width="60%" height="60%"/>

*    Checking media status:
<img src="https://github.com/godfreynolan/DJITutorialsKotlin/blob/main/4-MediaManager/Images/demo8.gif" alt="drawing" width="60%" height="60%"/>

*    Moving to a different time in a playing video:
<img src="https://github.com/godfreynolan/DJITutorialsKotlin/blob/main/4-MediaManager/Images/demo9.gif" alt="drawing" width="60%" height="60%"/>


---
### Summary

In this tutorial, you have learned how to use **MediaManager** to preview photos, play videos, download or delete files, you also learn how to get and show the video playback status info. By using the **MediaManager**, the users can get the metadata for all the multimedia files, and has access to each individual multimedia file. Hope you enjoy it!

---
### License

MIT
