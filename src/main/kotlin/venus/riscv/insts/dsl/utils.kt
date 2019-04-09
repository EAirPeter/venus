package venus.riscv.insts.dsl

internal fun compareUnsigned(v1: Int, v2: Int): Int {
    return (v1 xor Int.MIN_VALUE).compareTo(v2 xor Int.MIN_VALUE)
}

internal fun compareUnsignedLong(v1: Long, v2: Long): Int {
    return (v1 xor Long.MIN_VALUE).compareTo(v2 xor Long.MIN_VALUE)
}
