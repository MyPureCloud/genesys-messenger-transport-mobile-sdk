package com.genesys.cloud.messenger.transport.util.logs

import okhttp3.logging.HttpLoggingInterceptor

internal fun Log.okHttpLogger(): HttpLoggingInterceptor.Logger =
    HttpLoggingInterceptor.Logger { message -> kermit.i(message) }
