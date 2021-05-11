package venus.linker

import venus.assembler.AssemblerError
import venus.riscv.MemorySegments
import venus.riscv.Program
import venus.riscv.insts.dsl.relocators.Relocator

/**
 * Describes how to relocate a given instructions
 *
 * @param relocator the relocator to use
 * @param label the target label
 * @param offset the byte offset the instruction is at
 */
data class RelocationInfo(val relocator: Relocator, val offset: Int,
                          val label: String, val labelOffset: Int)

/**
 * Describes how to relocate data bytes
 *
 * @param offset the byte offset in the data segment
 * @param label the target label
 */
data class DataRelocationInfo(val offset: Int,
                              val label: String, val labelOffset: Int)

/**
 * A singleton which links a list of programs into one program.
 *
 * @see LinkedProgram
 */
object Linker {
    /**
     * Links together the given list of programs.
     *
     * This consists of several steps:
     *      - Collect all the labels in each program.
     *      - Keep track of all the instructions which need to be relocated.
     *      - Append all the instructions together.
     *      - Append all of the debugging info together.
     *      - Append all of the data segments together
     * Once the above is done, all the appropriate relocation is performed.
     *
     * @param progs a list of programs which will be linked
     * @return a [LinkedProgram] which can be used in the [venus.simulator.Simulator]
     * @todo refactor this into multiple functions
     */
    fun link(progs: List<Program>): LinkedProgram {
        val linkedProgram = LinkedProgram()
        val globalTable = HashMap<String, Int>()
        val toRelocate = ArrayList<RelocationInfo>()
        val toRelocateRodata = ArrayList<DataRelocationInfo>()
        val toRelocateData = ArrayList<DataRelocationInfo>()
        var textTotalOffset = 0
        var rodataTotalOffset = 0
        var dataTotalOffset = 0

        for (prog in progs) {
            for ((label, offset) in prog.labels) {
                val start = if (offset >= MemorySegments.STATIC_BEGIN) {
                    dataTotalOffset
                } else if (offset >= MemorySegments.CONST_BEGIN) {
                    rodataTotalOffset
                } else {
                    textTotalOffset
                }
                val location = start + offset

                if (prog.isGlobalLabel(label)) {
                    val previousValue = globalTable.put(label, location)
                    if (previousValue != null) {
                        throw AssemblerError("label $label defined global in two different files")
                    }
                    if (label == "main") {
                        if (location >= MemorySegments.STATIC_BEGIN) {
                            throw AssemblerError("label $label must not be defined in data segment")
                        } else if (location >= MemorySegments.CONST_BEGIN) {
                            throw AssemblerError("label $label must not be defined in rodata segment")
                        }
                        linkedProgram.startPC = location
                    }
                }
            }

            prog.insts.forEach(linkedProgram.prog::add)
            prog.debugInfo.forEach {
                linkedProgram.dbg.add(ProgramDebugInfo(prog.name, it))
            }
            prog.rodataSegment.forEach(linkedProgram.prog::addToRodata)
            prog.dataSegment.forEach(linkedProgram.prog::addToData)

            for ((relocator, offset, label, labelOffset)
                in prog.relocationTable) {

                val location = textTotalOffset + offset
                val mcode = linkedProgram.prog.insts[location / 4]

                if (label == "") {
                    relocator(mcode, location, labelOffset)
                } else {
                    val toAddress = prog.labels.get(label)
                    if (toAddress != null) {
                        /* TODO: fix this for variable length instructions */
                        relocator(mcode, location, toAddress + labelOffset)
                    } else {
                        /* need to relocate globally */
                        toRelocate.add(RelocationInfo(
                            relocator, location,
                            label, labelOffset))
                    }
                }
            }

            for ((offset, label, labelOffset) in prog.rodataRelocationTable) {
                val toAddress0 = prog.labels.get(label)
                val location = rodataTotalOffset + offset
                if (toAddress0 != null) {
                    val toAddress = toAddress0 + labelOffset
                    linkedProgram.prog.overwriteRodata(location, toAddress.toByte())
                    linkedProgram.prog.overwriteRodata(
                            location + 1,
                            (toAddress shr 8).toByte())
                    linkedProgram.prog.overwriteRodata(
                            location + 2,
                            (toAddress shr 16).toByte())
                    linkedProgram.prog.overwriteRodata(
                            location + 3,
                            (toAddress shr 24).toByte())
                } else {
                    /* need to relocate globally */
                    toRelocateRodata.add(DataRelocationInfo(
                            location, label, labelOffset))
                }
            }

            for ((offset, label, labelOffset) in prog.dataRelocationTable) {
                val toAddress0 = prog.labels.get(label)
                val location = dataTotalOffset + offset
                if (toAddress0 != null) {
                    val toAddress = toAddress0 + labelOffset
                    linkedProgram.prog.overwriteData(location, toAddress.toByte())
                    linkedProgram.prog.overwriteData(
                        location + 1,
                        (toAddress shr 8).toByte())
                    linkedProgram.prog.overwriteData(
                        location + 2,
                        (toAddress shr 16).toByte())
                    linkedProgram.prog.overwriteData(
                        location + 3,
                        (toAddress shr 24).toByte())
                } else {
                    /* need to relocate globally */
                    toRelocateData.add(DataRelocationInfo(
                        location, label, labelOffset))
                }
            }

            textTotalOffset += prog.textSize
            rodataTotalOffset += prog.rodataSize
            dataTotalOffset += prog.dataSize
        }

        for ((relocator, offset, label) in toRelocate) {
            val toAddress = globalTable.get(label) ?:
                    throw AssemblerError("label $label used but not defined")

            val mcode = linkedProgram.prog.insts[offset / 4]
            relocator(mcode, offset, toAddress)
        }

        for ((location, label) in toRelocateRodata) {
            val toAddress = globalTable.get(label) ?:
            throw AssemblerError("label $label used but not defined")
            linkedProgram.prog.overwriteRodata(location, toAddress.toByte())
            linkedProgram.prog.overwriteRodata(location + 1, (toAddress shr 8).toByte())
            linkedProgram.prog.overwriteRodata(location + 2, (toAddress shr 16).toByte())
            linkedProgram.prog.overwriteRodata(location + 3, (toAddress shr 24).toByte())
        }

        for ((location, label) in toRelocateData) {
            val toAddress = globalTable.get(label) ?:
                    throw AssemblerError("label $label used but not defined")
            linkedProgram.prog.overwriteData(location, toAddress.toByte())
            linkedProgram.prog.overwriteData(location + 1, (toAddress shr 8).toByte())
            linkedProgram.prog.overwriteData(location + 2, (toAddress shr 16).toByte())
            linkedProgram.prog.overwriteData(location + 3, (toAddress shr 24).toByte())
        }

        return linkedProgram
    }
}
