package org.sw_08.eu4h.interpretation

import org.sw_08.eu4h.abstract_syntax.Type
import org.sw_08.eu4h.abstract_syntax.LogicalOp

sealed interface Val {
    fun asInt() : Int = (this as IntVal).n
    fun asBool() : Boolean = (this as BoolVal).b
    fun asDouble() : Double = (this as DoubleVal).d
    fun asString() : String = (this as StringVal).s
}

data class IntVal(val n: Int) : Val {
    override fun toString() = n.toString()
}
data class BoolVal(val b: Boolean) : Val {
    override fun toString() = b.toString()
}
data class DoubleVal(val d: Double) : Val {
    override fun toString() = d.toString()
}
data class StringVal(val s: String) : Val {
    override fun toString() = s.toString()
}
data class CountryVal(val country: String) : Val {
    init {
        require(country.matches(Regex("^[A-Z][A-Z0-9]{2}$"))) { 
            "Country must be 1 alphabetical character, then 2 alphanumerical characters. All alphabetical characters must be uppercase." 
        }
    }

    override fun toString() = country
}
data class ProvinceVal(val province: String) : Val {
    init {
        require(province.matches(Regex("^[0-9][0-9]*$"))) { 
            "Province must only be composed of digits, and cannot be blank." 
        }
    }

    override fun toString() = province
}
data class MissionVal(
    val name: String,
    var position: Int,
    var icon: String,
    var triggers: String,
    var triggerScopeStack: MutableList<String>,
    var effects: String,
    var effectScopeStack: MutableList<String>
) : Val {
    override fun toString(): String =
        "Mission(name='$name', position=$position, icon='$icon', triggers='$triggers', triggerScopeStack='$triggerScopeStack', effects='$effects', effectScopeStack='$effectScopeStack')"
}
data class TriggerDef(
    val scope: String, // TODO, should be country, province, or dual
    val type: Type
)

data class EffectDef(
    val scope: String, // TODO, should be country, province, or dual
    val type: Type
)

data class ArrayVal(val elements: MutableList<Val>) : Val {
    override fun asString(): String = elements.toString()
}

data class LogicalVal(val op: LogicalOp) : Val {
    override fun toString() = op.name
}