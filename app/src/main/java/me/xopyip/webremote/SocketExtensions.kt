package me.xopyip.webremote

import android.util.Log
import java.net.Socket


fun Socket.writePacket(type: MessageType, arg1: Int, arg2: Int, data: ByteArray) {
    this.getOutputStream().write(AdbPacket(type.cmd, arg1, arg2, data).toBuffer())
}

fun Socket.exec(command: String, local_id: Int) {
    Log.i("WebRemote", "Command: $command")
    this.writePacket(MessageType.OPEN, local_id, 0, "shell:$command".toByteArray())
}