package com.frozenlab.hack.websocket

import okhttp3.OkHttpClient
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import javax.security.cert.CertificateException

private fun getTrustThemAllManagerArray(): Array<X509TrustManager> {

    return arrayOf(
        object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(
                chain: Array<java.security.cert.X509Certificate>,
                authType: String
            ) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(
                chain: Array<java.security.cert.X509Certificate>,
                authType: String
            ) {
            }

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                return arrayOf()
            }
        }
    )
}

fun getUnsafeOkHttpClientBuilder(): OkHttpClient.Builder {

    val trustAllCerts = getTrustThemAllManagerArray()

    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, trustAllCerts, java.security.SecureRandom())

    return OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0])
        .hostnameVerifier(
            HostnameVerifier { _, _ -> true }
        )
}