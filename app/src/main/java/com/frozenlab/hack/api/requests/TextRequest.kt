package com.frozenlab.hack.api.requests

import com.google.gson.annotations.SerializedName

class TextRequest {
    @SerializedName("text")
    var text: String = ""
}