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

class OrderDetails {
    var id:              Int    = -1
    var title:           String = ""
    var description:     String = ""
    var createdAt:       Date = Date()
    var factDate:        Date? = null
    var priority:        Int  = -1
    var requiredDate:    Date? = null
    var typeCards:       Int = -1
    var typePerformers:  Int = -1
    var closedAt:        Date = Date()
    var reaction:        Int  = -1
    var departmentCode:      String = ""
    var departmentShortCode: String = ""
    var departmentName:      String = ""
    var departmentShortName: String = ""

    class Deserializer: JsonDeserializer<OrderDetails> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): OrderDetails {

            val item = OrderDetails()

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
            item.title           = jsonObject.get("short_desc")?.asStringOrNull() ?: ""
            item.description     = jsonObject.get("full_desc")?.asStringOrNull() ?: ""
            item.closedAt        = jsonObject.get("closed_at")?.asDateOrNull(Preferences.jsonDateFormat) ?: Date()
            item.reaction        = jsonObject.get("reaction")?.asIntOrNull() ?: -1
            item.departmentCode      = jsonObject.get("department_code")?.asStringOrNull() ?: ""
            item.departmentShortCode = jsonObject.get("department_short_code")?.asStringOrNull() ?: ""
            item.departmentName      = jsonObject.get("department_name")?.asStringOrNull() ?: ""
            item.departmentShortName = jsonObject.get("department_short_name")?.asStringOrNull() ?: ""

            return item
        }

    }
}