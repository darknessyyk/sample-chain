package me.darknessyyk.chain.util

import java.security.*


object CryptoUtil {
    val SIGNATURE_ALGORITHM = "SHA256withRSA"

    fun generate(): KeyPair? {
        val kpg: KeyPairGenerator
        try {
            kpg =  KeyPairGenerator.getInstance("RSA")
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            return null
        }
        kpg.initialize(1024, SecureRandom())
        val keys = kpg.generateKeyPair()
        return keys
    }

    fun sign(message: ByteArray, privateKey: PrivateKey): ByteArray? {
        val sig: Signature
        try {
            sig =  Signature.getInstance(SIGNATURE_ALGORITHM)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            return null
        }

        try {
            sig.initSign(privateKey, SecureRandom())
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
            return null
        }

        try {
            sig.update(message)
            return sig.sign()
        } catch (e: SignatureException) {
            e.printStackTrace()
            return null
        }
    }

    fun verify(publicKey: PublicKey, message: ByteArray, signature: ByteArray): Boolean {
        val sig: Signature
        try {
            sig = Signature.getInstance(SIGNATURE_ALGORITHM)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            return false
        }

        try {
            sig.initVerify(publicKey)
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
            return false
        }

        try {
            sig.update(message)
            return sig.verify(signature)
        } catch (e: SignatureException) {
            e.printStackTrace()
            return false
        }
    }
}

