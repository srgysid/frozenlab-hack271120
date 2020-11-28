package com.frozenlab.hack.api.models

import com.google.gson.annotations.SerializedName

class ClassifierValue {
    @SerializedName("prob")
    var probability: Double = 0.0

    @SerializedName("value")
    var value: Int = -1

}