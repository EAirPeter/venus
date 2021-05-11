package venus.simulator

/**
 * Thrown when errors occur during simulation.
 */
class AccessError : Throwable {
    constructor(pc: Int, addr: Int, size: Int) : super("Invalid access pc=$pc addr=$addr size=$size") {
        this.pc = pc
        this.addr = addr
        this.size = size
    }

    val pc: Int
    val addr: Int
    val size: Int
}
