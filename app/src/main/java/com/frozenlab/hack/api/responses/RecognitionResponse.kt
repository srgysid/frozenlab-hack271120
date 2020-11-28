package com.frozenlab.hack.api.responses

import com.google.gson.annotations.SerializedName

class RecognitionResponse {
    @SerializedName("text")
    var text: String? = null
}