package com.genesys.cloud.messenger.composeapp.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlatformTest {
    
    @Test
    fun testDeviceInfoCreation() {
        val deviceInfo = DeviceInfo(
            model = "Test Model",
            osVersion = "1.0.0",
            appVersion = "1.0.0",
            deviceId = "test-device-id"
        )
        
        assertEquals("Test Model", deviceInfo.model)
        assertEquals("1.0.0", deviceInfo.osVersion)
        assertEquals("1.0.0", deviceInfo.appVersion)
        assertEquals("test-device-id", deviceInfo.deviceId)
    }
    
    @Test
    fun testDeviceInfoCopy() {
        val originalDeviceInfo = DeviceInfo(
            model = "Original Model",
            osVersion = "1.0.0",
            appVersion = "1.0.0",
            deviceId = "original-id"
        )
        
        val copiedDeviceInfo = originalDeviceInfo.copy(model = "Updated Model")
        
        assertEquals("Updated Model", copiedDeviceInfo.model)
        assertEquals("1.0.0", copiedDeviceInfo.osVersion)
        assertEquals("1.0.0", copiedDeviceInfo.appVersion)
        assertEquals("original-id", copiedDeviceInfo.deviceId)
    }
    
    @Test
    fun testDeviceInfoEquality() {
        val deviceInfo1 = DeviceInfo(
            model = "Test Model",
            osVersion = "1.0.0",
            appVersion = "1.0.0",
            deviceId = "test-id"
        )
        
        val deviceInfo2 = DeviceInfo(
            model = "Test Model",
            osVersion = "1.0.0",
            appVersion = "1.0.0",
            deviceId = "test-id"
        )
        
        val deviceInfo3 = DeviceInfo(
            model = "Different Model",
            osVersion = "1.0.0",
            appVersion = "1.0.0",
            deviceId = "test-id"
        )
        
        assertEquals(deviceInfo1, deviceInfo2)
        assertNotEquals(deviceInfo1, deviceInfo3)
    }
    
    @Test
    fun testDeviceInfoToString() {
        val deviceInfo = DeviceInfo(
            model = "Test Model",
            osVersion = "1.0.0",
            appVersion = "1.0.0",
            deviceId = "test-id"
        )
        
        val toString = deviceInfo.toString()
        assertNotNull(toString)
        assertTrue(toString.contains("Test Model"))
        assertTrue(toString.contains("1.0.0"))
        assertTrue(toString.contains("test-id"))
    }
    
    @Test
    fun testDeviceInfoHashCode() {
        val deviceInfo1 = DeviceInfo(
            model = "Test Model",
            osVersion = "1.0.0",
            appVersion = "1.0.0",
            deviceId = "test-id"
        )
        
        val deviceInfo2 = DeviceInfo(
            model = "Test Model",
            osVersion = "1.0.0",
            appVersion = "1.0.0",
            deviceId = "test-id"
        )
        
        assertEquals(deviceInfo1.hashCode(), deviceInfo2.hashCode())
    }
}