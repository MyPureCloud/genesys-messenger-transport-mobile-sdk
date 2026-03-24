package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.core.TlsVersion
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion as OkHttpTlsVersion

internal fun connectionSpecForMinimumTls(minimumTlsVersion: TlsVersion): ConnectionSpec? =
    when (minimumTlsVersion) {
        TlsVersion.SYSTEM_DEFAULT -> null
        TlsVersion.TLS_1_2 ->
            ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(OkHttpTlsVersion.TLS_1_2, OkHttpTlsVersion.TLS_1_3)
                .build()
        TlsVersion.TLS_1_3 ->
            ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(OkHttpTlsVersion.TLS_1_3)
                .build()
    }

internal fun OkHttpClient.Builder.applyMinimumTlsVersion(minimumTlsVersion: TlsVersion): OkHttpClient.Builder {
    connectionSpecForMinimumTls(minimumTlsVersion)?.let { spec ->
        connectionSpecs(listOf(spec, ConnectionSpec.CLEARTEXT))
    }
    return this
}