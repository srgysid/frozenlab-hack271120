package com.frozenlab.hack.api.models

import com.google.gson.annotations.SerializedName

class Department: Item() {

    @SerializedName("short_name")
    var titleShort: String = ""

    @SerializedName("code")
    var code: String = ""

    @SerializedName("short_code")
    var codeShort: String    = ""

    @SerializedName("parent_id")
    var parentId: Int? = null
}