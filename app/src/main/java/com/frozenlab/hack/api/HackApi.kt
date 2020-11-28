package com.frozenlab.hack.api

import com.frozenlab.hack.api.models.*
import com.frozenlab.hack.api.requests.CreateOrderRequest
import com.frozenlab.hack.api.requests.FCMRequest
import com.frozenlab.hack.api.requests.LoginRequest
import com.frozenlab.hack.api.requests.TextRequest
import com.frozenlab.hack.api.responses.NewAccessTokenResponse
import com.frozenlab.welive.api.models.UserProfile
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

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

    // Orders

    @Multipart
    @POST("/v1/orders/create")
    fun createOrder(
        @Part("json_data") request: RequestBody,
        @Part files:  ArrayList<MultipartBody.Part>
    ): Completable

    @GET("/v1/orders")
    fun getOrdersList(
        @Query("status_id")    statusId:     Int? = null,
        @Query("flow_type_id") flowTypeId:   Int? = null,
        @Query("page")         page:         Int? = null,
        @Query("per-page")     countPerPage: Int? = null
    ): Single<Response<ArrayList<OrderItem>>>

    @GET("/v1/orders/view")
    fun getOrderDetails(@Query("id") issueId: Int): Single<OrderDetails>

    @GET("/v1/orders/performers")
    fun getPerformers(): Single<ArrayList<Performer>>

    // Directories

    @GET("/v1/directory/type-order")
    fun getOrderTypes(): Single<ArrayList<Item>>

    @GET("/v1/directory/type-message")
    fun getMessageTypes(): Single<ArrayList<TypeMessage>>

    @GET("/v1/directory/department")
    fun getDepartments(): Single<ArrayList<Department>>
}
