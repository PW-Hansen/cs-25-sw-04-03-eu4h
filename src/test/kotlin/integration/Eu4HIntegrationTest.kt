import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.sw_08.eu4h.abstract_syntax.AssignTrigger
import org.sw_08.eu4h.abstract_syntax.NumV
import org.sw_08.eu4h.abstract_syntax.StringV
import org.sw_08.eu4h.abstract_syntax.IntT
import org.sw_08.eu4h.interpretation.Interpreter
import org.sw_08.eu4h.interpretation.EnvV
import org.sw_08.eu4h.interpretation.MissionVal
import org.sw_08.eu4h.interpretation.TriggerDef
import syntactic_analysis.Parser
import syntactic_analysis.Scanner

class Eu4hIntegrationTest {

    @BeforeEach
    fun setup() {
        Interpreter.missions.clear()
        Interpreter.triggers.clear()
    }

    @Test
    fun testAssignTriggerSucceedsWithCorrectInput() {
        // Set up a mission and a trigger expecting an Int, and pass an Int
        Interpreter.missions["my_mission"] = MissionVal("my_mission", 0, "", "", "", effects = "", effectScope = "")
        Interpreter.triggers["army_size"] = TriggerDef("country", IntT)
        val env = EnvV()
        val stmt = AssignTrigger("my_mission", "army_size", NumV(100, 1), 1)

        // Should not throw any exception
        assertDoesNotThrow {
            Interpreter.evalStmt(stmt, env)
        }
    }
    @Test
    fun testAssignTriggerFailsIfMissionNotDefined() {
        // Set up a trigger, but do NOT set up a mission
        Interpreter.triggers["army_size"] = TriggerDef("country", IntT)
        val env = EnvV()
        val stmt = AssignTrigger("missing_mission", "army_size", NumV(100, 1), 1)

        val exception = assertThrows<IllegalStateException> {
            Interpreter.evalStmt(stmt, env)
        }
        assertTrue(exception.message!!.contains("Mission 'missing_mission' not found"))
    }

    @Test
    fun testAssignTriggerFailsIfTriggerNotDefined() {
        // Set up a mission, but do NOT set up a trigger
        Interpreter.missions["my_mission"] = MissionVal("my_mission", 0, "", "", "", effects = "", effectScope = "")
        val env = EnvV()
        val stmt = AssignTrigger("my_mission", "missing_trigger", NumV(100, 1), 1)

        val exception = assertThrows<IllegalStateException> {
            Interpreter.evalStmt(stmt, env)
        }
        assertTrue(exception.message!!.contains("Trigger 'missing_trigger' not found"))
    }
    @Test
    fun testAssignTriggerFailsIfWrongInputType() {
        // Set up a mission and a trigger expecting an Int, but pass a String
        Interpreter.missions["my_mission"] = MissionVal("my_mission", 0, "", "", "", effects = "", effectScope = "")
        Interpreter.triggers["army_size"] = TriggerDef("country", IntT)
        val env = EnvV()
        val stmt = AssignTrigger("my_mission", "army_size", StringV("not a number", 1), 1)

        val exception = assertThrows<IllegalStateException> {
            Interpreter.evalStmt(stmt, env)
        }
        assertTrue(exception.message!!.contains("Type mismatch"))
    }
}