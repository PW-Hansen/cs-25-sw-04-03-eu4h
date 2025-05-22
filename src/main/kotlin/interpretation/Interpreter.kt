package org.sw_08.eu4h.interpretation
import org.sw_08.eu4h.pretty_printing.PrettyPrinter
import org.sw_08.eu4h.abstract_syntax.*

fun typeMatches(type: Type, value: Val): Boolean {
    return when (type) {
        is IntT -> value is IntVal
        is BoolT -> value is BoolVal
        is DoubleT -> value is DoubleVal
        is StringT -> value is StringVal
        is CountryT -> value is CountryVal
        is ProvinceT -> value is ProvinceVal
        is LogicalT -> value is LogicalVal
        is MissionT -> value is MissionVal
        else -> false
    }
}

class Interpreter {
    companion object {
        val missions: MutableMap<String, MissionVal> = mutableMapOf()
        val triggers: MutableMap<String, TriggerDef> = mutableMapOf()

        fun evalStmt(stmt: Stmt, envV: EnvV): Unit {
            when (stmt) {
                Skip -> { /* Nothing to do here */ }
                is Print -> {
                    val value = evalExpr(stmt.value!!, envV)
                    if (value is MissionVal) {
                        println(PrettyPrinter.printMissionBlock(value))
                    } else {
                        println(value.toString())
                    }
                }

                is Assign -> {
                    val value = evalExpr(stmt.value, envV)
                    when (val lhs = stmt.lhs) {
                        is Ref -> {
                            envV.set(lhs.name, value)
                            if (value is MissionVal) {
                                // TODO discuss whether only identifier should be added to the map.
                                missions[lhs.name] = value
                                missions[value.name] = value
                            }
                        }
                        is FieldAccess -> {
                            val baseVal = evalExpr(lhs.base, envV)
                            if (baseVal is MissionVal) {
                                when (lhs.field) {
                                    "name" -> error("Name cannot be overwritten.")
                                    "position" -> baseVal.position = value.asInt()
                                    "icon" -> baseVal.icon = value.asString()
                                    "triggers" -> baseVal.triggers = value.asString()
                                    "triggerScopeStack" -> error("Cannot assign to triggerScopeStack directly")
                                    "effects" -> baseVal.effects = value.asString()
                                    "effectScopeStack" -> error("Cannot assign to triggerScopeStack directly")
                                    else -> error("Unknown field '${lhs.field}' for mission")
                                }
                            } else {
                                error("Field assignment on non-mission value")
                            }
                        }
                        is ArrayAccess -> {
                            // Support a[0] = 5;
                            // Recursively evaluate all but the last index
                            fun assignArray(arrAccess: ArrayAccess, value: Val) {
                                val base = evalExpr(arrAccess.base, envV)
                                val idx = evalExpr(arrAccess.index, envV).asInt()
                                if (base !is ArrayVal)
                                    error("Assignment to non-array value")
                                if (arrAccess.base is ArrayAccess) {
                                    // Recurse if multidimensional, but you may need a loop for arbitrary depth
                                    assignArray(arrAccess.base, value)
                                } else {
                                    if (idx < 0 || idx >= base.elements.size)
                                        error("Array index $idx out of bounds [0, ${base.elements.size})")
                                    base.elements[idx] = value
                                }
                            }
                            assignArray(lhs, value)
                        }
                        else -> error("Invalid assignment target")
                    }


                }

                is Comp -> {
                    evalStmt(stmt.stmt1!!, envV)
                    evalStmt(stmt.stmt2!!, envV)
                }

                is Declaration -> envV.bind(stmt.identifier!!, null)

                is If -> {
                    val condition = evalExpr(stmt.condition!!, envV)
                    if (condition.asBool())
                        evalStmt(stmt.thenBody!!, envV.newScope())
                    else
                        evalStmt(stmt.elseBody!!, envV.newScope())
                }

                is While -> {
                    // Note: "var" means "can change", "val" means "read-only"
                    var condition = evalExpr(stmt.condition!!, envV)
                    while (condition.asBool()) {
                        evalStmt(stmt.body!!, envV)
                        condition = evalExpr(stmt.condition!!, envV)
                    }
                }

                is PushStmt -> {
                    val arrVal = envV.tryGet(stmt.arrayName)
                    if (arrVal !is ArrayVal) error("Cannot push to non-array variable '${stmt.arrayName}'")
                    val value = evalExpr(stmt.value, envV)
                    arrVal.elements.add(value)
                }
                
                is PopStmt -> {
                    val arrVal = envV.tryGet(stmt.arrayName)
                    if (arrVal !is ArrayVal) error("Cannot pop from non-array variable '${stmt.arrayName}'")
                    if (arrVal.elements.isEmpty()) error("Cannot pop from empty array '${stmt.arrayName}'")
                    arrVal.elements.removeAt(arrVal.elements.size - 1)
                }

                is CreateTrigger -> {
                    if (triggers.containsKey(stmt.name)) {
                        error("Trigger '${stmt.name}' already exists.")
                    }
                    triggers[stmt.name] = TriggerDef(stmt.scope, stmt.type)
                }

                is AssignTrigger -> {
                    val trigger = triggers[stmt.triggerName] ?: error("Trigger '${stmt.triggerName}' not found.")
                    val mission = missions[stmt.missionName] ?: error("Mission '${stmt.missionName}' not found.")
                    val value = evalExpr(stmt.expr, envV)
                    // Type check
                    if (!typeMatches(trigger.type, value)) {
                        error("Type mismatch: trigger '${stmt.triggerName}' expects ${trigger.type}, got ${value::class.simpleName}")
                    }

                    // Trigger assignment value.
                    var triggerAssignment = "${stmt.triggerName} = ${value}"

                    // Scope compatibility check
                    val permitted_scope = mission.triggerScopeStack.last()
                    if (trigger.scope != permitted_scope && permitted_scope != "dual") {
                        // Invalid scope, print a warning, then create commented-out trigger assignment.
                        println("Warning: Trigger '${stmt.triggerName}' used in invalid scope on line ${stmt.lineNumber}.")
                        // # is used to comment out content in EU4 script files.
                        triggerAssignment = "# $triggerAssignment # Invalid scope usage, permitted scope is '$permitted_scope'."
                    } 

                    // Extending the trigger string with the new trigger.
                    if (mission.triggers == "") {
                        mission.triggers = triggerAssignment
                    } else if (mission.triggers.endsWith("}")) {
                        mission.triggers = mission.triggers.substring(0, mission.triggers.length - 1) + "\n$triggerAssignment\n}"
                    } else {
                        mission.triggers += "\n$triggerAssignment"
                    }
                }

                is CreateEffect -> {
                    if (triggers.containsKey(stmt.name)) {
                        error("Effect '${stmt.name}' already exists.")
                    }
                    triggers[stmt.name] = TriggerDef(stmt.scope, stmt.type)
                }

                is AssignEffect -> {
                    val effect = triggers[stmt.effectName] ?: error("Effect '${stmt.effectName}' not found.")
                    val mission = missions[stmt.missionName] ?: error("Mission '${stmt.missionName}' not found.")
                    val value = evalExpr(stmt.expr, envV)
                    // Type check
                    if (!typeMatches(effect.type, value)) {
                        error("Type mismatch: effect '${stmt.effectName}' expects ${effect.type}, got ${value::class.simpleName}")
                    }

                    // Effect assignment value.
                    var effectAssignment = "${stmt.effectName} = ${value}"

                    // Scope compatibility check
                    val permitted_scope = mission.effectScopeStack.last()
                    if (effect.scope != permitted_scope && permitted_scope != "dual") {
                        // Invalid scope, print a warning, then create commented-out effect assignment.
                        println("Warning: Effect '${stmt.effectName}' used in invalid scope on line ${stmt.lineNumber}.")
                        // # is used to comment out content in EU4 script files.
                        effectAssignment = "# $effectAssignment # Invalid scope usage, permitted scope is '$permitted_scope'."
                    } 

                    // Extending the effect string with the new effect.
                    if (mission.effects == "") {
                        mission.effects = effectAssignment
                    } else if (mission.effects.endsWith("}")) {
                        mission.effects = mission.effects.substring(0, mission.effects.length - 1) + "\n$effectAssignment\n}"
                    } else {
                        mission.effects += "\n$effectAssignment"
                    }
                }

                is OpenScope -> {
                    val mission = missions[stmt.missionName] ?: error("Mission '${stmt.missionName}' not found.")
                    val scopeVal = evalExpr(stmt.scope, envV) ?: error("Scope value could not be evaluated.")

                    val scopeType = when (scopeVal) {
                        is CountryVal -> "country"
                        is ProvinceVal -> "province"
                        is LogicalVal -> "logical"
                        else -> error("Invalid input, third argument must be a country, province, or logical.")
                    }

                    when (stmt.spaceName) {
                        "trigger" ->  { 
                            if (scopeType == "logical") {
                                val previousScope = mission.triggerScopeStack.last()
                                mission.triggerScopeStack.add(previousScope)
                            } else {
                                mission.triggerScopeStack.add(scopeType)
                            }

                            val newScope = when (scopeVal) {
                                is CountryVal -> scopeVal.country
                                is ProvinceVal -> scopeVal.province
                                is LogicalVal -> scopeVal.op
                                else -> error("Invalid input, third argument must be a country, province, or logical.")
                            }

                            val triggerScopeStackChange = "$newScope = {}"

                            if (mission.triggers == "") {
                                mission.triggers = triggerScopeStackChange
                            } else if (mission.triggers.endsWith("}")) {
                                mission.triggers = mission.triggers.substring(0, mission.triggers.length - 1) + "\n$triggerScopeStackChange\n}"
                            } else {
                                mission.triggers += "\n$triggerScopeStackChange"
                            }
                        }
                        "effect" ->  { 
                            if (scopeType == "logical") {
                                val previousScope = mission.effectScopeStack.last()
                                mission.effectScopeStack.add(previousScope)
                            } else {
                                mission.effectScopeStack.add(scopeType)
                            }

                            val newScope = when (scopeVal) {
                                is CountryVal -> scopeVal.country
                                is ProvinceVal -> scopeVal.province
                                is LogicalVal -> scopeVal.op
                                else -> error("Invalid input, third argument must be a country, province, or logical.")
                            }

                            val effectScopeStackChange = "$newScope = {}"

                            if (mission.effects == "") {
                                mission.effects = effectScopeStackChange
                            } else if (mission.effects.endsWith("}")) {
                                mission.effects = mission.effects.substring(0, mission.effects.length - 1) + "\n$effectScopeStackChange\n}"
                            } else {
                                mission.effects += "\n$effectScopeStackChange"
                            }
                        }
                        else -> error("Invalid space name '${stmt.spaceName}'. Expected 'trigger' or 'effect'.")
                    }
                }

                is CloseScope -> {
                    val mission = missions[stmt.missionName] ?: error("Mission '${stmt.missionName}' not found.")
                    when (stmt.spaceName) {
                        "trigger" -> mission.triggerScopeStack.removeAt(mission.triggerScopeStack.size - 1)
                        "effect" -> mission.effectScopeStack.removeAt(mission.effectScopeStack.size - 1)
                        else -> error("Invalid space name '${stmt.spaceName}'. Expected 'trigger' or 'effect'.")
                    }

                    when (stmt.spaceName) {
                        "trigger" -> {
                            if (mission.triggers.endsWith("}")) {
                                mission.triggers += "\n"
                            } else {
                                error("Cannot close the ROOT scope.")
                            }
                        }
                        "effect" -> {
                            if (mission.effects.endsWith("}")) {
                                mission.effects += "\n"
                            } else {
                                error("Cannot close the ROOT scope.")
                            }
                        }
                        else -> error("Invalid space name '${stmt.spaceName}'. Expected 'trigger' or 'effect'.")
                    }
                }
            }
        }

        fun evalExpr(expr: Expr, envV: EnvV): Val {
            return when (expr) {
                is BoolV -> BoolVal(expr.value)
                is NumV -> IntVal(expr.value)
                is DoubleV -> DoubleVal(expr.value)
                is StringV -> StringVal(expr.value)
                is CountryV -> CountryVal(expr.value)
                is ProvinceV -> ProvinceVal(expr.value)
                is LogicalV -> LogicalVal(expr.op)
                is MissionV -> MissionVal(expr.name, expr.position, expr.icon, expr.triggers, expr.triggerScopeStack, expr.effects, expr.effectScopeStack)
                is Ref -> envV.tryGet(expr.name)!! // The static analysis ensures this value is never null

                is BinaryOp -> {
                    val v1 = evalExpr(expr.exprLeft, envV)
                    val v2 = evalExpr(expr.exprRight, envV)

                    return when (expr.op) {
                        BinaryOperators.ADD -> {
                            if (v1 is StringVal && v2 is StringVal) {
                                StringVal(v1.asString() + v2.asString())
                            } else if (v1 is IntVal && v2 is IntVal) {
                                IntVal(v1.asInt() + v2.asInt())
                            } else if (v1 is DoubleVal && v2 is DoubleVal){
                                DoubleVal(v1.asDouble() + v2.asDouble())
                            }  else if (v1 is IntVal && v2 is DoubleVal){
                                DoubleVal(v1.asInt() + v2.asDouble())
                            } else if (v1 is DoubleVal && v2 is IntVal){
                                DoubleVal(v1.asDouble() + v2.asInt())
                            } else {
                                error("Cannot add ${v1::class.simpleName} and ${v2::class.simpleName}")
                            }
                        }
                        BinaryOperators.SUB -> {
                            if (v1 is IntVal && v2 is IntVal) {
                                IntVal(v1.asInt() - v2.asInt())
                            } else if (v1 is DoubleVal && v2 is DoubleVal){
                                DoubleVal(v1.asDouble() - v2.asDouble())
                            }  else if (v1 is IntVal && v2 is DoubleVal){
                                DoubleVal(v1.asInt() - v2.asDouble())
                            } else if (v1 is DoubleVal && v2 is IntVal){
                                DoubleVal(v1.asDouble() - v2.asInt())
                            } else {
                                error("Cannot subtract ${v1::class.simpleName} and ${v2::class.simpleName}")
                            }
                        }
                        BinaryOperators.MUL -> {
                            if (v1 is IntVal && v2 is IntVal) {
                                IntVal(v1.asInt() * v2.asInt())
                            } else if (v1 is DoubleVal && v2 is DoubleVal){
                                DoubleVal(v1.asDouble() * v2.asDouble())
                            }  else if (v1 is IntVal && v2 is DoubleVal){
                                DoubleVal(v1.asInt() * v2.asDouble())
                            } else if (v1 is DoubleVal && v2 is IntVal){
                                DoubleVal(v1.asDouble() * v2.asInt())
                            } else {
                                error("Cannot multiply ${v1::class.simpleName} and ${v2::class.simpleName}")
                            }
                        }
                        BinaryOperators.LT -> {
                            if (v1 is IntVal && v2 is IntVal) {
                                BoolVal(v1.asInt() < v2.asInt())
                            } else if (v1 is DoubleVal && v2 is DoubleVal){
                                BoolVal(v1.asDouble() < v2.asDouble())
                            }  else if (v1 is IntVal && v2 is DoubleVal){
                                BoolVal(v1.asInt() < v2.asDouble())
                            } else if (v1 is DoubleVal && v2 is IntVal){
                                BoolVal(v1.asDouble() < v2.asInt())
                            } else {
                                error("Cannot compare ${v1::class.simpleName} and ${v2::class.simpleName}")
                            }
                        }
                        BinaryOperators.EQ -> BoolVal(v1 == v2) // IntVal and BoolVal are "data classes" and auto-generate "equals" based on member-values.
                        BinaryOperators.OR -> BoolVal(v1.asBool() || v2.asBool())
                    }
                }

                is FieldAccess -> {
                    val baseVal = evalExpr(expr.base, envV)
                    when (baseVal) {
                        is MissionVal -> {
                            when (expr.field) {
                                "name" -> StringVal(baseVal.name)
                                "position" -> IntVal(baseVal.position)
                                "icon" -> StringVal(baseVal.icon)
                                "triggers" -> StringVal(baseVal.triggers)
                                "triggerScopeStack" -> StringVal(baseVal.triggerScopeStack.joinToString(","))
                                "effects" -> StringVal(baseVal.effects)
                                "effectScopeStack" -> StringVal(baseVal.effectScopeStack.joinToString(","))
                                else -> error("Unknown field '${expr.field}' for mission")
                            }
                        }
                        is ArrayVal -> {
                            when (expr.field) {
                                "length" -> IntVal(baseVal.elements.size)
                                else -> error("Unknown field '${expr.field}' for array")
                            }
                        }
                        else -> error("Field access on unsupported value")
                    }
                }

                is UnaryOp -> when (expr.op) {
                    UnaryOperators.NOT -> BoolVal(!(evalExpr(expr.expr, envV).asBool()))
                    UnaryOperators.NEG -> {
                        if(evalExpr(expr.expr, envV) is IntVal){
                            IntVal(-(evalExpr(expr.expr, envV).asInt()))
                        } else if (evalExpr(expr.expr, envV) is DoubleVal){
                            DoubleVal(-(evalExpr(expr.expr, envV).asDouble()))
                        } else {
                            error("${evalExpr(expr.expr, envV)::class.simpleName} cannot be negative")
                        }
                    }
                }

                is ArrayLiteralExpr -> {
                    val vals = expr.elements.map { evalExpr(it, envV) }.toMutableList()
                    ArrayVal(vals)
                }

                is ArrayAccess -> {
                    // Evaluate the base and index
                    val base = evalExpr(expr.base, envV)
                    val idxVal = evalExpr(expr.index, envV)
                    if (base !is ArrayVal) error("Tried to index non-array value")
                    val idx = idxVal.asInt()
                    if (idx < 0 || idx >= base.elements.size)
                        error("Array index $idx out of bounds [0, ${base.elements.size})")
                    base.elements[idx]
                }
                is ArrayLit -> {
                    val vals = expr.elements.map { evalExpr(it, envV) }.toMutableList()
                    ArrayVal(vals)
                }
            }
        }
    }
}