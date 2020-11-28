package com.frozenlab.hack.api.models

import com.frozenlab.extensions.asDateOrNull
import com.frozenlab.extensions.asIntOrNull
import com.frozenlab.extensions.asStringOrNull
import com.frozenlab.hack.Preferences
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.util.*

class OrderItem {

    var id:              Int = -1
    var createdAt:       Date = Date()
    var factDate:        Date? = null
    var priority:        Int  = -1
    var requiredDate:    Date? = null
    var typeCards:       Int = -1
    var typeMessageName: String = ""
    var typeOrderName:   String = ""
    var typePerformers:  Int = -1

    class Deserializer: JsonDeserializer<OrderItem> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): OrderItem {

            val item = OrderItem()

            if(json == null) {
                return item
            }

            val jsonObject = json.asJsonObject

            item.id              = jsonObject.get("id")?.asIntOrNull() ?: -1
            item.createdAt       = jsonObject.get("created_at")?.asDateOrNull(Preferences.jsonDateFormat) ?: Date()
            item.factDate        = jsonObject.get("fact_date")?.asDateOrNull(Preferences.jsonDateFormat)
            item.requiredDate    = jsonObject.get("required_date")?.asDateOrNull(Preferences.jsonDateFormat)
            item.priority        = jsonObject.get("priority")?.asIntOrNull() ?: -1
            item.typeCards       = jsonObject.get("type_cards")?.asIntOrNull() ?: -1
            item.typePerformers  = jsonObject.get("type_performers")?.asIntOrNull() ?: -1
            item.typeMessageName = jsonObject.get("type_message_name")?.asStringOrNull() ?: ""
            item.typeOrderName   = jsonObject.get("type_order_name")?.asStringOrNull() ?: ""

            return item
        }

    }
}