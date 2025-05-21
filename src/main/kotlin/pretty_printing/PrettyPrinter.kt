package org.sw_08.eu4h.pretty_printing

import org.sw_08.eu4h.abstract_syntax.*
import org.sw_08.eu4h.interpretation.MissionVal

fun autoIndent(block: String, baseIndent: String = "\t"): String {
    val lines = block.lines()
    val result = StringBuilder()
    var indentLevel = 0
    for (rawLine in lines) {
        val line = rawLine.trimEnd().trimIndent()
        // Decrease indent before lines ending with }
        val trimmed = line.trimStart()
        if (trimmed.startsWith("}")) {
            indentLevel = (indentLevel - 1).coerceAtLeast(0)
        }
        result.append(baseIndent.repeat(indentLevel)).append(line).append("\n")
        // Increase indent after lines ending with {
        if (trimmed.endsWith("{")) indentLevel++
    }
    return result.toString().trimEnd()
}

class PrettyPrinter {
    companion object {
        fun printStmt(stmt: Stmt?, depth: Int = 0): String =
            when(stmt) {
                null, Skip     -> ""
                is Comp        -> printStmt(stmt.stmt1, depth) + "\n" + printStmt(stmt.stmt2, depth)
                is Declaration -> indent(depth) + printType(stmt.type) + " " + (stmt.identifier ?: "") + ";"
                is Assign      -> indent(depth) + printExpr(stmt.lhs) + " = " + printExpr(stmt.value) + ";"
                is Print       -> indent(depth) + "print " + printExpr(stmt.value) + ";"
                is If          -> {
                    indent(depth) + "if (" + printExpr(stmt.condition) + ") then \n" + printStmt(stmt.thenBody, depth + 1) + "\n" + (
                        if (stmt.elseBody != Skip)
                            indent(depth) + "else\n" + printStmt(stmt.elseBody, depth + 1) + "\n"
                        else
                            ""
                    ) + indent(depth) + "endif\n"
                }
                is While       -> {
                    indent(depth) + "while (" + printExpr(stmt.condition) + ") do \n" + (
                        printStmt(stmt.body, depth + 1)
                    ) + "\n" + indent(depth) + "endwhile\n"
                }
                is CreateTrigger -> indent(depth) +
                    "create_trigger = (" +
                    stmt.scope + ", " +
                    "\"" + stmt.name + "\", " +
                    printType(stmt.type) +
                    ");"
                is AssignTrigger -> indent(depth) +
                    "assign_trigger = (" +
                    stmt.missionName + ", " +
                    "\"" + stmt.triggerName + "\", " +
                    printExpr(stmt.expr) +
                    ");"
                
                is OpenScope -> indent(depth) +
                    "open_scope = (" +
                    stmt.missionName + ", " +
                    "\"" + stmt.spaceName + "\", " +
                    stmt.scope +
                    ");"
                is CloseScope -> indent(depth) +
                    "close_scope = (" +
                    stmt.missionName + ", " +
                    "\"" + stmt.spaceName + "\"" +
                    ");"
            }

        fun printExpr(expr: Expr?): String =
            when(expr) {
                null -> ""
                is UnaryOp -> unaryOpString(expr.op) + surround(expr.expr)
                is BinaryOp -> surround(expr.exprLeft) + binaryOpString(expr.op) + surround(expr.exprRight)
                is Ref -> expr.name
                is BoolV -> expr.value.toString()
                is NumV -> expr.value.toString()
                is DoubleV -> expr.value.toString()
                is StringV -> expr.value.toString()
                is CountryV -> expr.value.toString()
                is ProvinceV -> expr.value.toString()
                is MissionV -> "Mission(${expr.name}, ${expr.position}, ${expr.icon}, ${expr.triggers}, ${expr.triggerScope}, ${expr.effects}, ${expr.effectScope})"
                is FieldAccess -> printExpr(expr.base) + "." + expr.field
            }

        fun printType(type: Type?): String =
            when(type) {
                null -> ""
                BoolT -> "bool"
                IntT -> "int"
                DoubleT -> "double"
                StringT -> "string"
                CountryT -> "country"
                ProvinceT -> "province"
                MissionT -> "mission"
            }


        private fun indent(depth: Int)
        = "    ".repeat(depth)

        fun binaryOpString(op: BinaryOperators): String =
            when (op) {
                BinaryOperators.ADD -> " + "
                BinaryOperators.SUB -> " - "
                BinaryOperators.MUL -> " * "
                BinaryOperators.LT -> " < "
                BinaryOperators.EQ -> " = "
                BinaryOperators.OR -> " || "
            }

        fun unaryOpString(op: UnaryOperators): String =
            when (op) {
                UnaryOperators.NOT -> "!"
                UnaryOperators.NEG -> "-"
            }

        private fun surround(expr: Expr?) =
            if (expr is BinaryOp)
                "(${printExpr(expr)})"
            else
                printExpr(expr)

        fun printMissionBlock(mission: MissionVal): String {
            val raw = """
                ${mission.name} = {
                position = ${mission.position}
                icon = ${mission.icon}

                required_missions =  {
                }

                provinces_to_highlight = {
                }

                trigger = {
                ${mission.triggers}
                }

                effect = {
                ${mission.effects}
                }
                }
            """.trimIndent()
            return autoIndent(raw)
        }
    }
}