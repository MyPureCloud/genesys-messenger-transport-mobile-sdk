package transport.core

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.toCorrectiveAction
import org.junit.Test
import kotlin.random.Random

internal class ErrorCodeTest {

    @Test
    fun whenMapFrom() {
        assertThat(ErrorCode.mapFrom(4000)).isEqualTo(ErrorCode.FeatureUnavailable)
        assertThat(ErrorCode.mapFrom(4001)).isEqualTo(ErrorCode.FileTypeInvalid)
        assertThat(ErrorCode.mapFrom(4002)).isEqualTo(ErrorCode.FileSizeInvalid)
        assertThat(ErrorCode.mapFrom(4003)).isEqualTo(ErrorCode.FileContentInvalid)
        assertThat(ErrorCode.mapFrom(4004)).isEqualTo(ErrorCode.FileNameInvalid)
        assertThat(ErrorCode.mapFrom(4005)).isEqualTo(ErrorCode.FileNameTooLong)
        assertThat(ErrorCode.mapFrom(4006)).isEqualTo(ErrorCode.SessionHasExpired)
        assertThat(ErrorCode.mapFrom(4006)).isEqualTo(ErrorCode.SessionHasExpired)
        assertThat(ErrorCode.mapFrom(4007)).isEqualTo(ErrorCode.SessionNotFound)
        assertThat(ErrorCode.mapFrom(4008)).isEqualTo(ErrorCode.AttachmentHasExpired)
        assertThat(ErrorCode.mapFrom(4009)).isEqualTo(ErrorCode.AttachmentNotFound)
        assertThat(ErrorCode.mapFrom(4010)).isEqualTo(ErrorCode.AttachmentNotSuccessfullyUploaded)
        assertThat(ErrorCode.mapFrom(4011)).isEqualTo(ErrorCode.MessageTooLong)
        assertThat(ErrorCode.mapFrom(4013)).isEqualTo(ErrorCode.CustomAttributeSizeTooLarge)
        assertThat(ErrorCode.mapFrom(4020)).isEqualTo(ErrorCode.MissingParameter)
        assertThat(ErrorCode.mapFrom(4029)).isEqualTo(ErrorCode.RequestRateTooHigh)
        assertThat(ErrorCode.mapFrom(5000)).isEqualTo(ErrorCode.UnexpectedError)
        assertThat(ErrorCode.mapFrom(1001)).isEqualTo(ErrorCode.WebsocketError)
        assertThat(ErrorCode.mapFrom(1002)).isEqualTo(ErrorCode.WebsocketAccessDenied)
        assertThat(ErrorCode.mapFrom(-1009)).isEqualTo(ErrorCode.NetworkDisabled)
        assertThat(ErrorCode.mapFrom(6000)).isEqualTo(ErrorCode.CancellationError)
        assertThat(ErrorCode.mapFrom(6001)).isEqualTo(ErrorCode.AuthFailed)
        assertThat(ErrorCode.mapFrom(6002)).isEqualTo(ErrorCode.AuthLogoutFailed)
        assertThat(ErrorCode.mapFrom(6003)).isEqualTo(ErrorCode.RefreshAuthTokenFailure)
        assertThat(ErrorCode.mapFrom(6004)).isEqualTo(ErrorCode.HistoryFetchFailure)
        assertThat(ErrorCode.mapFrom(6005)).isEqualTo(ErrorCode.ClearConversationFailure)

        val randomIn300Range = Random.nextInt(300, 400)
        ErrorCode.mapFrom(randomIn300Range).run {
            assertThat(this).isEqualTo(ErrorCode.RedirectResponseError(randomIn300Range))
            assertThat(this.code).isEqualTo(randomIn300Range)
        }

        val randomIn400Range = Random.nextInt(400, 500)
        ErrorCode.mapFrom(randomIn400Range).run {
            assertThat(this).isEqualTo(ErrorCode.ClientResponseError(randomIn400Range))
            assertThat(this.code).isEqualTo(randomIn400Range)
        }

        val randomIn500Range = Random.nextInt(500, 600)
        ErrorCode.mapFrom(randomIn500Range).run {
            assertThat(this).isEqualTo(ErrorCode.ServerResponseError(randomIn500Range))
            assertThat(this.code).isEqualTo(randomIn500Range)
        }
    }

    @Test
    fun whenErrorCodeToCorrectiveAction() {
        assertThat(ErrorCode.ClientResponseError(400).toCorrectiveAction()).isEqualTo(
            CorrectiveAction.BadRequest
        )
        assertThat(ErrorCode.ClientResponseError(403).toCorrectiveAction()).isEqualTo(
            CorrectiveAction.Forbidden
        )
        assertThat(ErrorCode.ClientResponseError(404).toCorrectiveAction()).isEqualTo(
            CorrectiveAction.NotFound
        )
        assertThat(ErrorCode.ClientResponseError(408).toCorrectiveAction()).isEqualTo(
            CorrectiveAction.RequestTimeOut
        )
        assertThat(ErrorCode.ClientResponseError(429).toCorrectiveAction()).isEqualTo(
            CorrectiveAction.TooManyRequests
        )
        assertThat(
            ErrorCode.ClientResponseError(randomCodeExcludingKnown()).toCorrectiveAction()
        ).isEqualTo(CorrectiveAction.Unknown)
        assertThat(ErrorCode.AuthFailed.toCorrectiveAction()).isEqualTo(CorrectiveAction.ReAuthenticate)
        assertThat(ErrorCode.RefreshAuthTokenFailure.toCorrectiveAction()).isEqualTo(
            CorrectiveAction.ReAuthenticate
        )
        assertThat(ErrorCode.AuthLogoutFailed.toCorrectiveAction()).isEqualTo(CorrectiveAction.ReAuthenticate)
        assertThat(ErrorCode.CustomAttributeSizeTooLarge.toCorrectiveAction()).isEqualTo(
            CorrectiveAction.CustomAttributeSizeTooLarge
        )
    }

    private fun randomCodeExcludingKnown(): Int {
        val errorCodesToExclude = arrayOf(400, 403, 404, 408, 429)
        var random = Random.nextInt(401, 500)
        while (errorCodesToExclude.contains(random)) {
            random = Random.nextInt(401, 500)
        }
        return random
    }
}
