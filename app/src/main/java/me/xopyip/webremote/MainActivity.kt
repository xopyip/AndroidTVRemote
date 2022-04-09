package me.xopyip.webremote

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent.*
import androidx.fragment.app.FragmentActivity
import io.javalin.Javalin
import java.net.Socket
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Loads [MainFragment].
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* ===================================================================== */

        val path = applicationContext.filesDir.absolutePath + "/"
        val executorService: ExecutorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            val app = Javalin.create().start(7070)
            val codes = mapOf(
                "volup" to KEYCODE_VOLUME_UP,
                "voldown" to KEYCODE_VOLUME_DOWN,
                "mute" to KEYCODE_VOLUME_MUTE,

                "chup" to KEYCODE_CHANNEL_UP,
                "chdown" to KEYCODE_CHANNEL_DOWN,
                "menu" to KEYCODE_MENU,
                "apps" to KEYCODE_APP_SWITCH,

                "up" to KEYCODE_DPAD_UP,
                "down" to KEYCODE_DPAD_DOWN,
                "left" to KEYCODE_DPAD_LEFT,
                "right" to KEYCODE_DPAD_RIGHT,
                "center" to KEYCODE_DPAD_CENTER,
                "enter" to KEYCODE_ENTER,
                "playpause" to KEYCODE_MEDIA_PLAY_PAUSE,
                "stop" to KEYCODE_MEDIA_STOP,
                "next" to KEYCODE_MEDIA_NEXT,
                "prev" to KEYCODE_MEDIA_PREVIOUS,
                "rewind" to KEYCODE_MEDIA_REWIND,
                "ff" to KEYCODE_MEDIA_FAST_FORWARD,

                "onoff" to KEYCODE_TV_POWER,


                "back" to KEYCODE_BACK,
                "home" to KEYCODE_HOME
            )

            val sock = Socket("localhost", 5555)
            sock.writePacket(MessageType.CONNECT, 0x01000000, 4096, "host::".toByteArray())
            val inputStream = sock.getInputStream()

            val keys = Crypto.getKeys(path)

            var authChecked = false
            var connected = false
            while (!connected) {
                if (inputStream.available() > 0) {
                    val packetFromBuffer = AdbPacket.fromBuffer(inputStream)
                    when(MessageType.values().first { it.cmd == packetFromBuffer.command }){
                        MessageType.AUTH -> {
                            Log.i("WebRemote", "Auth")
                            if (packetFromBuffer.arg1 == 1) {
                                if(authChecked){
                                    sock.writePacket(MessageType.AUTH, 3, 0, Crypto.getAdbPublic(keys.public as RSAPublicKey))
                                }else{
                                    sock.writePacket(MessageType.AUTH, 2, 0, Crypto.encrypt(packetFromBuffer.data, keys.private))
                                    authChecked = true
                                }
                            }
                        }
                        MessageType.CONNECT -> {
                            Log.i("WebRemote", "CONNECT")
                            val version = packetFromBuffer.arg1
                            val maxdata = packetFromBuffer.arg2
                            val systemId = String(packetFromBuffer.data)
                            println("Connected with $systemId, version $version, maxdata $maxdata")
                            connected = true
                        }
                        else -> {
                            println("Unknown command ${packetFromBuffer.command}")
                        }
                    }
                }
            }
            var local_id = 0

            app.get("/") { ctx ->
                val code = ctx.queryParam("code")
                if (code != null && codes.containsKey(code)) {
                    if(connected){
                        sock.exec("input keyevent " + codes[code], local_id++);
                    }
                }
                ctx.html(codes.map { (k) -> "<a href=\"/?code=${k}\">${k}</a>" }.joinToString())
            }
        }
        /* ===================================================================== */



        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()
        }
    }
}


