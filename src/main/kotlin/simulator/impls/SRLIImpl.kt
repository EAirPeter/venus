package venus.simulator.impls

import venus.riscv.Instruction
import venus.riscv.InstructionField
import venus.simulator.SimulatorState
import venus.simulator.InstructionImplementation

object SRLIImpl : InstructionImplementation {
    override operator fun invoke(inst: Instruction, state: SimulatorState) {
        val rs1: Int = inst.getField(InstructionField.RS1)
        val shamt: Int = inst.getField(InstructionField.SHAMT)
        val rd: Int = inst.getField(InstructionField.RD)
        state.setReg(rd, state.getReg(rs1) ushr shamt)
        state.pc += inst.length
    }
}