package com.frozenlab.hack.api.requests

import com.google.gson.annotations.SerializedName

class FCMRequest {

    @SerializedName("new_token")
    var newToken: String? = null

    @SerializedName("old_token")
    var oldToken: String? = null

    // Application ID:
    // 1 - User application
    // 2 - Performer application
    @SerializedName("app_id")
    val appId: Int = 1
}