package org.sw_08.eu4h.interpretation

sealed interface Val {
    fun asInt() : Int = (this as IntVal).n
    fun asBool() : Boolean = (this as BoolVal).b
}

data class IntVal(val n: Int) : Val {
    override fun toString() = n.toString()
}
data class BoolVal(val b: Boolean) : Val {
    override fun toString() = b.toString()
}
data class CountryVal(val country: String) : Val {
    init {
        require(country.matches(Regex("^@[A-Z][A-Z0-9]{2}$"))) { 
            "Country must be @ followed by 1 alphabetical character, then 2 alphanumerical characters. All alphabetical characters must be uppercase." 
        }
    }

    override fun toString() = country
}
data class ProvinceVal(val province: String) : Val {
    init {
        require(province.matches(Regex("^@[0-9][0-9]*$"))) { 
            "Province must be @ followed by at least 1 digit." 
        }
    }

    override fun toString() = province
}
data class MissionVal(val mission: String) : Val {
    init {
        require(mission.matches(Regex("^@\"[A-Za-z0-9_]+\"$"))) {
            "Mission must be @ followed by any number of alphanumerical chars or underscores." 
        }
    }

    override fun toString() = mission
}
