package com.frozenlab.hack.api.models

import com.frozenlab.extensions.asDateOrNull
import com.frozenlab.extensions.asIntOrNull
import com.frozenlab.extensions.asStringOrNull
import com.frozenlab.hack.Preferences
import com.frozenlab.welive.api.models.PersonName
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
import java.util.*

class Performer {

    var id:   Int = -1
    var name: PersonName = PersonName("")

    class Deserializer: JsonDeserializer<Performer> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): Performer {
            val item = Performer()

            if(json == null) {
                return item
            }

            val jsonObject = json.asJsonObject

            item.id   = jsonObject.get("id")?.asIntOrNull() ?: -1
            item.name = PersonName(jsonObject.get("name")?.asStringOrNull() ?: "")

            return item
        }

    }
}