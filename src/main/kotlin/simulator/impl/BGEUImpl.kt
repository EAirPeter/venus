package venus.simulator.impl

import venus.riscv.Instruction
import venus.riscv.InstructionField
import venus.simulator.SimulatorState
import venus.simulator.InstructionImplementation

object BGEUImpl : InstructionImplementation {
    override operator fun invoke(inst: Instruction, state: SimulatorState) {
        val rs1: Int = inst.getField(InstructionField.RS1)
        val rs2: Int = inst.getField(InstructionField.RS2)
        val imm: Int = constructBranchImmediate(inst)
        val v1: Int = state.getReg(rs1)
        val v2: Int = state.getReg(rs2)
        if (compareUnsigned(v1, v2) >= 0)
            state.pc += imm
        else
            state.pc += inst.length
    }
}
