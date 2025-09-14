package com.genesys.cloud.messenger.composeapp.platform

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import java.io.File

/**
 * Android implementation of PlatformContext
 */
actual interface PlatformContext

/**
 * Android wrapper for Context
 */
class AndroidPlatformContext(val context: Context) : PlatformContext

/**
 * Android implementation of PlatformOperations
 */
actual class PlatformOperations actual constructor(context: PlatformContext) {
    
    private val androidContext: Context = (context as AndroidPlatformContext).context
    
    /**
     * Show an Android toast message
     */
    actual fun showToast(message: String) {
        Toast.makeText(androidContext, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Get Android device information
     */
    actual fun getDeviceInfo(): DeviceInfo {
        val packageInfo = androidContext.packageManager.getPackageInfo(androidContext.packageName, 0)
        val deviceId = Settings.Secure.getString(androidContext.contentResolver, Settings.Secure.ANDROID_ID)
        
        return DeviceInfo(
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            osVersion = "Android ${Build.VERSION.RELEASE}",
            appVersion = packageInfo.versionName ?: "Unknown",
            deviceId = deviceId ?: "Unknown"
        )
    }
    
    /**
     * Open URL in Android default browser
     */
    actual fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        androidContext.startActivity(intent)
    }
    
    /**
     * Get Android app storage directory
     */
    actual fun getStorageDirectory(): String {
        return androidContext.filesDir.absolutePath
    }
    
    /**
     * Check if network is available on Android
     */
    @Suppress("MissingPermission")
    actual fun isNetworkAvailable(): Boolean {
        val connectivityManager = androidContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
}