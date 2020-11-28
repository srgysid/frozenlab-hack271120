package com.frozenlab.hack.api

import com.frozenlab.hack.api.requests.FCMRequest
import com.frozenlab.hack.api.requests.LoginRequest
import com.frozenlab.hack.api.responses.NewAccessTokenResponse
import com.frozenlab.welive.api.models.UserProfile
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface HackApi {

    // Login

    @POST("/v1/login")
    fun login(@Body request: LoginRequest): Single<NewAccessTokenResponse>

    @POST("/v1/login/logout")
    fun logout(): Completable

    // User

    @POST("/v1/user/fcm")
    fun sendFCMToken(@Body request: FCMRequest): Completable

    @GET("/v1/user/profile")
    fun getUserProfile(): Single<UserProfile>
}