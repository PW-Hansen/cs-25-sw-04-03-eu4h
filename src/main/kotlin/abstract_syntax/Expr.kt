package org.sw_08.eu4h.abstract_syntax

/* When matching on a sealed interface in a switch-case, Kotlin will warn you if you forgot to match a possible case */
sealed interface Expr {
    val lineNumber: Int
}

class UnaryOp(var op: UnaryOperators, var expr: Expr, override val lineNumber: Int) : Expr

class BinaryOp(var op: BinaryOperators, var exprLeft: Expr, var exprRight: Expr, override val lineNumber: Int) : Expr


class Ref(var name: Var, override val lineNumber: Int) : Expr

class BoolV(var value: Boolean, override val lineNumber: Int) : Expr

class NumV(var value: Num, override val lineNumber: Int) : Expr

class DoubleV(var value: Double, override val lineNumber: Int) : Expr

class StringV(var value: String, override val lineNumber: Int) : Expr

class CountryV(val value: String, override val lineNumber: Int) : Expr

class ProvinceV(val value: String, override val lineNumber: Int) : Expr

data class ArrayLiteralExpr(val elements: List<Expr>, override val lineNumber: Int) : Expr

data class ArrayAccess(val base: Expr, val index: Expr, override val lineNumber: Int) : Expr

data class MissionV(
    val name: String,
    var position: Int,
    var icon: String,
    var triggers: String,
    var triggerScopeStack: MutableList<String>,
    var effects: String,
    var effectScopeStack: MutableList<String>,
    override val lineNumber: Int
) : Expr

class FieldAccess(val base: Expr, val field: String, override val lineNumber: Int) : Expr

data class ArrayLit(val elements: List<Expr>, override val lineNumber: Int) : Expr

class LogicalV(val op: LogicalOp, override val lineNumber: Int) : Expr

enum class UnaryOperators {
    NOT, NEG
}

enum class BinaryOperators {
    ADD, SUB, MUL, LT, EQ, OR, AND
}
