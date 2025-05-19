import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sw_08.eu4h.abstract_syntax.BinaryOp
import org.sw_08.eu4h.abstract_syntax.BinaryOperators
import org.sw_08.eu4h.abstract_syntax.NumV
import org.sw_08.eu4h.abstract_syntax.DoubleV
import org.sw_08.eu4h.abstract_syntax.StringV
import org.sw_08.eu4h.abstract_syntax.BoolV
import org.sw_08.eu4h.interpretation.EnvV
import org.sw_08.eu4h.interpretation.Interpreter

class InterpreterBasicTypesTest {

    private val env = EnvV()

    @Test
    fun testCreateInt() {
        val expr = NumV(42, 1)
        val result = Interpreter.evalExpr(expr, env)
        assertEquals(42, result.asInt())
    }

    @Test
    fun testCreateDouble() {
        val expr = DoubleV(3.14, 1)
        val result = Interpreter.evalExpr(expr, env)
        assertEquals(3.14, result.asDouble())
    }

    @Test
    fun testCreateString() {
        val expr = StringV("hello", 1)
        val result = Interpreter.evalExpr(expr, env)
        assertEquals("hello", result.asString())
    }

    @Test
    fun testCreateBool() {
        val expr = BoolV(true, 1)
        val result = Interpreter.evalExpr(expr, env)
        assertEquals(true, result.asBool())
    }

    @Test
    fun testAddIntAndInt() {
        val expr = BinaryOp(BinaryOperators.ADD, NumV(1, 1), NumV(2, 1), 1)
        val result = Interpreter.evalExpr(expr, env)
        assertEquals(3, result.asInt())
    }

    @Test
    fun testAddStringAndString() {
        val expr = BinaryOp(BinaryOperators.ADD, StringV("a", 1), StringV("b", 1), 1)
        val result = Interpreter.evalExpr(expr, env)
        assertEquals("ab", result.asString())
    }

    @Test
    fun testAddDoubleAndDouble() {
        val expr = BinaryOp(BinaryOperators.ADD, DoubleV(1.5, 1), DoubleV(2.5, 1), 1)
        val result = Interpreter.evalExpr(expr, env)
        assertEquals(4.0, result.asDouble())
    }

    @Test
    fun testAddIntAndDouble() {
        val expr = BinaryOp(BinaryOperators.ADD, NumV(1, 1), DoubleV(2.5, 1), 1)
        val result = Interpreter.evalExpr(expr, env)
        assertEquals(3.5, result.asDouble())
    }

    @Test
    fun testAddDoubleAndInt() {
        val expr = BinaryOp(BinaryOperators.ADD, DoubleV(1.5, 1), NumV(2, 1), 1)
        val result = Interpreter.evalExpr(expr, env)
        assertEquals(3.5, result.asDouble())
    }

    @Test
    fun testAddBoolAndIntFails() {
        val expr = BinaryOp(BinaryOperators.ADD, BoolV(true, 1), NumV(1, 1), 1)
        assertThrows<IllegalStateException> {
            Interpreter.evalExpr(expr, env)
        }
    }

    @Test
    fun testAddStringAndIntFails() {
        val expr = BinaryOp(BinaryOperators.ADD, StringV("a", 1), NumV(1, 1), 1)
        assertThrows<IllegalStateException> {
            Interpreter.evalExpr(expr, env)
        }
    }

    @Test
    fun testAddIntAndStringFails() {
        val expr = BinaryOp(BinaryOperators.ADD, NumV(1, 1), StringV("a", 1), 1)
        assertThrows<IllegalStateException> {
            Interpreter.evalExpr(expr, env)
        }
    }
}