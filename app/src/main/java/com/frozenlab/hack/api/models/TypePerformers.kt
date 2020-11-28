package com.frozenlab.hack.api.models

import androidx.annotation.StringRes
import com.frozenlab.hack.R

enum class TypePerformers(val id: Int, @StringRes val titleId: Int) {
    ALL(1, R.string.performers_all),
    SELECTED(2, R.string.performers_currents)
}