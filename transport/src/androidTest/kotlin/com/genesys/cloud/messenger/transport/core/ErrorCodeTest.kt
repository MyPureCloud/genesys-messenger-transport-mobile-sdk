package com.genesys.cloud.messenger.transport.core

import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

internal class ErrorCodeTest {

    @Test
    fun whenMapFrom() {
        assertTrue(ErrorCode.mapFrom(4000) is ErrorCode.FeatureUnavailable)
        assertTrue(ErrorCode.mapFrom(4001) is ErrorCode.FileTypeInvalid)
        assertTrue(ErrorCode.mapFrom(4002) is ErrorCode.FileSizeInvalid)
        assertTrue(ErrorCode.mapFrom(4003) is ErrorCode.FileContentInvalid)
        assertTrue(ErrorCode.mapFrom(4004) is ErrorCode.FileNameInvalid)
        assertTrue(ErrorCode.mapFrom(4005) is ErrorCode.FileNameTooLong)
        assertTrue(ErrorCode.mapFrom(4006) is ErrorCode.SessionHasExpired)
        assertTrue(ErrorCode.mapFrom(4007) is ErrorCode.SessionNotFound)
        assertTrue(ErrorCode.mapFrom(4008) is ErrorCode.AttachmentHasExpired)
        assertTrue(ErrorCode.mapFrom(4009) is ErrorCode.AttachmentNotFound)
        assertTrue(ErrorCode.mapFrom(4010) is ErrorCode.AttachmentNotSuccessfullyUploaded)
        assertTrue(ErrorCode.mapFrom(4011) is ErrorCode.MessageTooLong)
        assertTrue(ErrorCode.mapFrom(4013) is ErrorCode.CustomAttributeSizeTooLarge)
        assertTrue(ErrorCode.mapFrom(4020) is ErrorCode.MissingParameter)
        assertTrue(ErrorCode.mapFrom(4029) is ErrorCode.RequestRateTooHigh)
        assertTrue(ErrorCode.mapFrom(5000) is ErrorCode.UnexpectedError)
        assertTrue(ErrorCode.mapFrom(1001) is ErrorCode.WebsocketError)
        val randomIn300Range = Random.nextInt(300, 400)
        assertEquals(
            ErrorCode.mapFrom(randomIn300Range),
            ErrorCode.RedirectResponseError(randomIn300Range)
        )
        val randomIn400Range = Random.nextInt(400, 500)
        assertEquals(
            ErrorCode.mapFrom(randomIn400Range),
            ErrorCode.ClientResponseError(randomIn400Range)
        )
        val randomIn500Range = Random.nextInt(500, 600)
        assertEquals(
            ErrorCode.mapFrom(randomIn500Range),
            ErrorCode.ServerResponseError(randomIn500Range)
        )
    }

    @Test
    fun whenErrorCodeToCorrectiveAction() {
        assertEquals(ErrorCode.ClientResponseError(400).toCorrectiveAction(), CorrectiveAction.BadRequest)
        assertEquals(ErrorCode.ClientResponseError(403).toCorrectiveAction(), CorrectiveAction.Forbidden)
        assertEquals(ErrorCode.ClientResponseError(404).toCorrectiveAction(), CorrectiveAction.NotFound)
        assertEquals(ErrorCode.ClientResponseError(408).toCorrectiveAction(), CorrectiveAction.RequestTimeOut)
        assertEquals(ErrorCode.ClientResponseError(429).toCorrectiveAction(), CorrectiveAction.TooManyRequests)
        assertEquals(ErrorCode.ClientResponseError(randomCodeExcludingKnown()).toCorrectiveAction(), CorrectiveAction.Unknown)
    }

    private fun randomCodeExcludingKnown(): Int {
        val errorCodesToExclude = arrayOf(400, 403, 404, 408, 429)
        var random = Random.nextInt(401, 500)
         while (errorCodesToExclude.contains(random))  {
             random = Random.nextInt(401, 500)
        }
        return random
    }
}
