package me.xopyip.webremote

import java.io.InputStream

class AdbPacket(val command: Int, val arg1: Int, val arg2: Int, val data: ByteArray) {
    private val magic = (0xFFFFFFFF - command).toInt()

    fun toBuffer(): ByteArray {
        val buffer = ByteArray(24 + data.size)
        buffer.writeUInt32LE(command, 0)
        buffer.writeUInt32LE(arg1, 4)
        buffer.writeUInt32LE(arg2, 8)
        buffer.writeUInt32LE(data.size, 12)
        buffer.writeUInt32LE(crc(this.data), 16)
        buffer.writeUInt32LE(magic, 20)
        for (i in data.indices)
            buffer[24 + i] = data[i]
        return buffer
    }


    companion object {

        private fun crc(data: ByteArray?): Int {
            if (data == null)
                return 0
            var res = 0
            for (element in data) {
                res = (res + element.toInt()) and (0xFFFFFFFF).toInt()
            }
            return res
        }

        fun fromBuffer(inst: InputStream): AdbPacket {
            val buffer = ByteArray(24)
            inst.read(buffer)
            val command = buffer.readUInt32LE(0)
            val arg1 = buffer.readUInt32LE(4)
            val arg2 = buffer.readUInt32LE(8)
            val dataLength = buffer.readUInt32LE(12)
            val dataCRC = buffer.readUInt32LE(16)
            val magic = buffer.readUInt32LE(20)
            val dataBuffer = ByteArray(dataLength)
            inst.read(dataBuffer)
            //todo: check crc and magic
            return AdbPacket(command, arg1, arg2, dataBuffer)
        }
    }
}