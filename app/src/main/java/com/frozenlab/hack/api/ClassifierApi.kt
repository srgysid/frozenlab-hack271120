package com.frozenlab.hack.api

import com.frozenlab.hack.api.models.ClassifierValue
import com.frozenlab.hack.api.requests.TextRequest
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.POST

interface ClassifierApi {

    @POST("/predict")
    fun getValues(@Body request: TextRequest): Single<ArrayList<ClassifierValue>>
}