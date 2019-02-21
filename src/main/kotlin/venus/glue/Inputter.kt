package venus.glue

internal object Inputter {

    private val buffer: StringBuilder = StringBuilder()
    private var bufferPntr: Int = -1

    internal fun bufferLine(): Int {
        var line = readLine()
        if (line == null) {
            bufferPntr = -1
            return -1
        } else {
            bufferPntr = 0
            buffer.clear()
            buffer.append(line)
            return buffer.length
        }
    }

    internal fun nextByte(): Int {
        if (bufferPntr == -1) {
            if (bufferLine() == -1) {
                return -1
            }
        }
        if (bufferPntr >= buffer.length) {
            bufferPntr = -1
            return '\n'.toInt()
        } else {
            bufferPntr += 1
            return buffer.get(bufferPntr - 1).toInt()
        }
    }
        
}
