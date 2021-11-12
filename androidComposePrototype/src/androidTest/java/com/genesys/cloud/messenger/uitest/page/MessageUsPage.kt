package com.genesys.cloud.messenger.uitest.page

import android.app.Activity
import android.os.Build
import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.genesys.cloud.messenger.uitest.support.ApiHelper.API
import com.genesys.cloud.messenger.uitest.support.ApiHelper.answerNewConversation
import com.genesys.cloud.messenger.uitest.support.ApiHelper.sendOutboundSmsMessage
import org.awaitility.Awaitility
import java.io.File
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit

class MessageUsPage(activity: Activity) : BasePage(activity) {
    private val title = "Message Us"
    private val messageFieldText = "Send a message…"
    private val replyMessageText = "Reply to a message…"
    private val sendMessageButton = "Send Message"
    private val imageUploadButton = "Upload Image"
    data class DeviceType(var emulator: Boolean, var model: String)

    // Wait until android compose prototype begins
    fun verifyPageIsVisible(waitTime: Long = 20) {
        waitForElementWithUIAutomator(messageFieldText, waitTime)
    }

    fun isSendButtonEnabled(): Boolean {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val sendButton = mDevice.findObject(UiSelector().description(sendMessageButton))
        return sendButton.isEnabled()
    }

    fun waitForSendButtonEnabled() {
        Awaitility.await().atMost(waitTime, TimeUnit.SECONDS)
            .until {
                isSendButtonEnabled() == true
            }
    }

    fun selectSendButton() {
        waitForSendButtonEnabled()
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val sendButton = mDevice.findObject(UiSelector().description(sendMessageButton))
        sendButton.click()
    }

    // Sends a message to the agent
    fun enterMessage(message: String) {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val messageField = mDevice.findObject(UiSelector().text(messageFieldText))
        messageField.click()
        messageField.clearTextField()
        messageField.legacySetText(message)
        selectSendButton()
    }

    // Send a response from the agent
    fun sendResponse(message: String) {
        val apiHelper = API()
        val conversation = apiHelper.answerNewConversation()!!
        apiHelper.sendOutboundSmsMessage(conversation, message)
    }

    // Check for the appropriate response to be received
    fun lookForResponse(): String {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val responseBubble = mDevice.findObject(
            By.textContains(replyMessageText)
        )
        if (responseBubble != null) return responseBubble.text
        else return " "
    }

    // Wait for the appropriate response to be received
    fun verifyResponseReceived(message: String) {
        Awaitility.await().atMost(waitTime, TimeUnit.SECONDS)
            .until {
                lookForResponse().contains(message, ignoreCase = true)
            }
    }

    // Clicks on the Upload Image button
    fun tapUploadImageButton() {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val uploadImageButton = mDevice.findObject(UiSelector().description(imageUploadButton))
        uploadImageButton.click()
    }

    // Take a screenshot and store it on the device
    fun takeScreenshot(filename: String): Boolean {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "app_screenshots"
        )
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                println("Screenshot Test - Failed to create directory")
            }
        }
        val file = File(dir.getPath() + File.separator + filename)
        val successScreenshot = mDevice.takeScreenshot(file)
        if (!successScreenshot) {
            println("Screenshot not successful")
            AssertionError("Unable to make a screenshot.")
            return false
        } else
            return true
    }

    // Remove screen shot when finished with it
    fun deleteScreenshot(filename: String) {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "app_screenshots"
        )
        val file = File(dir.getPath() + File.separator + filename)
        val deleteSuccess = file.delete()
    }

    // Find screenshot on device and select it to be uploaded. Physical devices and emulators handle files differently. Have to accomodate all of them.
    fun uploadImage(fileName: String) {
        val deviceType = DeviceType(
            Build.FINGERPRINT.contains("generic"),
            Build.MODEL
        ).also { print(it) }
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val moreOptionsButton = mDevice.findObject(By.desc("More options"))
        if (moreOptionsButton != null) {
            moreOptionsButton.click()
            sleep(2000)
            val storageButton = mDevice.findObject(By.text("Show internal storage"))
            if (storageButton != null) {
                storageButton.click()
            } else {
                mDevice.pressBack()
            }
        }
        sleep(2000)
        val imageButton = mDevice.findObject(By.clazz("android.widget.ImageButton"))
        imageButton.click()
        sleep(2000)
        val pixelEmulatorModel = deviceType.model.contains("AOSP", ignoreCase = true)
        val sdkModel = deviceType.model.contains("SDK", ignoreCase = true)
        val sdkGPhone = deviceType.model.startsWith("sdk_gphone", true)
        val galaxyModel = deviceType.model.contains("Galaxy", ignoreCase = true)
        val samsungModel = deviceType.model.contains("SM-", ignoreCase = true)
        val pixelModel = deviceType.model.contains("Pixel", ignoreCase = true)
        val emulatorAOSP = deviceType.emulator && pixelEmulatorModel
        val emulatorSDK = deviceType.emulator && sdkModel
        val galaxyDevice = !(deviceType.emulator) && (galaxyModel || samsungModel)
        val pixelDevice = !(deviceType.emulator) && pixelModel
        when {
            emulatorAOSP -> {
                val aospSDK = mDevice.findObject(
                    UiSelector().resourceId(
                        "android:id/title"
                    ).textContains(
                        "AOSP"
                    )
                )
                aospSDK?.click()
            }
            emulatorSDK -> {
                val androidSDK = mDevice.findObject(
                    UiSelector().resourceId(
                        "android:id/title"
                    ).textContains(
                        "SDK"
                    )
                )
                androidSDK?.click()
            }
            galaxyDevice -> {
                val galaxyResource = mDevice.findObject(UiSelector().resourceId("android:id/title").textStartsWith("Galaxy"))
                galaxyResource?.click()
            }
            pixelDevice -> {
                val pixelResource = mDevice.findObject(UiSelector().resourceId("android:id/title").textStartsWith("Pixel"))
                pixelResource?.click()
            }
            else -> {
                AssertionError("Device not found.")
            }
        }
        sleep(2000)
        val picturesText = mDevice.findObject(UiSelector().textContains("Pictures"))
        try {
            picturesText?.click()
        } catch (e: Throwable) {
            if (pixelModel) {
                val pixelMenu = mDevice.findObject(UiSelector().resourceId("com.google.android.documentsui:id/sub_menu_grid"))
                pixelMenu?.click()
            } else {
                val gridviewButton = mDevice.findObject(By.desc("Grid view"))
                try {
                    gridviewButton?.click()
                } catch (e: Throwable) {
                    val subMenu = mDevice.findObject(UiSelector().resourceId("com.android.documentsui:id/sub_menu_grid"))
                    subMenu?.click()
                }
            }
            val pictures = mDevice.findObject(UiSelector().textContains("Pictures"))
            try {
                pictures?.click()
            } catch (e: Throwable) {
                if (pixelModel) {
                    val pixelMenu = mDevice.findObject(UiSelector().resourceId("com.google.android.documentsui:id/sub_menu_grid"))
                    pixelMenu?.click()
                }
            }
        }
        sleep(2000)
        val appScreenshots = mDevice.findObject(UiSelector().textContains("app_screenshots"))
        appScreenshots?.click()
        sleep(2000)
        val fileInList = mDevice.findObject(UiSelector().textContains(fileName))
        try {
            fileInList?.click()
        } catch (e: Throwable) {
            when {
                pixelEmulatorModel -> {
                    val listButton = mDevice.findObject(By.desc("List view"))
                    listButton.click()
                }
                ((pixelModel) || (samsungModel) || (sdkGPhone)) -> {
                    val subMenu = mDevice.findObject(UiSelector().resourceId("com.google.android.documentsui:id/sub_menu_list"))
                    subMenu?.click()
                }
                else -> {
                    val subList = mDevice.findObject(UiSelector().resourceId("com.android.documentsui:id/sub_menu_list"))
                    subList?.click()
                }
            }
            sleep(2000)
            val fileText = mDevice.findObject(UiSelector().textContains(fileName))
            try {
                fileText?.click()
            } catch (e: Throwable) {
                if ((pixelModel) || (samsungModel) || (sdkGPhone)) {
                    val subGrid = mDevice.findObject(UiSelector().resourceId("com.google.android.documentsui:id/sub_menu_grid"))
                    subGrid?.click()
                } else {
                    val subGrid = mDevice.findObject(UiSelector().resourceId("com.android.documentsui:id/sub_menu_grid"))
                    subGrid?.click()
                }
                sleep(2000)
                val fName = mDevice.findObject(UiSelector().textContains(fileName))
                fName?.click()
            }
        }
    }

    fun lookForFileAccessPermission() {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            mDevice.findObject(By.textContains("All Android Compose"))
            val allowButton = mDevice.findObject(By.textContains("Allow"))
            allowButton.click()
        } catch
        (e: Throwable) {
        }
    }
}
