package com.frozenlab.welive.api.models

import com.frozenlab.extensions.asStringOrNull
import com.google.gson.*
import java.lang.reflect.Type

class UserProfile {

    var name:  PersonName = PersonName("")
    var email: String     = ""
    var phone: String     = ""

    class UserProfileSerializer: JsonSerializer<UserProfile> {

        override fun serialize(src: UserProfile?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {

            val jsonObject = JsonObject()

            jsonObject.addProperty("first_name",  src?.name?.firstName)
            jsonObject.addProperty("second_name", src?.name?.secondName)
            jsonObject.addProperty("third_name",  src?.name?.thirdName)

            jsonObject.addProperty("email",           src?.email)
            jsonObject.addProperty("phone",           src?.phone)

            return jsonObject
        }
    }

    class UserProfileDeserializer : JsonDeserializer<UserProfile> {

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): UserProfile {

            val userProfile = UserProfile()

            if(json == null) {
                return userProfile
            }

            val jsonObject = json.asJsonObject

            userProfile.name = PersonName(
                jsonObject.get("first_name")?.asStringOrNull() ?: "",
                jsonObject.get("second_name")?.asStringOrNull() ?: "",
                jsonObject.get("third_name")?.asStringOrNull() ?: ""
            )

            userProfile.email = jsonObject.get("email")?.asStringOrNull() ?: ""
            userProfile.phone = jsonObject.get("phone")?.asStringOrNull() ?: ""

            return userProfile
        }
    }

}