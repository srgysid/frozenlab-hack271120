package com.frozenlab.hack.api.models

import com.google.gson.annotations.SerializedName

class TypeMessage: Item() {

    @SerializedName("type_order_id")
    var typeOrderId: Int    = -1
}