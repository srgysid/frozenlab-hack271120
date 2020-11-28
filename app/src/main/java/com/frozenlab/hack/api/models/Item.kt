package com.frozenlab.hack.api.models

import com.google.gson.annotations.SerializedName

open class Item {

    @SerializedName("id")
    var id:    Int    = -1

    @SerializedName("name")
    var title: String = ""
}