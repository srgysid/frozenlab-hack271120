package com.frozenlab.hack.api.requests

import com.google.gson.annotations.SerializedName

class LoginRequest {

    @SerializedName("username")
    var login:    String = ""

    @SerializedName("password")
    var password: String = ""
}