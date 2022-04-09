package me.xopyip.webremote

enum class MessageType(type: String) {
    AUTH("AUTH"),
    OPEN("OPEN"),
    CONNECT("CNXN");

    var cmd = 0

    init {
        for (i in 0..3) {
            cmd = cmd shl 8
            cmd = cmd or (type[3 - i].code and 0xFF)
        }
    }
}