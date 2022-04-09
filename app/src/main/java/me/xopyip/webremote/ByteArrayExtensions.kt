package me.xopyip.webremote


fun ByteArray.writeUInt32LE(value: Int, offset: Int = 0) {
    this[offset] = (value shr 0).toByte()
    this[offset + 1] = (value shr 8).toByte()
    this[offset + 2] = (value shr 16).toByte()
    this[offset + 3] = (value shr 24).toByte()
}

fun ByteArray.readUInt32LE(offset: Int = 0): Int {
    return (this[offset].toInt() shl 0) or
            (this[offset + 1].toInt() shl 8) or
            (this[offset + 2].toInt() shl 16) or
            (this[offset + 3].toInt() shl 24)
}