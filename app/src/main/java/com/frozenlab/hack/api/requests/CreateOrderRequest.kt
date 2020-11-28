package com.frozenlab.hack.api.requests

import com.google.gson.annotations.SerializedName

class CreateOrderRequest {

    @SerializedName("short_desc")
    var title: String = ""

    @SerializedName("full_desc")
    var description: String = ""

    @SerializedName("type_cards")
    var typeCards: Int = -1

    @SerializedName("type_performers")
    var typePerformer: Int = -1

    @SerializedName("priority")
    var priority: Int = -1

    @SerializedName("type_message_id")
    var typeMessageId: Int = -1

    @SerializedName("performer_ids")
    var performers: IntArray? = null
}