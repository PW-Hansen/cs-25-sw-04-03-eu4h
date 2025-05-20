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
                                    "name" -> baseVal.name = value.asString()
                                    "position" -> baseVal.position = value.asInt()
                                    "icon" -> baseVal.icon = value.asString()
                                    "triggers" -> baseVal.triggers = value.asString()
                                    "effects" -> baseVal.effects = value.asString()
                                    else -> error("Unknown field '${lhs.field}' for mission")
                                }
                            } else {
                                error("Field assignment on non-mission value")
                            }
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
                    // TODO, scope compatibility check
                    val triggerAssignment = "\t\t${stmt.triggerName} = ${value}"
                    if (mission.triggers == "") {
                        mission.triggers = triggerAssignment
                    } else {
                        mission.triggers += "\n$triggerAssignment"
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
                is MissionV -> MissionVal(expr.name, expr.position, expr.icon, expr.triggers, expr.effects)
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
                    if (baseVal is MissionVal) {
                        when (expr.field) {
                            "name" -> StringVal(baseVal.name)
                            "position" -> IntVal(baseVal.position)
                            "icon" -> StringVal(baseVal.icon)
                            "triggers" -> StringVal(baseVal.triggers)
                            "effects" -> StringVal(baseVal.effects)
                            else -> error("Unknown field '${expr.field}' for mission")
                        }
                    } else {
                        error("Field access on non-mission value")
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
            }
        }
    }
}