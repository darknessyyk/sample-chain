package me.darknessyyk.chain.model

import me.darknessyyk.chain.util.ByteArrayWrapper
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey
import java.util.*

class Output(
        val value: Double,
        val address: PublicKey) {

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }

        val output = other as Output

        if (value != output.value)
            return false
        if ((address as RSAPublicKey).publicExponent != (output.address as RSAPublicKey).publicExponent)
            return false
        if ((address as RSAPublicKey).modulus != (output.address as RSAPublicKey).modulus)
            return false
        return true
    }

    override fun hashCode(): Int {
        var hash = 1
        hash = hash * 17 + value.toInt() * 10000
        hash = hash * 31 + (address as RSAPublicKey).publicExponent.hashCode()
        hash = hash * 31 + (address as RSAPublicKey).modulus.hashCode()
        return hash
    }
}

class Input(prevHash: ByteArray,
            val outputIndex: Int) {
    val prevTxHash: ByteArray = prevHash.copyOf()
    var signature: ByteArray? = null
        set(value) {
            field = value?.copyOf()
        }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }

        val input = other as Input

        if (prevTxHash.size != input.prevTxHash.size)
            return false
        prevTxHash.indices
                .filter { prevTxHash[it] != input.prevTxHash[it] }
                .forEach { return false }
        if (outputIndex != input.outputIndex)
            return false
        if (signature!!.size != input.signature!!.size)
            return false
        return signature!!.indices.none { signature!![it] != input.signature!![it] }
    }

    override fun hashCode(): Int {
        var hash = 1
        hash = hash * 17 + Arrays.hashCode(prevTxHash)
        hash = hash * 31 + outputIndex
        hash = hash * 31 + Arrays.hashCode(signature)
        return hash
    }
}

class Transaction {
    var hash: ByteArray? = null
    val inputs: ArrayList<Input>
    val outputs: ArrayList<Output>
    val isCoinbase: Boolean

    constructor() {
        hash = null
        inputs = ArrayList<Input>()
        outputs = ArrayList<Output>()
        isCoinbase = false
    }

    constructor(tx: Transaction) {
        hash = tx.hash!!.clone()
        inputs = ArrayList(tx.inputs)
        outputs = ArrayList(tx.outputs)
        isCoinbase = false
    }

    constructor(coin: Double, address: PublicKey) {
        isCoinbase = true
        inputs = ArrayList<Input>()
        outputs = ArrayList<Output>()
        addOutput(coin, address)
        finalize()
    }

    fun addInput(prevTxHash: ByteArray, outputIndex: Int) {
        val input = Input(prevTxHash, outputIndex)
        inputs.add(input)
    }

    fun addOutput(value: Double, address: PublicKey) {
        val output = Output(value, address)
        outputs.add(output)
    }

    fun removeInput(index: Int) {
        inputs.removeAt(index)
    }

    fun removeInput(ut: UTXO) {
        for (i in inputs.indices) {
            val input = inputs[i]
            val u = UTXO(input.prevTxHash, input.outputIndex)
            if (u == ut) {
                inputs.removeAt(i)
                return
            }
        }
    }

    fun getRawDataToSign(index: Int): ByteArray? {
        // ith input and all outputs
        val sigData = ArrayList<Byte>()
        if (index > inputs.size)
            return null
        val input = inputs[index]
        val prevTxHash = input.prevTxHash
        val b = ByteBuffer.allocate(Integer.SIZE / 8)
        b.putInt(input.outputIndex)
        val outputIndex = b.array()
        prevTxHash.indices.mapTo(sigData) { prevTxHash[it] }
        outputIndex.indices.mapTo(sigData) { outputIndex[it] }
        for (output in outputs) {
            val bo = ByteBuffer.allocate(java.lang.Double.SIZE / 8)
            bo.putDouble(output.value)
            val value = bo.array()
            val addressExponent = (output.address as RSAPublicKey).publicExponent.toByteArray()
            val addressModulus = (output.address as RSAPublicKey).modulus.toByteArray()
            value.indices.mapTo(sigData) { value[it] }
            addressExponent.indices.mapTo(sigData) { addressExponent[it] }
            addressModulus.indices.mapTo(sigData) { addressModulus[it] }
        }
        return sigData.toByteArray()
    }

    fun addSignature(signature: ByteArray, index: Int) {
        inputs[index].signature = signature
    }

    val rawTx: ByteArray
        get() {
            val rawTx = ArrayList<Byte>()
            for (input in inputs) {
                val prevTxHash = input.prevTxHash
                val b = ByteBuffer.allocate(Integer.SIZE / 8)
                b.putInt(input.outputIndex)
                val outputIndex = b.array()
                val signature = input.signature
                prevTxHash.indices.mapTo(rawTx) { prevTxHash[it] }
                outputIndex.indices.mapTo(rawTx) { outputIndex[it] }
                if (signature != null)
                    signature.indices.mapTo(rawTx) { signature[it] }
            }
            for (output in outputs) {
                val b = ByteBuffer.allocate(java.lang.Double.SIZE / 8)
                b.putDouble(output.value)
                val value = b.array()
                val addressExponent = (output.address as RSAPublicKey).publicExponent.toByteArray()
                val addressModulus = (output.address as RSAPublicKey).modulus.toByteArray()
                value.indices.mapTo(rawTx) { value[it] }
                addressExponent.indices.mapTo(rawTx) { addressExponent[it] }
                addressModulus.indices.mapTo(rawTx) { addressModulus[it] }
            }
            return rawTx.toByteArray()
        }

    fun finalize() {
        try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(rawTx)
            hash = md.digest()
        } catch (x: NoSuchAlgorithmException) {
            x.printStackTrace(System.err)
        }

    }

    fun getInput(index: Int): Input? {
        if (index < inputs.size) {
            return inputs[index]
        }
        return null
    }

    fun getOutput(index: Int): Output? {
        if (index < outputs.size) {
            return outputs[index]
        }
        return null
    }

    fun numInputs(): Int {
        return inputs.size
    }

    fun numOutputs(): Int {
        return outputs.size
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }

        val tx = other as Transaction?
        // inputs and outputs should be same
        if (tx!!.numInputs() != numInputs())
            return false

        (0..numInputs() - 1)
                .filter { getInput(it) != tx.getInput(it) }
                .forEach { return false }

        if (tx.numOutputs() != numOutputs())
            return false

        return (0..numOutputs() - 1).none { getOutput(it) != tx.getOutput(it) }
    }

    override fun hashCode(): Int {
        var hash = 1
        for (i in 0..numInputs() - 1) {
            hash = hash * 31 + getInput(i)!!.hashCode()
        }
        for (i in 0..numOutputs() - 1) {
            hash = hash * 31 + getOutput(i)!!.hashCode()
        }
        return hash
    }
}


class TransactionPool {

    private val pool: HashMap<ByteArrayWrapper, Transaction>

    constructor() {
        pool = HashMap<ByteArrayWrapper, Transaction>()
    }

    constructor(txPool: TransactionPool) {
        pool = HashMap<ByteArrayWrapper, Transaction>(txPool.pool)
    }

    fun addTransaction(tx: Transaction) {
        val hash = ByteArrayWrapper(tx.hash!!)
        pool.put(hash, tx)
    }

    fun removeTransaction(txHash: ByteArray) {
        val hash = ByteArrayWrapper(txHash)
        pool.remove(hash)
    }

    fun getTransaction(txHash: ByteArray): Transaction? {
        val hash = ByteArrayWrapper(txHash)
        return pool[hash]
    }

    val transactions: List<Transaction>
        get() {
            return pool.values.toList()
        }
}