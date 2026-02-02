package person.wangchen11.gdx.helper

object ByteHelper {
    const val MASK_BIT_AT_0: Int = 0x1
    const val MASK_BIT_AT_1: Int = 0x2
    const val MASK_BIT_AT_2: Int = 0x4
    const val MASK_BIT_AT_3: Int = 0x8
    const val MASK_BIT_AT_4: Int = 0x10
    const val MASK_BIT_AT_5: Int = 0x20
    const val MASK_BIT_AT_6: Int = 0x40
    const val MASK_BIT_AT_7: Int = 0x80
    const val MASK_BIT_AT_8: Int = 0x100
    const val MASK_BIT_AT_9: Int = 0x200
    const val MASK_BIT_AT_10: Int = 0x400
    const val MASK_BIT_AT_11: Int = 0x800
    const val MASK_BIT_AT_12: Int = 0x1000
    const val MASK_BIT_AT_13: Int = 0x2000
    const val MASK_BIT_AT_14: Int = 0x4000
    const val MASK_BIT_AT_15: Int = 0x8000
    const val MASK_BIT_AT_16: Int = 0x10000
    const val MASK_BIT_AT_17: Int = 0x20000

    const val MASK_BITS_1: Int = 0x1
    const val MASK_BITS_2: Int = 0x3
    const val MASK_BITS_3: Int = 0x7
    const val MASK_BITS_4: Int = 0xf
    const val MASK_BITS_5: Int = 0x1f
    const val MASK_BITS_6: Int = 0x3f
    const val MASK_BITS_7: Int = 0x7f
    const val MASK_BITS_8: Int = 0xff
    const val MASK_BITS_9: Int = 0x1ff
    const val MASK_BITS_10: Int = 0x3ff
    const val MASK_BITS_11: Int = 0x7ff
    const val MASK_BITS_12: Int = 0xfff
    const val MASK_BITS_13: Int = 0x1fff
    const val MASK_BITS_14: Int = 0x3fff
    const val MASK_BITS_15: Int = 0x7fff
    const val MASK_BITS_16: Int = 0xffff
    const val MASK_BITS_24: Int = 0xffffff
}

inline fun ByteArray.getInt(pos: Int): Int {
    return this[pos + 0].toInt().and(0xff).shl(0)
    .or(this[pos + 1].toInt().and(0xff).shl(8))
    .or(this[pos + 2].toInt().and(0xff).shl(16))
    .or(this[pos + 3].toInt().and(0xff).shl(24))
}

inline fun ByteArray.putInt(pos: Int, value: Int) {
    this[pos + 0] = value.ushr(0).and(0xff).toByte()
    this[pos + 1] = value.ushr(8).and(0xff).toByte()
    this[pos + 2] = value.ushr(16).and(0xff).toByte()
    this[pos + 3] = value.ushr(24).and(0xff).toByte()
}

inline fun ByteArray.getFloat(pos: Int): Float {
    return Float.fromBits(getInt(pos))
}

inline fun ByteArray.putFloat(pos: Int, value: Float) {
    putInt(pos, value.toBits())
}