import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sw_08.eu4h.abstract_syntax.*
import org.sw_08.eu4h.interpretation.*

class Eu4HOpenCloseScopeTests {

    private val env = EnvV()

    @BeforeEach
    fun setup() {
        Interpreter.missions.clear()
    }

    @Test
    fun testOpenScopeAddsToTriggerScopeStack() {
        Interpreter.missions["my_mission"] = MissionVal(
            "my_mission", 0, "", "", mutableListOf("country"), effects = "", effectScopeStack = mutableListOf("country")
        )
        // Open a province scope
        Interpreter.evalStmt(OpenScope("my_mission", "trigger", ProvinceV("151", 1), 1), env)
        assertEquals(listOf("country", "province"), Interpreter.missions["my_mission"]!!.triggerScopeStack)
    }

    @Test
    fun testCloseScopeRemovesFromTriggerScopeStack() {
        Interpreter.missions["my_mission"] = MissionVal(
            "my_mission", 0, "", "", mutableListOf("country", "province"), effects = "", effectScopeStack = mutableListOf("country")
        )
        // Close the scope
        Interpreter.evalStmt(CloseScope("my_mission", "trigger", 1), env)
        assertEquals(listOf("country"), Interpreter.missions["my_mission"]!!.triggerScopeStack)
    }

    @Test
    fun testCloseScopeOnRootThrows() {
        Interpreter.missions["my_mission"] = MissionVal(
            "my_mission", 0, "", "", mutableListOf("country"), effects = "", effectScopeStack = mutableListOf("country")
        )
        val exception = assertThrows<IllegalStateException> {
            Interpreter.evalStmt(CloseScope("my_mission", "trigger", 1), env)
        }
        assertTrue(exception.message!!.contains("Cannot close the ROOT scope"))
    }

    @Test
    fun testOpenScopeWithLogicalRepeatsScope() {
        Interpreter.missions["my_mission"] = MissionVal(
            "my_mission", 0, "", "", mutableListOf("country"), effects = "", effectScopeStack = mutableListOf("country")
        )
        // Open a logical scope (should repeat last scope)
        Interpreter.evalStmt(OpenScope("my_mission", "trigger", LogicalV(LogicalOp.AND, 1), 1), env)
        assertEquals(listOf("country", "country"), Interpreter.missions["my_mission"]!!.triggerScopeStack)
    }

    @Test
    fun testOpenAndCloseEffectScope() {
        Interpreter.missions["my_mission"] = MissionVal(
            "my_mission", 0, "", "", mutableListOf("country"), effects = "", effectScopeStack = mutableListOf("country")
        )
        // Open effect scope
        Interpreter.evalStmt(OpenScope("my_mission", "effect", CountryV("FRA", 1), 1), env)
        assertEquals(listOf("country", "country"), Interpreter.missions["my_mission"]!!.effectScopeStack)
        // Close effect scope
        Interpreter.evalStmt(CloseScope("my_mission", "effect", 1), env)
        assertEquals(listOf("country"), Interpreter.missions["my_mission"]!!.effectScopeStack)
    }
}