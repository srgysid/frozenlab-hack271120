package com.frozenlab.welive.api.models

class PersonName {

    constructor(firstName: String, secondName: String, thirdName: String) {
        this.firstName  = firstName
        this.secondName = secondName
        this.thirdName  = thirdName
    }

    constructor(fullName: String) {
        parseFullName(fullName)
    }

    var firstName:  String  = ""
    var secondName: String  = ""
    var thirdName:  String? = null

    val fullName: String
        get() = "$secondName $firstName" + if(!thirdName.isNullOrEmpty()) { " $thirdName"} else { "" }

    val firstSecondName: String
        get() = "$firstName $secondName"

    val firstThirdName: String
        get() = firstName + if(!thirdName.isNullOrEmpty()) { " $thirdName" } else { "" }

    val shortFullName: String
        get() = "$secondName ${ if(firstName.isNotBlank()) { firstName[0] + "." } else { "" } }" + if(!thirdName.isNullOrBlank()) { " ${ thirdName!![0]}."} else { "" }

    override fun toString(): String = fullName

    private fun parseFullName(fullName: String) {

        val regex = """\s+""".toRegex()
        val parts = regex.split(fullName.trim())

        when {
            parts.size > 2 -> {
                this.secondName = parts[0]
                this.firstName  = parts[1]
                this.thirdName  = parts[2]
            }
            parts.size == 2 -> {
                this.secondName = parts[0]
                this.firstName  = parts[1]
            }
            parts.size == 1 -> this.firstName = parts[0]
        }
    }
}