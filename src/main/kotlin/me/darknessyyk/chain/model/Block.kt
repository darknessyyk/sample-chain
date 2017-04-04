package me.darknessyyk.chain.model

import me.darknessyyk.chain.util.ByteArrayWrapper
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.util.*

class Block(val prevBlockHash: ByteArray?, address: PublicKey) {

    var hash: ByteArray? = null
        private set
    val coinbase: Transaction = Transaction(COINBASE, address)
    val transactions: MutableList<Transaction> = mutableListOf()

    fun getTransaction(index: Int): Transaction {
        return transactions[index]
    }

    fun addTransaction(tx: Transaction) {
        transactions.add(tx)
    }

    val rawBlock: ByteArray
        get() {
            val rawBlock = ArrayList<Byte>()
            if (prevBlockHash != null)
                prevBlockHash.indices.mapTo(rawBlock) { prevBlockHash[it] }
            for (i in transactions.indices) {
                val rawTx = transactions[i].rawTx
                transactions[i].rawTx.indices.mapTo(rawBlock) { rawTx[it] }
            }
            val raw = ByteArray(rawBlock.size)
            for (i in raw.indices)
                raw[i] = rawBlock[i]
            return raw
        }

    fun finalize() {
        try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(rawBlock)
            hash = md.digest()
        } catch (x: NoSuchAlgorithmException) {
            x.printStackTrace(System.err)
        }

    }

    companion object {
        val COINBASE = 25.0
    }
}

class BlockChain(genesisBlock: Block) {

    private inner class BlockNode(val block: Block,
                                  val parent: BlockNode?,
                                  val utxoPool: UTXOPool) {
        val children: MutableList<BlockNode> = mutableListOf()
        val height: Int

        init {
            if (parent != null) {
                height = parent.height + 1
                parent.children.add(this)
            } else {
                height = 1
            }
        }

    }

    private val blockNodes: MutableMap<ByteArrayWrapper, BlockNode>
    private var maxHeightNode: BlockNode?
    val transactionPool: TransactionPool


    init {
        blockNodes = HashMap()
        val genesisBlockNode = BlockNode(genesisBlock, null, UTXOPool())
        addCoinBaseTransactionToBlockNode(genesisBlock.coinbase, genesisBlockNode)
        val wrapper = ByteArrayWrapper(genesisBlockNode.block.hash!!)
        blockNodes.put(wrapper, genesisBlockNode)
        transactionPool = TransactionPool()
        maxHeightNode = genesisBlockNode
    }

    val maxHeightBlock: Block
        get() = maxHeightNode!!.block

    val maxHeightUTXOPool: UTXOPool
        get() = maxHeightNode!!.utxoPool

    fun addBlock(block: Block): Boolean {
        val prevHash = block.prevBlockHash ?: return false
        val parentNode = blockNodes[ByteArrayWrapper(prevHash)] ?: return false
        // handle transactions
        val txHandler = TxHandler(parentNode.utxoPool)
        val txsList = block.transactions
        val txs = txsList.toList()
        for (i in txs.indices) {
            txs[i].finalize()
        }
        val validTxs = txHandler.handleTxs(txs)
        if (txs.size != validTxs.size)
            return false

        // prepare a new block node
        val newBlockNode = BlockNode(block, parentNode, txHandler.utxoPool)

        // validate it
        if (newBlockNode.height <= maxHeightNode!!.height - CUT_OFF_AGE)
            return false

        // all validation done, start to add new block
        // transactions will be applied. remove from waiting pool
        for (tx in validTxs) {
            transactionPool.removeTransaction(tx.hash!!)
        }

        // add coinbase output to utxopool of new block
        val coinBaseTx = block.coinbase
        addCoinBaseTransactionToBlockNode(coinBaseTx, newBlockNode)

        // switch main chain if new block has the highest depth
        if (newBlockNode.height > this.maxHeightNode!!.height) {
            maxHeightNode = newBlockNode
        }

        // add new block
        this.blockNodes.put(ByteArrayWrapper(newBlockNode.block.hash!!), newBlockNode)

        return true
    }

    fun addTransaction(tx: Transaction) {
        transactionPool.addTransaction(tx)
    }

    private fun addCoinBaseTransactionToBlockNode(tx: Transaction, blockNode: BlockNode) {
        for (i in 0..tx.numOutputs() - 1) {
            val utxo = UTXO(tx.hash!!, i)
            blockNode.utxoPool.addUTXO(utxo, tx.getOutput(i)!!)
        }
    }

    companion object {

        val CUT_OFF_AGE = 10
    }
}
