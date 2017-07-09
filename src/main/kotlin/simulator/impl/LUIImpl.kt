package venus.simulator.impl

import venus.riscv.Instruction
import venus.riscv.InstructionField
import venus.simulator.SimulatorState
import venus.simulator.InstructionImplementation

object LUIImpl : InstructionImplementation {
    override operator fun invoke(inst: Instruction, state: SimulatorState) {
        val rd: Int = inst.getField(InstructionField.RD)
        val imm: Int = inst.getField(InstructionField.IMM_31_12) shl 12
        state.setReg(rd, imm)
        state.pc += inst.length
    }
}
