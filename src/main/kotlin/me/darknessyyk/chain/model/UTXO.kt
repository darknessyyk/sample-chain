package me.darknessyyk.chain.model

import java.util.*

// Unspent transaction output
class UTXO(
        txHash: ByteArray,
        val index: Int) : Comparable<UTXO> {
    val txHash: ByteArray = txHash.copyOf()

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (this.javaClass != other.javaClass) {
            return false
        }

        val utxo = other as UTXO
        val hash = utxo.txHash
        val index = utxo.index
        if (hash.size != txHash.size || this.index != index)
            return false
        return hash.indices.all { hash[it] == txHash[it] }
    }

//   Simple implementation of a UTXO hashCode that respects equality of UTXOs
//   utxo1.equals(utxo2) => utxo1.hashCode() == utxo2.hashCode())
    override fun hashCode(): Int {
        var hash = 1
        hash = hash * 17 + index
        hash = hash * 31 + Arrays.hashCode(txHash)
        return hash
    }

    override fun compareTo(other: UTXO): Int {
        val hash = other.txHash
        val index = other.index
        if (index > this.index)
            return -1
        else if (index < this.index)
            return 1
        else {
            val len1 = txHash.size
            val len2 = hash.size
            if (len2 > len1)
                return -1
            else if (len2 < len1)
                return 1
            else {
                for (i in 0..len1 - 1) {
                    if (hash[i] > txHash[i])
                        return -1
                    else if (hash[i] < txHash[i])
                        return 1
                }
                return 0
            }
        }
    }
}

class UTXOPool {
    private val pool: HashMap<UTXO, Output>

    constructor() {
        pool = HashMap<UTXO, Output>()
    }

    constructor(utxoPool: UTXOPool) {
        pool = HashMap<UTXO, Output>(utxoPool.pool)
    }

    fun addUTXO(utxo: UTXO, txOut: Output) {
        pool.put(utxo, txOut)
    }

    fun removeUTXO(utxo: UTXO) {
        pool.remove(utxo)
    }

    fun getTxOutput(ut: UTXO): Output? {
        return pool[ut]
    }

    operator fun contains(utxo: UTXO): Boolean {
        return pool.containsKey(utxo)
    }

    val allUTXO: List<UTXO>
        get() = pool.keys.toList()
}
