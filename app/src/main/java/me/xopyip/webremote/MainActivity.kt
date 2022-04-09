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

        val path = applicationContext.filesDir.absolutePath + "/"
        val executorService: ExecutorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            val app = Javalin.create().start(7070)
            val sock = Socket("localhost", 5555)
            sock.writePacket(MessageType.CONNECT, 0x01000000, 4096, "host::".toByteArray())
            val inputStream = sock.getInputStream()

            val keys = Crypto.getKeys(path)

            var authChecked = false
            var connected = false
            while (!connected) {
                if (inputStream.available() > 0) {
                    val packetFromBuffer = AdbPacket.fromBuffer(inputStream)
                    when (MessageType.values().first { it.cmd == packetFromBuffer.command }) {
                        MessageType.AUTH -> {
                            Log.i("WebRemote", "Auth")
                            if (packetFromBuffer.arg1 == 1) {
                                if (authChecked) {
                                    sock.writePacket(
                                        MessageType.AUTH,
                                        3,
                                        0,
                                        Crypto.getAdbPublic(keys.public as RSAPublicKey)
                                    )
                                } else {
                                    sock.writePacket(
                                        MessageType.AUTH,
                                        2,
                                        0,
                                        Crypto.encrypt(packetFromBuffer.data, keys.private)
                                    )
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

            app.get("/") { it.redirect("/index.html") }
            registerStaticFiles(app, "")
        }



        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()
        }
    }

    private fun registerStaticFiles(app: Javalin, path: String) {
        val files = assets.list("public$path")
        files?.forEach { it ->
            if (assets.list("public$path/$it")!!.isEmpty()) {
                app.get("$path/$it") { ctx ->
                    when {
                        it.endsWith(".html") -> ctx.html(
                            assets.open("public$path/$it").bufferedReader().use { it.readText() })
                        else -> ctx.result(assets.open("public$path/$it"))
                    }
                }
            } else {
                registerStaticFiles(app, "$path/$it")
            }
        }
    }
}


