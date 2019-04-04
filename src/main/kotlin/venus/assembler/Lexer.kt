package venus.assembler

typealias LineTokens = List<String>

/**
 * A singleton which can be used to lex a given line.
 */
object Lexer {
    private val charPatn = "'(?:\\.|[^\\'])'"
    private val strPatn = """"(?:\\.|[^\\""])*?""""
    private val otherTokenPatn = "[^:() \t,#\"']+"
    private val tokenPatn = "($charPatn|$strPatn|$otherTokenPatn)"
    private val labelPatn = "($otherTokenPatn)\\s*:"
    private val baseRegPatn = """\(\s*($otherTokenPatn)\s*\)"""
    private val tokenRE =
        Regex("""(#.*)|$labelPatn|$tokenPatn|\$baseRegPatn|(['""])|(\S)""")

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
                groups[5] != null -> throw AssemblerError("unclosed string")
                else -> throw AssemblerError("unexpected character '${groups[6]!!.value}'")
            }
        }
        return Pair(labels, insnTokens)
    }
            

    private fun addNonemptyWord(previous: ArrayList<String>, next: StringBuilder) {
        val word = next.toString()
        if (word.isNotEmpty()) {
            previous += word
        }
    }

    /**
     * Lex a line into a label (if there) and a list of arguments.
     *
     * @param line the line to lex
     * @return a pair containing the label and tokens
     * @see LineTokens
     */
    /*
    fun lexLine(line: String): Pair<LineTokens, LineTokens> {
        var currentWord = StringBuilder("")
        val previousWords = ArrayList<String>()
        val labels = ArrayList<String>()
        var escaped = false
        var inCharacter = false
        var inString = false
        var foundComment = false

        for (ch in line) {
            var wasDelimiter = false
            var wasLabel = false
            when (ch) {
                '#' -> foundComment = !inString && !inCharacter
                '\'' -> inCharacter = !(escaped xor inCharacter) && !inString
                '"' -> inString = !(escaped xor inString) && !inCharacter
                ':' -> {
                    if (!inString && !inCharacter) {
                        wasLabel = true
                        if (previousWords.isNotEmpty()) {
                            throw AssemblerError("label $currentWord in the middle of an instruction")
                        }
                    }
                }
                ' ', '\t', '(', ')', ',' -> wasDelimiter = !inString && !inCharacter
            }
            escaped = !escaped && ch == '\\'

            if (foundComment) break

            if (wasDelimiter) {
                addNonemptyWord(previousWords, currentWord)
                currentWord = StringBuilder("")
            } else if (wasLabel) {
                addNonemptyWord(labels, currentWord)
                currentWord = StringBuilder("")
            } else {
                currentWord.append(ch)
            }
        }

        addNonemptyWord(previousWords, currentWord)

        return Pair(labels, previousWords)
    }
    */
}
