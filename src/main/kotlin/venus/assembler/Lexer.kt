package venus.assembler

typealias LineTokens = List<String>

/**
 * A singleton that can be used to lex a given line.
 */
object Lexer {
    private val charPatn = """'(?:\\.|[^\\'])'"""
    private val strPatn = "\"(?:\\\\.|[^\\\\\"\"])*?\""
    private val otherTokenPatn = """[^:() \t,#""']+"""
    private val tokenPatn = "($charPatn|$strPatn|$otherTokenPatn)"
    private val labelPatn = "($otherTokenPatn)\\s*:"
    private val baseRegPatn = """\(\s*($otherTokenPatn)\s*\)"""
    private val tokenRE =
        Regex("""(#.*)|$labelPatn|$tokenPatn|$baseRegPatn|(['""])""")

    fun lexLine(line: String): Pair<LineTokens, LineTokens> {
        val labels = ArrayList<String>()
        val insnTokens = ArrayList<String>()

        var baseRegUsed = false
        for (mat in tokenRE.findAll(line)) {
            val groups = mat.groups
            when {
                groups[1] != null -> Unit
                groups[2] != null && !insnTokens.isEmpty() -> {
                    throw AssemblerError("label ${groups[2]!!.value} in the middle of an instruction")
                }
                groups[2] != null -> labels.add(groups[2]!!.value)
                groups[3] != null -> insnTokens.add(groups[3]!!.value)
                groups[4] != null -> {
                    baseRegUsed = true
                    insnTokens.add(groups[4]!!.value)
                }
                else -> throw AssemblerError("unclosed string")
            }
        }
        return Pair(labels, insnTokens)
    }
}
