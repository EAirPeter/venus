package venus.riscv

fun userStringToInt(s: String): Int {
    if (isCharacterLiteral(s)) {
        return characterLiteralToInt(s)
    }

    val radix = when {
        s.startsWith("0x") -> 16
        s.startsWith("0b") -> 2
        s.drop(1).startsWith("0x") -> 16
        s.drop(1).startsWith("0b") -> 2
        else -> return s.toLong().toInt()
    }

    val skipSign = when (s.first()) {
        '+', '-' -> 1
        else -> 0
    }

    val noRadixString = s.take(skipSign) + s.drop(skipSign + 2)
    return noRadixString.toLong(radix).toInt()
}

fun isNumeral(s: String): Boolean {
    try {
        userStringToInt(s)
        return true
    } catch (e: NumberFormatException) {
        return false
    }
}

private fun isCharacterLiteral(s: String) =
        s.first() == '\'' && s.last() == '\''

private fun characterLiteralToInt(s: String): Int {
    val stripSingleQuotes = s.drop(1).dropLast(1)
    if (stripSingleQuotes == "\\'") return '\''.toInt()
    if (stripSingleQuotes == "\"") return '"'.toInt() //"

    val jsonString = "\"$stripSingleQuotes\""
    try {
        val parsed = JSON.parse<String>(jsonString)
        if (parsed.isEmpty()) throw NumberFormatException("character literal $s is empty")
        if (parsed.length > 1) throw NumberFormatException("character literal $s too long")
        return parsed[0].toInt()
    } catch (e: Throwable) {
        throw NumberFormatException("could not parse character literal $s")
    }
}

/** Return the symbolic part of LABELARG, where LABELARG may be either
 *  <symbol>, <symbol>+<decimal numeral>, or <symbol>-<decimal numeral>.
 */
fun symbolPart(labelArg: String): String {
    for (i in labelArg.indices) {
        if (labelArg[i] == '+' || labelArg[i] == '-') {
            return labelArg.substring(0, i)
        }
    }
    return labelArg
}

/** Return the numeric offset part of LABELARG, where LABELARG may be either
 *  <symbol> (result 0), <symbol>+<decimal numeral> (result
 *  <decimal numeral> as an Int), or <symbol>-<decimal numeral>.
 */
fun labelOffsetPart(labelArg: String): Int {
    for (i in labelArg.indices) {
        if (labelArg[i] == '+' || labelArg[i] == '-') {
            return labelArg.substring(i).toInt()
        }
    }
    return 0
}
