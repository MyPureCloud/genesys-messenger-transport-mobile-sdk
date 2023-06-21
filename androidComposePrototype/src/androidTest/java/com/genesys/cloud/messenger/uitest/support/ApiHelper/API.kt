package com.genesys.cloud.messenger.uitest.support.ApiHelper

import androidx.test.espresso.IdlingResource
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.genesys.cloud.messenger.uitest.support.testConfig
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class API : IdlingResource {

    val mapper = jacksonObjectMapper()
    val agentToken = testConfig.agentToken
    val agentEmail = testConfig.agentEmail

    fun publicApiCall(httpMethod: String, httpURL: String, payload: ByteArray? = null): JsonNode? {
        val url = URL("${testConfig.apiBaseAddress}$httpURL")
        var output: JsonNode? = null
        setIdle(false)
        println("Sending $httpMethod to $url.")

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = httpMethod
            setRequestProperty("Authorization", "bearer $agentToken")
            setRequestProperty("User-Agent", "Android-MTSDK-Testing")
            setRequestProperty("Content-Type", "application/json")

            // send the payload if needed.
            if (payload != null) {
                outputStream.write(payload)
                outputStream.flush()
            }

            try {
                inputStream.bufferedReader().use {
                    it.lines().forEach { line ->
                        output = mapper.readValue(line)
                    }
                    setIdle(true)
                }
            } catch (error: FileNotFoundException) {
                // FileNotFoundException can be received when a 404 is returned by an endpoint.
                throw Error("An error was received while sending an HTTP Request. The target endpoint was: ${error.message} \n$responseCode \n$responseMessage")
                setIdle(false)
            }
        }
        return output
    }

    inline fun <reified T> parseJsonToClass(jsonResult: JsonNode?): T {
        val jsonString = jsonResult?.toString() ?: ""
        return mapper.readValue(jsonString)
    }

    inline fun <reified T> parseJsonToClass(jsonResult: List<JsonNode>?): T {
        val jsonString = jsonResult.toString()
        return mapper.readValue(jsonString)
    }

    // Idling Resource code.
    private val isIdle = AtomicBoolean(true)

    // written from main thread, read from any thread.
    @Volatile private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName(): String = "HTTP Request"

    override fun isIdleNow(): Boolean = isIdle.get()

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
        this.resourceCallback = resourceCallback
    }

    private fun setIdle(isIdleNow: Boolean) {
        if (isIdleNow == isIdle.get()) {
            return
        }
        isIdle.set(isIdleNow)
        if (isIdleNow) {
            resourceCallback?.onTransitionToIdle()
        }
    }
}
