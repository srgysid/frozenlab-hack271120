package com.frozenlab.hack.api.models

import androidx.annotation.StringRes
import com.frozenlab.hack.R

enum class TypeCard(val id: Int, @StringRes val titleId: Int) {
    CONSTANT_PERIODIC(1, R.string.card_constant_periodic),
    MESSAGE(2, R.string.card_message),
    INSTRUCTION(3, R.string.card_instruction)
}