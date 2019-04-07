package venus.assembler.pseudos

import venus.assembler.AssemblerPassOne
import venus.assembler.LineTokens
import venus.assembler.PseudoWriter
import venus.riscv.insts.dsl.relocators.PCRelHiRelocator
import venus.riscv.insts.dsl.relocators.PCRelLoStoreRelocator
import venus.riscv.insts.dsl.relocators.ImmAbsStoreRelocator
import venus.riscv.userStringToInt

/**
 * Writes a store pseudoinstruction. (Those applied to a label)
 */
object Store : PseudoWriter() {
    override operator fun invoke(args: LineTokens, state: AssemblerPassOne): List<LineTokens> {
        checkArgsLength(args, 4)
        val hasParens = args[3].startsWith('(')
        var argsv = args

        if (hasParens) {
            argsv = listOf(args[0], args[1], args[2],
                           args[3].substring(1, args[3].length - 1))
        }
        val label = args[2]
        try {
            userStringToInt(label)
            /* if it's a number, this is just an ordinary store instruction */
            return listOf(argsv)
        } catch (e: NumberFormatException) {
            if (hasParens) {
                state.addRelocation(ImmAbsStoreRelocator, state.getOffset(),
                                    label)
                return listOf(argsv)
            }
            /* assume it's a label */            
        }

        val auipc = listOf("auipc", argsv[3], "0")
        state.addRelocation(PCRelHiRelocator, state.getOffset(), label)

        val store = listOf(args[0], args[1], "0", argsv[3])
        state.addRelocation(PCRelLoStoreRelocator, state.getOffset() + 4, label)

        return listOf(auipc, store)
    }
}
