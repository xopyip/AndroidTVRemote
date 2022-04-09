package me.xopyip.webremote

import java.io.File
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher

//functions from https://github.com/cgutman/AdbLib/blob/master/src/com/cgutman/adblib/AdbCrypto.java
object Crypto {

    private const val KEY_LENGTH_BITS = 2048
    private const val KEY_LENGTH_BYTES = KEY_LENGTH_BITS / 8
    private const val KEY_LENGTH_WORDS = KEY_LENGTH_BYTES / 4

    private val SIGNATURE_PADDING = intArrayOf(
        0x00, 0x01, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00,
        0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00,
        0x04, 0x14
    ).map { it.toByte() }

    fun getKeys(path: String): KeyPair {
        val publicKey: RSAPublicKey
        val privateKey: RSAPrivateKey
        if (File(path + "public.key").exists()) {
            val publicBytes = File(path + "public.key").readBytes()
            val privateBytes = File(path + "private.key").readBytes()
            val publicSpec = X509EncodedKeySpec(publicBytes)
            val privateSpec = PKCS8EncodedKeySpec(privateBytes)
            val kf = KeyFactory.getInstance("RSA")
            publicKey = kf.generatePublic(publicSpec) as RSAPublicKey
            privateKey = kf.generatePrivate(privateSpec) as RSAPrivateKey
        } else {
            val generator = KeyPairGenerator.getInstance("RSA")
            generator.initialize(KEY_LENGTH_BITS)
            val pair = generator.generateKeyPair()
            privateKey = pair.private as RSAPrivateKey
            publicKey = pair.public as RSAPublicKey
            File(path + "public.key").writeBytes(publicKey.encoded)
            File(path + "private.key").writeBytes(privateKey.encoded)
        }
        return KeyPair(publicKey, privateKey)
    }

    private fun rsaToAdb(pubkey: RSAPublicKey): ByteArray? {
        var rr: BigInteger
        var rem: BigInteger
        var n: BigInteger
        val n0inv: BigInteger
        val r32: BigInteger = BigInteger.ZERO.setBit(32)
        n = pubkey.modulus
        val r: BigInteger = BigInteger.ZERO.setBit(KEY_LENGTH_WORDS * 32)
        rr = r.modPow(BigInteger.valueOf(2), n)
        rem = n.remainder(r32)
        n0inv = rem.modInverse(r32)
        val myN = IntArray(KEY_LENGTH_WORDS)
        val myRr = IntArray(KEY_LENGTH_WORDS)
        var res: Array<BigInteger>
        for (i in 0 until KEY_LENGTH_WORDS) {
            res = rr.divideAndRemainder(r32)
            rr = res[0]
            rem = res[1]
            myRr[i] = rem.toInt()
            res = n.divideAndRemainder(r32)
            n = res[0]
            rem = res[1]
            myN[i] = rem.toInt()
        }
        val bbuf: ByteBuffer = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN)
        bbuf.putInt(KEY_LENGTH_WORDS)
        bbuf.putInt(n0inv.negate().toInt())
        for (i in myN) bbuf.putInt(i)
        for (i in myRr) bbuf.putInt(i)
        bbuf.putInt(pubkey.publicExponent.toInt())
        return bbuf.array()
    }

    fun getAdbPublic(public: RSAPublicKey): ByteArray {
        val convertedKey = rsaToAdb(public)
        val keyString = StringBuilder(720)

        keyString.append(Base64.getEncoder().encodeToString(convertedKey))
        keyString.append(" unknown@unknown")
        keyString.append('\u0000')
        return keyString.toString().toByteArray(charset("UTF-8"))
    }

    fun encrypt(data: ByteArray, private: PrivateKey): ByteArray {
        val encryptCipher = Cipher.getInstance("RSA/ECB/NoPadding")
        encryptCipher.init(Cipher.ENCRYPT_MODE, private)
        encryptCipher.update(SIGNATURE_PADDING.toByteArray())
        return encryptCipher.doFinal(data)
    }

}