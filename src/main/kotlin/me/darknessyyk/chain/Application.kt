package me.darknessyyk.chain

import me.darknessyyk.chain.model.Block
import me.darknessyyk.chain.model.BlockChain
import me.darknessyyk.chain.model.Transaction
import me.darknessyyk.chain.util.CryptoUtil


open class Application {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val keyPairAlice = CryptoUtil.generate() ?: throw Exception("Key pair error")
            val keyPairBob = CryptoUtil.generate() ?: throw Exception("Key pair error")

            // Alice mined a genesis block and no transaction
            val genesisBlock = Block(null, keyPairAlice.public)
            genesisBlock.finalize()

            // Block chain created
            val blockChain = BlockChain(genesisBlock)

            // Bob mined 0th block and this can contains transactions
            val block0 = Block(genesisBlock.hash, keyPairBob.public)

            // Alice transfer all 25.0 coin to Bob
            val txBlock0Tx1 = Transaction()
            txBlock0Tx1.addInput(genesisBlock.coinbase.hash!!, 0)
            txBlock0Tx1.addOutput(25.0, keyPairBob.public)
            val sig = CryptoUtil.sign(txBlock0Tx1.getRawDataToSign(0)!!, keyPairAlice.private)!!
            txBlock0Tx1.getInput(0)!!.signature = sig
            block0.addTransaction(txBlock0Tx1)

            // Add 0th block to the chain
            block0.finalize()
            val result = blockChain.addBlock(block0)

            // Should be true... Tada!!
            println(result)

        }
    }
}