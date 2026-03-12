package com.genesys.cloud.messenger.uitest.support.ApiHelper

import androidx.test.espresso.IdlingResource
import com.genesys.cloud.messenger.uitest.support.testConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class API : IdlingResource {
    val json = Json { ignoreUnknownKeys = true }
    val agentToken = testConfig.agentToken
    val agentEmail = testConfig.agentEmail

    fun publicApiCall(
        httpMethod: String,
        httpURL: String,
        payload: ByteArray? = null
    ): JsonElement? {
        val url = URL("${testConfig.apiBaseAddress}$httpURL")
        var output: JsonElement? = null
        setIdle(false)
        println("Sending $httpMethod to $url.")

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = httpMethod
            setRequestProperty("Authorization", "bearer $agentToken")
            setRequestProperty("User-Agent", "Android-MTSDK-Testing")
            setRequestProperty("Content-Type", "application/json")

            if (payload != null) {
                outputStream.write(payload)
                outputStream.flush()
            }

            try {
                inputStream.bufferedReader().use { reader ->
                    reader.readText().let { text ->
                        if (text.isNotBlank()) output = json.parseToJsonElement(text)
                    }
                }
                setIdle(true)
            } catch (error: FileNotFoundException) {
                setIdle(true)
                throw Error("An error was received while sending an HTTP Request. The target endpoint was: ${error.message} \n$responseCode \n$responseMessage")
            }
        }
        return output
    }

    inline fun <reified T> parseJsonToClass(jsonResult: JsonElement?): T {
        val jsonString = jsonResult?.toString() ?: "{}"
        return json.decodeFromString(serializer(), jsonString)
    }

    inline fun <reified T> parseJsonToClass(jsonResult: List<JsonElement>?): T {
        val jsonString = if (jsonResult.isNullOrEmpty()) "[]" else "[${jsonResult.joinToString(",") { it.toString() }}]"
        return json.decodeFromString(serializer(), jsonString)
    }

    private val isIdle = AtomicBoolean(true)
    @Volatile private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName(): String = "HTTP Request"

    override fun isIdleNow(): Boolean = isIdle.get()

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
        this.resourceCallback = resourceCallback
    }

    private fun setIdle(isIdleNow: Boolean) {
        if (isIdleNow == isIdle.get()) return
        isIdle.set(isIdleNow)
        if (isIdleNow) resourceCallback?.onTransitionToIdle()
    }
}
