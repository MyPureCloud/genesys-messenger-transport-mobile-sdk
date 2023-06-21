package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.util.Vault
import java.lang.IllegalArgumentException

class FakeVault(private val keys: Keys) : Vault(keys) {

    private var fakeJwtToken: String? = null
    private var fakeAuthRefreshToken: String? = null

    override fun store(key: String, value: String) {
        when (key) {
            keys.authRefreshTokenKey -> fakeAuthRefreshToken = value
            keys.tokenKey -> fakeJwtToken = value
            else -> throw IllegalArgumentException("Key is not supported in FakeVault.")
        }
    }

    override fun fetch(key: String): String? {
        return when (key) {
            keys.authRefreshTokenKey -> fakeAuthRefreshToken
            keys.tokenKey -> fakeJwtToken
            else -> null
        }
    }
}
