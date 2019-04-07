package venus.riscv.insts.dsl.relocators

import venus.riscv.InstructionField
import venus.riscv.MachineCode
import venus.assembler.AssemblerError

private object ImmAbsStoreRelocator32 : Relocator32 {
    override operator fun invoke(mcode: MachineCode, pc: Int, target: Int) {
        if (target in -2048..2047) {
            mcode[InstructionField.IMM_4_0] = target
            mcode[InstructionField.IMM_11_5] = target shr 5
        } else {
            throw AssemblerError("immediate value out of range: $target")
        }
    }
}

val ImmAbsStoreRelocator = Relocator(ImmAbsStoreRelocator32, NoRelocator64)
