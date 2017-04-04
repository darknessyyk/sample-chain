package me.darknessyyk.chain.model

import me.darknessyyk.chain.util.CryptoUtil
import java.util.*

class TxHandler(utxoPool: UTXOPool) {
    val utxoPool: UTXOPool = UTXOPool(utxoPool)

    /**
     * @return true if:
     * * (1) all outputs claimed by `tx` are in the current UTXO pool,
     * * (2) the signatures on each input of `tx` are valid,
     * * (3) no UTXO is claimed multiple times by `tx`,
     * * (4) all of `tx`s output values are non-negative, and
     * * (5) the sum of `tx`s input values is greater than or equal to the sum of its output
     * * values; and false otherwise.
     */
    fun isValidTx(tx: Transaction): Boolean {
        val sumValueOutputs = tx.outputs.stream().mapToDouble(Output::value).sum()
        var sumValueInputs = 0.0

        val checkedUtxoSet = HashSet<UTXO>()
        for (i in 0..tx.numInputs() - 1) {
            val input = tx.getInput(i)!!
            val claimedUtxo = UTXO(input.prevTxHash, input.outputIndex)

            // (1)
            val claimedOutput = utxoPool.getTxOutput(claimedUtxo) ?: return false

            // (2)
            if (!CryptoUtil.verify(claimedOutput.address, tx.getRawDataToSign(i)!!, input.signature!!))
                return false

            // (3)
            if (checkedUtxoSet.contains(claimedUtxo))
                return false

            sumValueInputs += claimedOutput.value
            checkedUtxoSet.add(claimedUtxo)
        }

        // (4)
        if (tx.outputs.filter { it.value < 0 }.count() > 0)
            return false

        // (5)
        if (sumValueInputs < sumValueOutputs)
            return false

        return true

    }

    fun handleTxs(possibleTxs: List<Transaction>): List<Transaction> {
        val validTxs = mutableListOf<Transaction>()
        for (possibleTx in possibleTxs) {
            if (isValidTx(possibleTx)) {
                validTxs.add(possibleTx)
                val inputs = possibleTx.inputs
                for (k in inputs.indices) {
                    utxoPool.removeUTXO(UTXO(inputs[k].prevTxHash, inputs[k].outputIndex))
                }
                val outputs = possibleTx.outputs
                for (k in outputs.indices) {
                    utxoPool.addUTXO(UTXO(possibleTx.hash!!, k), outputs[k])
                }
            }
        }
        return validTxs
    }
}


