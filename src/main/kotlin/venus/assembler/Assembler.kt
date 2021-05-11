package venus.assembler

import venus.assembler.pseudos.checkArgsLength
import venus.riscv.MemorySegments
import venus.riscv.Program
import venus.riscv.insts.dsl.Instruction
import venus.riscv.insts.dsl.relocators.Relocator
import venus.riscv.unescapeString
import venus.riscv.userStringToInt
import venus.riscv.isNumeral

/**
 * This singleton implements a simple two-pass assembler to transform files into programs.
 */
object Assembler {
    /**
     * Assembles the given code into an unlinked Program.
     *
     * @param text the code to assemble.
     * @return an unlinked program.
     * @see venus.linker.Linker
     * @see venus.simulator.Simulator
     */
    fun assemble(text: String): AssemblerOutput {
        val (passOneProg, talInstructions, passOneErrors) = AssemblerPassOne(text).run()
        if (passOneErrors.isNotEmpty()) {
            return AssemblerOutput(passOneProg, passOneErrors)
        }
        val passTwoOutput = AssemblerPassTwo(passOneProg, talInstructions).run()
        return passTwoOutput
    }
}

data class DebugInfo(val lineNo: Int, val line: String)
data class DebugInstruction(val debug: DebugInfo, val LineTokens: List<String>)
data class PassOneOutput(
        val prog: Program,
        val talInstructions: List<DebugInstruction>,
        val errors: List<AssemblerError>
)
data class AssemblerOutput(val prog: Program, val errors: List<AssemblerError>)

/**
 * Pass #1 of our two pass assembler.
 *
 * It parses labels, expands pseudo-instructions and follows assembler directives.
 * It populations [talInstructions], which is then used by [AssemblerPassTwo] in order to actually assemble the code.
 */
internal class AssemblerPassOne(private val text: String) {
    /** The program we are currently assembling */
    private val prog = Program()
    /** The text offset where the next instruction will be written */
    private var currentTextOffset = MemorySegments.TEXT_BEGIN
    /** The rodata offset where more rodata will be written */
    private var currentRodataOffset = MemorySegments.CONST_BEGIN
    /** The data offset where more data will be written */
    private var currentDataOffset = MemorySegments.STATIC_BEGIN
    /** Whether or not we are currently in the text segment */
    private var inTextSegment = true
    /** Whether or not we are currently in the rodata segment */
    private var inRodataSegment = false
    /** TAL Instructions which will be added to the program */
    private val talInstructions = ArrayList<DebugInstruction>()
    /** The current line number (for user-friendly errors) */
    private var currentLineNumber = 0
    /** List of all errors encountered */
    private val errors = ArrayList<AssemblerError>()

    fun run(): PassOneOutput {
        doPassOne()
        return PassOneOutput(prog, talInstructions, errors)
    }

    private fun doPassOne() {
        for (line in text.lines()) {
            try {
                currentLineNumber++

                val offset = getOffset()

                val (labels, args) = Lexer.lexLine(line)
                for (label in labels) {
                    val oldOffset = prog.addLabel(label, offset)
                    if (oldOffset != null) {
                        throw AssemblerError("label $label defined twice")
                    }
                }

                if (args.isEmpty() || args[0].isEmpty()) continue // empty line

                if (isAssemblerDirective(args[0])) {
                    parseAssemblerDirective(args[0], args.drop(1))
                } else {
                    val expandedInsts = replacePseudoInstructions(args)
                    for (inst in expandedInsts) {
                        if (inTextSegment) {
                            val dbg = DebugInfo(currentLineNumber, line)
                            talInstructions.add(DebugInstruction(dbg, inst))
                            currentTextOffset += 4
                        } else if (inRodataSegment) {
                            throw AssemblerError("could not emit instructions in rodata segment")
                        } else {
                            throw AssemblerError("could not emit instructions in data segment")
                        }
                    }
                }
            } catch (e: AssemblerError) {
                errors.add(AssemblerError(currentLineNumber, e))
            }
        }
    }

    /** Gets the current offset (either text or data) depending on where we are writing */
    fun getOffset() = if (inTextSegment) currentTextOffset else
        if (inRodataSegment) currentRodataOffset else currentDataOffset

    /**
     * Determines if the given token is an assembler directive
     *
     * @param cmd the token to check
     * @return true if the token is an assembler directive
     * @see parseAssemblerDirective
     */
    private fun isAssemblerDirective(cmd: String) = cmd.startsWith(".")

    /**
     * Replaces any pseudoinstructions which occur in our program.
     *
     * @param tokens a list of strings corresponding to the space delimited line
     * @return the corresponding TAL instructions (possibly unchanged)
     */
    private fun replacePseudoInstructions(tokens: LineTokens): List<LineTokens> {
        try {
            val cmd = getInstruction(tokens)
            val pw = PseudoDispatcher.valueOf(cmd).pw
            return pw(tokens, this)
        } catch (t: Throwable) {
            /* TODO: don't use throwable here */
            /* not a pseudoinstruction, or expansion failure */
            return listOf(tokens)
        }
    }

    /**
     * Changes the assembler state in response to directives
     *
     * @param directive the assembler directive, starting with a "."
     * @param args any arguments following the directive
     * @param line the original line (which is needed for some directives)
     */
    private fun parseAssemblerDirective(directive: String, args: LineTokens) {
        when (directive) {
            ".rodata" -> {
                inTextSegment = false
                inRodataSegment = true
            }
            ".data" -> {
                inTextSegment = false
                inRodataSegment = false
            }
            ".text" -> {
                inTextSegment = true
                inRodataSegment = false
            }

            ".byte" -> {
                for (arg in args) {
                    val byte = userStringToInt(arg)
                    if (byte !in -127..255) {
                        throw AssemblerError("invalid byte $byte too big")
                    }
                    if (inTextSegment) {
                        throw AssemblerError("could not define byte in text segment")
                    } else if (inRodataSegment) {
                        prog.addToRodata(byte.toByte())
                        currentRodataOffset++
                    } else {
                        prog.addToData(byte.toByte())
                        currentDataOffset++
                    }
                }
            }

            ".string", ".asciiz" -> {
                checkArgsLength(args, 1)
                val ascii: String = try {
                    val str = args[0]
                    if (str.length < 2 || str[0] != str[str.length - 1] || str[0] != '"') {
                        throw IllegalArgumentException()
                    } // '"'
                    unescapeString(str.drop(1).dropLast(1))
                } catch (e: Throwable) {
                    throw AssemblerError("couldn't parse ${args[0]} as a string")
                }
                for (c in ascii) {
                    if (c.toInt() !in 0..127) {
                        throw AssemblerError("unexpected non-ascii character: $c")
                    }
                    if (inTextSegment) {
                        throw AssemblerError("could not define string in text segment")
                    } else if (inRodataSegment) {
                        prog.addToRodata(c.toByte())
                        currentRodataOffset++
                    } else {
                        prog.addToData(c.toByte())
                        currentDataOffset++
                    }
                }

                /* Add NUL terminator */
                if (inTextSegment) {
                    throw AssemblerError("could not define string in text segment")
                } else if (inRodataSegment) {
                    prog.addToRodata(0)
                    currentRodataOffset++
                } else {
                    prog.addToData(0)
                    currentDataOffset++
                }
            }

            ".word" -> {
                for (arg in args) {
                    try {
                        val word = userStringToInt(arg)
                        if (inTextSegment) {
                            throw AssemblerError("could not define word in text segment")
                        } else if (inRodataSegment) {
                            prog.addToRodata(word.toByte())
                            prog.addToRodata((word shr 8).toByte())
                            prog.addToRodata((word shr 16).toByte())
                            prog.addToRodata((word shr 24).toByte())
                            currentRodataOffset += 4
                        } else {
                            prog.addToData(word.toByte())
                            prog.addToData((word shr 8).toByte())
                            prog.addToData((word shr 16).toByte())
                            prog.addToData((word shr 24).toByte())
                            currentDataOffset += 4
                        }
                    } catch (e: NumberFormatException) {
                        /* arg is not a number; interpret as label */
                        if (inTextSegment) {
                            throw AssemblerError("could not define word in text segment")
                        } else if (inRodataSegment) {
                            prog.addRodataRelocation(
                                    prog.symbolPart(arg),
                                    prog.labelOffsetPart(arg),
                                    currentRodataOffset - MemorySegments.CONST_BEGIN)
                            prog.addToRodata(0)
                            prog.addToRodata(0)
                            prog.addToRodata(0)
                            prog.addToRodata(0)
                            currentRodataOffset += 4
                        } else {
                            prog.addDataRelocation(
                                    prog.symbolPart(arg),
                                    prog.labelOffsetPart(arg),
                                    currentDataOffset - MemorySegments.STATIC_BEGIN)
                            prog.addToData(0)
                            prog.addToData(0)
                            prog.addToData(0)
                            prog.addToData(0)
                            currentDataOffset += 4
                        }
                    }
                }
            }

            ".space" -> {
                checkArgsLength(args, 1)
                try {
                    val reps = userStringToInt(args[0])
                    if (inTextSegment) {
                        throw AssemblerError("could not add space in text segment")
                    } else if (inRodataSegment) {
                        for (c in 1..reps) {
                            prog.addToRodata(0)
                        }
                        currentRodataOffset += reps
                    } else {
                        for (c in 1..reps) {
                            prog.addToData(0)
                        }
                        currentDataOffset += reps
                    }
                } catch (e: NumberFormatException) {
                    throw AssemblerError("${args[0]} not a valid argument")
                }
            }

            ".globl" -> {
                args.forEach(prog::makeLabelGlobal)
            }

            ".align" -> {
                checkArgsLength(args, 1)
                val pow2 = userStringToInt(args[0])
                if (pow2 < 0 || pow2 > 8) {
                    throw AssemblerError(".align argument must be between 0 and 8, inclusive")
                }
                val mask = (1 shl pow2) - 1 // Sets pow2 rightmost bits to 1

                /* Add padding until data offset aligns with given power of 2 */
                if (inTextSegment) {
                    throw AssemblerError("could not align in text segment")
                } else if (inRodataSegment) {
                    while ((currentRodataOffset and mask) != 0) {
                        prog.addToRodata(0)
                        currentRodataOffset++
                    }
                } else {
                    while ((currentDataOffset and mask) != 0) {
                        prog.addToData(0)
                        currentDataOffset++
                    }
                }
            }

            ".equiv", ".equ", ".set" -> {
                checkArgsLength(args, 2)
                val oldDefn = prog.addEqu(args[0], args[1])
                if (directive == ".equiv" && oldDefn != null) {
                    throw AssemblerError("attempt to redefine ${args[0]}")
                }
            }

            ".float", ".double" -> {
                println("Warning: $directive not currently supported!")
            }

            else -> throw AssemblerError("unknown assembler directive $directive")
        }
    }

    fun addRelocation(relocator: Relocator, offset: Int, label: String) =
            prog.addRelocation(
                relocator, prog.symbolPart(label),
                prog.labelOffsetPart(label), offset)

}

/**
 * Pass #2 of our two pass assembler.
 *
 * It writes TAL instructions to the program, and also adds debug info to the program.
 * @see addInstruction
 * @see venus.riscv.Program.addDebugInfo
 */
internal class AssemblerPassTwo(val prog: Program, val talInstructions: List<DebugInstruction>) {
    private val errors = ArrayList<AssemblerError>()
    fun run(): AssemblerOutput {
        resolveEquivs(prog)
        for ((dbg, inst) in talInstructions) {
            try {
                addInstruction(inst)
                prog.addDebugInfo(dbg)
            } catch (e: AssemblerError) {
                val (lineNumber, _) = dbg
                errors.add(AssemblerError(lineNumber, e))
            }
        }
        return AssemblerOutput(prog, errors)
    }

    /**
     * Adds machine code corresponding to our instruction to the program.
     *
     * @param tokens a list of strings corresponding to the space delimited line
     */
    private fun addInstruction(tokens: LineTokens) {
        if (tokens.isEmpty() || tokens[0].isEmpty()) return
        val cmd = getInstruction(tokens)
        val inst = Instruction[cmd]
        val mcode = inst.format.fill()
        inst.parser(prog, mcode, tokens.drop(1))
        prog.add(mcode)
    }

    /** Resolve all labels in PROG defined by .equiv, .equ, or .set and add
     *  these to PROG as ordinary labels.  Checks for duplicate or
     *  conflicting definition. */
    private fun resolveEquivs(prog: Program) {
        val conflicts = prog.labels.keys.intersect(prog.equivs.keys)
        if (conflicts.isNotEmpty()) {
            throw AssemblerError("conflicting definitions for $conflicts")
        }

        val processing = HashSet<String>()
        for (equiv in prog.equivs.keys) {
            if (equiv !in prog.labels.keys) {
                prog.labels[equiv] = findDefn(equiv, prog, processing)
            }
        }
    }

    /** Return the ultimate definition of SYM, an .equ-defined symbol, in
     *  PROG, assuming that if SYM is in ACTIVE, it is part of a
     *  circular chain of definitions. */
    private fun findDefn(sym: String, prog: Program,
                         active: HashSet<String>): Int {
        // FIXME: Global symbols not defined in this program.
        if (sym in active) {
            throw AssemblerError("circularity in definition of $sym")
        }
        val value = prog.equivs[sym]!!
        if (isNumeral(value)) {
            return userStringToInt(value)
        } else if (value in prog.labels.keys) {
            return prog.labels[value]!!
        } else if (value in prog.equivs.keys) {
            active.add(sym)
            val result = findDefn(value, prog, active)
            active.remove(sym)
            return result
        } else {
            throw AssemblerError("undefined symbol: $value")
        }
    }

}

/**
 * Gets the instruction from a line of code
 *
 * @param tokens the tokens from the current line
 * @return the instruction (aka the first argument, in lowercase)
 */
private fun getInstruction(tokens: LineTokens) = tokens[0].toLowerCase()
