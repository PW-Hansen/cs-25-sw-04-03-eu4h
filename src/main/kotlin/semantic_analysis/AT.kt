package org.sw_08.eu4h.semantic_analysis

import org.sw_08.eu4h.abstract_syntax.Type

data class AT(var isAssigned: Boolean, val type: Type) {
    fun clone(): AT = AT(isAssigned, type)
}