package com.frozenlab.hack.api.requests

import com.google.gson.annotations.SerializedName

class LoginRequest {

    @SerializedName("phone")
    var phone:    String = ""

    @SerializedName("password")
    var password: String = ""
}