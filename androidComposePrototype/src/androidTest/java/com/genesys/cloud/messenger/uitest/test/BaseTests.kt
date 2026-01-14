package com.genesys.cloud.messenger.uitest.test

import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.genesys.cloud.messenger.androidcomposeprototype.MainActivity
import com.genesys.cloud.messenger.uitest.page.MessengerPage
import com.genesys.cloud.messenger.uitest.page.OpeningPage
import com.genesys.cloud.messenger.uitest.support.ApiHelper.API
import com.jaredrummler.android.device.DeviceName
import org.junit.Before
import org.junit.Rule

open class BaseTests {
    companion object {
        open val apiHelper by lazy { API() }

        // Will run before running any test.
        init {
            setUpDevice()
        }

        // Log the device name and SDK version
        fun setUpDevice() {
            val device = DeviceName.getDeviceName()
            println("DEVICE NAME: $device")
            val SDKVersionNumber = android.os.Build.VERSION.SDK_INT
            println("SDK Version Number: $SDKVersionNumber")
        }
    }

    @get:Rule
    private val rule = ActivityTestRule(MainActivity::class.java, false, false)

    @get:Rule
    var permissionRule = GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    // Run before every test.
    @Before
    fun beforeEachTest() {
        println("Before each test.")
        rule.launchActivity(null)
    }

    // Messenger page shortcut
    fun messenger(func: MessengerPage.() -> Unit) {
        MessengerPage(rule.activity).apply(func)
        return Unit
    }

    // Opening page shortcut
    fun opening(func: OpeningPage.() -> Unit) {
        OpeningPage(rule.activity).apply(func)
        return Unit
    }
}
