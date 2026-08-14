package com.dmitriim.localailab.core.ui.text

import androidx.annotation.StringRes

sealed interface UiText {
    data class Dynamic(val value: String) : UiText

    data class Resource(
        @param:StringRes val id: Int,
        val formatArgs: List<Any> = emptyList(),
    ) : UiText
}
