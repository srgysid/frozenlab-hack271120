package com.frozenlab.hack.api.models

import androidx.annotation.StringRes
import com.frozenlab.hack.R

enum class OrderPriority(val id: Int, @StringRes val titleId: Int) {
    MOST_IMPORTANT(1, R.string.priority_most_important),
    HIGH(2, R.string.priority_high),
    MEDIUM(3, R.string.priority_medium),
    LOW(4, R.string.priority_low)
}