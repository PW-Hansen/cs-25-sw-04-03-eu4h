package org.sw_08.eu4h.interpretation

import org.sw_08.eu4h.abstract_syntax.*

fun typeMatches(type: Type, value: Val): Boolean {
    return when (type) {
        is IntT -> value is IntVal
        is BoolT -> value is BoolVal
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
                    println(value.toString())
                }

                is Assign -> {
                    val value = evalExpr(stmt.value!!, envV)
                    envV.set(stmt.identifier!!, value)
                    if (value is MissionVal) {
                        // TODO discuss whether only identifier should be added to the map.
                        missions[stmt.identifier!!] = value
                        missions[value.name] = value
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
                    // TODO, applying trigger to mission
                }
            }
        }

        fun evalExpr(expr: Expr, envV: EnvV): Val {
            return when (expr) {
                is BoolV -> BoolVal(expr.value)
                is NumV -> IntVal(expr.value)
                is StringV -> StringVal(expr.value)
                is CountryV -> CountryVal(expr.value)
                is ProvinceV -> ProvinceVal(expr.value)
                is MissionV -> MissionVal(expr.name, expr.position, expr.icon, expr.triggers, expr.effects)
                is Ref -> envV.tryGet(expr.name)!! // The static analysis ensures this value is never null

                is BinaryOp -> {
                    val v1 = evalExpr(expr.exprLeft, envV)
                    val v2 = evalExpr(expr.exprRight, envV)

                    return when (expr.op) {
                        BinaryOperators.ADD -> IntVal(v1.asInt() + v2.asInt())
                        BinaryOperators.SUB -> IntVal(v1.asInt() - v2.asInt())
                        BinaryOperators.MUL -> IntVal(v1.asInt() * v2.asInt())
                        BinaryOperators.LT -> BoolVal(v1.asInt() < v2.asInt())
                        BinaryOperators.EQ -> BoolVal(v1 == v2) // IntVal and BoolVal are "data classes" and auto-generate "equals" based on member-values.
                        BinaryOperators.OR -> BoolVal(v1.asBool() || v2.asBool())
                    }
                }

                is UnaryOp -> when (expr.op) {
                    UnaryOperators.NOT -> BoolVal(!(evalExpr(expr.expr, envV).asBool()))
                    UnaryOperators.NEG -> IntVal(-(evalExpr(expr.expr, envV).asInt()))
                }
            }
        }
    }
}