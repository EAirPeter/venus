package venus.assembler

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import venus.simulator.Simulator

class AssemblerTest {
    @Test
    fun assembleLexerTest() {
        val prog = Assembler.assemble("""
        addi x1 x0 5
        addi x2 x1 5
        add x3 x1 x2
        andi x3 x3 8
        """)
        var sim = Simulator(prog.dump())
        sim.run()
        assertEquals(8, sim.state.getReg(3))
    }

    @Test
    fun storeLoadTest() {
        val prog = Assembler.assemble("""
        addi x1 x0 100
        sw 60(x0) x1
        lw x2 -40(x1)
        """)
        var sim = Simulator(prog.dump())
        assertTrue(sim.step())
        assertEquals(100, sim.state.getReg(1))
        assertTrue(sim.step())
        assertEquals(100, sim.state.getReg(1))
        assertEquals(100, sim.state.mem.loadWord(60))
        assertTrue(sim.step())
        //assertEquals(100, sim.state.getReg(1))
        assertEquals(100, sim.state.getReg(2))
    }

    @Test
    fun branchTest() {
        val prog = Assembler.assemble("""
        add x2 x2 x3
        addi x1 x0 5
        start: add x2 x2 x3
        addi x3 x3 1
        bne x3 x1 start
        """)
        var sim = Simulator(prog.dump())
        for (i in 1..16) assertTrue(sim.step())
        assertEquals(10, sim.state.getReg(2))
    }
}