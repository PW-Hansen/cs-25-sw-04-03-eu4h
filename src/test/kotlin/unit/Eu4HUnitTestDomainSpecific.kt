import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.sw_08.eu4h.abstract_syntax.*
import org.sw_08.eu4h.interpretation.*

class InterpreterDomainSpecificTest {

    private val env = EnvV()

    @Test
    fun testCreateMission() {
        val mission = MissionV(
            name = "test_mission",
            position = 1,
            icon = "mission_icon",
            triggers = "",
            triggerScopeStack = mutableListOf("country"),
            effects = "",
            effectScopeStack = mutableListOf("country"),
            lineNumber = 1
        )
        val result = Interpreter.evalExpr(mission, env)
        assertTrue(result is MissionVal)
        assertEquals("test_mission", (result as MissionVal).name)
        assertEquals(1, result.position)
        assertEquals("mission_icon", result.icon)
        assertEquals(listOf("country"), result.triggerScopeStack)
        assertEquals(listOf("country"), result.effectScopeStack)
    }

    @Test
    fun testCreateTrigger() {
        Interpreter.triggers.clear()
        val stmt = CreateTrigger("country", "army_size", IntT, 1)
        Interpreter.evalStmt(stmt, env)
        assertTrue(Interpreter.triggers.containsKey("army_size"))
        val triggerDef = Interpreter.triggers["army_size"]!!
        assertEquals("country", triggerDef.scope)
        assertEquals(IntT, triggerDef.type)
    }

    @Test
    fun testCreateEffect() {
        Interpreter.effects.clear()
        val stmt = CreateEffect("province", "add_dev", IntT, 1)
        Interpreter.evalStmt(stmt, env)
        assertTrue(Interpreter.effects.containsKey("add_dev"))
        val effectDef = Interpreter.effects["add_dev"]!!
        assertEquals("province", effectDef.scope)
        assertEquals(IntT, effectDef.type)
    }

    @Test
    fun testCreateTriggerDuplicateThrows() {
        Interpreter.triggers.clear()
        val stmt = CreateTrigger("country", "army_size", IntT, 1)
        Interpreter.evalStmt(stmt, env)
        val duplicate = CreateTrigger("country", "army_size", IntT, 2)
        val exception = assertThrows<IllegalStateException> {
            Interpreter.evalStmt(duplicate, env)
        }
        assertTrue(exception.message!!.contains("already defined") || exception.message!!.contains("already exists"))
    }

    @Test
    fun testCreateEffectDuplicateThrows() {
        Interpreter.effects.clear()
        val stmt = CreateEffect("province", "add_dev", IntT, 1)
        Interpreter.evalStmt(stmt, env)
        val duplicate = CreateEffect("province", "add_dev", IntT, 2)
        val exception = assertThrows<IllegalStateException> {
            Interpreter.evalStmt(duplicate, env)
        }
        assertTrue(exception.message!!.contains("already defined") || exception.message!!.contains("already exists"))
    }
}