package io.paymenthighway.sdk.service

import io.paymenthighway.sdk.BuildConfig
import io.paymenthighway.sdk.model.*
import io.paymenthighway.sdk.util.timestamp
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


internal data class EncryptionKeyApiResult(override val result: ApiResultInfo, val key: String? = null): ApiResultInterface

internal interface PaymentHighwayEndpoint {

    @GET("/mobile/{transactionId}/key")
    fun encryptionKey(@Path("transactionId") transactionId: TransactionId): Call<EncryptionKeyApiResult>

    @POST("/mobile/{transactionId}/tokenize")
    fun tokenizeTransaction(@Path("transactionId") transactionId: TransactionId, @Body tokenizeData: TokenizeData): Call<ApiResult>

    companion object Factory {
        fun create(config: PaymentConfig): PaymentHighwayEndpoint {
            val retrofit = Retrofit.Builder()
                    .baseUrl(config.environment.baseURL)
                    .callbackExecutor(Executors.newCachedThreadPool())
                    .client(makeHttpClient(config.merchantId, config.accountId))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

            return retrofit.create(PaymentHighwayEndpoint::class.java);
        }
    }
}

private fun makeHttpClient(merchantId: MerchantId, accountId: AccountId) = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(headersInterceptor(merchantId, accountId))
        .addInterceptor(loggingInterceptor())
        .build()

private fun headersInterceptor(merchantId: MerchantId, accountId: AccountId) = Interceptor { chain ->
    chain.proceed(chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("SPH-Merchant", merchantId.id)
            .header("SPH-Account", accountId.id)
            .header("SPH-Request-ID", UUID.randomUUID().toString().toLowerCase())
            .header("SPH-Timestamp", Date().timestamp())
            .build())
}

private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
}
