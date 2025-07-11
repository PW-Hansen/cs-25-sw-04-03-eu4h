package org.sw_08.eu4h.semantic_analysis

import org.sw_08.eu4h.abstract_syntax.*
import org.sw_08.eu4h.pretty_printing.PrettyPrinter


class AssignAndTypeChecker {
    val errors: MutableList<String> = mutableListOf()

    val hasErrors get() = errors.isNotEmpty()

    // Keeps track of what types are permitted for triggers.
    private val triggerTypes = mutableMapOf<String, Type>()


    constructor(stmt: Stmt, envAT: EnvAT) {
        stmtT(stmt, envAT)
    }

    constructor(expr: Expr, envAT: EnvAT) {
        exprT(expr, envAT)
    }


    private fun stmtT(stmt: Stmt, envAT: EnvAT): Unit {
        when (stmt) {
            Skip -> { /* Nothing to do here */ }

            is Print -> exprT(stmt.value!!, envAT) // Anything is fine as long as the value-expression is well-formed.

            is Assign -> {
                val exprType = exprT(stmt.value, envAT)
                when (val lhs = stmt.lhs) {
                    is Ref -> {
                        val at = envAT.tryGet(lhs.name)
                        if (at == null)
                            errors.add("Line ${stmt.lineNumber}: Assignment to undeclared variable '${lhs.name}'")
                        else {
                            at.isAssigned = true
                            if (exprType != null && exprType.javaClass != at.type.javaClass)
                                errors.add("Line ${stmt.lineNumber}: Assignment has variable with type '${PrettyPrinter.printType(at.type)}' but expression with type '${PrettyPrinter.printType(exprType)}'.")
                        }
                    }
                    is FieldAccess -> {
                        val baseType = exprT(lhs.base, envAT)
                        if (baseType !is MissionT) {
                            errors.add("Line ${stmt.lineNumber}: Field assignment on non-mission value '${PrettyPrinter.printType(baseType)}'.")
                        } else {
                            val fieldType = when (lhs.field) {
                                "name", "icon", "triggers", "triggerScopeStack", "effects", "effectScopeStack" -> StringT
                                "position" -> IntT
                                else -> {
                                    errors.add("Line ${stmt.lineNumber}: Unknown field '${lhs.field}' for mission.")
                                    null
                                }
                            }
                            if (exprType != null && fieldType != null && exprType.javaClass != fieldType.javaClass)
                                errors.add("Line ${stmt.lineNumber}: Assignment to field '${lhs.field}' expects type '${PrettyPrinter.printType(fieldType)}', but got '${PrettyPrinter.printType(exprType)}'.")
                        }
                    }
                    else -> errors.add("Line ${stmt.lineNumber}: Invalid assignment target.")
                }
            }

            is Comp -> {
                stmtT(stmt.stmt1!!, envAT)
                stmtT(stmt.stmt2!!, envAT)
            }

            is Declaration -> {
                if (envAT.isLocal(stmt.identifier!!))
                    errors.add("Line ${stmt.lineNumber}: Redeclaration of variable '${stmt.identifier}' is same scope.")
                else
                    envAT.bind(stmt.identifier!!, AT(false, stmt.type!!)) // Declared variable is not assigned by default, thus 'false'.
            }

            is If -> {
                val exprType = exprT(stmt.condition!!, envAT)
                if (exprType != null && exprType !is BoolT)
                    errors.add("Line ${stmt.condition!!.lineNumber}: If statement requires a condition with type 'bool' but got '${PrettyPrinter.printType(exprType)}'.")

                val envAT1 = envAT.clone()
                val envAT2 = envAT.clone()
                stmtT(stmt.thenBody!!, envAT1.newScope()) // The 'then'- and 'else'-bodies gets their own scopes.
                stmtT(stmt.elseBody!!, envAT2.newScope())

                // Only propagate assignments which are guaranteed to happen in both branches.
                for (variable in envAT.domain())
                    envAT.tryGet(variable)!!.isAssigned =
                        envAT1.tryGet(variable)!!.isAssigned && envAT2.tryGet(variable)!!.isAssigned
            }

            is While -> {
                val exprType = exprT(stmt.condition!!, envAT)
                if (exprType != null && exprType !is BoolT)
                    errors.add("Line ${stmt.condition!!.lineNumber}: While statement requires a condition with type 'bool' but got '${PrettyPrinter.printType(exprType)}'.")

                // The body gets its own scope. Also, the scope is cloned since neither assignments nor declarations propagate out of a while-loop.
                stmtT(stmt.body!!, envAT.clone().newScope())
            }

            is PushStmt -> {
                val at = envAT.tryGet(stmt.arrayName)
                if (at == null)
                    errors.add("Line ${stmt.lineNumber}: Push to undeclared variable '${stmt.arrayName}'")
                else if (at.type !is ArrayT)
                    errors.add("Line ${stmt.lineNumber}: Push to non-array variable '${stmt.arrayName}'")
                else {
                    val valueType = exprT(stmt.value, envAT)
                    // Type check?
                }
            }
            is PopStmt -> {
                val at = envAT.tryGet(stmt.arrayName)
                if (at == null)
                    errors.add("Line ${stmt.lineNumber}: Pop from undeclared variable '${stmt.arrayName}'")
                else if (at.type !is ArrayT)
                    errors.add("Line ${stmt.lineNumber}: Pop from non-array variable '${stmt.arrayName}'")
            }

            is CreateTrigger -> {
                if (triggerTypes.containsKey(stmt.name)) {
                    errors.add("Line ${stmt.lineNumber}: Trigger '${stmt.name}' is already defined.")
                } else {
                    triggerTypes[stmt.name] = stmt.type
                }
            }

            is AssignTrigger -> {
                val exprType = exprT(stmt.expr, envAT)
                val expectedType = triggerTypes[stmt.triggerName]
                if (expectedType == null) {
                    errors.add("Line ${stmt.lineNumber}: Trigger '${stmt.triggerName}' is not defined.")
                } else if (exprType != null && exprType.javaClass != expectedType.javaClass) {
                    errors.add("Line ${stmt.lineNumber}: Trigger '${stmt.triggerName}' expects type '${PrettyPrinter.printType(expectedType)}', but got '${PrettyPrinter.printType(exprType)}'.")
                }
            }

            is CreateEffect -> {
                if (triggerTypes.containsKey(stmt.name)) {
                    errors.add("Line ${stmt.lineNumber}: Effect '${stmt.name}' is already defined.")
                } else {
                    triggerTypes[stmt.name] = stmt.type
                }
            }

            is AssignEffect -> {
                val exprType = exprT(stmt.expr, envAT)
                val expectedType = triggerTypes[stmt.effectName]
                if (expectedType == null) {
                    errors.add("Line ${stmt.lineNumber}: Effect '${stmt.effectName}' is not defined.")
                } else if (exprType != null && exprType.javaClass != expectedType.javaClass) {
                    errors.add("Line ${stmt.lineNumber}: Effect '${stmt.effectName}' expects type '${PrettyPrinter.printType(expectedType)}', but got '${PrettyPrinter.printType(exprType)}'.")
                }
            }

            is OpenScope -> {
                val exprType = exprT(stmt.scope, envAT)
                if (exprType == null) {
                    errors.add("Line ${stmt.lineNumber}: Scope expression is not well-typed.")
                } else if (exprType != CountryT && exprType != ProvinceT && exprType != LogicalT) {
                    errors.add("Line ${stmt.lineNumber}: Scope expression must be of type country, province, or logical.")
                }
            }

            is CloseScope -> {
                // No checks required here.
            }
        }
    }

    private fun exprT(expr: Expr, envAT: EnvAT): Type? { // Returns "nullable Type" since we cannot type un-bound variables
        return when (expr) {
            is BoolV -> BoolT
            is NumV -> IntT
            is DoubleV -> DoubleT
            is StringV -> StringT
            is CountryV -> CountryT
            is ProvinceV -> ProvinceT
            is LogicalV -> LogicalT
            is MissionV -> MissionT
            is ArrayLiteralExpr -> ArrayT

            is FieldAccess -> {
                val baseType = exprT(expr.base, envAT)
                if (baseType is MissionT) {
                    return when (expr.field) {
                        "name" -> StringT
                        "position" -> IntT
                        "icon" -> StringT
                        "triggers" -> StringT
                        "triggerScopeStack" -> StringT
                        "effects" -> StringT
                        "effectScopeStack" -> StringT
                        else -> error("Unknown field '${expr.field}' for mission")
                    }
                } else if (baseType is ArrayT) {
                    return when (expr.field) {
                        "length" -> IntT
                        else -> error("Unknown field '${expr.field}' for array")
                    }
                } else {
                    errors.add("Line ${expr.lineNumber}: Field access on non-mission/non-array value '${PrettyPrinter.printType(baseType)}'.")
                    null
                }
            }

            is Ref -> {
                val at = envAT.tryGet(expr.name)

                if (at == null)
                    errors.add("Line ${expr.lineNumber}: Use of declared variable '${expr.name}'.")
                else if (!at.isAssigned)
                    errors.add("Line ${expr.lineNumber}: Use of unassigned variable '${expr.name}'.")

                return at?.type
            }

            is ArrayAccess -> {
                val baseType = exprT(expr.base, envAT)
                val idxType = exprT(expr.index, envAT)
                if (baseType !is ArrayT) {
                    errors.add("Line ${expr.lineNumber}: Tried to index non-array expression of type '${PrettyPrinter.printType(baseType)}'.")
                    return null
                }
                if (idxType !is IntT) {
                    errors.add("Line ${expr.lineNumber}: Array index must be of type 'int', got '${PrettyPrinter.printType(idxType)}'.")
                    return null
                }
                return null
            }

            is ArrayLit -> {
                if (expr.elements.isEmpty()) {
                    return ArrayT
                }
                val firstType = exprT(expr.elements[0], envAT)
                for (el in expr.elements) {
                    val elType = exprT(el, envAT)
                    if (elType != null && elType.javaClass != firstType?.javaClass) {
                        errors.add("Line ${expr.lineNumber}: Array elements have inconsistent types: '${PrettyPrinter.printType(firstType)}' vs '${PrettyPrinter.printType(elType)}'.")
                    }
                }
                return ArrayT
            }

            is BinaryOp -> {
                val typeL = exprT(expr.exprLeft, envAT)
                val typeR = exprT(expr.exprRight, envAT)

                when (expr.op) {
                    BinaryOperators.ADD -> {
                        if (typeL != null && typeR != null) {
                            if (typeL is IntT && typeR is IntT) {
                                // Accepted
                            } else if (typeL is DoubleT && typeR is DoubleT) {
                                // Accepted
                            } else if (typeL is DoubleT && typeR is IntT) {
                                // Accepted
                            } else if (typeL is IntT && typeR is DoubleT) {
                                // Accepted
                            } else if (typeL is StringT && typeR is StringT) {
                                // Accepted
                            } else {
                                errors.add("Line ${expr.lineNumber}: Operator '+' expected operands of the same type (int or string), but got '${PrettyPrinter.printType(typeL)}' and '${PrettyPrinter.printType(typeR)}'.")
                            }
                        }
                    }
                    BinaryOperators.SUB,
                    BinaryOperators.MUL,
                    BinaryOperators.LT -> {
                        if (typeL != null && typeL !is IntT && typeL !is DoubleT)
                            errors.add("Line ${expr.exprLeft.lineNumber}: Operator '${PrettyPrinter.binaryOpString(expr.op)}' expected a left operand of type 'int' or 'double', but got '${PrettyPrinter.printType(typeL)}'.")
                        if (typeR != null && typeR !is IntT && typeR !is DoubleT)
                            errors.add("Line ${expr.exprRight.lineNumber}: Operator '${PrettyPrinter.binaryOpString(expr.op)}' expected a right operand of type 'int' or 'double', but got '${PrettyPrinter.printType(typeL)}'.")
                    }
                    BinaryOperators.OR -> {
                        if (typeL != null && typeL !is BoolT)
                            errors.add("Line ${expr.exprLeft.lineNumber}: Operator '${PrettyPrinter.binaryOpString(expr.op)}' expected a left operand of type 'bool', but got '${PrettyPrinter.printType(typeL)}'.")
                        if (typeR != null && typeR !is BoolT)
                            errors.add("Line ${expr.exprRight.lineNumber}: Operator '${PrettyPrinter.binaryOpString(expr.op)}' expected a right operand of type 'bool', but got '${PrettyPrinter.printType(typeL)}'.")
                    }
                    BinaryOperators.AND -> {
                        if (typeL != null && typeL !is BoolT)
                            errors.add("Line ${expr.exprLeft.lineNumber}: Operator '${PrettyPrinter.binaryOpString(expr.op)}' expected a left operand of type 'bool', but got '${PrettyPrinter.printType(typeL)}'.")
                        if (typeR != null && typeR !is BoolT)
                            errors.add("Line ${expr.exprRight.lineNumber}: Operator '${PrettyPrinter.binaryOpString(expr.op)}' expected a right operand of type 'bool', but got '${PrettyPrinter.printType(typeL)}'.")
                    }
                    BinaryOperators.EQ -> {
                        if (typeL != null && typeR != null && typeL.javaClass != typeR.javaClass)
                            errors.add("Line ${expr.lineNumber}: Operator '${PrettyPrinter.binaryOpString(expr.op)}' expected operands of the same type, but got '${PrettyPrinter.printType(typeL)}' and '${PrettyPrinter.printType(typeR)}'.")
                    }
                }

                return when (expr.op) {
                    BinaryOperators.ADD -> {
                        if (typeL is IntT && typeR is IntT) { IntT }
                        else if (typeL is DoubleT && typeR is DoubleT) { DoubleT }
                        else if (typeL is DoubleT && typeR is IntT) { DoubleT }
                        else if (typeL is IntT && typeR is DoubleT) { DoubleT }
                        else if (typeL is StringT && typeR is StringT)  { StringT }
                        else { null }
                    }
                    BinaryOperators.SUB, BinaryOperators.MUL -> {
                        if (typeL is IntT && typeR is IntT) { IntT }
                        else if (typeL is DoubleT && typeR is DoubleT) { DoubleT }
                        else if (typeL is DoubleT && typeR is IntT) { DoubleT }
                        else if (typeL is IntT && typeR is DoubleT) { DoubleT }
                        else { null }
                    }
                    BinaryOperators.LT, BinaryOperators.EQ, BinaryOperators.OR, BinaryOperators.AND -> BoolT
                }
            }

            is UnaryOp -> {
                val type = exprT(expr.expr, envAT)

                when (expr.op) {
                    UnaryOperators.NOT -> {
                        if (type != null && type !is BoolT)
                            errors.add("Line ${expr.lineNumber}: Operator '${PrettyPrinter.unaryOpString(expr.op)}' expected an operand of type 'bool', but got '${PrettyPrinter.printType(type)}'.")
                    }
                    UnaryOperators.NEG -> {
                        if (type != null && type !is IntT && type !is DoubleT)
                            errors.add("Line ${expr.lineNumber}: Operator '${PrettyPrinter.unaryOpString(expr.op)}' expected an operand of type 'int' or 'double', but got '${PrettyPrinter.printType(type)}'.")
                    }
                }

                return when (expr.op) {
                    UnaryOperators.NOT -> BoolT
                    UnaryOperators.NEG -> {
                        if(type is IntT){ IntT }
                        else if (type is DoubleT) { DoubleT }
                        else { null }
                    }
                }
            }
        }
    }
}