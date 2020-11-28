package com.frozenlab.hack.api.responses

import com.google.gson.annotations.SerializedName

class RecognitionPartialResponse {
    @SerializedName("partial")
    var partial: String? = null
}